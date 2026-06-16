variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

variable "cluster_version" {
  type = string
}

variable "subnet_ids" {
  type = list(string)
}

variable "vpc_id" {
  type = string
}

variable "node_instance_type" {
  type = string
}

variable "node_desired_count" {
  type = number
}

variable "node_min_count" {
  type = number
}

variable "node_max_count" {
  type = number
}

variable "lab_role_arn" {
  description = "ARN de uma IAM role pré-existente a ser reutilizada (ex.: LabRole no AWS Academy). Vazio = o módulo cria as roles."
  type        = string
  default     = ""
}
