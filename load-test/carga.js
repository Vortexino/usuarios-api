// Actividad 1 (Linea Base) y Actividad 2 (Incremento Progresivo).
//
// Uso:
//   k6 run -e BASE_URL=https://xxxx.execute-api.us-east-2.amazonaws.com \
//          -e VUS=10 -e DURATION=1m load-test/carga.js
//
// Corre una vez por cada fila de la tabla:
//   VUS=10   DURATION=1m   (linea base)
//   VUS=50   DURATION=2m
//   VUS=100  DURATION=2m
//   VUS=250  DURATION=2m
//   VUS=500  DURATION=2m
//
// Al final del resumen de k6 (stdout) estan: avg/min/max de http_req_duration,
// p(95)/p(99), http_reqs (para requests/segundo) y http_req_failed (% error).
// Guarda la salida de cada corrida, por ejemplo:
//   k6 run -e VUS=50 -e DURATION=2m load-test/carga.js | tee resultados/carga_50vus.txt

import { sleep, check } from 'k6';
import { Rate } from 'k6/metrics';
import { autenticar, enviarNotificacion } from './common.js';

export const erroresNotificacion = new Rate('errores_notificacion');

export const options = {
  scenarios: {
    carga_constante: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS) || 10,
      duration: __ENV.DURATION || '1m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<5000'],
  },
  summaryTrendStats: ['avg', 'min', 'max', 'med', 'p(95)', 'p(99)'],
};

// setup() corre una sola vez, no por VU: todos los VUs reusan el mismo token.
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
