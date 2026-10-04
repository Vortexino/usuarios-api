# ============================================================
# Parseo del único secret DATABASE_URL en sus partes (host, db,
# usuario, password)
# ============================================================
locals {
  db_parts = regex(
    "postgresql://(?P<user>[^:]+):(?P<pass>[^@]+)@(?P<host>[^/]+)/(?P<db>[^?]+)",
    var.database_url
  )

  jdbc_url = "jdbc:postgresql://${local.db_parts.host}/${local.db_parts.db}?sslmode=require"
}

# ============================================================
# S3: bucket para las fotos de perfil subidas desde la app
# ============================================================
data "aws_caller_identity" "current" {}

resource "aws_s3_bucket" "uploads" {
  bucket = "${var.project_name}-uploads-${data.aws_caller_identity.current.account_id}"
}

resource "aws_s3_bucket_public_access_block" "uploads" {
  bucket = aws_s3_bucket.uploads.id

  block_public_acls       = false
  block_public_policy     = false
  ignore_public_acls      = false
  restrict_public_buckets = false
}

resource "aws_s3_bucket_policy" "uploads_public_read" {
  bucket = aws_s3_bucket.uploads.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Sid       = "PublicReadUploads"
      Effect    = "Allow"
      Principal = "*"
      Action    = "s3:GetObject"
      Resource  = "${aws_s3_bucket.uploads.arn}/uploads/*"
    }]
  })

resource "aws_s3_bucket" "lambda_artifacts" {
  bucket = "${var.project_name}-lambda-artifacts-${data.aws_caller_identity.current.account_id}"
}

resource "aws_s3_object" "lambda_jar" {
  bucket = aws_s3_bucket.lambda_artifacts.id
  key    = "lambda.jar"
  source = var.lambda_jar_path
  etag   = filemd5(var.lambda_jar_path)
}

  depends_on = [aws_s3_bucket_public_access_block.uploads]
}

# ============================================================
# IAM: rol que la función Lambda asume para ejecutarse
# ============================================================
resource "aws_iam_role" "lambda_exec" {
  name = "${var.project_name}-lambda-exec-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "lambda.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "lambda_logs" {
  role       = aws_iam_role.lambda_exec.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_role_policy" "lambda_s3_access" {
  name = "${var.project_name}-lambda-s3-access"
  role = aws_iam_role.lambda_exec.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["s3:PutObject", "s3:GetObject"]
      Resource = "${aws_s3_bucket.uploads.arn}/uploads/*"
    }]
  })
}

# ============================================================
# CloudWatch: grupo de logs de la función
# ============================================================
resource "aws_cloudwatch_log_group" "lambda_logs" {
  name              = "/aws/lambda/${var.project_name}"
  retention_in_days = 14
}

# ============================================================
# Lambda: la función en sí
# ============================================================
resource "aws_lambda_function" "api" {
  function_name = var.project_name
  role          = aws_iam_role.lambda_exec.arn
  handler       = "com.uni.api.StreamLambdaHandler::handleRequest"
  runtime       = "java21"
  memory_size   = var.lambda_memory_mb
  timeout       = var.lambda_timeout_seconds

  s3_bucket         = aws_s3_bucket.lambda_artifacts.id
  s3_key            = aws_s3_object.lambda_jar.key
  source_code_hash  = filebase64sha256(var.lambda_jar_path)

  environment {
    variables = {
      SPRING_PROFILES_ACTIVE     = "lambda"
      SPRING_DATASOURCE_URL      = local.jdbc_url
      SPRING_DATASOURCE_USERNAME = local.db_parts.user
      SPRING_DATASOURCE_PASSWORD = local.db_parts.pass
      JWT_SECRET                 = var.jwt_secret
      APP_S3_BUCKET_NAME         = aws_s3_bucket.uploads.bucket
      APP_S3_REGION              = var.aws_region
    }
  }

  depends_on = [
    aws_iam_role_policy_attachment.lambda_logs,
    aws_cloudwatch_log_group.lambda_logs,
  ]
}

# ============================================================
# API Gateway: HTTP API
# ============================================================
resource "aws_apigatewayv2_api" "api" {
  name          = "${var.project_name}-gateway"
  protocol_type = "HTTP"

  cors_configuration {
    allow_origins = ["*"]
    allow_methods = ["GET", "POST", "PUT", "DELETE", "OPTIONS"]
    allow_headers = ["*"]
  }
}

resource "aws_apigatewayv2_integration" "lambda" {
  api_id                 = aws_apigatewayv2_api.api.id
  integration_type       = "AWS_PROXY"
  integration_uri        = aws_lambda_function.api.invoke_arn
  payload_format_version = "2.0"
}

resource "aws_apigatewayv2_route" "catch_all" {
  api_id    = aws_apigatewayv2_api.api.id
  route_key = "ANY /{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.lambda.id}"
}

resource "aws_apigatewayv2_route" "root" {
  api_id    = aws_apigatewayv2_api.api.id
  route_key = "ANY /"
  target    = "integrations/${aws_apigatewayv2_integration.lambda.id}"
}

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.api.id
  name        = "$default"
  auto_deploy = true
}

resource "aws_lambda_permission" "apigw" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.api.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.api.execution_arn}/*/*"
}