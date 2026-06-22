package edu.upc.sistemas.tbcreditflow.audit.service;

import edu.upc.sistemas.tbcreditflow.audit.domain.AccionAuditoria;
import edu.upc.sistemas.tbcreditflow.audit.domain.RegistroAuditoria;
import edu.upc.sistemas.tbcreditflow.audit.repository.RegistroAuditoriaRepository;
import edu.upc.sistemas.tbcreditflow.common.HashUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Sellado encadenado de auditoría (§4.6) — HU09/HU11.
 * <pre>
 * hashPrevio     = hashIntegridad del último registro (o "" si es el primero)
 * hashIntegridad = SHA-256( id | solicitudId | accion | usuario | fecha | hashPrevio )
 * </pre>
 */
@Service
public class AuditService {

    private final RegistroAuditoriaRepository repository;

    public AuditService(RegistroAuditoriaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public RegistroAuditoria registrar(Long solicitudId, AccionAuditoria accion, String usuario) {
        LocalDateTime fecha = LocalDateTime.now();
        String hashPrevio = repository.findTopByOrderByIdDesc()
                .map(RegistroAuditoria::getHashIntegridad)
                .orElse("");

        // Se inserta primero para obtener el id autogenerado (parte del hash), y luego se sella.
        RegistroAuditoria registro = new RegistroAuditoria(solicitudId, accion, usuario, fecha, hashPrevio);
        registro = repository.save(registro);

        registro.sellar(calcularHash(registro.getId(), solicitudId, accion, usuario, fecha, hashPrevio));
        return repository.save(registro);
    }

    /**
     * Verifica la integridad de toda la cadena: cada registro debe tener el {@code hashPrevio} del
     * anterior y un {@code hashIntegridad} consistente con su contenido. HU11 (no repudio).
     */
    @Transactional(readOnly = true)
    public boolean verificarCadena() {
        List<RegistroAuditoria> registros = repository.findAll();
        registros.sort((a, b) -> Long.compare(a.getId(), b.getId()));

        String hashPrevioEsperado = "";
        for (RegistroAuditoria r : registros) {
            if (!hashPrevioEsperado.equals(r.getHashPrevio())) {
                return false;
            }
            String esperado = calcularHash(r.getId(), r.getSolicitudId(), r.getAccion(),
                    r.getUsuario(), r.getFecha(), r.getHashPrevio());
            if (!esperado.equals(r.getHashIntegridad())) {
                return false;
            }
            hashPrevioEsperado = r.getHashIntegridad();
        }
        return true;
    }

    private String calcularHash(Long id, Long solicitudId, AccionAuditoria accion, String usuario,
                                LocalDateTime fecha, String hashPrevio) {
        String payload = id + "|" + solicitudId + "|" + accion + "|" + usuario + "|" + fecha + "|" + hashPrevio;
        return HashUtil.sha256(payload);
    }
}
