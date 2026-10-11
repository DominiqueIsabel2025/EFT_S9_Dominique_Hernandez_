package cl.duoc.bffmobile.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Respuesta ligera para el canal Movil: solo el saldo disponible y las
 * ultimas 5 transacciones, para minimizar consumo de datos moviles y
 * mejorar la velocidad de carga en la app.
 */
public record ResumenMovilResponse(
        String numeroCuenta,
        String tipoCuenta,
        BigDecimal saldoDisponible,
        List<MovimientoMovilDTO> ultimosMovimientos
) {}
