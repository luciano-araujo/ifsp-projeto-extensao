#!/usr/bin/env bash
# Funcoes e variaveis comuns. Rode os scripts no Git Bash (Windows) com o AWS CLI v2 configurado (~/.aws/credentials).
set -euo pipefail
export MSYS_NO_PATHCONV=1 AWS_PAGER="" AWS_DEFAULT_REGION=us-east-1

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$HERE/../.." && pwd)"
STATE_DIR="$HERE/.state"
mkdir -p "$STATE_DIR"

REGION=us-east-1
ACCOUNT="$(aws sts get-caller-identity --query Account --output text)"
PROJECT=creator-workbench
CLUSTER=creator-workbench
NS=creator-workbench
NODEGROUP=workers
LAUNCH_TEMPLATE=creator-workbench-nodes
ECR_REGISTRY="$ACCOUNT.dkr.ecr.$REGION.amazonaws.com"
BUCKET="creator-workbench-materials-$ACCOUNT"
LOG_GROUP=/creator-workbench/containers
RDS_ID=creator-workbench-db
DB_SUBNET_GROUP=creator-workbench-db-subnet
# IDs fixos abaixo pertencem a ESTA conta/Learner Lab (VPC padrao 172.31.0.0/16); em outra conta, ajuste-os.
SG_APP=sg-08da4c9f8f5c0d45c     # legado (22 aberto ao mundo): NAO e usado pelo builder
SG_BUILDER_NAME=SG-CreatorWorkbench-Builder
SG_RDS=sg-0bcb25f9250e29521     # SG-CreatorWorkbench-RDS (3306 da VPC)
VPC_CIDR=172.31.0.0/16
EKS_VERSION=1.35
# subnets publicas em 3 AZs que o EKS aceita (us-east-1e nao aceita plano de controle)
EKS_SUBNETS="subnet-098b8a4940dcb9387,subnet-062df4ab57fc7706a,subnet-0082382002bca1861"
LAB_ROLE_ARN="arn:aws:iam::$ACCOUNT:role/LabRole"
KEY_NAME=vockey
KEY_FILE="$HOME/.ssh/labsuser.pem"
BUILDER_NAME=creator-workbench-builder
BACKEND_REF="${BACKEND_REF:-release/v3}"
FRONTEND_REPO="${FRONTEND_REPO:-https://github.com/HenriqueOrsoni/Creator-Workbench.git}"
FRONTEND_BRANCH="${FRONTEND_BRANCH:-release/v3}"
# arquivo com a chave da API do Claude (opcional). Nunca e impresso.
ANTHROPIC_ENV_FILE="${ANTHROPIC_ENV_FILE:-}"

log() { echo "[$(date +%H:%M:%S)] $*"; }

secret() { # secret NOME -> valor de .state/secrets.env
  grep -m1 "^$1=" "$STATE_DIR/secrets.env" | cut -d= -f2-
}

ensure_secrets() {
  if [ ! -f "$STATE_DIR/secrets.env" ]; then
    echo "Falta $STATE_DIR/secrets.env (DB_PASSWORD, JWT_SECRET, MAIL_USERNAME, MAIL_PASSWORD). Veja deploy/README.md." >&2
    exit 1
  fi
}

find_anthropic_env() { # imprime o caminho do automation/.env se existir
  local c
  for c in "$ANTHROPIC_ENV_FILE" "$REPO_ROOT/automation/.env" \
           "$(cd "$REPO_ROOT" && dirname "$(git rev-parse --path-format=absolute --git-common-dir 2>/dev/null)")/automation/.env"; do
    [ -n "$c" ] && [ -f "$c" ] && { echo "$c"; return; }
  done
}

builder_id() {
  aws ec2 describe-instances --filters "Name=tag:Name,Values=$BUILDER_NAME" "Name=tag:Project,Values=$PROJECT" \
    "Name=instance-state-name,Values=pending,running,stopping,stopped" --query 'Reservations[].Instances[].InstanceId' --output text
}

builder_ip() {
  aws ec2 describe-instances --filters "Name=tag:Name,Values=$BUILDER_NAME" "Name=instance-state-name,Values=running" \
    --query 'Reservations[].Instances[].PublicIpAddress' --output text
}

SSH_OPTS=(-i "$KEY_FILE" -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o LogLevel=ERROR -o ServerAliveInterval=30)
ssh_b() { ssh "${SSH_OPTS[@]}" "ec2-user@$(builder_ip)" "$@"; }
scp_b() { scp "${SSH_OPTS[@]}" "$@"; }   # uso: scp_b origem... ec2-user@IP:destino (use $(builder_ip))

cluster_status() { aws eks describe-cluster --name "$CLUSTER" --query cluster.status --output text 2>/dev/null || echo ABSENT; }
rds_status() { aws rds describe-db-instances --db-instance-identifier "$RDS_ID" --query 'DBInstances[0].DBInstanceStatus' --output text 2>/dev/null || echo ABSENT; }
rds_endpoint() { aws rds describe-db-instances --db-instance-identifier "$RDS_ID" --query 'DBInstances[0].Endpoint.Address' --output text; }

