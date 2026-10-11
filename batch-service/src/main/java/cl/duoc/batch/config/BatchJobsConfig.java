package cl.duoc.batch.config;

import cl.duoc.batch.listener.LimpiezaListener;
import cl.duoc.batch.listener.RechazoListener;
import cl.duoc.batch.listener.ResumenJobListener;
import cl.duoc.batch.model.EstadoRaw;
import cl.duoc.batch.model.InteresRaw;
import cl.duoc.batch.model.MovimientoRaw;
import cl.duoc.batch.model.Salidas.EstadoCuenta;
import cl.duoc.batch.model.Salidas.InteresCalculado;
import cl.duoc.batch.model.Salidas.MovimientoValido;
import cl.duoc.batch.processor.EstadoProcessor;
import cl.duoc.batch.processor.InteresProcessor;
import cl.duoc.batch.processor.MovimientoProcessor;
import cl.duoc.batch.processor.ValidacionException;
import cl.duoc.batch.service.ReporteService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.FaultTolerantStepBuilder;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.ItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.Date;

/**
 * Tres jobs (cada uno: paso de CARGA + paso de REPORTE):
 *   1) reporteTransaccionesDiariasJob     <- movimientos_financieros_diarios.csv
 *   2) calculoInteresesTrimestralesJob    <- intereses_trimestrales.csv
 *   3) estadosFinancierosAnualesJob       <- estados_financieros_anuales.csv
 *
 * El paso de carga es chunk-oriented, MULTIHILO, y tolerante a fallos:
 *   - skip: filas invalidas (formato o reglas de negocio) se omiten y se guardan como rechazadas
 *   - retry: errores transitorios de base de datos se reintentan 3 veces
 */
@Configuration
public class BatchJobsConfig {

    private final BatchProperties props;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager txManager;
    private final DataSource dataSource;
    private final JdbcTemplate jdbc;
    private final ResourceLoader resourceLoader;
    private final ReporteService reporteService;

    public BatchJobsConfig(BatchProperties props, JobRepository jobRepository, PlatformTransactionManager txManager,
                           DataSource dataSource, JdbcTemplate jdbc, ResourceLoader resourceLoader,
                           ReporteService reporteService) {
        this.props = props;
        this.jobRepository = jobRepository;
        this.txManager = txManager;
        this.dataSource = dataSource;
        this.jdbc = jdbc;
        this.resourceLoader = resourceLoader;
        this.reporteService = reporteService;
    }

    // ============================== Ejecucion paralela ==============================

