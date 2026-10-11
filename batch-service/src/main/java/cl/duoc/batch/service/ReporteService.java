package cl.duoc.batch.service;

import cl.duoc.batch.config.BatchProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Consulta los resultados de cada proceso y los exporta a un CSV en la carpeta de salida. */
@Service
public class ReporteService {

    public record Reporte(String job, String tabla, String sql, String archivo) {}

    public static final Map<String, Reporte> REPORTES = Map.of(
            "transacciones-diarias", new Reporte("reporteTransaccionesDiariasJob", "movimientos_validos",
                    "SELECT fecha, tipo, COUNT(*) AS cantidad, SUM(monto) AS total, "
                            + "SUM(CASE WHEN anomalia THEN 1 ELSE 0 END) AS anomalias "
                            + "FROM movimientos_validos GROUP BY fecha, tipo ORDER BY fecha, tipo",
                    "reporte_transacciones_diarias.csv"),
            "intereses-trimestrales", new Reporte("calculoInteresesTrimestralesJob", "intereses_calculados",
                    "SELECT tipo, naturaleza, COUNT(*) AS cuentas, SUM(saldo) AS saldo_total, SUM(interes) AS interes_total "
                            + "FROM intereses_calculados GROUP BY tipo, naturaleza ORDER BY tipo",
                    "reporte_intereses_trimestrales.csv"),
            "estados-anuales", new Reporte("estadosFinancierosAnualesJob", "estados_cuenta",
                    "SELECT cuenta_id, "
                            + "SUM(CASE WHEN naturaleza = 'INGRESO' THEN monto ELSE 0 END) AS ingresos, "
                            + "SUM(CASE WHEN naturaleza = 'EGRESO' THEN monto ELSE 0 END) AS egresos, "
                            + "SUM(CASE WHEN naturaleza = 'INGRESO' THEN monto ELSE -monto END) AS saldo_neto, "
                            + "COUNT(*) AS movimientos "
                            + "FROM estados_cuenta GROUP BY cuenta_id ORDER BY cuenta_id",
                    "reporte_estados_financieros_anuales.csv"));

    private final JdbcTemplate jdbc;
    private final BatchProperties props;

    public ReporteService(JdbcTemplate jdbc, BatchProperties props) {
        this.jdbc = jdbc;
        this.props = props;
    }

    public List<Map<String, Object>> consultar(String clave) {
        return jdbc.queryForList(REPORTES.get(clave).sql());
    }

    /** Genera el archivo CSV del reporte y devuelve su ruta. */
    public Path exportar(String clave) throws IOException {
        Reporte rep = REPORTES.get(clave);
        List<Map<String, Object>> filas = consultar(clave);
        Path dir = Path.of(props.getOutputDir());
        Files.createDirectories(dir);
        Path archivo = dir.resolve(rep.archivo());
        StringBuilder sb = new StringBuilder();
        if (!filas.isEmpty()) {
            sb.append(String.join(",", filas.get(0).keySet())).append('\n');
            for (Map<String, Object> fila : filas) {
                sb.append(fila.values().stream().map(String::valueOf).collect(Collectors.joining(","))).append('\n');
            }
        }
        Files.writeString(archivo, sb.toString(), StandardCharsets.UTF_8);
        return archivo;
    }

    public List<Map<String, Object>> rechazados(String job, int limite) {
        return jdbc.queryForList(
                "SELECT motivo, dato FROM registros_rechazados WHERE job_nombre = ? LIMIT ?", job, limite);
    }

    public int totalRechazados(String job) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM registros_rechazados WHERE job_nombre = ?", Integer.class, job);
        return n == null ? 0 : n;
    }
}
