package cl.duoc.pagos.dto;

import java.math.BigDecimal;

public record CuentaDTO(String numeroCuenta, Long clienteId, String tipoCuenta,
                        BigDecimal saldoDisponible, BigDecimal saldoContable,
                        String estado, BigDecimal limiteRetiroDiarioCajero) {}
