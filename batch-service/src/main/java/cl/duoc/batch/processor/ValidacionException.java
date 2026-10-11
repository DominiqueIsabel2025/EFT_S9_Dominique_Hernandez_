package cl.duoc.batch.processor;

/** Registro que no cumple las reglas de negocio: Spring Batch lo OMITE (skip) y se registra como rechazado. */
public class ValidacionException extends RuntimeException {
    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}
