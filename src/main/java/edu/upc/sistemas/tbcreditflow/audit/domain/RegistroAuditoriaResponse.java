package edu.upc.sistemas.tbcreditflow.audit.domain;

import java.time.LocalDateTime;

public record RegistroAuditoriaResponse(
        Long id,
        Long solicitudId,
        AccionAuditoria accion,
        String usuario,
        LocalDateTime fecha,
        String hashIntegridad,
        String hashPrevio
) {
    public static RegistroAuditoriaResponse from(RegistroAuditoria r) {
        return new RegistroAuditoriaResponse(
                r.getId(),
                r.getSolicitudId(),
                r.getAccion(),
                r.getUsuario(),
                r.getFecha(),
                r.getHashIntegridad(),
                r.getHashPrevio()
        );
    }
}
