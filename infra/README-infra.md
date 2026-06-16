# Infraestrutura — Terraform (AWS)

Provisionamento automatizado da infraestrutura para o **oficina-backend** na AWS usando Terraform.

## Arquitetura Provisionada

```
┌─────────────────────────────────────────────────────────┐
│                        AWS Cloud                        │
│                                                         │
│  ┌─────────────────── VPC 10.0.0.0/16 ───────────────┐  │
│  │                                                    │  │
│  │  ┌─── Subnet Pública ──┐  ┌─── Subnet Pública ──┐ │  │
│  │  │   us-east-1a         │  │   us-east-1b         │ │  │
│  │  │   NAT Gateway        │  │                      │ │  │
│  │  │   Internet Gateway   │  │                      │ │  │
│  │  └──────────────────────┘  └──────────────────────┘ │  │
│  │                                                    │  │
│  │  ┌─── Subnet Privada ──┐  ┌─── Subnet Privada ──┐ │  │
│  │  │   us-east-1a         │  │   us-east-1b         │ │  │
│  │  │                      │  │                      │ │  │
│  │  │  ┌── EKS Cluster ─────────────────────────┐   │ │  │
│  │  │  │  Worker Nodes (t3.medium, 2-5 nodes)   │   │ │  │
│  │  │  │  ┌──────────┐  ┌──────────┐            │   │ │  │
│  │  │  │  │oficina   │  │oficina   │            │   │ │  │
│  │  │  │  │app (2+)  │  │app (2+)  │            │   │ │  │
│  │  │  │  └──────────┘  └──────────┘            │   │ │  │
│  │  │  └────────────────────────────────────────┘   │ │  │
│  │  │                      │  │                      │ │  │
│  │  │  ┌── RDS PostgreSQL 16 ──┐                    │ │  │
│  │  │  │  db.t3.micro, 20 GB   │                    │ │  │
│  │  │  │  Backup 7 dias        │                    │ │  │
│  │  │  └───────────────────────┘                    │ │  │
│  │  └──────────────────────┘  └──────────────────────┘ │  │
│  └────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## Pré-requisitos

| Ferramenta | Versão Mínima | Instalação |
|---|---|---|
| Terraform | >= 1.5.0 | [terraform.io/downloads](https://developer.hashicorp.com/terraform/downloads) |
| AWS CLI | >= 2.x | [aws.amazon.com/cli](https://aws.amazon.com/cli/) |
| kubectl | >= 1.29 | [kubernetes.io/docs/tasks/tools](https://kubernetes.io/docs/tasks/tools/) |

Certifique-se de ter as credenciais AWS configuradas:
```bash
aws configure
# ou exporte as variáveis:
export AWS_ACCESS_KEY_ID="..."
export AWS_SECRET_ACCESS_KEY="..."
```

## Passo a Passo

### 1. Configurar variáveis

```bash
cd infra
cp terraform.tfvars.example terraform.tfvars
# Edite terraform.tfvars com seus valores (especialmente db_password)
```

### 2. Inicializar o Terraform

```bash
terraform init
```

### 3. Planejar (visualizar mudanças)

```bash
terraform plan
```

### 4. Aplicar (provisionar recursos)

```bash
terraform apply
```

Após a confirmação, o Terraform criará:
- 1 VPC com 2 subnets públicas e 2 privadas
- 1 Internet Gateway + 1 NAT Gateway
- 1 Cluster EKS (Kubernetes 1.29)
- 1 Node Group (2-5 instâncias t3.medium)
- 1 RDS PostgreSQL 16 (db.t3.micro, 20 GB)
- Security Groups, IAM Roles e Route Tables

### 5. Configurar kubectl

```bash
# Use o comando da saída do Terraform:
aws eks update-kubeconfig --region us-east-1 --name oficina-dev
```

### 6. Deployar a aplicação

```bash
# Atualizar o ConfigMap com o endpoint do RDS:
# kubectl edit configmap oficina-config -n oficina
# Alterar DB_URL para: jdbc:postgresql://<rds-endpoint>:5432/oficina

kubectl apply -f ../k8s/
```

## Recursos Criados

| Recurso | Tipo AWS | Descrição |
|---|---|---|
| VPC | `aws_vpc` | Rede isolada (10.0.0.0/16) |
| Subnets | `aws_subnet` | 2 públicas + 2 privadas (multi-AZ) |
| Internet Gateway | `aws_internet_gateway` | Acesso público |
| NAT Gateway | `aws_nat_gateway` | Saída internet para subnets privadas |
| EKS Cluster | `aws_eks_cluster` | Kubernetes 1.29 |
| Node Group | `aws_eks_node_group` | Worker nodes (t3.medium) |
| RDS | `aws_db_instance` | PostgreSQL 16, backup 7 dias |
| Security Groups | `aws_security_group` | Regras de firewall por componente |
| IAM Roles | `aws_iam_role` | Permissões para EKS e Nodes |

## Estimativa de Custos (us-east-1, mensal)

| Recurso | Custo Estimado |
|---|---|
| EKS Cluster | ~$73/mês |
| 2x t3.medium (nodes) | ~$60/mês |
| RDS db.t3.micro | ~$15/mês |
| NAT Gateway | ~$32/mês |
| **Total estimado** | **~$180/mês** |

> Para ambiente de desenvolvimento, pode-se reduzir custos usando `t3.small` para nodes e desligando recursos quando não estiverem em uso.

## Destruir Recursos

```bash
terraform destroy
```

> **Atenção**: Este comando destrói TODOS os recursos. Dados no RDS serão perdidos (skip_final_snapshot está habilitado para dev).

## Módulos

| Módulo | Diretório | Responsabilidade |
|---|---|---|
| **network** | `modules/network/` | VPC, Subnets, IGW, NAT, Route Tables |
| **eks** | `modules/eks/` | Cluster EKS, IAM Roles, Node Group, Security Groups |
| **rds** | `modules/rds/` | PostgreSQL 16, Subnet Group, Security Group |
