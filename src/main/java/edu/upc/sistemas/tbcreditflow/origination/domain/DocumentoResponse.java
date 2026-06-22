package edu.upc.sistemas.tbcreditflow.origination.domain;

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
                d.getSolicitudId(),
                d.getTipo(),
                d.getUrlArchivo(),
                d.getHash(),
                d.getFechaCarga()
        );
    }
}
