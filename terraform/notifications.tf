# ============================================================
# SNS: topic al que el backend publica cuando alguien pide
# enviar una notificación por correo
# ============================================================
resource "aws_sns_topic" "notifications" {
  name = "${var.project_name}-notifications"
}

# ============================================================
# SQS: cola que recibe los mensajes del topic SNS y los deja
# esperando a que la lambda de notificaciones los procese
# ============================================================
resource "aws_sqs_queue" "notifications" {
  name                       = "${var.project_name}-notifications-queue"
  visibility_timeout_seconds = 60  # debe ser >= al timeout de la lambda que la consume
}

# Policy que le da permiso a SNS de escribir mensajes en esta cola
resource "aws_sqs_queue_policy" "allow_sns" {
  queue_url = aws_sqs_queue.notifications.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Sid       = "AllowSNSPublish"
      Effect    = "Allow"
      Principal = { Service = "sns.amazonaws.com" }
      Action    = "sqs:SendMessage"
      Resource  = aws_sqs_queue.notifications.arn
      Condition = {
        ArnEquals = { "aws:SourceArn" = aws_sns_topic.notifications.arn }
      }
    }]
  })
}

# ============================================================
# IAM: rol que la Lambda de notificaciones asume para ejecutarse
# ============================================================
resource "aws_iam_role" "notification_lambda_exec" {
  name = "${var.project_name}-notification-lambda-exec-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "lambda.amazonaws.com" }
    }]
  })
}

# Permiso básico para escribir logs en CloudWatch
resource "aws_iam_role_policy_attachment" "notification_lambda_logs" {
  role       = aws_iam_role.notification_lambda_exec.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

# Permiso para leer/borrar mensajes de la cola SQS
resource "aws_iam_role_policy_attachment" "notification_lambda_sqs" {
  role       = aws_iam_role.notification_lambda_exec.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaSQSQueueExecutionRole"
}

# Permiso para enviar correos con SES
resource "aws_iam_role_policy" "notification_lambda_ses" {
  name = "${var.project_name}-notification-lambda-ses"
  role = aws_iam_role.notification_lambda_exec.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["ses:SendEmail", "ses:SendRawEmail"]
      Resource = "*"
    }]
  })
}

# ============================================================
# CloudWatch: grupo de logs de la Lambda de notificaciones
# ============================================================
resource "aws_cloudwatch_log_group" "notification_lambda_logs" {
  name              = "/aws/lambda/notification-lambda"
  retention_in_days = 14
}

# ============================================================
# Lambda: función de notificaciones
# ============================================================
resource "aws_lambda_function" "notifications" {
  function_name = "notification-lambda"
  role          = aws_iam_role.notification_lambda_exec.arn
  handler       = "com.uni.api.NotificationLambdaHandler::handleRequest"
  runtime       = "java21"
  memory_size   = 512
  timeout       = 30
  publish       = true

  s3_bucket        = aws_s3_bucket.lambda_artifacts.id
  s3_key           = aws_s3_object.lambda_jar.key
  source_code_hash = filebase64sha256(var.lambda_jar_path)

  environment {
    variables = {
      SES_SENDER_EMAIL = var.ses_sender_email
    }
  }

  depends_on = [
    aws_iam_role_policy_attachment.notification_lambda_logs,
    aws_iam_role_policy_attachment.notification_lambda_sqs,
    aws_cloudwatch_log_group.notification_lambda_logs,
  ]
}

# ============================================================
# Event Source Mapping: conecta la cola SQS con la Lambda,
# así cada mensaje que llegue a la cola dispara la Lambda
# ============================================================
resource "aws_lambda_event_source_mapping" "sqs_to_notification_lambda" {
  event_source_arn = aws_sqs_queue.notifications.arn
  function_name    = aws_lambda_function.notifications.arn
  batch_size       = 5
  enabled          = true
}

# Permiso para que la Lambda principal pueda publicar mensajes
# en el topic SNS de notificaciones
resource "aws_iam_role_policy" "lambda_sns_publish" {
  name = "${var.project_name}-lambda-sns-publish"
  role = aws_iam_role.lambda_exec.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = "sns:Publish"
      Resource = aws_sns_topic.notifications.arn
    }]
  })
}

resource "aws_sns_topic_subscription" "notifications_to_sqs" {
  topic_arn            = aws_sns_topic.notifications.arn
  protocol              = "sqs"
  endpoint              = aws_sqs_queue.notifications.arn
  raw_message_delivery  = true
}