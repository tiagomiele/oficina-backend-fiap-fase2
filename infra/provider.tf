terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Backend remoto (S3) — descomentar para uso em equipe
  # backend "s3" {
  #   bucket         = "oficina-terraform-state"
  #   key            = "oficina-backend/terraform.tfstate"
  #   region         = "us-east-1"
  #   dynamodb_table = "oficina-terraform-locks"
  #   encrypt        = true
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "oficina-backend"
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}
