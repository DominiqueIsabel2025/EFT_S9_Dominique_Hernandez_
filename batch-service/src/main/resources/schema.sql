-- Tablas de resultado de los procesos batch (las tablas de control de Spring Batch las crea el framework)
CREATE TABLE IF NOT EXISTS movimientos_validos (
    id        BIGINT,
    fecha     DATE,
    monto     DECIMAL(15,2),
    tipo      VARCHAR(20),
    anomalia  BOOLEAN
);

CREATE TABLE IF NOT EXISTS intereses_calculados (
    cuenta_id  BIGINT,
    nombre     VARCHAR(100),
    saldo      DECIMAL(15,2),
    edad       INT,
    tipo       VARCHAR(20),
    tasa       DECIMAL(6,4),
    interes    DECIMAL(15,2),
    naturaleza VARCHAR(10)
);

CREATE TABLE IF NOT EXISTS estados_cuenta (
    cuenta_id   BIGINT,
    fecha       DATE,
    transaccion VARCHAR(20),
    naturaleza  VARCHAR(10),
    monto       DECIMAL(15,2),
    descripcion VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS registros_rechazados (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_nombre VARCHAR(60),
    motivo     VARCHAR(255),
    dato       VARCHAR(255)
);
