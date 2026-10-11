package cl.duoc.batch.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Registros ya validados/transformados que se guardan en la base de datos. */
public final class Salidas {
    private Salidas() {}

    public record MovimientoValido(long id, LocalDate fecha, BigDecimal monto, String tipo, boolean anomalia) {}

    public record InteresCalculado(long cuentaId, String nombre, BigDecimal saldo, int edad, String tipo,
                                   BigDecimal tasa, BigDecimal interes, String naturaleza) {}

    public record EstadoCuenta(long cuentaId, LocalDate fecha, String transaccion, String naturaleza,
                               BigDecimal monto, String descripcion) {}
}
