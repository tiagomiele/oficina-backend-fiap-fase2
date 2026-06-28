# Guia — Automatizar a subida da infra com Terraform Cloud + GitHub (AWS Academy)

> **Para quem é este guia:** alguém que nunca usou Terraform Cloud. Cada passo
> explica **o que** você faz e **por quê**. Leia na ordem.

---

## 1. O que vamos montar (visão geral)

Hoje você sobe a infra **manualmente** no terminal do AWS Academy (o Runbook:
`terraform init/plan/apply` na mão). A ideia agora é tirar esse trabalho manual
e deixar o **Terraform Cloud** fazer por você.

**Terraform Cloud (HCP Terraform)** é um serviço gratuito da HashiCorp que:

- guarda o **state** do Terraform na nuvem (você não depende mais do arquivo
  `terraform.tfstate` na sua máquina);
- **executa** o `terraform plan` e o `terraform apply` nos servidores deles;
- guarda as **credenciais da AWS** num cofre (Variables), com segurança;
- dispara tudo a partir de um **push no GitHub** — você só aprova o apply.

### Desenho do fluxo

```
   Você edita infra/*.tf  ->  git push  ->  GitHub
                                              │
                                              ▼
                                    Terraform Cloud (workspace)
                                    - usa as credenciais AWS guardadas
                                    - roda plan  -> você revisa
                                    - roda apply -> você clica "Confirm"
                                              │
                                              ▼
                                      AWS  (VPC + EKS + RDS)
```

> **Importante (limitação do AWS Academy):** as credenciais do Learner Lab
> **expiram a cada sessão** (mudam toda vez que você abre o lab). Então, mesmo
> com a automação, **uma coisa continua manual:** atualizar as 3 credenciais no
> Terraform Cloud sempre que abrir o lab. Em uma conta AWS de verdade isso seria
> 100% automático e você nunca mais mexeria. O **Passo 6** mostra exatamente
> onde colar essas credenciais.

---

## 2. Pré-requisitos

- Conta no GitHub com o repositório `oficina-backend-fiap-fase2` (já tem).
- Lab do AWS Academy aberto (para pegar as credenciais quando for a hora).
- (Opcional) Terraform instalado na máquina, só se quiser testar pelo terminal.

Você **não** precisa instalar nada para a automação funcionar — ela roda na
nuvem.

---

## 3. Passo 1 — Criar a conta e a organização no Terraform Cloud

1. Acesse **https://app.terraform.io/signup/account** e crie uma conta grátis.
2. Confirme o e-mail e faça login em **https://app.terraform.io**.
3. Na primeira vez ele pede para **criar uma organização** (*organization*).
   - Dê um nome único, por exemplo: `oficina-fiap-SEUNOME`.
   - **Anote esse nome** — vamos usar como `TF_CLOUD_ORGANIZATION`.

> *O que é uma organização?* É só o "espaço" da sua conta onde ficam os
> workspaces. Pense como uma pasta-raiz.

---

## 4. Passo 2 — Criar o workspace conectado ao GitHub (VCS-driven)

Um **workspace** é onde o Terraform Cloud roda a sua infra. Vamos criar um do
tipo **"Version Control Workflow"**, que fica observando o seu repositório.

1. No Terraform Cloud, clique em **New** → **Workspace**.
2. Escolha **Version Control Workflow**.
3. Conecte sua conta do **GitHub** (ele abre uma tela de autorização do GitHub —
   aprove o acesso ao repositório `oficina-backend-fiap-fase2`).
4. Selecione o repositório `oficina-backend-fiap-fase2`.
5. **Workspace Name:** digite `oficina-backend-infra`
   - **Anote** — é o `TF_WORKSPACE`.
6. Clique em **Advanced options** e ajuste:
   - **Terraform Working Directory:** `infra`
     *(é a pasta onde estão os `.tf`; sem isso o TFC não acha os arquivos.)*
   - **Apply Method:** deixe em **Manual apply**
     *(assim você sempre revisa o plan e clica para aplicar — mais seguro.)*
7. Clique em **Create**.

> A partir daqui, todo `git push` que mexer em `infra/` faz o Terraform Cloud
> rodar um `plan` automaticamente.

---

## 5. Passo 3 — Ligar o bloco `cloud {}` no código

Para o Terraform usar o Terraform Cloud (em vez do state local), precisamos
ativar um pequeno bloco no código.

1. Abra o arquivo **`infra/backend.tf`**.
2. **Descomente** (tire o `#`) apenas estas linhas no fim do arquivo:

   ```hcl
   terraform {
     cloud {}
   }
   ```

3. Garanta que o **bloco `backend "s3"` continua comentado** (só pode haver um).

