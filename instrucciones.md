# Instrucciones para ejecutar y probar cada componente

## 0. Requisitos
- Docker Desktop (o Docker Engine) con **Compose v2** → `docker compose version`
- Postman
- Recomendado: **8 GB de RAM libres** para Docker (son 14 contenedores). Si tu PC tiene poca memoria: Docker Desktop → Settings → Resources → Memory ≥ 6 GB.
- (Opcional) `curl` y `jq` para el script de pruebas.

## 1. Levantar todo
```bash
cd EFT_S9_Dominique_Hernandez_
cp .env.example .env          # (opcional) cambiar secretos
docker compose up -d --build  # la primera vez tarda varios minutos (descarga Maven)
docker compose ps             # esperar hasta que todos estén "healthy" (~2-3 min)
```
> Los servicios arrancan en orden: Eureka y Config → Auth/Kafka → Clientes/Cuentas/Pagos/Batch → Gateway → BFF.
> Después de que todo esté *healthy* espera ~15 segundos para que Eureka propague el registro.

| Componente | URL |
|---|---|
| Eureka (panel) | http://localhost:8761 |
| Config Server | http://localhost:8888/batch-service/default |
| Auth Server | http://localhost:9000 |
| API Gateway | http://localhost:8080 |
| BFF Web / Móvil / ATM | http://localhost:8081 / :8082 / :8083 |
| Notificaciones | http://localhost:8084 |
| Batch (directo) | http://localhost:8105 |
| Kafka UI | http://localhost:8090 |

## 2. Importar Postman
1. Importar `postman/BancoXYZ_EFT_S9.postman_collection.json` y `postman/BancoXYZ_Local.postman_environment.json`.
2. Seleccionar el environment **Banco XYZ - Local / Docker**.
3. Ejecutar primero la carpeta **00** (guarda los tokens). Luego las demás, en orden (cada request trae tests automáticos).
4. La carpeta **10** (prueba de fallo) se ejecuta **solo** con `cuentas-service` detenido (ver sección 8). No la incluyas en *Run collection*.

## 3. Probar cada componente

### 3.1 Seguridad OAuth2 (`auth-server`) – carpetas 00 a 04
Token manual:
```bash
curl -s -u atm-terminal:atm-terminal-secret -d "grant_type=client_credentials&scope=atm.operate" http://localhost:9000/oauth2/token
```
Resultado esperado: sin token → **401**; token del canal equivocado → **403**; token correcto → **200**.

### 3.2 Eureka y Config Server – carpeta 09
- Abrir http://localhost:8761: deben aparecer `API-GATEWAY`, `CLIENTES-SERVICE`, `CUENTAS-SERVICE`, `PAGOS-SERVICE`, `BATCH-SERVICE`.
- Abrir http://localhost:8888/batch-service/default: muestra las tasas y parámetros del batch.

### 3.3 Microservicios de negocio (vía API Gateway) – carpetas 01 y 08
Necesitan `token_full` (carpeta 00).
| Servicio | Prueba |
|---|---|
| Clientes | `GET http://localhost:8080/api/legacy/clientes/1` · `POST /api/legacy/clientes` |
| Cuentas | `GET /api/legacy/cuentas/CTA-0001` · `POST /api/legacy/cuentas` (apertura) · `PUT /api/legacy/cuentas/{n}/estado` |
| Pagos | `POST /api/pagos/deposito` · `/api/pagos/pago` · `/api/pagos/transferencia` · `GET /api/legacy/transacciones/cuenta/CTA-0001` |

Regla de negocio desde el Config Server: una transferencia sobre $5.000.000 devuelve **422**.
Consistencia: una transferencia a una cuenta inexistente devuelve **404** y el dinero **vuelve** a la cuenta origen (compensación).

### 3.4 BFF – carpetas 02, 03 y 04
Web: `GET :8081/web/clientes/1/dashboard` · Móvil: `GET :8082/mobile/cuentas/CTA-0001/resumen` · Cajero: `GET :8083/atm/cuentas/CTA-0001/saldo` y `POST :8083/atm/cuentas/CTA-0001/retiro` con `{"monto":50000}`.

### 3.5 Kafka – carpeta 05
1. Haz un retiro de $50.000 y otro de $250.000 (carpeta 04).
2. `GET :8084/notifications` → una notificación `INFO` y una `ALERTA_MONTO_ALTO`.
3. Kafka UI (http://localhost:8090) → Topics → `banking.transacciones` → Messages.
4. Logs: `docker compose logs -f pagos-service notification-service`

### 3.6 Spring Batch – carpeta 07
```bash
# (con token_full) ejecutar los 3 procesos
POST http://localhost:8080/batch/jobs/transacciones-diarias/ejecutar?semana=semana_3
POST http://localhost:8080/batch/jobs/intereses-trimestrales/ejecutar?semana=semana_3
POST http://localhost:8080/batch/jobs/estados-anuales/ejecutar?semana=semana_3
```
La respuesta muestra `estado: COMPLETED`, y por paso: **leídos / escritos / omitidos** (ver tabla de resultados esperados en `readme.md`).
Luego:
- `GET /batch/reportes/{proceso}` → resumen calculado
- `GET /batch/rechazados/{proceso}?limite=10` → filas rechazadas con su motivo
- CSV generados: `docker compose exec batch-service ls -l /app/output`
- Logs del job: `docker compose logs batch-service`
- Archivos pequeños para probar rápido: `?semana=semana_1` o `semana_2`.

### 3.7 Escalabilidad horizontal
```bash
docker compose up -d --scale clientes-service=2
```
En Eureka aparece `CLIENTES-SERVICE` con **2 instancias**. Llama varias veces a `GET :8080/api/legacy/clientes/1` y observa en `docker compose logs clientes-service` cómo ambas instancias reciben tráfico (balanceo del gateway).

## 4. Pruebas automáticas con Postman
*Run collection* sobre las carpetas 00 a 09 (sin la 10). Todas las pruebas deben quedar en verde.

## 5. Script de consola (para la evidencia)
```bash
bash evidencia-ejecucion/comandos-prueba.sh | tee evidencia-ejecucion/salida-consola.txt
```
Guía de capturas: `evidencia-ejecucion/README.md`.

## 6. Resiliencia (Resilience4j) – carpetas 06 y 10
```bash
docker stop cuentas-service          # simular caída
# Postman carpeta 10 "PRUEBA DE FALLO" (repetir 6+ veces) -> 503, luego "circuit breaker ABIERTO"
# estado: GET :8083/actuator/circuitbreakers  (con token)
docker start cuentas-service         # recuperación: el circuito pasa a HALF_OPEN y luego CLOSED (~15-45 s)
```

## 7. Detener
```bash
docker compose down        # conserva los CSV generados (volumen)
docker compose down -v     # borra también el volumen
```

## 8. Problemas frecuentes
| Síntoma | Solución |
|---|---|
| 503 / "Unable to find instance" justo después de levantar | Esperar ~15 s y reintentar (Eureka aún propaga el registro) |
| Un contenedor queda *unhealthy* | `docker compose logs <servicio>`; suele ser falta de memoria: subir RAM de Docker |
| Token inválido tras reiniciar `auth-server` | La llave RSA se regenera: repetir la carpeta 00 |
| Puerto ocupado | Cerrar el programa que usa el puerto o cambiar el mapeo en `docker-compose.yml` |
