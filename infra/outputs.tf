output "vpc_id" {
  description = "ID da VPC criada"
  value       = module.network.vpc_id
}

output "eks_cluster_name" {
  description = "Nome do cluster EKS"
  value       = module.eks.cluster_name
}

output "eks_cluster_endpoint" {
  description = "Endpoint do cluster EKS"
  value       = module.eks.cluster_endpoint
}

output "eks_kubeconfig_command" {
  description = "Comando para configurar o kubectl"
  value       = "aws eks update-kubeconfig --region ${var.aws_region} --name ${module.eks.cluster_name}"
}

output "rds_endpoint" {
  description = "Endpoint de conexão do RDS PostgreSQL"
  value       = module.rds.db_endpoint
}

output "rds_connection_url" {
  description = "URL JDBC para a aplicação"
  value       = "jdbc:postgresql://${module.rds.db_endpoint}/${var.db_name}"
  sensitive   = true
}
