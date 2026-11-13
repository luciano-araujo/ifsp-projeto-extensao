# Deploy na AWS (Learner Lab) — Creator Workbench

Arquitetura: **EKS + ECR + ELB**, RDS MySQL 8.0, S3 e CloudWatch Logs. Região `us-east-1`. Todos os recursos levam a tag `Project=creator-workbench`.

```
Internet -> ELB clássico (HTTP 80) -> pod "web" [ nginx :8081 (frontend estático + proxy /api/) | Spring Boot :8080 ]
                                                         |                      |-> RDS MySQL (db_tcc)
                                                         |                      |-> S3 (materiais, credencial do nó = LabRole)
                                                         |                      '-> http://summary:8000 (pod "summary", FastAPI)
Fluent Bit (DaemonSet) -> CloudWatch Logs /creator-workbench/containers (retenção 7 dias)
```

Decisões:
- **nginx e backend no mesmo pod.** O `RateLimitFilter` só confia em `X-Forwarded-For` vindo de endereços exatos (sem CIDR). No mesmo pod o par é sempre `127.0.0.1` (padrão de `app.rate-limit.trusted-proxies`), então o limite por IP enxerga o IP real do cliente (o nginx recupera o IP do cliente que o ELB coloca em `X-Forwarded-For`).
- Nós `t3.medium` x2 em sub-redes públicas (sem NAT). O launch template `creator-workbench-nodes` define `HttpPutResponseHopLimit=2` para que os pods (S3, Fluent Bit) alcancem as credenciais do nó (LabRole).
- Um **builder** EC2 (t3.medium, `LabInstanceProfile`, docker) constrói as imagens e as envia ao ECR. Os manifestos são aplicados pela máquina local com `kubectl` (baixado em `~/bin` pelos scripts): no Learner Lab só o criador do cluster (role `voclabs`) é administrador; a `LabRole` do builder não pode receber acesso (o access entry dela é do tipo nó).
- Segredos nunca vão para o git: ficam em `deploy/aws/.state/secrets.env` (ignorado) e viram Secrets do Kubernetes pelo script. A chave `ANTHROPIC_API_KEY` vem de `automation/.env` (se existir) e nunca é impressa; sem ela o serviço de resumo usa o motor extrativo.

## Pré-requisitos (máquina local, Git Bash)
- AWS CLI v2 com credenciais válidas em `~/.aws/credentials` (perfil `default`); `~/.ssh/labsuser.pem` (key pair `vockey`).
- `deploy/aws/.state/secrets.env` com quatro linhas (valores sem aspas): `DB_PASSWORD=`, `JWT_SECRET=`, `MAIL_USERNAME=`, `MAIL_PASSWORD=`.

## Scripts (`deploy/aws/`, rodar com `bash` no Git Bash, na raiz do repositório)
| Script | O que faz |
|---|---|
| `up.sh` | Cria/completa ECR, S3 (privado, criptografado), log group, RDS, EKS + node group, builder; depois chama `redeploy.sh`. Idempotente. `--no-deploy` só cria a infraestrutura. |
| `redeploy.sh` | Empacota o backend (ref `release/v3`; `BACKEND_REF=` troca), clona o frontend (`release/v3` do GitHub), constrói as 3 imagens no builder, envia ao ECR, recria os Secrets/ConfigMap e faz o rollout (kubectl local). `SKIP_BUILD=1` só reaplica os manifestos com as últimas imagens. Procura a chave do Claude em `automation/.env`. |
| `seed.sh` | Insere Ana (ADMIN), Bia (CREATOR) e Rui (REVIEWER) `@creatorworkbench.test` direto no RDS (senha `Teste@123`) e popula equipe, ideias, projetos, roteiro e curso pela API. Rode uma vez por banco novo. |
| `status.sh` | Estado de EKS, nós, RDS, builder, pods, URL do ELB, HTTP 200/401 e custo/hora. |
| `teardown.sh` | Apaga só o que tem a tag do projeto/nomes conhecidos (namespace+ELB, node group, cluster, RDS sem snapshot, builder, ECR, S3, logs, launch template, tags de sub-rede) e lista o que sobrou. Mantém os itens gratuitos (SGs, DB subnet group, key pair). |

