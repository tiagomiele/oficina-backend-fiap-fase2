variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "subnet_ids" {
  type = list(string)
}

variable "db_instance_class" {
  type = string
}

variable "db_name" {
  type = string
}

variable "db_username" {
  type = string
}

variable "db_password" {
  type      = string
  sensitive = true
}

variable "allocated_storage" {
  type = number
}

variable "eks_security_group" {
  description = "Security group dos worker nodes do EKS (acesso ao RDS)"
  type        = string
}

variable "eks_cluster_security_group" {
  description = "Security group gerenciado pelo EKS, efetivamente usado pelos nodes (acesso ao RDS)"
  type        = string
}
