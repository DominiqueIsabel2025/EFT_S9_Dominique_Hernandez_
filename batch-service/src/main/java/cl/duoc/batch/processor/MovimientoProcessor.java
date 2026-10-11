package cl.duoc.batch.processor;

import cl.duoc.batch.config.BatchProperties;
import cl.duoc.batch.model.MovimientoRaw;
import cl.duoc.batch.model.Salidas.MovimientoValido;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.util.Set;

/** Reporte de transacciones diarias: valida fecha, monto (> 0) y tipo (credito/debito); marca montos altos. */
public class MovimientoProcessor implements ItemProcessor<MovimientoRaw, MovimientoValido> {

    private static final Set<String> TIPOS = Set.of("credito", "debito");
    private final BatchProperties props;

    public MovimientoProcessor(BatchProperties props) {
        this.props = props;
    }

    @Override
    public MovimientoValido process(MovimientoRaw r) {
        long id = Utilidades.parseLong(r.getId(), "id");
        var fecha = Utilidades.parseFecha(r.getFecha());
        BigDecimal monto = Utilidades.parseMonto(r.getMonto(), "monto");
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacionException("monto negativo o cero: " + monto);
        }
        String tipo = Utilidades.normalizar(r.getTipo());
        if (!TIPOS.contains(tipo)) {
            throw new ValidacionException("tipo de transaccion no valido: '" + r.getTipo() + "'");
        }
        return new MovimientoValido(id, fecha, monto, tipo, monto.compareTo(props.getUmbralAnomalia()) >= 0);
    }
}
