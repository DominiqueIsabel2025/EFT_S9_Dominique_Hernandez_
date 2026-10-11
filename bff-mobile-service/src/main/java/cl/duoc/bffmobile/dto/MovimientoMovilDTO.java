package cl.duoc.bffmobile.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Version reducida de una transaccion: solo lo esencial para la app movil. */
public record MovimientoMovilDTO(String tipo, BigDecimal monto, LocalDate fecha, String detalle) {}
