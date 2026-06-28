#!/usr/bin/env bash
# Deploy da aplicacao no EKS do AWS Academy usando o RDS (banco gerenciado).
#
# No AWS Academy o Postgres dentro do cluster NAO funciona: o driver EBS CSI
# precisa de IRSA (role via OIDC), que o lab bloqueia, entao o PVC nunca provisiona.
# A arquitetura correta (e a da Fase 2) usa o RDS criado pelo Terraform.
#
# Este script descobre o endpoint do RDS, cria namespace/ConfigMap/Secret/Deployment/
# Service/HPA e NAO aplica os manifests postgres-*.yaml.
#
# Uso:
#   DB_PASSWORD="Oficina123!" ./deploy-aws-academy.sh
# Variaveis opcionais: REGION (default us-west-2), IMAGE (default GHCR latest)
set -euo pipefail

REGION="${REGION:-us-west-2}"
IMAGE="${IMAGE:-ghcr.io/tiagomiele/oficina-backend-fiap-fase2:latest}"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [[ -z "${DB_PASSWORD:-}" ]]; then
  echo "ERRO: defina DB_PASSWORD com a MESMA senha da variavel db_password do Terraform." >&2
  echo "Ex.: DB_PASSWORD='Oficina123!' ./deploy-aws-academy.sh" >&2
  exit 1
fi

echo "==> Descobrindo o endpoint do RDS..."
RDS="$(aws rds describe-db-instances --region "$REGION" --query 'DBInstances[0].Endpoint.Address' --output text)"
if [[ -z "$RDS" || "$RDS" == "None" ]]; then
  echo "ERRO: endpoint do RDS vazio. A instancia existe e as credenciais do lab estao validas?" >&2
  exit 1
fi
STATUS="$(aws rds describe-db-instances --region "$REGION" --query 'DBInstances[0].DBInstanceStatus' --output text)"
echo "    RDS: $RDS (status: $STATUS)"

kubectl apply -f "$HERE/namespace.yaml"

cat <<YAML | kubectl apply -f -
apiVersion: v1
kind: ConfigMap
metadata:
  name: oficina-config
  namespace: oficina
  labels:
    app.kubernetes.io/part-of: oficina-backend
data:
  DB_URL: "jdbc:postgresql://${RDS}:5432/oficina"
  DB_USER: "oficina"
  SERVER_PORT: "8080"
  SPRING_PROFILES_ACTIVE: ""
  ADMIN_EMAIL: "admin@oficina.local"
YAML

cat <<YAML | kubectl apply -f -
apiVersion: v1
kind: Secret
metadata:
  name: oficina-secrets
  namespace: oficina
  labels:
    app.kubernetes.io/part-of: oficina-backend
type: Opaque
stringData:
  DB_PASSWORD: "${DB_PASSWORD}"
  JWT_SECRET: "change-me-in-production-at-least-32-chars-long-please"
  ADMIN_PASSWORD: "admin123"
YAML

sed "s#image: oficina-backend:latest#image: ${IMAGE}#" "$HERE/app-deployment.yaml" | kubectl apply -f -
kubectl apply -f "$HERE/app-service.yaml"
kubectl apply -f "$HERE/hpa.yaml"

echo
echo "==> Deploy aplicado. Acompanhe:"
echo "    kubectl get pods -n oficina -w"
echo "    kubectl get svc oficina-app -n oficina   # pegue o EXTERNAL-IP (ELB)"
echo "    depois: http://<EXTERNAL-IP>/swagger-ui/index.html"
