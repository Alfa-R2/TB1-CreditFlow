package edu.upc.sistemas.tbcreditflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

/**
 * La autenticación es manual (AuthService + filtro JWT), por lo que no se necesita el
 * UserDetailsService por defecto de Spring Boot; se excluye su autoconfiguración.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class TbCreditFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(TbCreditFlowApplication.class, args);
    }

}
