package com.smartcampus.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import com.smartcampus.auth.model.Administrateur;
import com.smartcampus.auth.model.Enseignant;
import com.smartcampus.auth.model.Etudiant;
import com.smartcampus.auth.model.Role;
import com.smartcampus.auth.model.Utilisateur;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UtilisateurRepositoryTest {

    private static final String HASH = "$2a$10$hashfictifpourlestests";

    @Autowired
    private UtilisateurRepository repository;

    @Autowired
    private TestEntityManager em;

    @Test
    void eachSubtypeIsStoredWithItsRole() {
        Etudiant etudiant = new Etudiant("etudiant@usms.ac.ma", HASH, "Sara", "Alaoui");
        etudiant.setCodeMassar("K130000000");
        etudiant.setNumero("E001");
        Enseignant enseignant = new Enseignant("prof@usms.ma", HASH, "Karim", "Bennani");
        enseignant.setGrade("PES");
        enseignant.setNumero("P001");
        repository.save(etudiant);
        repository.save(enseignant);
        repository.save(new Administrateur("admin@ensakh.ma", HASH, "Nadia", "Idrissi"));
        em.flush();
        em.clear();

        assertThat(repository.findAll())
                .extracting(Utilisateur::getRole)
                .containsExactlyInAnyOrder(Role.STUDENT, Role.TEACHER, Role.ADMIN);
        Utilisateur relu = repository.findByCourrielIgnoreCase("etudiant@usms.ac.ma").orElseThrow();
        assertThat(relu).isInstanceOf(Etudiant.class);
        assertThat(((Etudiant) relu).getCodeMassar()).isEqualTo("K130000000");
        assertThat(relu.getCreeLe()).isNotNull();
        assertThat(relu.isActif()).isTrue();
        assertThat(relu.getTentativesEchouees()).isZero();
    }

    @Test
    void emailIsStoredInLowerCaseAndFoundIgnoringCase() {
        repository.saveAndFlush(new Etudiant("  Sara.Alaoui@USMS.ac.ma ", HASH, "Sara", "Alaoui"));
        em.clear();

        Utilisateur relu = repository.findByCourrielIgnoreCase("SARA.ALAOUI@usms.ac.ma").orElseThrow();
        assertThat(relu.getCourriel()).isEqualTo("sara.alaoui@usms.ac.ma");
    }

    @Test
    void emailDifferingOnlyByCaseIsRejected() {
        repository.saveAndFlush(new Etudiant("doublon@usms.ac.ma", HASH, "A", "B"));

        assertThatThrownBy(() -> repository.saveAndFlush(new Etudiant("DOUBLON@usms.ac.ma", HASH, "C", "D")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void emailDomainMustMatchRole() {
        assertThatThrownBy(() -> repository.saveAndFlush(new Etudiant("etudiant@usms.ma", HASH, "A", "B")))
                .hasStackTraceContaining("ne correspond pas au domaine @usms.ac.ma");
    }

    @Test
    void unknownEmailGivesEmptyResult() {
        assertThat(repository.findByCourrielIgnoreCase("inconnu@usms.ac.ma")).isEmpty();
    }
}