    @Bean
    public TaskExecutor batchTaskExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(props.getThreads());
        ex.setMaxPoolSize(props.getThreads());
        ex.setThreadNamePrefix("batch-");
        ex.initialize();
        return ex;
    }

    // ============================== Lectores (@StepScope: reciben la "semana" como parametro) ==============================

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<MovimientoRaw> movimientoReader(@Value("#{jobParameters['semana']}") String semana)
            throws Exception {
        return csv("movimientoReader", semana, "movimientos_financieros_diarios.csv", MovimientoRaw.class,
                "id", "fecha", "monto", "tipo");
    }

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<InteresRaw> interesReader(@Value("#{jobParameters['semana']}") String semana)
            throws Exception {
        return csv("interesReader", semana, "intereses_trimestrales.csv", InteresRaw.class,
                "cuentaId", "nombre", "saldo", "edad", "tipo");
    }

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<EstadoRaw> estadoReader(@Value("#{jobParameters['semana']}") String semana)
            throws Exception {
        return csv("estadoReader", semana, "estados_financieros_anuales.csv", EstadoRaw.class,
                "cuentaId", "fecha", "transaccion", "monto", "descripcion");
    }

    /** Lector CSV (se salta el encabezado) envuelto en un lector sincronizado para poder usarlo con varios hilos. */
    private <T> SynchronizedItemStreamReader<T> csv(String nombre, String semana, String archivo, Class<T> tipo,
                                                    String... campos) throws Exception {
        String sem = (semana == null || semana.isBlank()) ? props.getSemanaPorDefecto() : semana;
        Resource recurso = resourceLoader.getResource(props.getInputDir() + sem + "/" + archivo);

        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setDelimiter(",");
        tokenizer.setNames(campos);

        BeanWrapperFieldSetMapper<T> mapper = new BeanWrapperFieldSetMapper<>();
        mapper.setTargetType(tipo);
        mapper.afterPropertiesSet();

        DefaultLineMapper<T> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(mapper);
        lineMapper.afterPropertiesSet();

        FlatFileItemReader<T> reader = new FlatFileItemReader<>();
        reader.setName(nombre);
        reader.setResource(recurso);
        reader.setLinesToSkip(1);
        reader.setStrict(true);          // si el archivo no existe, el job falla (y se reintenta)
        reader.setSaveState(false);      // obligatorio con varios hilos
        reader.setLineMapper(lineMapper);
        reader.afterPropertiesSet();

        SynchronizedItemStreamReader<T> sync = new SynchronizedItemStreamReader<>();
        sync.setDelegate(reader);
        return sync;
    }

    // ============================== Procesadores ==============================

    @Bean
    public MovimientoProcessor movimientoProcessor() {
        return new MovimientoProcessor(props);
    }

    @Bean
    @StepScope   // set de duplicados nuevo en cada ejecucion
    public InteresProcessor interesProcessor() {
        return new InteresProcessor(props);
    }

    @Bean
    public EstadoProcessor estadoProcessor() {
        return new EstadoProcessor();
    }

    // ============================== Escritores ==============================

    @Bean
    public JdbcBatchItemWriter<MovimientoValido> movimientoWriter() {
        return escritor("INSERT INTO movimientos_validos (id, fecha, monto, tipo, anomalia) "
                        + "VALUES (:id, :fecha, :monto, :tipo, :anomalia)",
                m -> new MapSqlParameterSource()
                        .addValue("id", m.id())
                        .addValue("fecha", Date.valueOf(m.fecha()))
                        .addValue("monto", m.monto())
                        .addValue("tipo", m.tipo())
                        .addValue("anomalia", m.anomalia()));
    }

    @Bean
    public JdbcBatchItemWriter<InteresCalculado> interesWriter() {
        return escritor("INSERT INTO intereses_calculados (cuenta_id, nombre, saldo, edad, tipo, tasa, interes, naturaleza) "
                        + "VALUES (:cuentaId, :nombre, :saldo, :edad, :tipo, :tasa, :interes, :naturaleza)",
                i -> new MapSqlParameterSource()
                        .addValue("cuentaId", i.cuentaId())
                        .addValue("nombre", i.nombre())
                        .addValue("saldo", i.saldo())
                        .addValue("edad", i.edad())
                        .addValue("tipo", i.tipo())
                        .addValue("tasa", i.tasa())
                        .addValue("interes", i.interes())
                        .addValue("naturaleza", i.naturaleza()));
    }

    @Bean
    public JdbcBatchItemWriter<EstadoCuenta> estadoWriter() {
        return escritor("INSERT INTO estados_cuenta (cuenta_id, fecha, transaccion, naturaleza, monto, descripcion) "
                        + "VALUES (:cuentaId, :fecha, :transaccion, :naturaleza, :monto, :descripcion)",
                e -> new MapSqlParameterSource()
                        .addValue("cuentaId", e.cuentaId())
                        .addValue("fecha", Date.valueOf(e.fecha()))
                        .addValue("transaccion", e.transaccion())
                        .addValue("naturaleza", e.naturaleza())
                        .addValue("monto", e.monto())
                        .addValue("descripcion", e.descripcion()));
    }

    private <T> JdbcBatchItemWriter<T> escritor(String sql, ItemSqlParameterSourceProvider<T> provider) {
        return new JdbcBatchItemWriterBuilder<T>()
                .dataSource(dataSource)
                .sql(sql)
                .itemSqlParameterSourceProvider(provider)
                .build();
    }

    // ============================== Pasos ==============================

    /** Paso de carga: lee -> valida/transforma -> escribe, con skip, retry y varios hilos. */
    private <I, O> Step pasoCarga(String nombre, String job, String tabla, ItemReader<I> reader,
                                  ItemProcessor<I, O> processor, ItemWriter<O> writer) {
        SimpleStepBuilder<I, O> simple = new StepBuilder(nombre, jobRepository)
                .<I, O>chunk(props.getChunkSize(), txManager)
                .reader(reader)
                .processor(processor)
                .writer(writer);
        simple.listener(new LimpiezaListener(jdbc, tabla, job));
        simple.taskExecutor(batchTaskExecutor());

        FixedBackOffPolicy espera = new FixedBackOffPolicy();
        espera.setBackOffPeriod(200);

        FaultTolerantStepBuilder<I, O> tolerante = simple.faultTolerant();
        tolerante.skipLimit(props.getSkipLimit());            // politica de termino: demasiados rechazos => FAILED
        tolerante.skip(ValidacionException.class);            // regla de negocio incumplida
        tolerante.skip(FlatFileParseException.class);         // fila con formato roto
        tolerante.noRollback(ValidacionException.class);      // omitir sin revertir el bloque completo
        tolerante.processorNonTransactional();
        tolerante.retryLimit(3);                              // errores transitorios de BD: 3 intentos
        tolerante.retry(TransientDataAccessException.class);
        tolerante.backOffPolicy(espera);
        tolerante.listener(new RechazoListener<I, O>(jdbc, job));
        return tolerante.build();
    }

    /** Paso final: genera el archivo CSV con el resultado. */
    private Step pasoReporte(String nombre, String clave) {
        return new StepBuilder(nombre, jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    reporteService.exportar(clave);
                    return RepeatStatus.FINISHED;
                }, txManager)
                .allowStartIfComplete(true)
                .build();
    }

    // ============================== Jobs ==============================

    @Bean
    public Job reporteTransaccionesDiariasJob(SynchronizedItemStreamReader<MovimientoRaw> movimientoReader,
                                              MovimientoProcessor movimientoProcessor,
                                              JdbcBatchItemWriter<MovimientoValido> movimientoWriter) {
        String job = "reporteTransaccionesDiariasJob";
        return new JobBuilder(job, jobRepository)
                .start(pasoCarga("cargaMovimientos", "transacciones-diarias", "movimientos_validos",
                        movimientoReader, movimientoProcessor, movimientoWriter))
                .next(pasoReporte("reporteMovimientos", "transacciones-diarias"))
                .listener(new ResumenJobListener())
                .build();
    }

    @Bean
    public Job calculoInteresesTrimestralesJob(SynchronizedItemStreamReader<InteresRaw> interesReader,
                                               InteresProcessor interesProcessor,
                                               JdbcBatchItemWriter<InteresCalculado> interesWriter) {
        String job = "calculoInteresesTrimestralesJob";
        return new JobBuilder(job, jobRepository)
                .start(pasoCarga("cargaIntereses", "intereses-trimestrales", "intereses_calculados",
                        interesReader, interesProcessor, interesWriter))
                .next(pasoReporte("reporteIntereses", "intereses-trimestrales"))
                .listener(new ResumenJobListener())
                .build();
    }

    @Bean
    public Job estadosFinancierosAnualesJob(SynchronizedItemStreamReader<EstadoRaw> estadoReader,
                                            EstadoProcessor estadoProcessor,
                                            JdbcBatchItemWriter<EstadoCuenta> estadoWriter) {
        String job = "estadosFinancierosAnualesJob";
        return new JobBuilder(job, jobRepository)
                .start(pasoCarga("cargaEstados", "estados-anuales", "estados_cuenta",
                        estadoReader, estadoProcessor, estadoWriter))
                .next(pasoReporte("reporteEstados", "estados-anuales"))
                .listener(new ResumenJobListener())
                .build();
    }
}
