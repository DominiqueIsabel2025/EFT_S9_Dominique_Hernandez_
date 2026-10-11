package cl.duoc.batch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;

/** Escribe en el log un resumen legible al terminar cada job (util para la evidencia). */
public class ResumenJobListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(ResumenJobListener.class);

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("=== INICIO job {} (semana={}) ===", jobExecution.getJobInstance().getJobName(),
                jobExecution.getJobParameters().getString("semana"));
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        for (StepExecution s : jobExecution.getStepExecutions()) {
            log.info("    paso {}: estado={} leidos={} escritos={} omitidos={}", s.getStepName(), s.getStatus(),
                    s.getReadCount(), s.getWriteCount(), s.getSkipCount());
        }
        log.info("=== FIN job {} -> {} ===", jobExecution.getJobInstance().getJobName(), jobExecution.getStatus());
    }
}
