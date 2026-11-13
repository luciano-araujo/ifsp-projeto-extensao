#!/usr/bin/env bash
# Dados de demonstracao: insere Ana (ADMIN), Bia (CREATOR), Rui (REVIEWER) e o admin do sistema @creatorworkbench.test direto no RDS
# (sem e-mail) e popula equipe, ideias, projetos, roteiro e curso pela API. So rode uma vez por banco novo.
# A senha do banco nunca vai na linha de comando: segue por stdin (MYSQL_PWD). Uso: bash deploy/aws/seed.sh   (senha = DEMO_PASSWORD de .state/secrets.env, gerada na 1a vez; nunca impressa)
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
ensure_demo_password
PASS="$(secret DEMO_PASSWORD)"
IP="$(builder_ip)"; [ -n "$IP" ] || { echo "Builder desligado" >&2; exit 1; }
ELB="$(cat "$STATE_DIR/elb_host" 2>/dev/null)"; [ -n "$ELB" ] || { echo "Rode redeploy.sh antes" >&2; exit 1; }
scp_b "$HERE/seed-users.sql" "ec2-user@$IP:seed-users.sql"
HASH="$(demo_hash)"
ssh_b 'bash -s' <<SQL
export MYSQL_PWD='$(secret DB_PASSWORD)'
HASH='$HASH'
command -v mysql >/dev/null || sudo dnf install -y mariadb105 >/dev/null
{ echo "SET @demo_hash='\$HASH';"; cat ~/seed-users.sql; } | mysql -h $(rds_endpoint) -u admin db_tcc
mysql -h $(rds_endpoint) -u admin db_tcc -e 'select id,email,name from users'
SQL
# gancho do plano: roda apply_plans depois que a equipe e criada e antes dos membros
cat > "$STATE_DIR/plan-hook.sh" <<HOOK
#!/usr/bin/env bash
source "$HERE/common.sh"
apply_plans
HOOK
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$(cygpath -w "$HERE/seed-demo.ps1")" "http://$ELB" "$PASS" "$(cygpath -w "$(command -v bash)")" "$(cygpath -w "$STATE_DIR/plan-hook.sh")"
apply_plans
