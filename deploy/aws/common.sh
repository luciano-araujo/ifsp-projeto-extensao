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
SG_APP=sg-08da4c9f8f5c0d45c     # SG-CreatorWorkbench-App (22, 80, 9000)
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
