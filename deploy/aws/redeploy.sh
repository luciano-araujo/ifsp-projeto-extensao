#!/usr/bin/env bash
# Reconstroi as imagens a partir do release/v3 (backend local + frontend do GitHub) e atualiza o cluster.
# Uso: bash deploy/aws/redeploy.sh        (BACKEND_REF=<ref> para outra ref; ANTHROPIC_ENV_FILE=<arquivo> para a chave do Claude)
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
ensure_secrets

# builder ligado?
BID="$(builder_id)"
[ -n "$BID" ] || { echo "Builder nao existe: rode deploy/aws/up.sh" >&2; exit 1; }
aws ec2 start-instances --instance-ids "$BID" >/dev/null 2>&1 || true
aws ec2 wait instance-running --instance-ids "$BID"
IP="$(builder_ip)"
log "Builder $BID em $IP"
for i in $(seq 1 40); do ssh_b true 2>/dev/null && break; sleep 5; done

# fontes do backend: ref escolhida + arquivos de deploy da arvore atual
REF="$BACKEND_REF"
git -C "$REPO_ROOT" cat-file -e "$REF:Dockerfile" 2>/dev/null || { log "A ref $REF ainda nao tem Dockerfile: usando HEAD"; REF=HEAD; }
log "Backend: $(git -C "$REPO_ROOT" rev-parse --short "$REF") ($REF)"
W="$STATE_DIR/work"; rm -rf "$W"; mkdir -p "$W/src" "$W/input"
git -C "$REPO_ROOT" archive "$REF" | tar -x -C "$W/src"
# se a ref escolhida e anterior aos arquivos de deploy, completa com os da arvore atual
for f in Dockerfile .dockerignore automation/Dockerfile automation/.dockerignore; do
  [ -f "$W/src/$f" ] || cp "$REPO_ROOT/$f" "$W/src/$f"
done
rm -rf "$W/src/deploy"; cp -r "$REPO_ROOT/deploy" "$W/src/deploy"; rm -rf "$W/src/deploy/aws/.state"
tar czf "$W/input/src.tar.gz" -C "$W/src" .

# segredos (arquivos temporarios locais, removidos ao final; nada e impresso)
umask 077
{
  echo "DB_URL=jdbc:mysql://$(rds_endpoint):3306/db_tcc?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true"
  echo "DB_USER=admin"
  echo "DB_PASSWORD=$(secret DB_PASSWORD)"
  echo "JWT_SECRET=$(secret JWT_SECRET)"
  echo "MAIL_USERNAME=$(secret MAIL_USERNAME)"
  echo "MAIL_PASSWORD=$(secret MAIL_PASSWORD)"
} > "$W/input/backend.env"
: > "$W/input/summary.env"
AENV="$(find_anthropic_env || true)"
if [ -n "$AENV" ]; then
  K="$(grep -m1 '^ANTHROPIC_API_KEY=' "$AENV" | cut -d= -f2- | tr -d '\r"' || true)"
  [ -n "$K" ] && echo "ANTHROPIC_API_KEY=$K" > "$W/input/summary.env"
  unset K
fi
[ -s "$W/input/summary.env" ] && log "Chave do Claude encontrada (nao sera impressa)" || log "Sem chave do Claude: motor extrativo"

ssh_b "rm -rf ~/deploy-input && mkdir -p ~/deploy-input"
scp_b "$W"/input/* "$REPO_ROOT/deploy/aws/builder/remote-deploy.sh" "ec2-user@$IP:deploy-input/"
rm -rf "$W/input"
ssh_b "chmod 700 ~/deploy-input && cd ~/deploy-input && sed -i 's/\r\$//' remote-deploy.sh && \
  ECR_REGISTRY='$ECR_REGISTRY' S3_BUCKET='$BUCKET' CLUSTER='$CLUSTER' FRONTEND_REPO='$FRONTEND_REPO' FRONTEND_BRANCH='$FRONTEND_BRANCH' BUILD_ONLY='${BUILD_ONLY:-}' \
  bash remote-deploy.sh; rc=\$?; rm -f ~/deploy-input/backend.env ~/deploy-input/summary.env; exit \$rc"
rm -rf "$W"
[ "${BUILD_ONLY:-}" = 1 ] || log "ELB: http://$(ssh_b 'cat ~/elb_host')"
