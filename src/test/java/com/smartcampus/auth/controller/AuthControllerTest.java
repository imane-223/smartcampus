package com.smartcampus.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.smartcampus.auth.model.Administrateur;
import com.smartcampus.auth.model.Etudiant;
import com.smartcampus.auth.repository.UtilisateurRepository;
import com.smartcampus.support.RealCsrf;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    private static final String PASSWORD = "Secret123";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UtilisateurRepository repository;

    @Autowired
    private PasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        repository.save(new Etudiant("etudiant@usms.ac.ma", encoder.encode(PASSWORD), "Sara", "Alaoui"));
        repository.save(new Administrateur("admin@ensakh.ma", encoder.encode(PASSWORD), "Nadia", "Idrissi"));
        Etudiant desactive = new Etudiant("ancien@usms.ac.ma", encoder.encode(PASSWORD), "Omar", "Tazi");
        desactive.setActif(false);
        repository.save(desactive);
    }

    @AfterEach
    void cleanUp() {
        // La base H2 est partagée entre contextes : ne rien laisser aux autres classes de test
        repository.deleteAll();
    }

    private static String body(String courriel, String motDePasse) {
        return "{\"courriel\":\"" + courriel + "\",\"motDePasse\":\"" + motDePasse + "\"}";
    }

    private MvcResult login(String courriel, String motDePasse) throws Exception {
        return mvc.perform(post("/api/auth/login").with(RealCsrf.from(mvc))
                .contentType(MediaType.APPLICATION_JSON).content(body(courriel, motDePasse)))
                .andReturn();
    }

    @Test
    void successfulLoginReturnsUserAndOpensASession() throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login").with(RealCsrf.from(mvc))
                .contentType(MediaType.APPLICATION_JSON).content(body("Etudiant@USMS.ac.ma", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courriel").value("etudiant@usms.ac.ma"))
                .andExpect(jsonPath("$.prenom").value("Sara"))
                .andExpect(jsonPath("$.nom").value("Alaoui"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.motDePasseHash").doesNotExist())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courriel").value("etudiant@usms.ac.ma"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void realBrowserFlowWithCsrfCookieAndHeader() throws Exception {
        MvcResult csrf = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andReturn();
        // Un seul cookie XSRF-TOKEN : celui du filtre, identique au jeton du corps
        assertThat(csrf.getResponse().getCookies()).filteredOn(c -> c.getName().equals("XSRF-TOKEN")).hasSize(1);
        Cookie xsrf = csrf.getResponse().getCookie("XSRF-TOKEN");
        assertThat(xsrf).isNotNull();
        assertThat(xsrf.isHttpOnly()).isFalse();
        String token = com.jayway.jsonpath.JsonPath.read(csrf.getResponse().getContentAsString(), "$.token");
        assertThat(token).isEqualTo(xsrf.getValue());

        MvcResult login = mvc.perform(post("/api/auth/login").cookie(xsrf).header("X-XSRF-TOKEN", token)
                .contentType(MediaType.APPLICATION_JSON).content(body("admin@ensakh.ma", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn();

        // Après connexion, l'ancien jeton est effacé puis un nouveau est émis.
        // Le navigateur applique les Set-Cookie dans l'ordre : c'est le dernier qui compte.
        List<Cookie> xsrfCookies = Arrays.stream(login.getResponse().getCookies())
                .filter(c -> c.getName().equals("XSRF-TOKEN")).toList();
        assertThat(xsrfCookies).isNotEmpty();
        Cookie renewed = xsrfCookies.getLast();
        assertThat(renewed.getValue()).isNotBlank().isNotEqualTo(token);
    }

    @Test
    void loginWithoutCsrfTokenIsRefused() throws Exception {
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(body("etudiant@usms.ac.ma", PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("CSRF"));
    }

    @Test
    void unknownEmailAndWrongPasswordGiveTheSameGeneric401() throws Exception {
        MvcResult unknown = login("inconnu@usms.ac.ma", PASSWORD);
        MvcResult wrong = login("etudiant@usms.ac.ma", "mauvais");

        for (MvcResult result : new MvcResult[] { unknown, wrong }) {
            assertThat(result.getResponse().getStatus()).isEqualTo(401);
            assertThat(result.getResponse().getContentAsString())
                    .contains("\"error\":\"INVALID_CREDENTIALS\"")
                    .contains("E-mail ou mot de passe incorrect.");
        }
    }

    @Test
    void disabledAccountIsRefused() throws Exception {
        mvc.perform(post("/api/auth/login").with(RealCsrf.from(mvc))
                .contentType(MediaType.APPLICATION_JSON).content(body("ancien@usms.ac.ma", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ACCOUNT_DISABLED"))
                .andExpect(jsonPath("$.message").value("Ce compte est désactivé."));
    }

    @Test
    void lockedAccountGivesRelativeDelayRetryAtAndRetryAfter() throws Exception {
        var etudiant = repository.findByCourrielIgnoreCase("etudiant@usms.ac.ma").orElseThrow();
        Instant lockedUntil = Instant.now().plus(10, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS);
        etudiant.setTentativesEchouees(5);
        etudiant.setVerrouilleJusquA(lockedUntil);
        repository.save(etudiant);

        mvc.perform(post("/api/auth/login").with(RealCsrf.from(mvc))
                .contentType(MediaType.APPLICATION_JSON).content(body("etudiant@usms.ac.ma", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ACCOUNT_LOCKED"))
                .andExpect(jsonPath("$.message").value(
                        "Compte temporairement verrouillé après plusieurs échecs. Réessayez dans 10 minutes."))
                .andExpect(jsonPath("$.retryAt").value(lockedUntil.toString()))
                .andExpect(header().string("Retry-After", matchesPattern("5[0-9]{2}|600")));
    }

    @Test
    void lockedAccountWithWrongPasswordStaysGeneric() throws Exception {
        var etudiant = repository.findByCourrielIgnoreCase("etudiant@usms.ac.ma").orElseThrow();
        etudiant.setTentativesEchouees(5);
        etudiant.setVerrouilleJusquA(Instant.now().plus(10, ChronoUnit.MINUTES));
        repository.save(etudiant);

        mvc.perform(post("/api/auth/login").with(RealCsrf.from(mvc))
                .contentType(MediaType.APPLICATION_JSON).content(body("etudiant@usms.ac.ma", "mauvais")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.retryAt").doesNotExist())
                .andExpect(header().doesNotExist("Retry-After"));
    }

    @Test
    void blankFieldsGive400Validation() throws Exception {
        mvc.perform(post("/api/auth/login").with(RealCsrf.from(mvc))
                .contentType(MediaType.APPLICATION_JSON).content(body("", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION"))
                .andExpect(jsonPath("$.fields.courriel").value("L'e-mail est obligatoire."))
                .andExpect(jsonPath("$.fields.motDePasse").value("Le mot de passe est obligatoire."));
    }

    @Test
    void meWithoutSessionIsRejectedByTheEntryPointAsJson() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/auth/me"));
    }

    @Test
    void meAfterAccountDisabledEndsTheSession() throws Exception {
        MockHttpSession session = (MockHttpSession) login("etudiant@usms.ac.ma", PASSWORD)
                .getRequest().getSession(false);
        var etudiant = repository.findByCourrielIgnoreCase("etudiant@usms.ac.ma").orElseThrow();
        etudiant.setActif(false);
        repository.save(etudiant);

        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
        assertThat(session.isInvalid()).isTrue();
    }

    // --- Déconnexion (SC1-3) ---

    @Test
    void logoutInvalidatesTheSession() throws Exception {
        MockHttpSession session = (MockHttpSession) login("etudiant@usms.ac.ma", PASSWORD)
                .getRequest().getSession(false);
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk());

        mvc.perform(post("/api/auth/logout").session(session).with(RealCsrf.from(mvc)))
                .andExpect(status().isNoContent());

        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutWithoutCsrfTokenIsRefused() throws Exception {
        MockHttpSession session = (MockHttpSession) login("etudiant@usms.ac.ma", PASSWORD)
                .getRequest().getSession(false);

        mvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("CSRF"));
        assertThat(session.isInvalid()).isFalse();
    }
}