> *Por que `cloud {}` está vazio?* Porque o nome da organização e do workspace
> vêm de variáveis de ambiente (`TF_CLOUD_ORGANIZATION` e `TF_WORKSPACE`), que o
> Terraform Cloud já injeta sozinho no workspace VCS-driven. Assim não
> escrevemos nomes fixos no código.

4. Salve, faça commit e push:

   ```bash
   git add infra/backend.tf
   git commit -m "infra: ativar Terraform Cloud (bloco cloud)"
   git push
   ```

---

## 6. Passo 4 — ⭐ ONDE COLOCAR AS CREDENCIAIS DO LAB ⭐

**Esta é a parte que você perguntou.** As credenciais **NÃO vão em nenhum
arquivo do repositório** (nunca comite credenciais!). Elas vão no **cofre do
Terraform Cloud**, dentro do workspace.

### 6.1 — Pegue as credenciais no AWS Academy

1. No AWS Academy, clique em **AWS Details** → **AWS CLI** → **Show**.
2. Vai aparecer um bloco assim (são **3** valores):

   ```ini
   aws_access_key_id=ASIA...
   aws_secret_access_key=xxxxxxxx...
   aws_session_token=yyyyyyyy...      <- existe porque é Academy
   ```

### 6.2 — Cole no Terraform Cloud (Workspace → Variables)

1. No Terraform Cloud, abra o workspace `oficina-backend-infra`.
2. Menu lateral: **Variables**.
3. Em **Workspace variables**, clique **+ Add variable** e crie **3 variáveis**,
   todas com **Category = `Environment variable`** (NÃO "Terraform variable")
   e marque **Sensitive** em todas:

   | Key (nome exato)        | Value                         | Category    | Sensitive |
   |-------------------------|-------------------------------|-------------|-----------|
   | `AWS_ACCESS_KEY_ID`     | (o `aws_access_key_id`)        | Environment | ✅        |
   | `AWS_SECRET_ACCESS_KEY` | (o `aws_secret_access_key`)    | Environment | ✅        |
   | `AWS_SESSION_TOKEN`     | (o `aws_session_token`)        | Environment | ✅        |

> **Por que "Environment variable" e não "Terraform variable"?**
> O provider da AWS lê as credenciais das variáveis de ambiente do sistema
> (`AWS_ACCESS_KEY_ID` etc.). Marcar como *Environment* faz o Terraform Cloud
> exportar esses valores no ambiente onde ele roda o `apply`, exatamente como se
> você tivesse colocado no `~/.aws/credentials`.

> 🔁 **Toda vez que abrir um novo lab do Academy:** volte nesta tela e
> **atualize os 3 valores** (as credenciais antigas expiram). É o único passo
> manual que sobra.

### 6.3 — Variáveis da aplicação (Terraform variables)

Ainda em **Variables**, adicione as variáveis do nosso Terraform como
**Category = `Terraform variable`** (estas NÃO são segredo, exceto a senha):

| Key                     | Value (exemplo)                                   | Sensitive |
|-------------------------|---------------------------------------------------|-----------|
| `aws_region`            | `us-west-2`                                       | não       |
| `lab_role_arn`          | `arn:aws:iam::SEU_ACCOUNT_ID:role/LabRole`        | não       |
| `access_entry_role_arn` | `arn:aws:iam::SEU_ACCOUNT_ID:role/voclabs`        | não       |
| `db_password`           | (uma senha forte que VOCÊ escolher)               | ✅        |
| `db_engine_version`     | `16`                                              | não       |

> Para descobrir o `SEU_ACCOUNT_ID`, rode no lab:
> `aws sts get-caller-identity --query Account --output text`
> ⚠️ **No Academy o Account ID pode mudar entre labs** — confira no começo de
> cada sessão e ajuste os ARNs de `lab_role_arn` e `access_entry_role_arn`.

> ⭐ **`access_entry_role_arn` = `voclabs` (NÃO `LabRole`).** No Academy você se
> autentica como a role **`voclabs`** (veja `assumed-role/voclabs/...` no
> `get-caller-identity`). Se a Access Entry for criada pra `LabRole`, o
> `kubectl` recusa com *"the server has asked for the client to provide
> credentials"* e você precisa criar a entry na mão. Apontando pra `voclabs`,
> o `kubectl` já funciona logo após o `update-kubeconfig`.

> *E o `create_state_backend`?* O default no código já é **`false`** (Academy não
> cria S3/DynamoDB, e o state fica no Terraform Cloud). Só adicione essa variável
> como `true` se estiver numa conta AWS normal e quiser o backend S3+DynamoDB.

> ℹ️ Não é mais necessário criar a variável `availability_zones`: o default do
> código já é `["us-west-2a", "us-west-2b"]`.

---

## 7. Passo 5 — Rodar a infra (o momento da automação)

