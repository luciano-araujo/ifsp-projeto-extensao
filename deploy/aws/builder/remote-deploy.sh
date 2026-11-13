#!/usr/bin/env bash
# Roda NO BUILDER (EC2). Enviado por deploy/aws/redeploy.sh junto com src.tar.gz, backend.env e (opcional) summary.env.
# Constroi as 3 imagens e envia ao ECR (os manifestos sao aplicados localmente pelo redeploy.sh).
set -euo pipefail
cd "$HOME/deploy-input"
: "${ECR_REGISTRY:?}" "${FRONTEND_REPO:?}" "${FRONTEND_BRANCH:?}"
NS=creator-workbench
IMAGE_TAG="b$(date +%Y%m%d%H%M%S)"
export ECR_REGISTRY IMAGE_TAG
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

echo "$IMAGE_TAG" > "$HOME/image_tag"
log "IMAGENS PRONTAS: $IMAGE_TAG"
