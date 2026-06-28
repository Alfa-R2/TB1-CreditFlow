package edu.upc.sistemas.tbcreditflow.origination.domain.dto;

import edu.upc.sistemas.tbcreditflow.origination.domain.TipoDocumento;
import edu.upc.sistemas.tbcreditflow.origination.domain.entity.Documento;

import java.time.LocalDateTime;

public record DocumentoResponse(
        Long id,
        Long solicitudId,
        TipoDocumento tipo,
        String urlArchivo,
        String hash,
        LocalDateTime fechaCarga
) {
    public static DocumentoResponse from(Documento d) {
        return new DocumentoResponse(
                d.getId(),
                d.getSolicitud().getId(),
                d.getTipo(),
                d.getUrlArchivo(),
                d.getHash(),
                d.getFechaCarga()
        );
    }
}