Com tudo configurado, há duas formas de disparar:

### Forma A (recomendada) — pelo próprio Terraform Cloud (VCS)
1. Faça qualquer `git push` que toque em `infra/` (o push do Passo 3 já serve).
2. No Terraform Cloud, abra o workspace → aba **Runs**. Vai haver um run
   **"Planning"** em andamento.
3. Quando terminar o **plan**, revise o que será criado (VPC, EKS, RDS…).
4. Clique em **Confirm & Apply** → escreva um comentário → **Confirm Plan**.
5. Acompanhe o **apply** (leva ~15–20 min: EKS e RDS são lentos).

### Forma B (opcional) — pelo GitHub Actions
Já deixamos pronto o workflow `.github/workflows/infra.yml`. Para usá-lo:
1. Crie um **token** no Terraform Cloud: ícone do usuário → **User Settings** →
   **Tokens** → **Create an API token**. Copie o valor.
2. No GitHub: repositório → **Settings** → **Secrets and variables** →
   **Actions** → **New repository secret**:
   - **Name:** `TF_API_TOKEN`
   - **Secret:** (cole o token do Terraform Cloud)
3. Edite `.github/workflows/infra.yml` e troque `SUA_ORGANIZACAO_AQUI` pelo nome
   da sua organização.
4. No GitHub: aba **Actions** → workflow **"Infra (Terraform Cloud)"** →
   **Run workflow** → escolha `plan` (ou `apply`).

> As duas formas usam o **mesmo** workspace e as **mesmas** credenciais (do
> Passo 4). A Forma A é mais simples; a Forma B é útil se você quiser tudo
> dentro do GitHub.

---

## 8. Passo 6 — Conectar o kubectl e subir a aplicação

A infra (cluster) está pronta, mas a **aplicação** ainda precisa ser instalada
no cluster. Isso continua sendo feito com `kubectl` (ou pela esteira `cd.yml`):

```bash
# pega o comando pronto do output do Terraform Cloud (aba Outputs) OU rode:
aws eks update-kubeconfig --region us-west-2 --name oficina-dev
kubectl get nodes   # tem que listar os 2 nós Ready
```

### Subir a app apontando pro RDS (recomendado no Academy)

> ⚠️ **Não use `kubectl apply -f k8s/` cru no Academy.** Aquele diretório sobe um
> **Postgres dentro do cluster** (`postgres-*.yaml`) que depende do driver **EBS
> CSI** — e o EBS CSI precisa de **IRSA** (role via OIDC), que o Academy bloqueia.
> Resultado: o PVC fica `Pending` e a app entra em `CrashLoopBackOff`.
> A arquitetura da Fase 2 usa o **RDS** (criado pelo Terraform) — use o script
> abaixo, que aponta a app pro RDS e **não** sobe o Postgres no cluster.

**PowerShell (Windows):**
```powershell
# passe a MESMA senha que você usou na variável db_password do Terraform
./k8s/deploy-aws-academy.ps1 -DbPassword "<sua-senha-do-rds>"
```

**Bash (terminal do lab):**
```bash
DB_PASSWORD="<sua-senha-do-rds>" ./k8s/deploy-aws-academy.sh
```

> 🔐 Use a **mesma** senha que você definiu na variável `db_password` do Terraform.
> Para produção, passe também um `JWT_SECRET`/`ADMIN_PASSWORD` próprios (parâmetros
> `-JwtSecret`/`-AdminPassword` no PowerShell ou variáveis de ambiente no bash);
> sem isso, os scripts usam valores de **desenvolvimento** e emitem um aviso.

O script descobre o endpoint do RDS sozinho (evita o bug de host vazio), cria o
ConfigMap/Secret corretos e sobe Deployment + Service + HPA.

Depois pegue o endereço do LoadBalancer e acesse o Swagger:

```bash
kubectl get pods -n oficina -w           # espere 1/1 Running
kubectl get svc oficina-app -n oficina   # pegue o EXTERNAL-IP (ELB, ~2-4 min)
# acesse http://<EXTERNAL-IP>/swagger-ui/index.html
```

> **HPA / autoscaling no Academy:** o HPA já vem configurado (`k8s/hpa.yaml`,
> 2→5 réplicas a 70% CPU). Porém o `metrics-server` **não funciona no Academy**:
> o EKS não emite o certificado de *serving* do kubelet (os CSRs
> `kubelet-serving` ficam `Approved` mas nunca `Issued`), então `kubectl top` e o
> HPA não leem CPU. É limitação do ambiente, não do projeto. Para demonstrar o
> escalonamento ao vivo, use `kubectl scale deployment oficina-app -n oficina
> --replicas=4` (e volte pra 2). Em conta AWS normal o HPA escala sozinho.

