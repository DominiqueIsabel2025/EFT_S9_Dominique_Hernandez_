package cl.duoc.cuentas.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "cuentas")
public class Cuenta {

    @Id
    private String numeroCuenta;

    private Long clienteId;
    private String tipoCuenta; // CORRIENTE, AHORRO, VISTA
    private BigDecimal saldoDisponible;
    private BigDecimal saldoContable;
    private String estado; // ACTIVA, BLOQUEADA, CERRADA
    private BigDecimal limiteRetiroDiarioCajero;

    public Cuenta() {}

    public Cuenta(String numeroCuenta, Long clienteId, String tipoCuenta, BigDecimal saldoDisponible,
                  BigDecimal saldoContable, String estado, BigDecimal limiteRetiroDiarioCajero) {
        this.numeroCuenta = numeroCuenta;
        this.clienteId = clienteId;
        this.tipoCuenta = tipoCuenta;
        this.saldoDisponible = saldoDisponible;
        this.saldoContable = saldoContable;
        this.estado = estado;
        this.limiteRetiroDiarioCajero = limiteRetiroDiarioCajero;
    }

    public String getNumeroCuenta() { return numeroCuenta; }
    public void setNumeroCuenta(String numeroCuenta) { this.numeroCuenta = numeroCuenta; }
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public String getTipoCuenta() { return tipoCuenta; }
    public void setTipoCuenta(String tipoCuenta) { this.tipoCuenta = tipoCuenta; }
    public BigDecimal getSaldoDisponible() { return saldoDisponible; }
    public void setSaldoDisponible(BigDecimal saldoDisponible) { this.saldoDisponible = saldoDisponible; }
    public BigDecimal getSaldoContable() { return saldoContable; }
    public void setSaldoContable(BigDecimal saldoContable) { this.saldoContable = saldoContable; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public BigDecimal getLimiteRetiroDiarioCajero() { return limiteRetiroDiarioCajero; }
    public void setLimiteRetiroDiarioCajero(BigDecimal limiteRetiroDiarioCajero) { this.limiteRetiroDiarioCajero = limiteRetiroDiarioCajero; }
}
