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
