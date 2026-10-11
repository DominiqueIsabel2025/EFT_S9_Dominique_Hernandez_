package cl.duoc.bffmobile.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransaccionLegacyDTO(Long id, String numeroCuenta, String tipoMovimiento,
                                    BigDecimal monto, LocalDateTime fecha, String descripcion, String canalOrigen) {}
