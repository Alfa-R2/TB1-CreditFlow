package edu.upc.sistemas.tbcreditflow.origination.domain.dto;

import edu.upc.sistemas.tbcreditflow.origination.domain.EstadoSolicitud;
import edu.upc.sistemas.tbcreditflow.origination.domain.entity.Solicitud;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SolicitudResponse(
        Long id,
        Long clienteId,
        Long asesorId,
        BigDecimal monto,
        Integer plazoMeses,
        EstadoSolicitud estado,
        LocalDateTime fechaRegistro
) {
    public static SolicitudResponse from(Solicitud s) {
        return new SolicitudResponse(
                s.getId(),
                s.getClienteId(),
                s.getAsesorId(),
                s.getMonto(),
                s.getPlazoMeses(),
                s.getEstado(),
                s.getFechaRegistro()
        );
    }
}
