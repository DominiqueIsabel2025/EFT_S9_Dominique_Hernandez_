package cl.duoc.batch.model;

/** Fila cruda de estados_financieros_anuales.csv. */
public class EstadoRaw {
    private String cuentaId, fecha, transaccion, monto, descripcion;
    public String getCuentaId() { return cuentaId; }
    public void setCuentaId(String cuentaId) { this.cuentaId = cuentaId; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public String getTransaccion() { return transaccion; }
    public void setTransaccion(String transaccion) { this.transaccion = transaccion; }
    public String getMonto() { return monto; }
    public void setMonto(String monto) { this.monto = monto; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    @Override public String toString() { return cuentaId + "," + fecha + "," + transaccion + "," + monto + "," + descripcion; }
}
