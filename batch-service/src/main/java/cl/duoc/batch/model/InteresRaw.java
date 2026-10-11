package cl.duoc.batch.model;

/** Fila cruda de intereses_trimestrales.csv. */
public class InteresRaw {
    private String cuentaId, nombre, saldo, edad, tipo;
    public String getCuentaId() { return cuentaId; }
    public void setCuentaId(String cuentaId) { this.cuentaId = cuentaId; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getSaldo() { return saldo; }
    public void setSaldo(String saldo) { this.saldo = saldo; }
    public String getEdad() { return edad; }
    public void setEdad(String edad) { this.edad = edad; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    @Override public String toString() { return cuentaId + "," + nombre + "," + saldo + "," + edad + "," + tipo; }
}
