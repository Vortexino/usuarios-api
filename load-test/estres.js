// Actividad 4 (Prueba de Estrés).
//
// Sube la carga por etapas hasta encontrar el punto de degradacion. Se
// detiene SOLA (abortOnFail) si:
//   - mas de 5% de errores (http_req_failed >= 0.05)
//   - latencia p95 > 5s (http_req_duration >= 5000ms)
// El throttling de Lambda y la saturacion de SQS no se pueden detectar desde
// el cliente: hay que mirarlos en CloudWatch en paralelo (ver README.md).
//
// Uso:
//   k6 run -e BASE_URL=https://xxxx.execute-api.us-east-2.amazonaws.com load-test/estres.js
//
// AVISO DE COSTO: esto manda miles de requests/min contra Lambda + API
// Gateway + SNS + SQS + otra Lambda, todo pago-por-uso. No lo corras sin
// una alarma de presupuesto activa en AWS Budgets. Si quieres parar antes
// de que termine solo, Ctrl+C detiene el test de inmediato.

import { sleep, check } from 'k6';
import { Rate } from 'k6/metrics';
import { autenticar, enviarNotificacion } from './common.js';

export const erroresNotificacion = new Rate('errores_notificacion');

export const options = {
  scenarios: {
    estres_progresivo: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 100 },
        { duration: '2m', target: 250 },
        { duration: '2m', target: 500 },
        { duration: '2m', target: 750 },
        { duration: '2m', target: 1000 },
        { duration: '2m', target: 1500 },
        { duration: '2m', target: 2000 },
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    http_req_failed: [{ threshold: 'rate<0.05', abortOnFail: true }],
    http_req_duration: [{ threshold: 'p(95)<5000', abortOnFail: true }],
  },
  summaryTrendStats: ['avg', 'min', 'max', 'med', 'p(95)', 'p(99)'],
};

export function setup() {
  const token = autenticar();
  return { token };
}

export default function (data) {
  const res = enviarNotificacion(data.token);

  const ok = check(res, {
    'status es 200': (r) => r.status === 200,
  });
  erroresNotificacion.add(!ok);

  sleep(1);
}