# kubectl local (a maquina nao tem): baixa para ~/bin se faltar
export PATH="$PATH:$HOME/bin"
ensure_kubectl() {
  command -v kubectl >/dev/null && return
  mkdir -p "$HOME/bin"
  curl -sL -o "$HOME/bin/kubectl.exe" "https://dl.k8s.io/release/$(curl -sL https://dl.k8s.io/release/stable.txt)/bin/windows/amd64/kubectl.exe"
  chmod +x "$HOME/bin/kubectl.exe"
}

# Certificado autoassinado (RSA 2048, 2 anos; SAN = DNS do ELB; o CN fica curto porque o limite do CN e 64 caracteres e o DNS do ELB tem 70) importado no ACM; reutiliza o existente.
ensure_cert() { # ensure_cert <dns-do-elb> -> imprime o ARN
  local host=$1 arn
  arn="$(cat "$STATE_DIR/cert_arn" 2>/dev/null || true)"
  if [ -n "$arn" ] && aws acm describe-certificate --certificate-arn "$arn" --query Certificate.DomainName --output text 2>/dev/null | grep -qx "$host"; then
    echo "$arn"; return
  fi
  local d="$STATE_DIR/cert"; mkdir -p "$d"
  (cd "$d" && openssl req -x509 -newkey rsa:2048 -nodes -sha256 -days 730 -keyout key.pem -out cert.pem \
    -subj "/CN=creator-workbench/O=Creator Workbench" -addext "subjectAltName=DNS:$host" >/dev/null 2>&1) || { echo "openssl falhou" >&2; return 1; }
  arn="$(aws acm import-certificate --certificate "fileb://$(cygpath -m "$d/cert.pem")" --private-key "fileb://$(cygpath -m "$d/key.pem")" \
    --tags Key=Project,Value=$PROJECT --query CertificateArn --output text)"
  echo "$arn" > "$STATE_DIR/cert_arn"
  echo "$arn"
}

# SG proprio do builder: SSH (22) somente do IP publico atual do operador (/32). Imprime o id do SG.
ensure_builder_sg() {
  local vpc sg myip old
  vpc="$(aws ec2 describe-security-groups --group-ids "$SG_RDS" --query 'SecurityGroups[0].VpcId' --output text)"
  sg="$(aws ec2 describe-security-groups --filters "Name=group-name,Values=$SG_BUILDER_NAME" "Name=vpc-id,Values=$vpc" --query 'SecurityGroups[0].GroupId' --output text 2>/dev/null)"
  if [ -z "$sg" ] || [ "$sg" = "None" ]; then
    sg="$(aws ec2 create-security-group --group-name "$SG_BUILDER_NAME" --description "SSH do operador para o builder do Creator Workbench" --vpc-id "$vpc" \
      --tag-specifications "ResourceType=security-group,Tags=[{Key=Project,Value=$PROJECT}]" --query GroupId --output text)"
  fi
  myip="$(curl -s -m 10 https://checkip.amazonaws.com | tr -d '[:space:]')"
  [ -n "$myip" ] || { echo "Nao consegui descobrir o IP publico (checkip.amazonaws.com)" >&2; return 1; }
  for old in $(aws ec2 describe-security-groups --group-ids "$sg" --query 'SecurityGroups[0].IpPermissions[].IpRanges[].CidrIp' --output text); do
    [ "$old" = "$myip/32" ] || aws ec2 revoke-security-group-ingress --group-id "$sg" --protocol tcp --port 22 --cidr "$old" >/dev/null
  done
  aws ec2 authorize-security-group-ingress --group-id "$sg" --protocol tcp --port 22 --cidr "$myip/32" >/dev/null 2>&1 || true
  echo "$sg"
}

# Aplica o plano AGENCIA a "Equipe Office Resolve" se a coluna teams.plan existir (idempotente; apos o Flyway do M5).
apply_plans() {
  [ -n "$(builder_ip)" ] || { log "Builder desligado: plano da equipe nao aplicado (rode seed/redeploy com o builder ligado)"; return 0; }
  ssh_b 'bash -s' <<SQL
export MYSQL_PWD='$(secret DB_PASSWORD)'
command -v mysql >/dev/null || sudo dnf install -y mariadb105 >/dev/null
Q="mysql -h $(rds_endpoint) -u admin -N db_tcc"
if [ "\$(\$Q -e "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='db_tcc' AND table_name='teams' AND column_name='plan'")" != "0" ]; then
  \$Q -e "INSERT INTO users (email, password, name) SELECT 'admin.sistema@creatorworkbench.test', '\\\$2a\\\$10\\\$ezeOSqVguD0LQe77yUcfcOcdyALnGM81r.0zSk/dlX22Fn9OFiu3i', 'Admin do Sistema' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM users WHERE email='admin.sistema@creatorworkbench.test')"
  \$Q -e "UPDATE teams SET plan='AGENCIA', plan_source='MANUAL', plan_expires_at=NULL WHERE name='Equipe Office Resolve'" && echo "plano AGENCIA aplicado (se a equipe existe)"
else
  echo "teams.plan ainda nao existe: nada a fazer"
fi
SQL
}
