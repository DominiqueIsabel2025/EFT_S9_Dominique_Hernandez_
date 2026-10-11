# EFT S9 – Desarrollo Backend Avanzado: Spring Cloud y Batch
**Desarrollo Backend III (PBY2203) – Duoc UC** · **Estudiante:** Dominique Hernandez · **Docente:** Alonso Esteban Castillo Pizarro

**Repositorio GitHub:** https://github.com/DominiqueIsabel2025/EFT_S9_Dominique_Hernandez_

Modernización de la plataforma del **Banco XYZ** (sistema legacy COBOL/Shell) hacia microservicios en la nube.
Este proyecto **parte del trabajo de la Semana 8** (OAuth2, Docker, Resilience4j, Kafka, BFF) y le agrega lo que pedía la Evaluación Final:
**Spring Batch, división del monolito en 3 microservicios, Eureka, Config Server y API Gateway.**

| Documento | Contenido |
|---|---|
| [`instrucciones.md`](instrucciones.md) | Cómo ejecutar y probar cada componente, paso a paso |
| [`despliegue.md`](despliegue.md) | Cómo desplegar en AWS (EC2 + Docker Compose) |
| `Informe_Tecnico_EFT_S9.pdf` | Informe técnico con diagramas |
| [`postman/`](postman) | Colección con pruebas automáticas |

## 1. Los 5 procesos clave de la migración

| # | Proceso | Qué se hizo | Dónde |
|---|---|---|---|
| 1 | **Migración de procesos batch** | 3 jobs en Spring Batch (transacciones diarias, intereses trimestrales, estados anuales) | `batch-service/` |
| 2 | **División del monolito** | El antiguo `core-banking-service` se separó en Clientes, Cuentas y Pagos | `clientes-service/`, `cuentas-service/`, `pagos-service/` |
| 3 | **Patrón BFF** | Un backend por canal: Web, Móvil y Cajeros | `bff-web-service/`, `bff-mobile-service/`, `bff-atm-service/` |
| 4 | **Seguridad distribuida** | OAuth2 (Authorization Server + Resource Servers con scopes por canal) | `auth-server/` + `SecurityConfig` de cada servicio |
| 5 | **Mensajería asíncrona** | Kafka: Pagos publica eventos, Notificaciones los consume | `pagos-service/.../messaging`, `notification-service/` |

Soporte de infraestructura: **Eureka** (descubrimiento), **Config Server** (configuración central), **API Gateway** (enrutamiento + balanceo), **Resilience4j**, **Docker Compose**.

## 2. Arquitectura

```
        Postman / Web / App Móvil / Cajero ──(1) token──► auth-server :9000 (OAuth2, JWT RS256)
                       │ (2) Bearer JWT
     ┌─────────────────┼─────────────────┐
     ▼                 ▼                 ▼
 bff-web :8081    bff-mobile :8082   bff-atm :8083      notification-service :8084 ◄── consume
     └─────────────────┼─────────────────┘                          ▲
        Resilience4j   ▼                                            │
                ┌─────────────────┐   lb:// (Eureka)         ┌──────┴───────┐
                │  api-gateway    │──► clientes-service      │    Kafka     │
                │     :8080       │──► cuentas-service  ◄─┐  │ banking.     │
                │ enruta+balancea │──► pagos-service ─────┘  │ transacciones│
                │                 │      │ (debito/credito)  └──────▲───────┘
                │                 │──► batch-service               │ publica
                └─────────────────┘      └─────────────────────────┘
        eureka-server :8761 (registro)      config-server :8888 (configuración central)
```

## 3. Estructura

```
EFT_S9_Dominique_Hernandez_/
├── auth-server/            OAuth2 Authorization Server (:9000)
├── eureka-server/          Registro de servicios (:8761)
├── config-server/          Configuración centralizada (:8888)  → config-repo/
├── api-gateway/            Enrutamiento y balanceo de carga (:8080)
├── clientes-service/       Gestión de Clientes (:8102)
├── cuentas-service/        Gestión de Cuentas: apertura, cierre, mantenimiento (:8101)
├── pagos-service/          Pagos, transferencias, depósitos, retiros + Kafka (:8103)
├── batch-service/          Spring Batch: 3 procesos (:8105)
├── bff-web-service/        BFF Web (:8081)
├── bff-mobile-service/     BFF Móvil (:8082)
├── bff-atm-service/        BFF Cajeros (:8083)
├── notification-service/   Consumidor Kafka (:8084)
├── postman/  evidencia-ejecucion/  aws/
├── docker-compose.yml  .env.example
└── readme.md  instrucciones.md  despliegue.md
```
Cada servicio tiene su propio `pom.xml`, `Dockerfile` (multi-stage, usuario no-root, `HEALTHCHECK`) y `application.yml`.

## 4. Cómo cada requerimiento queda cubierto

