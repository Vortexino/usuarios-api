#!/usr/bin/env bash
# Actividad 3 (monitoreo durante la prueba) y Actividad 5 (recuperacion).
#
# Uso:
#   ./monitorear.sh                 # corre hasta que lo detengas con Ctrl+C
#   ./monitorear.sh 300             # corre 300 segundos y se detiene sola
#
# Requiere AWS CLI configurado (aws configure) con permisos de lectura sobre
# CloudWatch, Lambda y SQS. Corre esto en TU máquina, en paralelo a k6 (otra
# terminal) para la Actividad 3, o justo despues de parar la carga para medir
# cuanto tarda en vaciarse la cola (Actividad 5).
#
# Ajusta estos 3 valores a tu entorno real:
REGION="us-east-2"
LAMBDA_PRINCIPAL="usuarios-api"
LAMBDA_NOTIFICACIONES="notification-lambda"
QUEUE_NAME="usuarios-api-notifications-queue"

set -u
QUEUE_URL=$(aws sqs get-queue-url --queue-name "$QUEUE_NAME" --region "$REGION" --query QueueUrl --output text 2>/dev/null)
if [ -z "$QUEUE_URL" ] || [ "$QUEUE_URL" == "None" ]; then
  echo "No pude resolver la URL de la cola '$QUEUE_NAME'. Revisa el nombre/region." >&2
  exit 1
fi

DURACION="${1:-0}"  # 0 = correr indefinidamente hasta Ctrl+C
INICIO=$(date +%s)

echo "timestamp,mensajes_visibles_sqs,mensajes_en_vuelo_sqs,ejecuciones_concurrentes_notif_lambda" | tee metricas.csv

while true; do
  AHORA=$(date +%s)
  if [ "$DURACION" != "0" ] && [ $((AHORA - INICIO)) -ge "$DURACION" ]; then
    break
  fi

  TS=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

  VISIBLES=$(aws sqs get-queue-attributes \
    --queue-url "$QUEUE_URL" \
    --attribute-names ApproximateNumberOfMessagesVisible ApproximateNumberOfMessagesNotVisible \
    --region "$REGION" \
    --query 'Attributes' --output json 2>/dev/null)

  MSG_VISIBLES=$(echo "$VISIBLES" | python3 -c "import json,sys; d=json.load(sys.stdin); print(d.get('ApproximateNumberOfMessagesVisible','?'))" 2>/dev/null)
  MSG_VUELO=$(echo "$VISIBLES" | python3 -c "import json,sys; d=json.load(sys.stdin); print(d.get('ApproximateNumberOfMessagesNotVisible','?'))" 2>/dev/null)

  CONCURRENCIA=$(aws cloudwatch get-metric-statistics \
    --namespace AWS/Lambda \
    --metric-name ConcurrentExecutions \
    --dimensions Name=FunctionName,Value="$LAMBDA_NOTIFICACIONES" \
    --start-time "$(date -u -d '-2 minutes' +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v-2M +%Y-%m-%dT%H:%M:%SZ)" \
    --end-time "$TS" \
    --period 60 \
    --statistics Maximum \
    --region "$REGION" \
    --query 'Datapoints[-1].Maximum' --output text 2>/dev/null)

  LINEA="$TS,$MSG_VISIBLES,$MSG_VUELO,$CONCURRENCIA"
  echo "$LINEA" | tee -a metricas.csv

  sleep 10
done

echo ""
echo "Listo. Datos guardados en metricas.csv"
echo "Para ver errores/throttles puntuales de cada Lambda:"
echo "  aws logs tail /aws/lambda/$LAMBDA_PRINCIPAL --region $REGION --since 10m"
echo "  aws logs tail /aws/lambda/$LAMBDA_NOTIFICACIONES --region $REGION --since 10m"
