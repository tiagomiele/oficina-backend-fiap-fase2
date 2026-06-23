# 🚀 Runbook — Deploy da Oficina Backend na AWS Academy (EKS + RDS)

> **Objetivo:** subir a aplicação na AWS do zero, **copiando e colando** os comandos na ordem.
> Testado no **AWS Academy Learner Lab** (região `us-west-2`). Tempo total: ~30-40 min (a maior parte é espera do EKS/RDS).
>
> ✅ Todos os ajustes que precisamos descobrir "na mão" já estão **versionados no Terraform** (versão do K8s, regra de security group, versão do Postgres, liberação da cluster-SG no RDS). Então este runbook é o **caminho feliz**: é só seguir.

---

## 📋 Pré-requisitos (só na 1ª vez)

Você vai usar o **terminal Linux do Learner Lab** (onde as credenciais AWS já vêm carregadas). Instale `terraform`, `kubectl` e o `aws-cli v2` **no seu home** (sem `sudo`).

```bash
# diretório pra binários locais
mkdir -p ~/bin && export PATH="$HOME/bin:$PATH"
echo 'export PATH="$HOME/bin:$PATH"' >> ~/.bashrc

# 1) AWS CLI v2 (o v1 do lab gera kubeconfig velho que o kubectl rejeita!)
cd ~
curl -fsSL "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o awscliv2.zip
unzip -q -o awscliv2.zip        # demora ~1 min extraindo, é normal
./aws/install -i ~/aws-cli -b ~/bin --update
hash -r

# 2) kubectl 1.31
curl -fsSLO "https://dl.k8s.io/release/v1.31.0/bin/linux/amd64/kubectl"
chmod +x kubectl && mv kubectl ~/bin/

# 3) terraform (se ainda não tiver)
curl -fsSLO "https://releases.hashicorp.com/terraform/1.9.5/terraform_1.9.5_linux_amd64.zip"
unzip -q -o terraform_1.9.5_linux_amd64.zip && mv terraform ~/bin/

# conferir versões (aws DEVE ser 2.x)
aws --version
kubectl version --client
terraform version
```

> 💡 O aviso `Starlark failed to allocate 4GB...` que aparece no `kubectl` é **ruído do ambiente do lab** e pode ser **ignorado** sempre.

---

## 🔑 Passo 0 — clonar o repo e checar credenciais

### 0.1 — Gerar o token do GitHub (o repo é **privado**)

O GitHub **não aceita mais senha** no `git clone` — você precisa de um **Personal Access Token (PAT)**. Caminho para pegar o token real:

1. Acesse: **https://github.com/settings/tokens/new**
   (ou pelo menu: foto do perfil → **Settings** → **Developer settings** → **Personal access tokens** → **Tokens (classic)** → **Generate new token (classic)**)
2. **Note**: `lab-clone`
3. **Expiration**: `7 days` (ou o que preferir)
4. **Select scopes**: marque a caixa **`repo`** (a primeira — cobre repositórios privados)
5. Role até o fim e clique em **Generate token**
6. **Copie o token** (começa com `ghp_...`) — ele **só aparece uma vez**. Guarde num lugar seguro.

> ⚠️ **Nunca** cole o token em chat, commit ou documentação. Se vazar, revogue em https://github.com/settings/tokens (**Delete**) e gere outro.

### 0.2 — Clonar com o token

Embuta o token na URL (troque `ghp_SEU_TOKEN` pelo seu):
```bash
cd ~
git clone https://tiagomiele:ghp_SEU_TOKEN@github.com/tiagomiele/oficina-backend-fiap-fase2.git
cd oficina-backend-fiap-fase2

# os fixes do AWS Academy estão na branch do PR #2:
git checkout devin/1781651195-terraform-aws-academy
```

