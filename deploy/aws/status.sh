#!/usr/bin/env bash
# Mostra o estado de tudo e a URL do ELB. Uso: bash deploy/aws/status.sh
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
set +e
echo "== Credenciais: conta $ACCOUNT"
echo "== EKS:        $(cluster_status)"
echo "== Node group: $(aws eks describe-nodegroup --cluster-name $CLUSTER --nodegroup-name $NODEGROUP --query nodegroup.status --output text 2>/dev/null || echo ABSENT)"
echo "== RDS:        $(rds_status)  $(aws rds describe-db-instances --db-instance-identifier $RDS_ID --query 'DBInstances[0].Endpoint.Address' --output text 2>/dev/null)"
echo "== Builder:    $(aws ec2 describe-instances --filters Name=tag:Name,Values=$BUILDER_NAME 'Name=instance-state-name,Values=pending,running,stopping,stopped' --query 'Reservations[].Instances[].[InstanceId,State.Name,PublicIpAddress]' --output text)"
echo "== Nodes:"
aws ec2 describe-instances --filters Name=tag:Project,Values=$PROJECT "Name=instance-state-name,Values=pending,running,stopped" \
  --query 'Reservations[].Instances[].[InstanceId,InstanceType,State.Name,Tags[?Key==`Name`]|[0].Value]' --output text
echo "== S3:         $(aws s3 ls "s3://$BUCKET" --summarize 2>/dev/null | tail -2 | tr '\n' ' ')"
echo "== ECR:        $(aws ecr describe-repositories --query 'repositories[?starts_with(repositoryName,`creator-workbench/`)].repositoryName' --output text)"
echo "== Logs:       $(aws logs describe-log-groups --log-group-name-prefix $LOG_GROUP --query 'logGroups[].[logGroupName,retentionInDays,storedBytes]' --output text)"
if [ -n "$(builder_ip)" ]; then
  echo "== Pods:"
  ssh_b "kubectl -n $NS get pods,svc -o wide 2>&1 | head -30"
  ELB="$(ssh_b 'cat ~/elb_host 2>/dev/null')"
  [ -n "$ELB" ] && { echo "== URL: http://$ELB"; echo "   HTTP: $(curl -s -o /dev/null -m 10 -w '%{http_code}' http://$ELB/)  /api/v1/teams sem token: $(curl -s -o /dev/null -m 10 -w '%{http_code}' http://$ELB/api/v1/teams)"; }
fi
echo "== Custo/hora aproximado (us-east-1): EKS 0,10 + 2 x t3.medium 0,0832 + builder t3.medium 0,0416 + RDS t3.micro 0,017 + ELB 0,025 = ~0,27 USD/h"
