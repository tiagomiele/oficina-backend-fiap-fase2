output "db_endpoint" {
  description = "Endpoint de conexão do RDS (host:porta)"
  value       = aws_db_instance.main.endpoint
}

output "db_instance_id" {
  description = "ID da instância RDS"
  value       = aws_db_instance.main.id
}

output "db_security_group_id" {
  description = "ID do security group do RDS"
  value       = aws_security_group.rds.id
}