**Erros comuns nesta etapa:**
- `Password authentication is not supported` → o que você colou na senha **não é um PAT válido** (era a senha da conta, token expirado, ou sem o escopo `repo`). Gere um novo seguindo o 0.1.
- `destination path '...' already exists and is not an empty directory` → a pasta já existe. Use o que já está lá (`cd oficina-backend-fiap-fase2 && git pull`) **ou** apague: `cd ~ && rm -rf oficina-backend-fiap-fase2` e clone de novo.
- `Stale file handle` ao apagar/clonar → você está **dentro** da pasta que tentou apagar. Saia antes: `cd ~` e só então rode o `rm -rf`/`git clone`.

### 0.3 — Checar credenciais da AWS

```bash
# confirma que as credenciais do lab estão ativas
aws sts get-caller-identity
```
A saída deve mostrar a conta `163061816974` e a `LabRole`. Se der erro de credencial, **clique em "AWS Details → Show" no lab e copie as credenciais** pro terminal (ou apenas use o terminal já autenticado do lab).

---

## 🏗️ Passo 1 — provisionar a infra (Terraform)

```bash
cd ~/oficina-backend-fiap-fase2/infra

# cria o terraform.tfvars a partir do exemplo
cp terraform.tfvars.example terraform.tfvars
```

Agora **edite o `terraform.tfvars`** com os valores que funcionam no Learner Lab (`us-west-2`).
Cole este bloco que já sobrescreve o arquivo com os valores corretos:

```bash
cat > terraform.tfvars <<'EOF'
aws_region  = "us-west-2"
environment = "dev"

# AWS Academy / Learner Lab: reutiliza a LabRole (criar IAM roles e bloqueado).
# Sem esta linha, o apply falha com 403 iam:CreateRole.
lab_role_arn = "arn:aws:iam::163061816974:role/LabRole"

# Rede
vpc_cidr           = "10.0.0.0/16"
availability_zones = ["us-west-2a", "us-west-2b"]

# EKS
cluster_version    = "1.31"
node_instance_type = "t3.medium"
node_desired_count = 2
node_min_count     = 2
node_max_count     = 5

# RDS
db_instance_class    = "db.t3.micro"
db_name              = "oficina"
db_username          = "oficina"
db_password          = "OficinaFase2!2026A"
db_allocated_storage = 20
EOF
```

