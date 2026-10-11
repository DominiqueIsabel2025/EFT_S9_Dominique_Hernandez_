package cl.duoc.batch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/** Parametros de negocio. Los valores reales llegan desde el Config Server (batch-service.yml). */
@ConfigurationProperties(prefix = "batch")
public class BatchProperties {

    private String inputDir = "classpath:data/";
    private String outputDir = "./output";
    private String semanaPorDefecto = "semana_3";
    private int chunkSize = 50;
    private int threads = 4;
    private int skipLimit = 900;
    private int maxReintentosJob = 3;
    private BigDecimal umbralAnomalia = new BigDecimal("3000");
    private int edadMinima = 18;
    private int edadMaxima = 120;
    private Map<String, BigDecimal> tasas = new LinkedHashMap<>(Map.of(
            "ahorro", new BigDecimal("0.015"),
            "prestamo", new BigDecimal("0.03"),
            "hipoteca", new BigDecimal("0.02")));

    public String getInputDir() { return inputDir; }
    public void setInputDir(String inputDir) { this.inputDir = inputDir; }
    public String getOutputDir() { return outputDir; }
    public void setOutputDir(String outputDir) { this.outputDir = outputDir; }
    public String getSemanaPorDefecto() { return semanaPorDefecto; }
    public void setSemanaPorDefecto(String semanaPorDefecto) { this.semanaPorDefecto = semanaPorDefecto; }
    public int getChunkSize() { return chunkSize; }
    public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }
    public int getThreads() { return threads; }
    public void setThreads(int threads) { this.threads = threads; }
    public int getSkipLimit() { return skipLimit; }
    public void setSkipLimit(int skipLimit) { this.skipLimit = skipLimit; }
    public int getMaxReintentosJob() { return maxReintentosJob; }
    public void setMaxReintentosJob(int maxReintentosJob) { this.maxReintentosJob = maxReintentosJob; }
    public BigDecimal getUmbralAnomalia() { return umbralAnomalia; }
    public void setUmbralAnomalia(BigDecimal umbralAnomalia) { this.umbralAnomalia = umbralAnomalia; }
    public int getEdadMinima() { return edadMinima; }
    public void setEdadMinima(int edadMinima) { this.edadMinima = edadMinima; }
    public int getEdadMaxima() { return edadMaxima; }
    public void setEdadMaxima(int edadMaxima) { this.edadMaxima = edadMaxima; }
    public Map<String, BigDecimal> getTasas() { return tasas; }
    public void setTasas(Map<String, BigDecimal> tasas) { this.tasas = tasas; }
}
