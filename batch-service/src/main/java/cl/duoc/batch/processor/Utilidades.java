package cl.duoc.batch.processor;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;

/** Funciones de limpieza compartidas por los tres procesos. */
public final class Utilidades {

    private Utilidades() {}

    // Los archivos legacy mezclan 4 formatos de fecha.
    private static final List<DateTimeFormatter> FORMATOS = List.of(
            fmt("uuuu-MM-dd"), fmt("uuuu/MM/dd"), fmt("dd/MM/uuuu"), fmt("dd-MM-uuuu"));

    private static DateTimeFormatter fmt(String patron) {
        return DateTimeFormatter.ofPattern(patron).withResolverStyle(ResolverStyle.STRICT);
    }

    public static LocalDate parseFecha(String texto) {
        String t = limpio(texto);
        for (DateTimeFormatter f : FORMATOS) {
            try {
                return LocalDate.parse(t, f);
            } catch (DateTimeParseException ignorado) {
                // prueba el siguiente formato
            }
        }
        throw new ValidacionException("Fecha invalida: '" + t + "'");
    }

    public static String limpio(String s) {
        return s == null ? "" : s.trim();
    }

    public static BigDecimal parseMonto(String texto, String campo) {
        String t = limpio(texto);
        if (t.isEmpty()) {
            throw new ValidacionException(campo + " faltante");
        }
        try {
            return new BigDecimal(t);
        } catch (NumberFormatException ex) {
            throw new ValidacionException(campo + " no numerico: '" + t + "'");
        }
    }

    public static long parseLong(String texto, String campo) {
        try {
            return Long.parseLong(limpio(texto));
        } catch (NumberFormatException ex) {
            throw new ValidacionException(campo + " invalido: '" + texto + "'");
        }
    }

    /** minusculas y sin tildes: "Depósito" -> "deposito". */
    public static String normalizar(String s) {
        String n = Normalizer.normalize(limpio(s).toLowerCase(), Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "");
    }
}
