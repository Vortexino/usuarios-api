variable "aws_region" {
  description = "Región de AWS donde se despliega todo"
  type        = string
  default     = "us-east-2"
}

variable "project_name" {
  description = "Prefijo usado en el nombre de todos los recursos"
  type        = string
  default     = "usuarios-api"
}

variable "lambda_jar_path" {
  description = "Ruta local al jar 'fat jar' (classifier aws) que se sube a Lambda"
  type        = string
  default     = "../target/api-0.0.1-SNAPSHOT-aws.jar"
}

variable "lambda_memory_mb" {
  type    = number
  default = 512
}

variable "lambda_timeout_seconds" {
  description = "Spring Boot en cold start puede tardar varios segundos"
  type        = number
  default     = 30
}

variable "database_url" {
  description = "Connection string completo de la base de datos cloud, formato postgresql://usuario:password@host/db?..."
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  type      = string
  sensitive = true
}