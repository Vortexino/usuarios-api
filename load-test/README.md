# Pruebas de carga — notification-lambda

## 0. Antes de correr nada

1. Instala k6: https://grafana.com/docs/k6/latest/set-up/install-k6/
2. Instala y configura AWS CLI (`aws configure`) con credenciales que tengan
   permiso de lectura sobre CloudWatch, Lambda y SQS.
3. **Pon una alarma de presupuesto en AWS Budgets** si no la tienes — las
   Actividades 4 (estrés) y 2 (500 VUs) generan miles de invocaciones reales
   contra Lambda/API Gateway/SNS/SQS/SES.
4. Verifica en la consola de SES (región us-east-2) que el correo que vas a
   usar como `DEST_EMAIL` esté en "Verified identities", o
   `notification-lambda` va a fallar el envío en el 100% de los casos
   (verás eso en CloudWatch como `Errors`, aunque la API responda 200 OK).
5. Edita `monitorear.sh`: confirma `LAMBDA_PRINCIPAL`, `LAMBDA_NOTIFICACIONES`
   y `QUEUE_NAME` contra los nombres reales en tu cuenta AWS.

Todos los comandos de abajo asumen que estás parado en esta carpeta
(`load-test/`).

## Actividad 1 — Línea base

```
k6 run -e BASE_URL=https://TU-API.execute-api.us-east-2.amazonaws.com \
       -e VUS=10 -e DURATION=1m \
       carga.js | tee resultados_linea_base.txt
```

Del resumen de k6 al final, anota: `http_req_duration` (avg/min/max),
`http_reqs` (÷ duración = requests/seg) y `http_req_failed` (% error).

## Actividad 2 — Incremento progresivo

Corre esto 4 veces, una por fila, guardando cada salida:

```
k6 run -e VUS=50  -e DURATION=2m carga.js | tee resultados_50vus.txt
k6 run -e VUS=100 -e DURATION=2m carga.js | tee resultados_100vus.txt
k6 run -e VUS=250 -e DURATION=2m carga.js | tee resultados_250vus.txt
k6 run -e VUS=500 -e DURATION=2m carga.js | tee resultados_500vus.txt
```

k6 ya reporta `p(95)` y `p(99)` en el resumen (los agregué en
`summaryTrendStats`) — no hace falta calcularlos a mano.

## Actividad 3 — Monitoreo AWS

En otra terminal, **mientras corre cada prueba de arriba**:

```
./monitorear.sh
```

(Ctrl+C para detenerlo). Esto te da cada 10s: mensajes visibles/en vuelo en
SQS y concurrencia de `notification-lambda`, guardado en `metricas.csv`.

Para lo que el script no cubre (API Gateway, Lambda principal, SNS), estos
comandos puntuales después de cada corrida:

```
# API Gateway: conteo y latencia
aws cloudwatch get-metric-statistics --namespace AWS/ApiGateway \
  --metric-name Count --dimensions Name=ApiName,Value=usuarios-api-gateway \
  --start-time $(date -u -d '-10 minutes' +%Y-%m-%dT%H:%M:%SZ) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%SZ) --period 60 --statistics Sum \
  --region us-east-2

aws cloudwatch get-metric-statistics --namespace AWS/ApiGateway \
  --metric-name Latency --dimensions Name=ApiName,Value=usuarios-api-gateway \
  --start-time $(date -u -d '-10 minutes' +%Y-%m-%dT%H:%M:%SZ) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%SZ) --period 60 --statistics Average \
  --region us-east-2

# Lambda principal: invocaciones, duracion, errores, concurrencia
for METRICA in Invocations Duration Errors ConcurrentExecutions; do
  echo "== $METRICA (usuarios-api) =="
  aws cloudwatch get-metric-statistics --namespace AWS/Lambda \
    --metric-name "$METRICA" --dimensions Name=FunctionName,Value=usuarios-api \
    --start-time $(date -u -d '-10 minutes' +%Y-%m-%dT%H:%M:%SZ) \
    --end-time $(date -u +%Y-%m-%dT%H:%M:%SZ) --period 60 \
    --statistics Sum Maximum --region us-east-2
done

# SNS: mensajes publicados
aws cloudwatch get-metric-statistics --namespace AWS/SNS \
  --metric-name NumberOfMessagesPublished \
  --dimensions Name=TopicName,Value=usuarios-api-notifications \
  --start-time $(date -u -d '-10 minutes' +%Y-%m-%dT%H:%M:%SZ) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%SZ) --period 60 --statistics Sum \
  --region us-east-2

# notification-lambda: throttles (ademas de lo que ya trae monitorear.sh)
aws cloudwatch get-metric-statistics --namespace AWS/Lambda \
  --metric-name Throttles --dimensions Name=FunctionName,Value=notification-lambda \
  --start-time $(date -u -d '-10 minutes' +%Y-%m-%dT%H:%M:%SZ) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%SZ) --period 60 --statistics Sum \
  --region us-east-2

# Logs crudos de ambas lambdas
aws logs tail /aws/lambda/usuarios-api --region us-east-2 --since 10m
aws logs tail /aws/lambda/notification-lambda --region us-east-2 --since 10m
```

(En macOS/BSD, `date -u -d '-10 minutes' ...` se escribe
`date -u -v-10M ...`.)

## Actividad 4 — Prueba de estrés

```
./monitorear.sh &         # monitoreo en paralelo, en segundo plano
k6 run estres.js | tee resultados_estres.txt
```

El script se detiene solo si el error rate pasa de 5% o la p95 pasa de 5s
(son los dos criterios de parada que puede detectar un cliente HTTP). Los
otros dos criterios del enunciado —throttling en Lambda y saturación de
SQS— los ves en `metricas.csv` y en las métricas de `Throttles` de arriba;
si aparecen antes de que k6 aborte solo, para el test con Ctrl+C y anota en
qué etapa (cuántos VUs) ocurrió.

## Actividad 5 — Prueba de recuperación

1. Lanza una carga alta y sostenida, por ejemplo:
   ```
   k6 run -e VUS=1000 -e DURATION=2m carga.js
   ```
2. En el momento en que esa corrida termine (o la detengas con Ctrl+C),
   corre inmediatamente:
   ```
   ./monitorear.sh 600
   ```
   Esto registra cada 10s, durante 10 minutos, cómo baja
   `ApproximateNumberOfMessagesVisible` en SQS y la concurrencia de
   `notification-lambda` — esos dos números volviendo a 0 son tu "tiempo de
   recuperación".
3. Revisa en CloudWatch Logs si hubo throttles o errores residuales mientras
   la cola se vaciaba.

## Qué mirar para responder las preguntas de análisis

- **Máximo de usuarios concurrentes soportados**: la última etapa de
  `estres.js` que corrió completa antes de que abortara (o antes de que
  aparecieran errores/latencia/throttling en `metricas.csv`).
- **Cuello de botella**: compara dónde aparece el primer síntoma —
  ¿`http_req_duration` sube antes de que `ConcurrentExecutions` de
  `notification-lambda` se acerque a su límite de cuenta (1000 por defecto en
  cuentas nuevas)? ¿O sube `ApproximateNumberOfMessagesVisible` en SQS
  (la cola se acumula porque la Lambda de notificaciones no la vacía al
  mismo ritmo)? Eso te dice si el límite está en API Gateway/Lambda
  principal o en el consumidor SQS→Lambda.
- **¿SNS perdió mensajes?**: compara `NumberOfMessagesPublished` (SNS) contra
  `NumberOfMessagesSent` (SQS, lo ves con
  `aws cloudwatch get-metric-statistics --namespace AWS/SQS --metric-name NumberOfMessagesSent ...`).
  Si no coinciden, ahí está la pérdida.
