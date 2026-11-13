#!/usr/bin/env bash
# Remove SOMENTE o que o projeto criou (tag Project=creator-workbench e nomes conhecidos) e lista o que sobrou.
# Mantem os itens gratuitos antigos (security groups, DB subnet group, key pair vockey).
# Uso: bash deploy/aws/teardown.sh [--yes]
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
set +e
if [ "${1:-}" != "--yes" ]; then
  read -r -p "Apagar EKS, nodes, ELB, RDS (sem snapshot), builder, ECR, S3 ($BUCKET) e logs do Creator Workbench? [digite sim] " a
  [ "$a" = "sim" ] || { echo "Cancelado"; exit 1; }
fi
chk() { local rc=$1; shift; if [ "$rc" = 0 ]; then log "ok: $*"; else log "FALHOU/nao existia: $*"; fi; }

# 1) Service LoadBalancer (remove o ELB e o SG dele antes do cluster)
if [ -n "$(builder_ip)" ] && [ "$(cluster_status)" != "ABSENT" ]; then
  ssh_b "aws eks update-kubeconfig --name $CLUSTER --region $REGION >/dev/null && kubectl delete namespace $NS --timeout=300s" ; chk $? "namespace/ELB"
fi
# ELBs classicos remanescentes com a tag do projeto
for lb in $(aws elb describe-load-balancers --query 'LoadBalancerDescriptions[].LoadBalancerName' --output text); do
  if aws elb describe-tags --load-balancer-names "$lb" --query 'TagDescriptions[].Tags[?Key==`Project`].Value' --output text | grep -q "$PROJECT"; then
    aws elb delete-load-balancer --load-balancer-name "$lb"; chk $? "ELB $lb"
  fi
done

# 2) EKS
if aws eks describe-nodegroup --cluster-name $CLUSTER --nodegroup-name $NODEGROUP >/dev/null 2>&1; then
  aws eks delete-nodegroup --cluster-name $CLUSTER --nodegroup-name $NODEGROUP >/dev/null; chk $? "nodegroup"
  aws eks wait nodegroup-deleted --cluster-name $CLUSTER --nodegroup-name $NODEGROUP
fi
if [ "$(cluster_status)" != "ABSENT" ]; then
  aws eks delete-cluster --name $CLUSTER >/dev/null; chk $? "cluster"
  aws eks wait cluster-deleted --name $CLUSTER
fi
aws ec2 delete-launch-template --launch-template-name $LAUNCH_TEMPLATE >/dev/null 2>&1; chk $? "launch template"

# 3) RDS e builder
if [ "$(rds_status)" != "ABSENT" ]; then
  aws rds delete-db-instance --db-instance-identifier $RDS_ID --skip-final-snapshot --delete-automated-backups >/dev/null; chk $? "RDS"
fi
BID="$(builder_id)"
if [ -n "$BID" ]; then aws ec2 terminate-instances --instance-ids $BID >/dev/null; chk $? "builder $BID"; fi

# 4) ECR, S3, logs
for r in backend frontend summary; do aws ecr delete-repository --repository-name creator-workbench/$r --force >/dev/null 2>&1; chk $? "ECR $r"; done
if aws s3api head-bucket --bucket "$BUCKET" 2>/dev/null; then
  aws s3 rm "s3://$BUCKET" --recursive >/dev/null
  aws s3api list-object-versions --bucket "$BUCKET" --query '[Versions,DeleteMarkers][][].{Key:Key,VersionId:VersionId}' --output json 2>/dev/null | grep -q Key \
    && echo "(bucket com versoes: esvazie manualmente)"
  aws s3api delete-bucket --bucket "$BUCKET"; chk $? "S3 $BUCKET"
fi
aws logs delete-log-group --log-group-name $LOG_GROUP 2>/dev/null; chk $? "log group $LOG_GROUP"

# 5) tags de subnets e regra de SG que o up.sh acrescentou
for s in ${EKS_SUBNETS//,/ }; do
  aws ec2 delete-tags --resources $s --tags Key=kubernetes.io/cluster/$CLUSTER Key=kubernetes.io/role/elb
done
aws ec2 revoke-security-group-ingress --group-id $SG_RDS --protocol tcp --port 3306 --cidr $VPC_CIDR >/dev/null 2>&1

# 6) Verificacao
if [ "$(rds_status)" != "ABSENT" ]; then aws rds wait db-instance-deleted --db-instance-identifier $RDS_ID 2>/dev/null; fi
echo; echo "===== O QUE SOBROU ====="
echo "EC2 (nao terminadas):"; aws ec2 describe-instances --filters "Name=tag:Project,Values=$PROJECT" "Name=instance-state-name,Values=pending,running,stopping,stopped,shutting-down" --query 'Reservations[].Instances[].[InstanceId,State.Name]' --output text
echo "Volumes:"; aws ec2 describe-volumes --filters "Name=tag:Project,Values=$PROJECT" --query 'Volumes[].VolumeId' --output text
echo "EKS:"; aws eks list-clusters --query clusters --output text
echo "RDS:"; aws rds describe-db-instances --query 'DBInstances[].DBInstanceIdentifier' --output text
echo "ELB classico:"; aws elb describe-load-balancers --query 'LoadBalancerDescriptions[].LoadBalancerName' --output text
echo "ELBv2:"; aws elbv2 describe-load-balancers --query 'LoadBalancers[].LoadBalancerName' --output text
echo "NAT / EIP:"; aws ec2 describe-nat-gateways --filter Name=state,Values=available,pending --query 'NatGateways[].NatGatewayId' --output text; aws ec2 describe-addresses --query 'Addresses[].PublicIp' --output text
echo "ECR:"; aws ecr describe-repositories --query 'repositories[].repositoryName' --output text
echo "S3:"; aws s3 ls
echo "Logs:"; aws logs describe-log-groups --log-group-name-prefix /creator-workbench --query 'logGroups[].logGroupName' --output text
echo "Recursos com a tag Project=$PROJECT:"; aws resourcegroupstaggingapi get-resources --tag-filters Key=Project,Values=$PROJECT --query 'ResourceTagMappingList[].ResourceARN' --output text
echo "Gratuitos mantidos: SGs $SG_APP $SG_RDS, DB subnet group $DB_SUBNET_GROUP, key pair $KEY_NAME"
echo "(cobrancas podem aparecer com ate ~1 dia de atraso)"