### Parte 1 – Spring Batch (`batch-service`)
| Requerimiento | Solución |
|---|---|
| Reescribir procesos en Spring Batch | 3 jobs: `reporteTransaccionesDiariasJob`, `calculoInteresesTrimestralesJob`, `estadosFinancierosAnualesJob` |
| Steps leer / procesar / escribir | Paso de **carga**: `FlatFileItemReader` (CSV) → `ItemProcessor` (reglas de negocio) → `JdbcBatchItemWriter`. Paso de **reporte**: genera un CSV resumen |
| Excepciones y reintentos | `skip` para filas inválidas (se guardan en `registros_rechazados` con su motivo) y `retry` ×3 con espera para errores transitorios de BD |
| Optimizar volumen | Step **multihilo** (4 hilos), bloques (chunks) de 50, lector sincronizado |
| Políticas de finalización y re-ejecución | `skipLimit` (si hay demasiados rechazos el job termina `FAILED`); re-ejecución automática hasta 3 veces con los mismos parámetros (reinicio de Spring Batch); la carga es idempotente (limpia antes de re-ejecutar) |
| Integridad / equivalencia con legacy | Mismas reglas que el legacy sobre los CSV de `fin_legacy_data`; resultados esperados verificables (ver tabla) |

Resultados esperados con `semana_3` (1.000 filas por archivo):

| Job | Válidas (escritas) | Rechazadas (omitidas) | Principales motivos |
|---|---|---|---|
| Transacciones diarias | 392 | 608 | tipo inválido, monto vacío/≤0, fecha imposible |
| Intereses trimestrales | 421 | 579 | edad/saldo faltante, tipo `-1`/`unknown`, edad fuera de rango, duplicados |
| Estados anuales | 759 | 241 | monto cero/vacío, depósito con monto negativo |

Los parámetros (tasas, tamaño de bloque, hilos, límite de rechazos) viven en el **Config Server** (`config-server/src/main/resources/config-repo/batch-service.yml`).

### Parte 2 – BFF
Tres BFF independientes (Web: datos completos; Móvil: saldo + últimos 5 movimientos; Cajero: saldo y retiro con límite diario), cada uno con su **scope OAuth2** (`web.access`, `mobile.access`, `atm.operate`). Los BFF no cambiaron respecto a la S8: solo apuntan al API Gateway.

### Parte 3 – Microservicios con Spring Cloud
| Requerimiento | Solución |
|---|---|
| 3 microservicios (Cuentas, Pagos, Clientes) | `cuentas-service`, `pagos-service`, `clientes-service`, cada uno con su propia base de datos |
| Spring Cloud Config | `config-server` (modo *native*) entrega reglas de negocio a Pagos y Batch |
| Eureka | `eureka-server`; todos los servicios de negocio se registran y se encuentran por nombre |
| Balanceo y enrutamiento | `api-gateway` con rutas `lb://…` (balanceo cliente-side entre instancias) |
| OAuth2 | Todos los servicios son *Resource Servers* (JWT + scopes); Pagos reenvía el token a Cuentas |
| Resilience4j + alternativas | Pagos → Cuentas: CircuitBreaker, Retry (solo lecturas), Bulkhead, timeouts y *fallback* a un 503 controlado; BFF → Gateway igual que en S8 |
| Kafka | Pagos **publica** `TransaccionEvent` en `banking.transacciones`; `notification-service` **consume** y genera `INFO` / `ALERTA_MONTO_ALTO` |
| Consistencia distribuida | **Compensación (saga simple)**: si algo falla tras mover el saldo, Pagos revierte el movimiento en Cuentas |
| Monitoreo / registro | Actuator (`health`, `circuitbreakers`, …), logs de cada job y de eventos Kafka, Kafka UI, panel Eureka |
| Escalabilidad horizontal | `docker compose up -d --scale clientes-service=2`; Eureka registra ambas y el gateway balancea |
| Docker / compose / AWS | 14 contenedores orquestados con un comando; guía en `despliegue.md` |

## 5. Comparación legacy vs. nuevo sistema
| Aspecto | Legacy (COBOL + Shell) | Nuevo (Spring Cloud) |
|---|---|---|
| Procesos batch | Scripts sin reintentos ni trazabilidad de rechazos | Jobs con skip/retry, rechazos con motivo, reinicio automático, 4 hilos |
| Arquitectura | Monolito, un fallo afecta todo | Microservicios independientes + Circuit Breaker |
| Escalabilidad | Vertical (mainframe) | Horizontal (más instancias registradas en Eureka) |
| Seguridad | Centralizada y limitada | OAuth2 distribuido, scopes por canal |
| Integración | Acoplada | Eventos asíncronos con Kafka |
| Configuración | Dentro del código/scripts | Centralizada (Config Server) |
| Despliegue | Manual en servidores | `docker compose up` / AWS |

## 6. Limitaciones conocidas (y mejoras futuras)
- Bases de datos H2 en memoria (se reinician con el contenedor). Mejora: PostgreSQL/RDS por servicio.
- **HTTPS**: en local se usa HTTP; en AWS se recomienda terminar TLS en un Application Load Balancer con certificado ACM (ver `despliegue.md`).
- Escalar Cuentas/Pagos con H2 produciría datos distintos por instancia: requiere BD compartida (por eso la demo de escalado usa `clientes-service`).
- Clientes OAuth2 y llave RSA del auth-server en memoria; Kafka de un nodo sin TLS (en producción: MSK).
- Mejoras: Spring Cloud Gateway con rate-limit, trazas distribuidas (Micrometer/Zipkin), CI/CD (GitHub Actions → ECR → ECS).
