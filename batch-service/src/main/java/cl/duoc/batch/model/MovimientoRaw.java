package cl.duoc.batch.model;

/** Fila cruda de movimientos_financieros_diarios.csv (todo texto: la validacion se hace en el processor). */
public class MovimientoRaw {
    private String id, fecha, monto, tipo;
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public String getMonto() { return monto; }
    public void setMonto(String monto) { this.monto = monto; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    @Override public String toString() { return id + "," + fecha + "," + monto + "," + tipo; }
}
