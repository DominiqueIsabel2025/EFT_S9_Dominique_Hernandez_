package cl.duoc.batch.listener;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Se ejecuta ANTES de cada (re)ejecucion del paso de carga: borra lo cargado antes.
 * Asi un reintento automatico despues de una falla no duplica registros (idempotencia).
 */
public class LimpiezaListener implements StepExecutionListener {

    private final JdbcTemplate jdbc;
    private final String tabla;
    private final String job;

    public LimpiezaListener(JdbcTemplate jdbc, String tabla, String job) {
        this.jdbc = jdbc;
        this.tabla = tabla;
        this.job = job;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        jdbc.update("DELETE FROM " + tabla);
        jdbc.update("DELETE FROM registros_rechazados WHERE job_nombre = ?", job);
    }
}
