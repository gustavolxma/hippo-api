package com.backend.hippo_api;

import com.backend.hippo_api.infrastructure.email.EmailService;
import com.backend.hippo_api.infrastructure.email.LogEmailService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

// Sobe a aplicação inteira (com banco H2 em memória no lugar do PostgreSQL) para conferir
// que todos os componentes e propriedades se encaixam.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:hippo;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class HippoApiApplicationTests {

    @Autowired
    private EmailService emailService;

    @Test
    void aplicacaoSobeComEmailEmModoLogPorPadrao() {
        assertThat(emailService).isInstanceOf(LogEmailService.class);
    }
}
