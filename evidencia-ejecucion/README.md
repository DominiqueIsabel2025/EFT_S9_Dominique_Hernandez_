# Evidencia de ejecución

Ejecuta el proyecto y guarda las capturas en esta carpeta con estos nombres:

| # | Captura | Cómo obtenerla | Criterio |
|---|---|---|---|
| 01 | `01-docker-build.png` | final de `docker compose build` | Docker |
| 02 | `02-compose-ps.png` | `docker compose ps` (todos *healthy*) | docker-compose |
| 03 | `03-eureka.png` | http://localhost:8761 (servicios registrados) | Spring Cloud |
| 04 | `04-config-server.png` | http://localhost:8888/batch-service/default | Spring Cloud |
| 05 | `05-postman-tokens-401-403.png` | Postman carpetas 00-04 | OAuth2 |
| 06 | `06-batch-3-jobs.png` | Postman carpeta 07 (3 jobs COMPLETED con leídos/escritos/omitidos) | Spring Batch |
| 07 | `07-batch-reportes-rechazados.png` | Postman: reportes y registros rechazados | Spring Batch |
| 08 | `08-batch-csv-generados.png` | `docker compose exec batch-service ls -l /app/output` | Spring Batch |
| 09 | `09-pagos-transferencia.png` | Postman carpeta 08 | Microservicios |
| 10 | `10-kafka-ui.png` | http://localhost:8090 → topic `banking.transacciones` | Kafka |
| 11 | `11-circuit-open.png` | con `cuentas-service` detenido: 503 y breaker `OPEN` | Resilience4j |
| 12 | `12-circuit-recovery.png` | tras `docker start cuentas-service`: `CLOSED` y 200 | Resilience4j |
| 13 | `13-escalado.png` | `docker compose up -d --scale clientes-service=2` + Eureka con 2 instancias | Escalabilidad |
| 14 | `14-postman-runner.png` | *Run collection* (sin la carpeta 10) | General |

**Salida de consola reproducible:** `bash evidencia-ejecucion/comandos-prueba.sh | tee evidencia-ejecucion/salida-consola.txt`
