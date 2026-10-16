import http from 'k6/http';
import { check } from 'k6';

// BASE_URL se pasa con -e BASE_URL=... (sin / al final). Por defecto usa el
// endpoint conocido del backend.
export const BASE_URL = __ENV.BASE_URL || 'https://jko0hy37uc.execute-api.us-east-2.amazonaws.com';

// Correo destino de la notificación. Debe estar verificado en SES si la
// cuenta sigue en modo sandbox, o notification-lambda fallará en el envío
// (aunque el endpoint responda 200 igual, porque SNS/SQS son asíncronos).
export const DEST_EMAIL = __ENV.DEST_EMAIL || 'test@test.com';

// Crea un usuario de prueba único por corrida y devuelve su token JWT.
// Se ejecuta una sola vez por test (setup), no por VU.
export function autenticar() {
  const matricula = `loadtest_${Date.now()}_${Math.floor(Math.random() * 100000)}`;
  const password = 'LoadTest123!';

  const registro = http.post(
    `${BASE_URL}/api/auth/register`,
    JSON.stringify({
      matricula,
      nombre: 'Usuario Carga',
      email: `${matricula}@loadtest.local`,
      password,
    }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  check(registro, { 'registro devolvio 201': (r) => r.status === 201 });

  const login = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ matricula, password }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  check(login, { 'login devolvio 200': (r) => r.status === 200 });

  const token = login.json('token');
  if (!token) {
    throw new Error(
      `No se pudo obtener token (registro=${registro.status}, login=${login.status}, body=${login.body})`
    );
  }
  return token;
}

// Un envio de notificacion con el token ya obtenido.
export function enviarNotificacion(token) {
  const payload = JSON.stringify({
    email: DEST_EMAIL,
    subject: 'Prueba',
    message: 'Mensaje de carga',
  });

  return http.post(`${BASE_URL}/api/notifications/send`, payload, {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
  });
}
