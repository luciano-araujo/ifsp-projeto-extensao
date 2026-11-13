#!/usr/bin/env bash
# Roda NO BUILDER (EC2). Enviado por deploy/aws/redeploy.sh junto com src.tar.gz, backend.env e (opcional) summary.env.
# Constroi as 3 imagens, envia ao ECR, cria os segredos e aplica os manifestos.
set -euo pipefail
cd "$HOME/deploy-input"
: "${ECR_REGISTRY:?}" "${S3_BUCKET:?}" "${CLUSTER:?}" "${FRONTEND_REPO:?}" "${FRONTEND_BRANCH:?}"
NS=creator-workbench
IMAGE_TAG="b$(date +%Y%m%d%H%M%S)"
export ECR_REGISTRY S3_BUCKET IMAGE_TAG
log() { echo "[builder $(date +%H:%M:%S)] $*"; }

while [ ! -f /var/tmp/builder-ready ]; do log "aguardando user-data do builder"; sleep 10; done
command -v envsubst >/dev/null || sudo dnf install -y gettext >/dev/null

aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin "$ECR_REGISTRY" >/dev/null

# --- fontes ---------------------------------------------------------------------------------------------------
rm -rf "$HOME/build" && mkdir -p "$HOME/build/backend"
tar xzf src.tar.gz -C "$HOME/build/backend"
log "clonando frontend $FRONTEND_REPO ($FRONTEND_BRANCH)"
git clone --quiet --depth 1 --branch "$FRONTEND_BRANCH" "$FRONTEND_REPO" "$HOME/build/frontend"
cp "$HOME/build/backend/deploy/frontend/nginx.conf" "$HOME/build/frontend/nginx.conf"
cp "$HOME/build/backend/deploy/frontend/dockerignore" "$HOME/build/frontend/.dockerignore"
rm -f "$HOME/build/frontend/.env.local" "$HOME/build/frontend/.env.production"

# --- imagens --------------------------------------------------------------------------------------------------
build_push() { # nome contexto [dockerfile]
  local name=$1 ctx=$2 df=${3:-}
  log "build $name"
  if [ -n "$df" ]; then docker build -q -f "$df" -t "$ECR_REGISTRY/creator-workbench/$name:$IMAGE_TAG" "$ctx"
  else docker build -q -t "$ECR_REGISTRY/creator-workbench/$name:$IMAGE_TAG" "$ctx"; fi
  docker push -q "$ECR_REGISTRY/creator-workbench/$name:$IMAGE_TAG" >/dev/null
}
build_push backend "$HOME/build/backend" &
P1=$!
build_push summary "$HOME/build/backend/automation" &
P2=$!
build_push frontend "$HOME/build/frontend" "$HOME/build/backend/deploy/frontend/Dockerfile" &
P3=$!
FAIL=0
for p in $P1 $P2 $P3; do wait "$p" || FAIL=1; done
[ "$FAIL" = 0 ] || { log "falha em algum build"; exit 1; }

[ "${BUILD_ONLY:-}" = 1 ] && { log "BUILD_ONLY: imagens $IMAGE_TAG prontas"; exit 0; }
# --- cluster --------------------------------------------------------------------------------------------------
aws eks update-kubeconfig --name "$CLUSTER" --region us-east-1 >/dev/null
K="$HOME/build/backend/deploy/k8s"
kubectl apply -f "$K/00-namespace.yaml"
kubectl apply -f "$K/10-service-web.yaml"
log "aguardando DNS do ELB"
ELB_HOST=""
for i in $(seq 1 60); do
  ELB_HOST="$(kubectl -n $NS get svc web -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || true)"
  [ -n "$ELB_HOST" ] && break
  sleep 5
done
[ -n "$ELB_HOST" ] || { log "o ELB nao foi criado"; kubectl -n $NS describe svc web | tail -20; exit 1; }
export ELB_HOST
log "ELB: http://$ELB_HOST"

kubectl -n $NS create secret generic backend-env --from-env-file=backend.env --dry-run=client -o yaml | kubectl apply -f -
if [ -s summary.env ]; then
  kubectl -n $NS create secret generic summary-env --from-env-file=summary.env --dry-run=client -o yaml | kubectl apply -f -
  log "ANTHROPIC_API_KEY presente: resumo usa o Claude (com fallback extrativo)"
else
  kubectl -n $NS delete secret summary-env --ignore-not-found
  log "sem ANTHROPIC_API_KEY: resumo usa o motor extrativo"
fi
for f in 20-configmap 30-summary 40-web 50-fluent-bit; do
  envsubst '${ELB_HOST} ${S3_BUCKET} ${ECR_REGISTRY} ${IMAGE_TAG}' < "$K/$f.yaml" | kubectl apply -f -
done
kubectl -n $NS rollout restart deployment/summary deployment/web >/dev/null 2>&1 || true
kubectl -n $NS rollout status deployment/summary --timeout=300s
kubectl -n $NS rollout status deployment/web --timeout=600s
kubectl -n $NS rollout status daemonset/fluent-bit --timeout=180s || true
kubectl -n $NS get pods -o wide
echo "$ELB_HOST" > "$HOME/elb_host"
log "PRONTO: http://$ELB_HOST (imagens $IMAGE_TAG)"
