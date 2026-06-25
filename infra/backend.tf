# ─── Backend remoto (S3) — igual ao modelo da aula ───
#
# O state do Terraform fica num bucket S3 (com lock no DynamoDB), permitindo
# trabalho em equipe e evitando perda do state local.
#
# IMPORTANTE — ordem de bootstrap (o bucket precisa existir ANTES do backend):
#   1) Deixe este bloco COMENTADO no primeiro apply.
#   2) Rode `terraform init` e `terraform apply` para criar o bucket/lock
#      definidos em bucket.tf (state ainda é local nesse momento).
#   3) DESCOMENTE o bloco abaixo, ajuste `bucket`/`region` se necessário.
#   4) Rode `terraform init -migrate-state` para mover o state local para o S3.
#
# Os detalhes estão no documento GUIA-DEPLOY-INFRA-TERRAFORM.md.

# terraform {
#   backend "s3" {
#     bucket         = "oficina-terraform-state-CHANGE_ME"
#     key            = "oficina-backend/terraform.tfstate"
#     region         = "us-east-1"
#     dynamodb_table = "oficina-terraform-locks"
#     encrypt        = true
#   }
# }
