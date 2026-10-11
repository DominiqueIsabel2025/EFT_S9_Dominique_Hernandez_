package cl.duoc.batch.service;

import cl.duoc.batch.config.BatchProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lanza los jobs y aplica la politica de re-ejecucion automatica: si un job termina FAILED se vuelve a
 * lanzar con los MISMOS parametros (Spring Batch lo trata como reinicio) hasta maxReintentosJob veces.
 */
@Service
public class BatchOrquestador {

    private static final Logger log = LoggerFactory.getLogger(BatchOrquestador.class);

    private final JobLauncher jobLauncher;
    private final ApplicationContext context;
    private final BatchProperties props;
    private final ReporteService reporteService;
    private final Map<String, Object> locks = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> ultimos = new ConcurrentHashMap<>();

    public BatchOrquestador(JobLauncher jobLauncher, ApplicationContext context, BatchProperties props,
                            ReporteService reporteService) {
        this.jobLauncher = jobLauncher;
        this.context = context;
        this.props = props;
        this.reporteService = reporteService;
    }

    public boolean existe(String clave) {
        return ReporteService.REPORTES.containsKey(clave);
    }

    public Map<String, Object> ejecutar(String clave, String semana) throws Exception {
        ReporteService.Reporte rep = ReporteService.REPORTES.get(clave);
        Job job = context.getBean(rep.job(), Job.class);
        String sem = (semana == null || semana.isBlank()) ? props.getSemanaPorDefecto() : semana;

        // un job a la vez por tipo (comparten tablas)
        synchronized (locks.computeIfAbsent(clave, k -> new Object())) {
            JobParameters params = new JobParametersBuilder()
                    .addString("semana", sem)
                    .addLong("ejecucion", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution exec;
            int intentos = 0;
            do {
                intentos++;
                exec = jobLauncher.run(job, params);
                if (exec.getStatus() != BatchStatus.COMPLETED) {
                    log.warn("[{}] intento {} termino en {}", rep.job(), intentos, exec.getStatus());
                }
            } while (exec.getStatus() != BatchStatus.COMPLETED && intentos <= props.getMaxReintentosJob());

            Map<String, Object> resumen = resumen(clave, rep, exec, sem, intentos);
            ultimos.put(clave, resumen);
            return resumen;
        }
    }

    public Map<String, Object> ultimo(String clave) {
        return ultimos.get(clave);
    }

    private Map<String, Object> resumen(String clave, ReporteService.Reporte rep, JobExecution exec, String semana,
                                        int intentos) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("job", rep.job());
        r.put("semana", semana);
        r.put("estado", exec.getStatus().toString());
        r.put("intentos", intentos);
        r.put("inicio", String.valueOf(exec.getStartTime()));
        r.put("fin", String.valueOf(exec.getEndTime()));
        List<Map<String, Object>> pasos = new ArrayList<>();
        for (StepExecution s : exec.getStepExecutions()) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("paso", s.getStepName());
            p.put("estado", s.getStatus().toString());
            p.put("leidos", s.getReadCount());
            p.put("escritos", s.getWriteCount());
            p.put("omitidos", s.getSkipCount());
            pasos.add(p);
        }
        r.put("pasos", pasos);
        r.put("rechazadosRegistrados", reporteService.totalRechazados(clave));
        r.put("archivoReporte", props.getOutputDir() + "/" + rep.archivo());
        return r;
    }
}
