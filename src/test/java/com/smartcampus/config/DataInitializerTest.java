package com.smartcampus.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.smartcampus.auth.model.Role;
import com.smartcampus.auth.model.Utilisateur;
import com.smartcampus.auth.repository.UtilisateurRepository;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DataInitializerTest {

    @Autowired
    private UtilisateurRepository repository;

    @Test
    void createsFourDemoAccountsOnlyOnce() {
        DataInitializer initializer = new DataInitializer(repository, new BCryptPasswordEncoder(4));

        initializer.run(null);
        initializer.run(null);

        assertThat(repository.count()).isEqualTo(4);
        assertThat(repository.findAll()).extracting(Utilisateur::getRole)
                .containsExactlyInAnyOrder(Role.STUDENT, Role.STUDENT, Role.TEACHER, Role.ADMIN);
        assertThat(repository.findByCourrielIgnoreCase("ancien@usms.ac.ma").orElseThrow().isActif()).isFalse();
    }

    @Test
    void recreatesOnlyTheMissingAccount() {
        DataInitializer initializer = new DataInitializer(repository, new BCryptPasswordEncoder(4));
        initializer.run(null);
        repository.delete(repository.findByCourrielIgnoreCase("prof@usms.ma").orElseThrow());

        initializer.run(null);

        assertThat(repository.count()).isEqualTo(4);
    }

    @Test
    void passwordsAreStoredAsBcryptHashes() {
        new DataInitializer(repository, new BCryptPasswordEncoder(4)).run(null);
        assertThat(repository.findAll()).allSatisfy(u -> assertThat(u.getMotDePasseHash())
                .startsWith("$2a$")
                .doesNotContain(DataInitializer.DEMO_PASSWORD));
    }
}
