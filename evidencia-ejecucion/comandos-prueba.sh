#!/bin/bash
# Pruebas de punta a punta para generar la evidencia de consola.
# Requisitos: curl y jq. Ejecutar con todo arriba:  docker compose up -d --build
# Uso:  bash evidencia-ejecucion/comandos-prueba.sh

AUTH=http://localhost:9000; CORE=http://localhost:8080; WEB=http://localhost:8081
MOB=http://localhost:8082; ATM=http://localhost:8083; NOT=http://localhost:8084

token() { # $1=client_id $2=secret $3=scopes
  curl -s -u "$1:$2" -d "grant_type=client_credentials&scope=$3" $AUTH/oauth2/token | jq -r .access_token
}
code() { curl -s -o /dev/null -w "HTTP %{http_code}\n" "$@"; }

echo "==== 0) ESTADO DE LOS CONTENEDORES ===="
docker compose ps

echo -e "\n==== 1) OAUTH2: token del terminal ATM (se muestran los claims) ===="
T_ATM=$(token atm-terminal atm-terminal-secret atm.operate)
echo "$T_ATM" | cut -d. -f2 | tr '_-' '/+' | base64 -d 2>/dev/null | jq .
T_WEB=$(token web-frontend web-frontend-secret web.access)
T_MOB=$(token mobile-app mobile-app-secret mobile.access)
T_FULL=$(token postman-client postman-secret "banking.read banking.write web.access mobile.access atm.operate")

echo -e "\n==== 2) SEGURIDAD: sin token -> 401 ===="
code $ATM/atm/cuentas/CTA-0001/saldo
echo "==== 2b) SEGURIDAD: token de ATM en el BFF Web -> 403 ===="
code -H "Authorization: Bearer $T_ATM" $WEB/web/clientes/1/dashboard
echo "==== 2c) SEGURIDAD: token de ATM directo al core -> 403 ===="
code -H "Authorization: Bearer $T_ATM" $CORE/api/legacy/clientes/1

echo -e "\n==== 3) BFF WEB (token web) ===="
curl -s -H "Authorization: Bearer $T_WEB" $WEB/web/clientes/1/dashboard | jq .
echo -e "\n==== 4) BFF MOVIL (token mobile) ===="
curl -s -H "Authorization: Bearer $T_MOB" $MOB/mobile/cuentas/CTA-0001/resumen | jq .

echo -e "\n==== 5) BFF CAJERO: saldo ===="
curl -s -H "Authorization: Bearer $T_ATM" $ATM/atm/cuentas/CTA-0001/saldo | jq .
echo -e "\n==== 6) BFF CAJERO: retiro \$50.000 (publica evento Kafka) ===="
curl -s -X POST -H "Authorization: Bearer $T_ATM" -H "Content-Type: application/json" -d '{"monto":50000}' $ATM/atm/cuentas/CTA-0001/retiro | jq .
echo -e "\n==== 7) BFF CAJERO: retiro \$250.000 (genera ALERTA) ===="
curl -s -X POST -H "Authorization: Bearer $T_ATM" -H "Content-Type: application/json" -d '{"monto":250000}' $ATM/atm/cuentas/CTA-0001/retiro | jq .
echo -e "\n==== 8) BFF CAJERO: retiro \$500.000 excede limite -> 403 ===="
curl -s -X POST -H "Authorization: Bearer $T_ATM" -H "Content-Type: application/json" -d '{"monto":500000}' $ATM/atm/cuentas/CTA-0001/retiro | jq .

sleep 3
echo -e "\n==== 9) KAFKA: notificaciones consumidas por notification-service ===="
curl -s -H "Authorization: Bearer $T_FULL" $NOT/notifications | jq .
echo -e "\n---- logs del consumidor ----"
docker compose logs --tail 5 notification-service

echo -e "\n==== 10) RESILIENCE4J: estado de los circuit breakers ===="
curl -s -H "Authorization: Bearer $T_FULL" $ATM/actuator/circuitbreakers | jq .

echo -e "\n==== 14) SPRING CLOUD: servicios registrados en Eureka ===="
curl -s -H "Accept: application/json" http://localhost:8761/eureka/apps | jq -r '.applications.application[] | "\(.name)  instancias=\(.instance|length)"'
echo -e "\n---- Config Server: parametros de batch-service ----"
curl -s http://localhost:8888/batch-service/default | jq '.propertySources[0].source'

echo -e "\n==== 15) PAGOS (via API Gateway): deposito y transferencia ===="
curl -s -X POST -H "Authorization: Bearer $T_FULL" -H "Content-Type: application/json" -d '{"numeroCuenta":"CTA-0002","monto":100000,"canal":"WEB","descripcion":"Deposito de prueba"}' $CORE/api/pagos/deposito | jq .
curl -s -X POST -H "Authorization: Bearer $T_FULL" -H "Content-Type: application/json" -d '{"cuentaOrigen":"CTA-0003","cuentaDestino":"CTA-0002","monto":50000,"canal":"WEB"}' $CORE/api/pagos/transferencia | jq .
echo "---- transferencia sobre el maximo del Config Server -> 422 ----"
curl -s -w " -> HTTP %{http_code}\n" -X POST -H "Authorization: Bearer $T_FULL" -H "Content-Type: application/json" -d '{"cuentaOrigen":"CTA-0003","cuentaDestino":"CTA-0001","monto":9000000}' $CORE/api/pagos/transferencia

echo -e "\n==== 16) SPRING BATCH: los 3 procesos (semana_3) ===="
for j in transacciones-diarias intereses-trimestrales estados-anuales; do
  echo "--- $j ---"
  curl -s -X POST -H "Authorization: Bearer $T_FULL" "$CORE/batch/jobs/$j/ejecutar?semana=semana_3" | jq '{job, estado, intentos, pasos, rechazadosRegistrados}'
done
echo "---- reporte de intereses por tipo ----"
curl -s -H "Authorization: Bearer $T_FULL" $CORE/batch/reportes/intereses-trimestrales | jq .
echo "---- ejemplos de registros rechazados ----"
curl -s -H "Authorization: Bearer $T_FULL" "$CORE/batch/rechazados/intereses-trimestrales?limite=5" | jq .
echo "---- archivos CSV generados ----"
docker compose exec batch-service ls -l /app/output

echo -e "\n==== 17) PRUEBA DE FALLO: se detiene cuentas-service ===="
docker stop cuentas-service
for i in 1 2 3 4 5 6 7 8; do
  echo -n "Intento $i: "
  curl -s -w " -> HTTP %{http_code}\n" -H "Authorization: Bearer $T_MOB" $MOB/mobile/cuentas/CTA-0001/resumen
done
echo -e "\n==== 18) Circuit breaker del BFF movil (debe estar OPEN) ===="
curl -s -H "Authorization: Bearer $T_FULL" $MOB/actuator/circuitbreakers | jq '.circuitBreakers.coreBankingRead | {state, failureRate, failedCalls: .bufferedCalls}'

echo -e "\n==== 19) RECUPERACION: se levanta cuentas-service ===="
docker start cuentas-service
echo "Esperando 45s (arranque del core + espera del breaker)..."; sleep 45
curl -s -w "\n-> HTTP %{http_code}\n" -H "Authorization: Bearer $T_MOB" $MOB/mobile/cuentas/CTA-0001/resumen | tail -3
curl -s -H "Authorization: Bearer $T_FULL" $MOB/actuator/circuitbreakers | jq '.circuitBreakers.coreBankingRead.state'
