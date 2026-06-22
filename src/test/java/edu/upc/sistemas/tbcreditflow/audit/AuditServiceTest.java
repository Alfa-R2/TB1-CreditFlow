package edu.upc.sistemas.tbcreditflow.audit;

import edu.upc.sistemas.tbcreditflow.audit.domain.AccionAuditoria;
import edu.upc.sistemas.tbcreditflow.audit.domain.RegistroAuditoria;
import edu.upc.sistemas.tbcreditflow.audit.service.AuditService;
import edu.upc.sistemas.tbcreditflow.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** HU09/HU11 — sellado encadenado de auditoría y verificación de la cadena. */
class AuditServiceTest extends AbstractIntegrationTest {

    @Autowired
    private AuditService auditService;

    @Test
    void registrosSeEncadenanYLaCadenaEsVerificable() {
        RegistroAuditoria r1 = auditService.registrar(101L, AccionAuditoria.APROBADA, "comite_a");
        RegistroAuditoria r2 = auditService.registrar(102L, AccionAuditoria.RECHAZADA, "comite_b");

        // primer registro: hashPrevio vacío; hash de 64 hex
        assertThat(r1.getHashPrevio()).isEmpty();
        assertThat(r1.getHashIntegridad()).hasSize(64);

        // segundo registro: hashPrevio == hashIntegridad del primero (cadena)
        assertThat(r2.getHashPrevio()).isEqualTo(r1.getHashIntegridad());
        assertThat(r2.getHashIntegridad()).hasSize(64);

        assertThat(auditService.verificarCadena()).isTrue();
    }
}
