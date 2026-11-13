#!/usr/bin/env bash
# Dados de demonstracao: insere Ana (ADMIN), Bia (CREATOR) e Rui (REVIEWER) @creatorworkbench.test direto no RDS (sem e-mail)
# e popula equipe, ideias, projetos, roteiro e curso pela API. So rode uma vez por banco novo.
# Uso: bash deploy/aws/seed.sh [senha-dos-usuarios]
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
PASS="${1:-Teste@123}"
IP="$(builder_ip)"; [ -n "$IP" ] || { echo "Builder desligado" >&2; exit 1; }
ELB="$(cat "$STATE_DIR/elb_host" 2>/dev/null)"
scp_b "$HERE/seed-users.sql" "ec2-user@$IP:seed-users.sql"
ssh_b "command -v mysql >/dev/null || sudo dnf install -y mariadb105 >/dev/null; \
  mysql -h $(rds_endpoint) -u admin -p'$(secret DB_PASSWORD)' db_tcc < ~/seed-users.sql && \
  mysql -h $(rds_endpoint) -u admin -p'$(secret DB_PASSWORD)' db_tcc -e 'select id,email,name from users'"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$(cygpath -w "$HERE/seed-demo.ps1")" "http://$ELB" "$PASS"