> *Terraform Cloud cria a INFRA; quem instala a APP é o kubectl / GitHub Actions
> (cd.yml). São papéis diferentes.*

---

## 9. Passo 7 — Destruir a infra (economizar crédito do lab)

Quando terminar o teste, **destrua tudo** para não gastar crédito:

- **No Terraform Cloud:** workspace → **Settings** → **Destruction and
  Deletion** → **Queue destroy plan** → confirme.
- Isso roda um `terraform destroy` na nuvem e remove VPC, EKS, RDS, NAT etc.

> ⚠️ Faça isso **enquanto o lab ainda está ativo** (as credenciais precisam
> estar válidas para o destroy funcionar).

---

## 10. Resumo — o que mudou em relação ao Runbook manual

| Etapa                         | Antes (Runbook manual)              | Agora (Terraform Cloud)                         |
|-------------------------------|-------------------------------------|-------------------------------------------------|
| Onde rodam plan/apply         | Terminal do lab, na mão             | Nuvem da HashiCorp, via push/clique             |
| Onde fica o state             | Arquivo local `terraform.tfstate`   | Cofre do Terraform Cloud (não se perde)         |
| Onde ficam as credenciais AWS | `~/.aws/credentials` no lab         | Workspace → Variables (Environment, Sensitive)  |
| Disparo                       | Você digita os comandos             | `git push` (ou botão no GitHub Actions)         |
| O que ainda é manual          | Tudo                                | Só atualizar as 3 credenciais a cada novo lab   |
| Instalar a aplicação (k8s)    | `kubectl apply`                     | `kubectl apply` ou esteira `cd.yml` (igual)     |

---

## 11. Problemas comuns

- **"Error: No valid credential sources found" no apply:** as credenciais do lab
  expiraram. Volte ao **Passo 4.2** e atualize os 3 valores no workspace.
- **`UnauthorizedOperation` / `AccessDenied`:** você está numa região não
  liberada. No Academy use **`us-west-2`** (variable `aws_region`).
- **`Error: Required token could not be found` (GitHub Actions):** falta o secret
  `TF_API_TOKEN` no GitHub (Passo 5, Forma B).
- **Plan não dispara após o push:** confira no workspace → **Settings** →
  **Version Control** se o **Working Directory** está como `infra`.
- **`iam:CreateRole` negado:** o `lab_role_arn` não foi preenchido — sem ele o
  Terraform tenta criar IAM roles (proibido no Academy). Preencha no Passo 6.3.
- **`InvalidParameterValue: us-east-1a ... invalid` no apply:** os defaults de
  `aws_region`/`availability_zones` estão desalinhados. Já corrigido no código
  (defaults `us-west-2`); se persistir, confira a variável `aws_region` = `us-west-2`.
- **`kubectl`: "the server has asked for the client to provide credentials":** a
  Access Entry foi criada pra `LabRole`, mas você usa `voclabs`. Aponte
  `access_entry_role_arn` pra `voclabs` (Passo 6.3) ou crie a entry na mão:
  ```bash
  aws eks create-access-entry --cluster-name oficina-dev --region us-west-2 \
    --principal-arn arn:aws:iam::SEU_ACCOUNT_ID:role/voclabs --type STANDARD
  aws eks associate-access-policy --cluster-name oficina-dev --region us-west-2 \
    --principal-arn arn:aws:iam::SEU_ACCOUNT_ID:role/voclabs \
    --policy-arn arn:aws:eks::aws:cluster-access-policy/AmazonEKSClusterAdminPolicy \
    --access-scope type=cluster
  ```
- **App em `CrashLoopBackOff` (probe `:8080 connection refused`):** a app não
  conecta no banco no boot. Causas comuns:
  - **DB_URL com host vazio** (`jdbc:postgresql:///oficina`): no PowerShell,
    `$RDS:5432` é interpretado errado por causa do `:` — use `${RDS}:5432` (com
    chaves) ou rode o `deploy-aws-academy.ps1`, que já trata isso.
  - **Senha divergente:** o `DB_PASSWORD` do Secret precisa ser igual à
    `db_password` do Terraform (senha master do RDS).
  - Confira: `kubectl get cm oficina-config -n oficina -o jsonpath="{.data.DB_URL}"`.
- **Pods do `oficina-db` em `Pending` / EBS CSI `CrashLoopBackOff`:** EBS CSI
  precisa de IRSA (bloqueado no Academy). Não use o Postgres no cluster — use o
  RDS via `deploy-aws-academy.ps1` (não aplique `k8s/postgres-*.yaml`).
- **`kubectl logs/top`: `tls: internal error` e HPA `cpu: <unknown>`:** o EKS do
  Academy não emite o cert de serving do kubelet. Sem solução simples no lab;
  para o vídeo, demonstre escalonamento com `kubectl scale`.
