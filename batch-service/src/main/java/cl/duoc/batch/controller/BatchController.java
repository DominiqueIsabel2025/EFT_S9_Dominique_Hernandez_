package cl.duoc.batch.controller;

import cl.duoc.batch.service.BatchOrquestador;
import cl.duoc.batch.service.ReporteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Procesos disponibles ({clave}): transacciones-diarias | intereses-trimestrales | estados-anuales
 */
@RestController
@RequestMapping("/batch")
public class BatchController {

    private final BatchOrquestador orquestador;
    private final ReporteService reporteService;

    public BatchController(BatchOrquestador orquestador, ReporteService reporteService) {
        this.orquestador = orquestador;
        this.reporteService = reporteService;
    }

    private ResponseEntity<Object> desconocido(String clave) {
        return ResponseEntity.status(404).body(Map.of("error", "Proceso desconocido: " + clave,
                "disponibles", ReporteService.REPORTES.keySet()));
    }

    /** Ejecuta el job (con re-ejecucion automatica si falla). semana: semana_1 | semana_2 | semana_3 */
    @PostMapping("/jobs/{clave}/ejecutar")
    public ResponseEntity<Object> ejecutar(@PathVariable String clave,
                                           @RequestParam(required = false) String semana) throws Exception {
        if (!orquestador.existe(clave)) return desconocido(clave);
        return ResponseEntity.ok(orquestador.ejecutar(clave, semana));
    }

    @GetMapping("/jobs/{clave}/ultima")
    public ResponseEntity<Object> ultima(@PathVariable String clave) {
        if (!orquestador.existe(clave)) return desconocido(clave);
        Map<String, Object> u = orquestador.ultimo(clave);
        return u == null ? ResponseEntity.status(404).body(Map.of("mensaje", "Aun no se ha ejecutado")) : ResponseEntity.ok(u);
    }

    @GetMapping("/reportes/{clave}")
    public ResponseEntity<Object> reporte(@PathVariable String clave) {
        if (!orquestador.existe(clave)) return desconocido(clave);
        List<Map<String, Object>> filas = reporteService.consultar(clave);
        return ResponseEntity.ok(filas);
    }

    @GetMapping("/rechazados/{clave}")
    public ResponseEntity<Object> rechazados(@PathVariable String clave,
                                             @RequestParam(defaultValue = "20") int limite) {
        if (!orquestador.existe(clave)) return desconocido(clave);
        return ResponseEntity.ok(reporteService.rechazados(clave, Math.min(limite, 500)));
    }
}
