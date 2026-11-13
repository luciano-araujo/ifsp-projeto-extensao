#!/usr/bin/env bash
# Cria (ou completa) toda a infraestrutura: ECR, S3, CloudWatch Logs, RDS, EKS + nodes, builder, e faz o deploy (redeploy.sh).
# Idempotente: pode ser repetido; recursos que ja existem sao pulados. Uso: bash deploy/aws/up.sh [--no-deploy]
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
ensure_secrets
TAGS_KV="Key=Project,Value=$PROJECT"

log "Conta $ACCOUNT, regiao $REGION"

# --- ECR, S3, logs -----------------------------------------------------------------------------------------
for r in backend frontend summary; do
  aws ecr describe-repositories --repository-names "creator-workbench/$r" >/dev/null 2>&1 \
    || aws ecr create-repository --repository-name "creator-workbench/$r" --tags "$TAGS_KV" >/dev/null
done
aws s3api head-bucket --bucket "$BUCKET" 2>/dev/null || aws s3api create-bucket --bucket "$BUCKET" >/dev/null
# a cada passada: bloqueio de acesso publico, criptografia padrao e tag
aws s3api put-public-access-block --bucket "$BUCKET" --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
aws s3api put-bucket-encryption --bucket "$BUCKET" --server-side-encryption-configuration '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"AES256"}}]}'
aws s3api put-bucket-tagging --bucket "$BUCKET" --tagging "TagSet=[{Key=Project,Value=$PROJECT}]"
if [ "$(aws logs describe-log-groups --log-group-name-prefix "$LOG_GROUP" --query 'length(logGroups)' --output text)" = "0" ]; then
  aws logs create-log-group --log-group-name "$LOG_GROUP" --tags Project=$PROJECT
fi
aws logs put-retention-policy --log-group-name "$LOG_GROUP" --retention-in-days 7

# --- RDS ----------------------------------------------------------------------------------------------------
if [ "$(rds_status)" = "ABSENT" ]; then
  log "Criando RDS $RDS_ID (db.t3.micro, MySQL 8.0)"
  aws rds create-db-instance --db-instance-identifier "$RDS_ID" --db-instance-class db.t3.micro --engine mysql --engine-version 8.0 \
    --allocated-storage 20 --storage-type gp2 --master-username admin --master-user-password "$(secret DB_PASSWORD)" --db-name db_tcc \
    --db-subnet-group-name "$DB_SUBNET_GROUP" --vpc-security-group-ids "$SG_RDS" --no-publicly-accessible \
    --backup-retention-period 0 --no-multi-az --storage-encrypted --tags "$TAGS_KV" >/dev/null
fi
aws ec2 authorize-security-group-ingress --group-id "$SG_RDS" --protocol tcp --port 3306 --cidr "$VPC_CIDR" >/dev/null 2>&1 || true

# --- EKS ----------------------------------------------------------------------------------------------------
if [ "$(cluster_status)" = "ABSENT" ]; then
  log "Criando cluster EKS $CLUSTER $EKS_VERSION (LabRole)"
  aws eks create-cluster --name "$CLUSTER" --kubernetes-version "$EKS_VERSION" --role-arn "$LAB_ROLE_ARN" \
    --resources-vpc-config "subnetIds=$EKS_SUBNETS,endpointPublicAccess=true,endpointPrivateAccess=false" --tags Project=$PROJECT >/dev/null
fi
for s in ${EKS_SUBNETS//,/ }; do   # o Service LoadBalancer descobre as subnets pelas tags
  aws ec2 create-tags --resources "$s" --tags "Key=kubernetes.io/cluster/$CLUSTER,Value=shared" "Key=kubernetes.io/role/elb,Value=1"
done

# --- builder (EC2 que constroi as imagens; os manifestos sao aplicados localmente com kubectl) ----------------------------------------------
SG_B="$(ensure_builder_sg)"
BID="$(builder_id)"
if [ -z "$BID" ]; then
  log "Criando builder EC2"
  AMI="$(aws ssm get-parameter --name /aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64 --query Parameter.Value --output text)"
  UD="$STATE_DIR/builder-userdata.sh"
  cat > "$UD" <<'UDEOF'
#!/bin/bash
dnf install -y docker git tar
systemctl enable --now docker
usermod -aG docker ec2-user
touch /var/tmp/builder-ready
UDEOF
  BID="$(aws ec2 run-instances --image-id "$AMI" --instance-type t3.medium --key-name "$KEY_NAME" --security-group-ids "$SG_B" \
    --iam-instance-profile Name=LabInstanceProfile --user-data "file://$(cygpath -m "$UD")" \
    --block-device-mappings 'DeviceName=/dev/xvda,Ebs={VolumeSize=30,VolumeType=gp3,DeleteOnTermination=true}' \
    --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=$BUILDER_NAME},{Key=Project,Value=$PROJECT}]" \
                         "ResourceType=volume,Tags=[{Key=Project,Value=$PROJECT}]" \
    --query 'Instances[0].InstanceId' --output text)"
else
  aws ec2 start-instances --instance-ids "$BID" >/dev/null 2>&1 || true
  aws ec2 modify-instance-attribute --instance-id "$BID" --groups "$SG_B"
fi
aws ec2 wait instance-running --instance-ids "$BID"

# --- nodes -----------------------------------------------------------------------------------------------------
log "Aguardando cluster ACTIVE (costuma levar 10 a 15 min)"
aws eks wait cluster-active --name "$CLUSTER"
if ! aws ec2 describe-launch-templates --launch-template-names "$LAUNCH_TEMPLATE" >/dev/null 2>&1; then
  # hop limit 2: os pods (S3 do backend, Fluent Bit) precisam alcancar as credenciais do no pelo IMDS
  aws ec2 create-launch-template --launch-template-name "$LAUNCH_TEMPLATE" \
    --launch-template-data '{"MetadataOptions":{"HttpEndpoint":"enabled","HttpTokens":"required","HttpPutResponseHopLimit":2}}' \
    --tag-specifications "ResourceType=launch-template,Tags=[{Key=Project,Value=$PROJECT}]" >/dev/null
fi
if ! aws eks describe-nodegroup --cluster-name "$CLUSTER" --nodegroup-name "$NODEGROUP" >/dev/null 2>&1; then
  log "Criando node group ($NODEGROUP, 2 x t3.medium)"
  aws eks create-nodegroup --cluster-name "$CLUSTER" --nodegroup-name "$NODEGROUP" --node-role "$LAB_ROLE_ARN" \
    --subnets ${EKS_SUBNETS//,/ } --scaling-config minSize=2,maxSize=2,desiredSize=2 --instance-types t3.medium \
    --launch-template "name=$LAUNCH_TEMPLATE,version=\$Latest" --tags Project=$PROJECT >/dev/null
fi
log "Aguardando nodes"
aws eks wait nodegroup-active --cluster-name "$CLUSTER" --nodegroup-name "$NODEGROUP"

log "Aguardando RDS disponivel"
aws rds wait db-instance-available --db-instance-identifier "$RDS_ID"

[ "${1:-}" = "--no-deploy" ] && { log "Infra pronta (sem deploy)."; exit 0; }
bash "$HERE/redeploy.sh"
log "Para o demo: bash deploy/aws/seed.sh"
