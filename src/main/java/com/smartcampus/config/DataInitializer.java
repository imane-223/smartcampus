package com.smartcampus.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.smartcampus.auth.model.Administrateur;
import com.smartcampus.auth.model.Enseignant;
import com.smartcampus.auth.model.Etudiant;
import com.smartcampus.auth.model.Utilisateur;
import com.smartcampus.auth.repository.UtilisateurRepository;

/**
 * Comptes de démonstration (données fictives). Désactivé par défaut : il faut
 * smartcampus.demo-data.enabled=true, ce qui ne doit jamais être le cas en production.
 * Idempotent : chaque compte n'est créé que s'il n'existe pas déjà.
 */
@Component
@ConditionalOnBooleanProperty("smartcampus.demo-data.enabled")
public class DataInitializer implements ApplicationRunner {

    /** Mot de passe commun des comptes de démonstration, documenté dans le README. */
    public static final String DEMO_PASSWORD = "Demo2026!";

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UtilisateurRepository repository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UtilisateurRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String hash = passwordEncoder.encode(DEMO_PASSWORD);

        Etudiant etudiant = new Etudiant("etudiant@usms.ac.ma", hash, "Sara", "Alaoui");
        etudiant.setCodeMassar("K130000001");
        etudiant.setFiliere("Génie informatique");
        Enseignant enseignant = new Enseignant("prof@usms.ma", hash, "Karim", "Bennani");
        enseignant.setGrade("PES");
        enseignant.setSpecialite("Génie logiciel");
        Administrateur admin = new Administrateur("admin@ensakh.ma", hash, "Nadia", "Idrissi");
        Etudiant desactive = new Etudiant("ancien@usms.ac.ma", hash, "Omar", "Tazi");
        desactive.setActif(false);

        int created = 0;
        for (Utilisateur compte : new Utilisateur[] { etudiant, enseignant, admin, desactive }) {
            if (repository.findByCourrielIgnoreCase(compte.getCourriel()).isEmpty()) {
                repository.save(compte);
                created++;
                log.info("Compte de démonstration créé : {} ({})", compte.getCourriel(), compte.getRole());
            }
        }
        if (created == 0) {
            log.info("Comptes de démonstration déjà présents : rien à créer.");
        }
    }
}
