package cl.duoc.pagos.service;

import cl.duoc.pagos.client.CuentasClient;
import cl.duoc.pagos.dto.CuentaDTO;
import cl.duoc.pagos.messaging.TransaccionEvent;
import cl.duoc.pagos.messaging.TransaccionEventPublisher;
import cl.duoc.pagos.model.Transaccion;
import cl.duoc.pagos.repository.TransaccionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Logica de pagos. Como cada servicio tiene su propia base de datos no hay una transaccion
 * ACID que abarque ambos: se usa una COMPENSACION (patron saga simple). Si falla algo despues
 * de mover el saldo, se revierte el movimiento en cuentas-service.
 */
@Service
public class PagosService {

    private static final Logger log = LoggerFactory.getLogger(PagosService.class);

    private final CuentasClient cuentasClient;
    private final TransaccionRepository transaccionRepository;
    private final TransaccionEventPublisher eventPublisher;

    public PagosService(CuentasClient cuentasClient, TransaccionRepository transaccionRepository,
                        TransaccionEventPublisher eventPublisher) {
        this.cuentasClient = cuentasClient;
        this.transaccionRepository = transaccionRepository;
        this.eventPublisher = eventPublisher;
    }

    /** RETIRO o PAGO: descuenta saldo. */
    public CuentaDTO egreso(String cuenta, BigDecimal monto, String tipo, String canal, String descripcion) {
        CuentaDTO actualizada = cuentasClient.debitar(cuenta, monto);
        try {
            registrar(cuenta, tipo, monto, canal, descripcion, actualizada.saldoDisponible());
        } catch (RuntimeException ex) {
            log.error("[SAGA] Fallo al registrar {} en {}; se compensa el debito", tipo, cuenta, ex);
            cuentasClient.acreditar(cuenta, monto);
            throw ex;
        }
        return actualizada;
    }

    /** DEPOSITO: abona saldo. */
    public CuentaDTO deposito(String cuenta, BigDecimal monto, String canal, String descripcion) {
        CuentaDTO actualizada = cuentasClient.acreditar(cuenta, monto);
        try {
            registrar(cuenta, "DEPOSITO", monto, canal, descripcion, actualizada.saldoDisponible());
        } catch (RuntimeException ex) {
            log.error("[SAGA] Fallo al registrar DEPOSITO en {}; se compensa", cuenta, ex);
            cuentasClient.debitar(cuenta, monto);
            throw ex;
        }
        return actualizada;
    }

    /** TRANSFERENCIA: debita origen, acredita destino; si el destino falla, devuelve el dinero al origen. */
    public CuentaDTO transferencia(String origen, String destino, BigDecimal monto, String canal) {
        CuentaDTO desdeOrigen = cuentasClient.debitar(origen, monto);
        CuentaDTO desdeDestino;
        try {
            desdeDestino = cuentasClient.acreditar(destino, monto);
        } catch (RuntimeException ex) {
            log.error("[SAGA] Fallo al acreditar {}; se devuelve el dinero a {}", destino, origen);
            cuentasClient.acreditar(origen, monto);
            throw ex;
        }
        registrar(origen, "TRANSFERENCIA", monto, canal, "Transferencia a " + destino, desdeOrigen.saldoDisponible());
        registrar(destino, "DEPOSITO", monto, canal, "Transferencia desde " + origen, desdeDestino.saldoDisponible());
        return desdeOrigen;
    }

    private void registrar(String cuenta, String tipo, BigDecimal monto, String canal,
                           String descripcion, BigDecimal saldoResultante) {
        LocalDateTime ahora = LocalDateTime.now();
        transaccionRepository.save(new Transaccion(cuenta, tipo, monto, ahora, descripcion, canal));
        eventPublisher.publicar(new TransaccionEvent(UUID.randomUUID().toString(), cuenta, tipo, monto,
                saldoResultante, canal, ahora.toString()));
    }
}
