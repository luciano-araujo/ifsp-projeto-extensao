#!/usr/bin/env bash
# Reconstroi as imagens a partir do release/v3 (backend local + frontend do GitHub) no builder, envia ao ECR
# e atualiza o cluster (kubectl LOCAL: o criador do cluster, role voclabs, e o unico administrador; LabRole nao pode ser usado no builder).
# Uso: bash deploy/aws/redeploy.sh   (BACKEND_REF=<ref>; ANTHROPIC_ENV_FILE=<arquivo>; SKIP_BUILD=1 so reaplica os manifestos)
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
ensure_secrets
g() { (cd "$REPO_ROOT" && git "$@"); }
ensure_kubectl
NSK=(kubectl -n "$NS")

IMAGE_TAG=""
if [ "${SKIP_BUILD:-}" != 1 ]; then
  BID="$(builder_id)"
  [ -n "$BID" ] || { echo "Builder nao existe: rode deploy/aws/up.sh" >&2; exit 1; }
  aws ec2 start-instances --instance-ids "$BID" >/dev/null 2>&1 || true
  SG_B="$(ensure_builder_sg)"; aws ec2 modify-instance-attribute --instance-id "$BID" --groups "$SG_B"
  aws ec2 wait instance-running --instance-ids "$BID"
  IP="$(builder_ip)"
  log "Builder $BID em $IP"
  for i in $(seq 1 40); do ssh_b true 2>/dev/null && break; sleep 5; done

  REF="$BACKEND_REF"
  g cat-file -e "$REF:Dockerfile" 2>/dev/null || { log "A ref $REF ainda nao tem Dockerfile: usando HEAD"; REF=HEAD; }
  log "Backend: $(g rev-parse --short "$REF") ($REF)"
  W="$(mktemp -d)"; mkdir -p "$W/src" "$W/input"
  g archive "$REF" | tar -x -C "$W/src"
  for f in Dockerfile .dockerignore automation/Dockerfile automation/.dockerignore; do
    [ -f "$W/src/$f" ] || cp "$REPO_ROOT/$f" "$W/src/$f"
  done
  rm -rf "$W/src/deploy"; cp -r "$REPO_ROOT/deploy" "$W/src/deploy"; rm -rf "$W/src/deploy/aws/.state"
  tar czf "$W/input/src.tar.gz" -C "$W/src" .
  cp "$REPO_ROOT/deploy/aws/builder/remote-deploy.sh" "$W/input/"
  ssh_b "rm -rf ~/deploy-input && mkdir -p ~/deploy-input"
  scp_b "$W"/input/* "ec2-user@$IP:deploy-input/"
  rm -rf "$W"
  ssh_b "cd ~/deploy-input && sed -i 's/\r\$//' remote-deploy.sh && \
    ECR_REGISTRY='$ECR_REGISTRY' FRONTEND_REPO='$FRONTEND_REPO' FRONTEND_BRANCH='$FRONTEND_BRANCH' bash remote-deploy.sh"
fi
IMAGE_TAG="$(ssh_b 'cat ~/image_tag')"
export ECR_REGISTRY IMAGE_TAG S3_BUCKET="$BUCKET"

# --- cluster ---------------------------------------------------------------------------------------------------
aws eks update-kubeconfig --name "$CLUSTER" --region "$REGION" >/dev/null
K="$(cygpath -m "$REPO_ROOT")/deploy/k8s"
render() { sed -e "s|\${ELB_HOST}|$ELB_HOST|g" -e "s|\${S3_BUCKET}|$S3_BUCKET|g" -e "s|\${ECR_REGISTRY}|$ECR_REGISTRY|g" -e "s|\${IMAGE_TAG}|$IMAGE_TAG|g" "$1"; }
kubectl apply -f "$K/00-namespace.yaml"
SVC="$K/10-service-web.yaml"
if ! "${NSK[@]}" get svc web >/dev/null 2>&1; then   # 1a vez: sem HTTPS, so para obter o DNS do ELB
  grep -v '#SSL' "$SVC" | kubectl apply -f -
fi
log "Aguardando DNS do ELB"
ELB_HOST=""
for i in $(seq 1 60); do
  ELB_HOST="$("${NSK[@]}" get svc web -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || true)"
  [ -n "$ELB_HOST" ] && break
  sleep 5
done
[ -n "$ELB_HOST" ] || { log "O ELB nao foi criado"; "${NSK[@]}" describe svc web | tail -20; exit 1; }
echo "$ELB_HOST" > "$STATE_DIR/elb_host"
SSL_CERT_ARN="$(ensure_cert "$ELB_HOST")"
sed -e "s|\${SSL_CERT_ARN}|$SSL_CERT_ARN|g" -e 's| #SSL$||' "$SVC" | kubectl apply -f - >/dev/null   # HTTP 80 + HTTPS 443
log "ELB: http://$ELB_HOST  https://$ELB_HOST (certificado autoassinado, $SSL_CERT_ARN)"

# segredos: arquivos temporarios, nada e impresso
T="$(mktemp -d)"; TW="$(cygpath -m "$T")"; umask 077
trap 'rm -rf "$T"' EXIT
{
  echo "DB_URL=jdbc:mysql://$(rds_endpoint):3306/db_tcc?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true"
  echo "DB_USER=admin"
  echo "DB_PASSWORD=$(secret DB_PASSWORD)"
  echo "JWT_SECRET=$(secret JWT_SECRET)"
  echo "MAIL_USERNAME=$(secret MAIL_USERNAME)"
  echo "MAIL_PASSWORD=$(secret MAIL_PASSWORD)"
  echo "APP_ADMIN_EMAILS=admin.sistema@creatorworkbench.test"
} > "$T/backend.env"
: > "$T/summary.env"
AENV="$(find_anthropic_env || true)"
if [ -n "$AENV" ]; then
  KEYV="$(grep -m1 '^ANTHROPIC_API_KEY=' "$AENV" | cut -d= -f2- | tr -d '\r"' || true)"
  [ -n "$KEYV" ] && echo "ANTHROPIC_API_KEY=$KEYV" > "$T/summary.env"
  unset KEYV
fi
"${NSK[@]}" create secret generic backend-env --from-env-file="$TW/backend.env" --dry-run=client -o yaml | kubectl apply -f - >/dev/null
if [ -s "$T/summary.env" ]; then
  "${NSK[@]}" create secret generic summary-env --from-env-file="$TW/summary.env" --dry-run=client -o yaml | kubectl apply -f - >/dev/null
  log "ANTHROPIC_API_KEY presente: resumo usa o Claude (com fallback extrativo)"
else
  "${NSK[@]}" delete secret summary-env --ignore-not-found >/dev/null
  log "Sem ANTHROPIC_API_KEY: resumo usa o motor extrativo"
fi

for f in 20-configmap 30-summary 40-web 50-fluent-bit; do render "$K/$f.yaml" | kubectl apply -f - >/dev/null; done
"${NSK[@]}" rollout restart deployment/summary >/dev/null   # le o segredo de novo (a chave do Claude pode ter surgido)
"${NSK[@]}" rollout status deployment/summary --timeout=300s
"${NSK[@]}" rollout status deployment/web --timeout=600s
"${NSK[@]}" rollout status daemonset/fluent-bit --timeout=180s || true
"${NSK[@]}" get pods -o wide
apply_plans
set_demo_passwords
log "PRONTO: http://$ELB_HOST (imagens $IMAGE_TAG)"