> 🛑 **AWS Academy — atenção (causa de erro #1):** a linha `lab_role_arn` é **obrigatória**. No Learner Lab você **não tem permissão para criar IAM roles**; sem essa linha o `terraform apply` quebra com `AccessDenied ... iam:CreateRole`. Com ela, o Terraform **reutiliza a LabRole**. Confirme antes do apply:
> ```bash
> grep lab_role_arn terraform.tfvars   # deve mostrar o ARN da LabRole
> aws sts get-caller-identity --query Account --output text   # deve bater com o nº da conta no ARN (163061816974)
> ```
> Se a sua conta for diferente de `163061816974`, ajuste o número no `lab_role_arn`.

Provisione:
```bash
terraform init
terraform plan -out=tfplan
terraform apply tfplan      # digite 'yes' se pedir confirmação
```

⏱️ **Demora ~20 min** (EKS ~10 min + node group ~3 min + RDS ~7 min). É normal ficar mostrando `Still creating...`.

No fim aparece **`Apply complete!`** e os **Outputs**. **Anote o `rds_endpoint`** (ex.: `oficina-dev-db.xxxx.us-west-2.rds.amazonaws.com:5432`) — você vai usar no Passo 3.

```bash
# guarda os outputs em variáveis pra reaproveitar
RDS_HOST=$(terraform output -raw rds_endpoint | cut -d: -f1)
echo "RDS_HOST=$RDS_HOST"
```

---

## 🔌 Passo 2 — conectar o kubectl ao cluster

```bash
aws eks update-kubeconfig --region us-west-2 --name oficina-dev
kubectl get nodes
```
Deve listar **2 nós** com status `Ready` (pode levar ~1 min).

> ⚠️ Se aparecer `invalid apiVersion "client.authentication.k8s.io/v1alpha1"`, é porque o kubeconfig foi gerado pelo AWS CLI **v1**. Garanta que o `aws --version` mostra **2.x** e rode de novo o `update-kubeconfig`. (Se ainda persistir: `sed -i 's#v1alpha1#v1beta1#' "$HOME/.kube/config"`.)

---

## 🐳 Passo 3 — publicar a aplicação no EKS

### 3.1 — imagem do GHCR pública (só na 1ª vez)
A imagem precisa estar **pública** pro EKS baixar sem credencial. Confira/ajuste em:
👉 https://github.com/users/tiagomiele/packages/container/oficina-backend-fiap-fase2/settings
→ **Danger Zone → Change visibility → Public**. (Libera só a imagem Docker, não o repositório.)

### 3.2 — ajustar os manifestos pro RDS

> 🛑 **Atenção (causa de erro #3):** o **endpoint do RDS muda a cada `terraform apply`** (a AWS gera um trecho aleatório no hostname). Por isso o `DB_URL` do configmap **tem que ser setado a partir do `terraform output`**, nunca digitado/hardcoded. Se o `DB_URL` ficar errado (ou vazio), o app cai no default `localhost` e os pods entram em `CrashLoopBackOff` com `Connection to localhost:5432 refused`.

```bash
cd ~/oficina-backend-fiap-fase2

# (re)descobre o endpoint do RDS DESTE apply — rode sempre, mesmo em sessão nova
RDS_HOST=$(cd infra && terraform output -raw rds_endpoint | cut -d: -f1)
echo "RDS_HOST=$RDS_HOST"   # NÃO pode vir vazio

# aponta o app pro endpoint do RDS deste apply
sed -i "s#jdbc:postgresql://[^/]*/oficina#jdbc:postgresql://${RDS_HOST}:5432/oficina#" k8s/configmap.yaml

# usa a imagem publicada no GHCR
sed -i 's#image: oficina-backend:latest#image: ghcr.io/tiagomiele/oficina-backend-fiap-fase2:latest#' k8s/app-deployment.yaml
sed -i 's#imagePullPolicy: IfNotPresent#imagePullPolicy: Always#' k8s/app-deployment.yaml

# senha do banco em base64 (= "OficinaFase2!2026A", igual ao terraform.tfvars)
sed -i 's#DB_PASSWORD: ".*"#DB_PASSWORD: "T2ZpY2luYUZhc2UyITIwMjZB"#' k8s/secret.yaml

# conferir
grep DB_URL k8s/configmap.yaml
grep 'image:' k8s/app-deployment.yaml
grep DB_PASSWORD k8s/secret.yaml
```

> 💡 Se mudar a senha do banco, gere o base64 com: `echo -n 'SUA_SENHA' | base64`

### 3.3 — aplicar (NÃO use os manifestos de postgres in-cluster; o banco é o RDS)
```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/app-deployment.yaml
kubectl apply -f k8s/app-service.yaml

kubectl get pods -n oficina -w   # Ctrl+C quando ficarem 1/1 Running
```
Os pods passam por `ContainerCreating` → `Running` → `1/1`. Leva ~1-2 min.

---

## 🌐 Passo 4 — pegar a URL pública e validar

```bash
EXT=$(kubectl get svc oficina-app -n oficina -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
echo "URL base:  http://$EXT"
echo "Swagger:   http://$EXT/swagger-ui/index.html"

# health check (esperado: {"status":"UP",...})
curl -s "http://$EXT/actuator/health"; echo
# swagger (esperado: HTTP 200)
curl -s -o /dev/null -w "Swagger HTTP %{http_code}\n" "http://$EXT/swagger-ui/index.html"
```

> 🔎 **Como descobrir a URL do Swagger:** é sempre `http://<EXT>/swagger-ui/index.html`, onde `<EXT>` é o hostname do LoadBalancer (o comando acima já monta e imprime a URL pronta). Só existe **um** Swagger — o do serviço rodando no EKS; não há outro para escolher. Abra essa URL no navegador, faça login em `01.01 - Login` (`admin@oficina.local` / `admin123`), clique em **Authorize** 🔒, cole o token e teste os endpoints.
> ⏱️ O LoadBalancer (ELB) leva ~2-3 min pra começar a responder. Se der `timeout`/`Empty reply` na 1ª vez, espere 1 min e repita.

### Smoke test da API (login + criar serviço + listar = grava no RDS)
```bash
TOKEN=$(curl -s -X POST "http://$EXT/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@oficina.local","senha":"admin123"}' \
  | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
echo "TOKEN (tamanho): ${#TOKEN}"

curl -s -X POST "http://$EXT/servicos" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nome":"Troca de óleo","descricao":"Óleo + filtro","precoBase":150.00}'; echo

curl -s "http://$EXT/servicos" -H "Authorization: Bearer $TOKEN"; echo
```
Esperado: token grande, POST retorna `idServico`, GET lista o serviço criado → **persistiu no RDS** 🎯

### Swagger UI (forma visual)
Abra `http://$EXT/swagger-ui/index.html` → `POST /auth/login` (Try it out) → copie o `accessToken` → botão **Authorize** 🔒 → cole o token → teste os endpoints.

---

## 🗄️ Acessar o banco RDS direto (opcional)

O RDS **não tem IP público** (só os nós do EKS acessam). Pra inspecionar as tabelas, rode um `psql` **de dentro do cluster**:
```bash
kubectl run psql-tmp -n oficina --rm -it --restart=Never --image=postgres:16 -- \
  psql "postgresql://oficina:OficinaFase2!2026A@${RDS_HOST}:5432/oficina"
# dentro do psql:  \dt   (lista tabelas)   |   SELECT * FROM servico;   |   \q  (sair)
```

| Campo | Valor |
|---|---|
| Host | `$RDS_HOST` (output do terraform) |
| Porta | `5432` |
| Banco | `oficina` |
| Usuário | `oficina` |
| Senha | `OficinaFase2!2026A` |

---

## 💸 Passo 5 — DESTRUIR tudo (pra não gastar crédito!)

> **Sempre rode isto ao terminar.** Enquanto EKS/RDS/ELB estiverem de pé, consomem crédito do lab.

```bash
# 1) remove o LoadBalancer (criado pelo k8s, fora do Terraform) — evita ELB órfão
kubectl delete svc oficina-app -n oficina

# 2) derruba toda a infra
cd ~/oficina-backend-fiap-fase2/infra
terraform destroy        # digite 'yes'
```
⏱️ ~10-15 min. No fim: **`Destroy complete!`**

**Checklist final** (tudo deve vir vazio `[]`):
```bash
aws eks list-clusters --region us-west-2
aws rds describe-db-instances --region us-west-2 --query "DBInstances[].DBInstanceIdentifier"
aws elbv2 describe-load-balancers --region us-west-2 --query "LoadBalancers[].LoadBalancerName"
```

---

## 🛠️ Troubleshooting (erros que já enfrentamos e como resolver)

| Sintoma | Causa | Solução |
|---|---|---|
| `AccessDenied: ... iam:CreateRole` | No AWS Academy não se pode criar IAM roles | Defina `lab_role_arn = "arn:aws:iam::163061816974:role/LabRole"` no tfvars (já neste runbook) |
| `unsupported Kubernetes version 1.29` | AWS removeu 1.29 em us-west-2 | Use `cluster_version = "1.31"` (já no tfvars deste runbook) |
| `from_port (0) and to_port (65535) must both be 0 to use 'ALL' "-1"` | Regra de SG com protocolo `-1` exige porta 0 | Já corrigido no `modules/eks/main.tf` |
| `Cannot find version 16.3 for postgres` | Versão de Postgres indisponível na região | Terraform já descobre a versão 16 mais recente automaticamente |
| `kubectl`: `invalid apiVersion ...v1alpha1` | kubeconfig gerado pelo AWS CLI **v1** | Use AWS CLI **v2** e rode `update-kubeconfig` de novo |
| Pods em `CrashLoopBackOff` com `Connection to localhost:5432 refused` (Flyway) | `DB_URL` não aponta pro RDS (configmap não ajustado / `RDS_HOST` vazio) | Refaça o Passo 3.2 derivando `RDS_HOST` do `terraform output`, `kubectl apply -f k8s/configmap.yaml` e `kubectl rollout restart deployment/oficina-app -n oficina` |
| `git clone`: `Password authentication is not supported` | Usou senha da conta em vez de PAT, ou token sem escopo `repo` | Gere um PAT (classic, escopo `repo`) — Passo 0.1 — e use no clone |
| `git clone`: `Stale file handle` | Você está **dentro** da pasta que tentou apagar | `cd ~` antes de `rm -rf`/`git clone` |
| Pods em `CrashLoopBackOff` com `SocketTimeoutException: Connect timed out` (Flyway) | RDS não libera a **cluster security group** do EKS (só a node-SG) | Já corrigido no Terraform (`modules/rds` libera a cluster-SG). Se reaparecer ao vivo: `aws ec2 authorize-security-group-ingress --region us-west-2 --group-id <RDS_SG> --protocol tcp --port 5432 --source-group <CLUSTER_SG>` |
| Pods em `ImagePullBackOff` | Imagem do GHCR ainda **privada** | Torne o pacote **Public** (Passo 3.1) |
| `/actuator/health` dá timeout no 1º acesso | ELB ainda provisionando | Espere ~2-3 min e tente de novo |

---

## 📝 Resumo ultrarrápido (TL;DR)

```bash
# 0. CLONAR (repo privado: use PAT classic com escopo repo — Passo 0.1)
cd ~
git clone https://tiagomiele:ghp_SEU_TOKEN@github.com/tiagomiele/oficina-backend-fiap-fase2.git
cd oficina-backend-fiap-fase2 && git checkout devin/1781651195-terraform-aws-academy
# 1. INFRA
cd ~/oficina-backend-fiap-fase2/infra
cp terraform.tfvars.example terraform.tfvars   # já vem com us-west-2, k8s 1.31 e lab_role_arn (LabRole)!
sed -i 's/ALTERAR_PARA_SENHA_SEGURA/OficinaFase2!2026A/' terraform.tfvars   # caso esteja com placeholder
terraform init && terraform apply -auto-approve
RDS_HOST=$(terraform output -raw rds_endpoint | cut -d: -f1)
# 2. KUBECTL
aws eks update-kubeconfig --region us-west-2 --name oficina-dev
# 3. APP
cd ~/oficina-backend-fiap-fase2
sed -i "s#jdbc:postgresql://[^/]*/oficina#jdbc:postgresql://${RDS_HOST}:5432/oficina#" k8s/configmap.yaml
sed -i 's#image: oficina-backend:latest#image: ghcr.io/tiagomiele/oficina-backend-fiap-fase2:latest#' k8s/app-deployment.yaml
kubectl apply -f k8s/namespace.yaml -f k8s/configmap.yaml -f k8s/secret.yaml -f k8s/app-deployment.yaml -f k8s/app-service.yaml
# 4. VALIDAR
EXT=$(kubectl get svc oficina-app -n oficina -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
curl -s "http://$EXT/actuator/health"; echo
# 5. DESTRUIR (ao terminar!)
kubectl delete svc oficina-app -n oficina
cd ~/oficina-backend-fiap-fase2/infra && terraform destroy -auto-approve
```
