package cl.duoc.bffweb.dto;

import java.math.BigDecimal;

public record CuentaLegacyDTO(String numeroCuenta, Long clienteId, String tipoCuenta,
                               BigDecimal saldoDisponible, BigDecimal saldoContable,
                               String estado, BigDecimal limiteRetiroDiarioCajero) {}
