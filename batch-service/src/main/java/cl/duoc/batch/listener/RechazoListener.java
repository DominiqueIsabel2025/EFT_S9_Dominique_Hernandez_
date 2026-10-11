package cl.duoc.batch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.jdbc.core.JdbcTemplate;

/** Guarda en la tabla registros_rechazados cada fila omitida (skip) y el motivo. */
public class RechazoListener<I, O> implements SkipListener<I, O> {

    private static final Logger log = LoggerFactory.getLogger(RechazoListener.class);

    private final JdbcTemplate jdbc;
    private final String job;

    public RechazoListener(JdbcTemplate jdbc, String job) {
        this.jdbc = jdbc;
        this.job = job;
    }

    @Override
    public void onSkipInRead(Throwable t) {
        String dato = (t instanceof FlatFileParseException f) ? f.getInput() : "(fila ilegible)";
        guardar("LECTURA: " + t.getMessage(), dato);
    }

    @Override
    public void onSkipInProcess(I item, Throwable t) {
        guardar(t.getMessage(), String.valueOf(item));
    }

    private void guardar(String motivo, String dato) {
        log.debug("[{}] registro rechazado: {} -> {}", job, motivo, dato);
        jdbc.update("INSERT INTO registros_rechazados (job_nombre, motivo, dato) VALUES (?, ?, ?)",
                job, recortar(motivo, 250), recortar(dato, 250));
    }

    private static String recortar(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
