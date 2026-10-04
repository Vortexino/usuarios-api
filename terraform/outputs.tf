output "api_base_url" {
  description = "URL base de la API. Esta es la que va en BASE_URL de RetrofitClient.java"
  value       = aws_apigatewayv2_api.api.api_endpoint
}

output "lambda_function_name" {
  value = aws_lambda_function.api.function_name
}

output "cloudwatch_log_group" {
  value = aws_cloudwatch_log_group.lambda_logs.name
}

output "s3_bucket_name" {
  description = "Bucket donde quedan las fotos de perfil subidas"
  value       = aws_s3_bucket.uploads.bucket
}