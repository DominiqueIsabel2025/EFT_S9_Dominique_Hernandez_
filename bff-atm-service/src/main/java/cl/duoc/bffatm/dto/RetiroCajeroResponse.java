package cl.duoc.bffatm.dto;

import java.math.BigDecimal;

public record RetiroCajeroResponse(String numeroCuenta, BigDecimal montoRetirado, BigDecimal saldoDisponibleActual, String mensaje) {}
