package edu.upc.sistemas.tbcreditflow.audit;

import edu.upc.sistemas.tbcreditflow.audit.domain.AccionAuditoria;
import edu.upc.sistemas.tbcreditflow.audit.domain.entity.RegistroAuditoria;
import edu.upc.sistemas.tbcreditflow.audit.service.AuditService;
import edu.upc.sistemas.tbcreditflow.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** HU09/HU11 — sellado encadenado de auditoría y verificación de la cadena. */
class AuditServiceTest extends AbstractIntegrationTest {

    @Autowired
    private AuditService auditService;

    @Test
    void registrosSeEncadenanYLaCadenaEsVerificable() {
        var s1 = crearSolicitud(new BigDecimal("4200"), new BigDecimal("1000"),
                new BigDecimal("30000"), 24);
        var s2 = crearSolicitud(new BigDecimal("5200"), new BigDecimal("1500"),
                new BigDecimal("40000"), 36);

        RegistroAuditoria r1 = auditService.registrar(s1.getId(), AccionAuditoria.APROBADA, "comite_a");
        RegistroAuditoria r2 = auditService.registrar(s2.getId(), AccionAuditoria.RECHAZADA, "comite_b");

        // primer registro: hashPrevio vacío; hash de 64 hex
        assertThat(r1.getHashPrevio()).isEmpty();
        assertThat(r1.getHashIntegridad()).hasSize(64);

        // segundo registro: hashPrevio == hashIntegridad del primero (cadena)
        assertThat(r2.getHashPrevio()).isEqualTo(r1.getHashIntegridad());
        assertThat(r2.getHashIntegridad()).hasSize(64);

        assertThat(auditService.verificarCadena()).isTrue();
    }
}
