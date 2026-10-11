package cl.duoc.batch.processor;

import cl.duoc.batch.model.EstadoRaw;
import cl.duoc.batch.model.Salidas.EstadoCuenta;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Estados financieros anuales. Normaliza el tipo ("Depósito" -> deposito), exige monto distinto de cero,
 * rechaza depositos negativos, deja los egresos como valor positivo y rellena descripciones vacias.
 */
public class EstadoProcessor implements ItemProcessor<EstadoRaw, EstadoCuenta> {

    private static final Set<String> INGRESOS = Set.of("deposito");
    private static final Set<String> EGRESOS = Set.of("retiro", "compra", "pago");

    @Override
    public EstadoCuenta process(EstadoRaw r) {
        long cuenta = Utilidades.parseLong(r.getCuentaId(), "cuenta_id");
        var fecha = Utilidades.parseFecha(r.getFecha());
        String trx = Utilidades.normalizar(r.getTransaccion());
        if (!INGRESOS.contains(trx) && !EGRESOS.contains(trx)) {
            throw new ValidacionException("tipo de transaccion no valido: '" + r.getTransaccion() + "'");
        }
        BigDecimal monto = Utilidades.parseMonto(r.getMonto(), "monto");
        if (monto.compareTo(BigDecimal.ZERO) == 0) {
            throw new ValidacionException("monto en cero");
        }
        boolean ingreso = INGRESOS.contains(trx);
        if (ingreso && monto.signum() < 0) {
            throw new ValidacionException("deposito con monto negativo: " + monto);
        }
        String desc = Utilidades.limpio(r.getDescripcion());
        if (desc.isEmpty()) {
            desc = "SIN DESCRIPCION";
        }
        return new EstadoCuenta(cuenta, fecha, trx, ingreso ? "INGRESO" : "EGRESO", monto.abs(), desc);
    }
}
