variable "aws_region" {
  description = "Região AWS para provisionar os recursos"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Ambiente (dev, staging, prod)"
  type        = string
  default     = "dev"
}

variable "project_name" {
  description = "Nome do projeto (usado como prefixo nos recursos)"
  type        = string
  default     = "oficina"
}

# ---------- Rede ----------

variable "vpc_cidr" {
  description = "CIDR block da VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  description = "Zonas de disponibilidade"
  type        = list(string)
  default     = ["us-east-1a", "us-east-1b"]
}

# ---------- EKS ----------

variable "cluster_version" {
  description = "Versão do Kubernetes no EKS"
  type        = string
  default     = "1.29"
}

variable "node_instance_type" {
  description = "Tipo de instância EC2 para os worker nodes"
  type        = string
  default     = "t3.medium"
}

variable "node_desired_count" {
  description = "Número desejado de worker nodes"
  type        = number
  default     = 2
}

variable "node_min_count" {
  description = "Número mínimo de worker nodes"
  type        = number
  default     = 2
}

variable "node_max_count" {
  description = "Número máximo de worker nodes"
  type        = number
  default     = 5
}

# ---------- RDS ----------

variable "db_instance_class" {
  description = "Classe da instância RDS"
  type        = string
  default     = "db.t3.micro"
}

variable "db_name" {
  description = "Nome do banco de dados"
  type        = string
  default     = "oficina"
}

variable "db_username" {
  description = "Usuário master do RDS"
  type        = string
  default     = "oficina"
}

variable "db_password" {
  description = "Senha do banco de dados"
  type        = string
  sensitive   = true
}

variable "db_allocated_storage" {
  description = "Armazenamento alocado em GB"
  type        = number
  default     = 20
}