## Depois de uma nova sessão do Learner Lab
Ao terminar a sessão, as instâncias EC2 são paradas e as credenciais mudam. Em uma nova sessão:
1. Cole o novo bloco `[default]` em `~/.aws/credentials`.
2. `bash deploy/aws/status.sh` mostra o que está de pé (o EKS, o RDS e o ELB continuam; EC2 parada pode ser religada).
3. Se os nós ou o builder estavam parados: `bash deploy/aws/up.sh` (religa o builder, espera nós e RDS e refaz o deploy). O RDS parado pelo lab é religado com `aws rds start-db-instance --db-instance-identifier creator-workbench-db`.
4. Para publicar uma versão nova do `release/v3`: `bash deploy/aws/redeploy.sh`.

O DNS do ELB não muda enquanto o Service `web` existir; o `redeploy.sh` o imprime ao final.

## Verificação rápida
```
curl -i http://<ELB>/                   # 200, tela de login
curl -i http://<ELB>/api/v1/teams       # 401 sem token
```
Logs: console do CloudWatch > Log groups > `/creator-workbench/containers` (um stream por pod/contêiner).

## Custo aproximado (us-east-1, sob demanda)
EKS 0,10 + 2 nós t3.medium 0,083 + builder t3.medium 0,042 + RDS db.t3.micro 0,017 + ELB 0,025 ≈ **US$ 0,27/hora** (mais armazenamento: centavos). Desligue o builder quando não estiver em uso (`aws ec2 stop-instances`) para economizar; ele é religado pelo `redeploy.sh`.

## HTTPS (RNF02)
O ELB atende **HTTP na porta 80** e **HTTPS na 443** (TLS termina no ELB; do ELB ao pod segue HTTP). O certificado é **autoassinado** (RSA 2048, 2 anos; SAN = DNS do ELB; o CN fica curto porque o limite do CN e 64 caracteres e o DNS do ELB tem 70), gerado por `redeploy.sh` com `openssl` e importado no ACM com a tag `Project=creator-workbench`; o ARN fica em `deploy/aws/.state/cert_arn` e a chave em `.state/cert/` (ignorado pelo git). As execuções seguintes reutilizam o certificado. O Service `web` ganha as anotações `aws-load-balancer-ssl-cert`/`ssl-ports` e a porta 443 em toda aplicação, então o HTTPS sobrevive aos redeploys. `CORS_ORIGINS` recebe `http://<elb>,https://<elb>`.

**O navegador mostra um aviso de certificado não confiável** (é esperado). Um certificado confiável exige um domínio próprio (ACM público + Route 53 ou CNAME). `curl -k https://<elb>/` ignora o aviso. O `teardown.sh` apaga o certificado.

## Segurança do builder e limitações conhecidas
- O builder usa o security group próprio `SG-CreatorWorkbench-Builder`, com SSH (22) apenas do IP público atual de quem roda o script (/32, via `checkip.amazonaws.com`); `up.sh` e `redeploy.sh` atualizam a regra a cada execução. O SG legado `SG-CreatorWorkbench-App` (22 aberto ao mundo) não é mais usado.
- Os IDs fixos de security groups, sub-redes e VPC em `deploy/aws/common.sh` pertencem **a esta conta do Learner Lab** (VPC padrão 172.31.0.0/16); em outra conta, ajuste-os.
- **Limitação do Learner Lab:** só existe a `LabRole`, com permissões amplas, usada como papel do cluster e dos nós. O launch template define `HttpPutResponseHopLimit=2`, então qualquer pod alcança as credenciais da `LabRole` pelo IMDS. Em uma conta própria use IRSA/Pod Identity com papéis mínimos.
- Segredos e chaves nunca vão para o git (`.state/`, Secrets do Kubernetes criados pelo script).

## Planos (M5)
Depois do rollout, `redeploy.sh` e `seed.sh` executam `apply_plans`: se a coluna `teams.plan` existir (migration do M5 já aplicada), a equipe "Equipe Office Resolve" recebe `plan='AGENCIA'`, `plan_source='MANUAL'`, `plan_expires_at=NULL`. É idempotente e não faz nada antes da migration. O segredo do backend também recebe `APP_ADMIN_EMAILS=admin.sistema@creatorworkbench.test` (usuário criado por `seed-users.sql`, senha `Teste@123`).
