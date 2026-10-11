package cl.duoc.batch.processor;

import cl.duoc.batch.config.BatchProperties;
import cl.duoc.batch.model.InteresRaw;
import cl.duoc.batch.model.Salidas.InteresCalculado;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Calculo de intereses trimestrales. Rechaza saldos faltantes/no positivos, edades fuera de rango,
 * tipos desconocidos y registros duplicados. ahorro -> ABONO ; prestamo/hipoteca -> CARGO.
 */
public class InteresProcessor implements ItemProcessor<InteresRaw, InteresCalculado> {

    private final BatchProperties props;
    // Seguro para hilos: el step corre con varios threads. Se crea un set nuevo en cada ejecucion (@StepScope).
    private final Set<String> vistos = ConcurrentHashMap.newKeySet();

    public InteresProcessor(BatchProperties props) {
        this.props = props;
    }

    @Override
    public InteresCalculado process(InteresRaw r) {
        long cuenta = Utilidades.parseLong(r.getCuentaId(), "cuenta_id");
        BigDecimal saldo = Utilidades.parseMonto(r.getSaldo(), "saldo");
        if (saldo.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacionException("saldo no positivo: " + saldo);
        }
        String edadTxt = Utilidades.limpio(r.getEdad());
        if (edadTxt.isEmpty()) {
            throw new ValidacionException("edad faltante");
        }
        int edad;
        try {
            edad = Integer.parseInt(edadTxt);
        } catch (NumberFormatException ex) {
            throw new ValidacionException("edad no numerica: '" + edadTxt + "'");
        }
        if (edad < props.getEdadMinima() || edad > props.getEdadMaxima()) {
            throw new ValidacionException("edad fuera de rango: " + edad);
        }
        String tipo = Utilidades.normalizar(r.getTipo());
        BigDecimal tasa = props.getTasas().get(tipo);
        if (tasa == null) {
            throw new ValidacionException("tipo de cuenta no valido: '" + r.getTipo() + "'");
        }
        String clave = cuenta + "|" + Utilidades.limpio(r.getNombre()) + "|" + saldo.toPlainString() + "|" + edad + "|" + tipo;
        if (!vistos.add(clave)) {
            throw new ValidacionException("registro duplicado");
        }
        BigDecimal interes = saldo.multiply(tasa).setScale(2, RoundingMode.HALF_UP);
        String naturaleza = "ahorro".equals(tipo) ? "ABONO" : "CARGO";
        return new InteresCalculado(cuenta, Utilidades.limpio(r.getNombre()), saldo, edad, tipo, tasa, interes, naturaleza);
    }
}
