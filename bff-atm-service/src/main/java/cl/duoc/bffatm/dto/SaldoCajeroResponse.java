package cl.duoc.bffatm.dto;

import java.math.BigDecimal;

/** Respuesta minima: solo lo que un cajero necesita mostrar en pantalla. */
public record SaldoCajeroResponse(String numeroCuenta, BigDecimal saldoDisponible, String estado) {}
