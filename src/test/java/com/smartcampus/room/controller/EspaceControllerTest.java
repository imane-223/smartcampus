package com.smartcampus.room.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.smartcampus.auth.model.Role;
import com.smartcampus.auth.security.AuthenticatedUser;
import com.smartcampus.room.model.Batiment;
import com.smartcampus.room.model.Espace;
import com.smartcampus.room.model.StatutEspace;
import com.smartcampus.room.model.TypeEspace;
import com.smartcampus.room.repository.BatimentRepository;
import com.smartcampus.room.repository.EspaceRepository;
import com.smartcampus.support.RealCsrf;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EspaceControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private EspaceRepository espaces;
    @Autowired private BatimentRepository batiments;
    private Batiment batiment;

    @BeforeEach
    void setUp() {
        espaces.deleteAll();
        batiments.deleteAll();
        batiment = batiments.saveAndFlush(new Batiment("Bâtiment A"));
    }

    @Test
    void anonymousCannotConsultTheCatalog() throws Exception {
        mvc.perform(get("/api/salles"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void anonymousCannotCreateSpaceEvenWithAValidCsrfToken() throws Exception {
        mvc.perform(post("/api/salles").with(RealCsrf.from(mvc))
                        .contentType(MediaType.APPLICATION_JSON).content(createBody("SAL-A101")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void studentsAndTeachersCanConsultButCannotCreateOrModify() throws Exception {
        Espace espace = save("SAL-A101", "Salle A101", StatutEspace.ACTIVE, false);
        for (Role role : List.of(Role.STUDENT, Role.TEACHER)) {
            MockHttpSession session = sessionAs(role);
            mvc.perform(get("/api/salles").session(session)).andExpect(status().isOk());
            mvc.perform(post("/api/salles").session(session).with(RealCsrf.from(mvc))
                            .contentType(MediaType.APPLICATION_JSON).content(createBody("SAL-A102")))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("FORBIDDEN"));
            mvc.perform(put("/api/salles/{id}", espace.getId()).session(session).with(RealCsrf.from(mvc))
                            .contentType(MediaType.APPLICATION_JSON).content(updateBody("SAL-A101", false)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void adminCreatesSpaceAndReceivesDto() throws Exception {
        mvc.perform(post("/api/salles").session(sessionAs(Role.ADMIN)).with(RealCsrf.from(mvc))
                        .contentType(MediaType.APPLICATION_JSON).content(createBody("sal-a101")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.code").value("SAL-A101"))
                .andExpect(jsonPath("$.nom").value("Salle A101"))
                .andExpect(jsonPath("$.batiment.id").value(batiment.getId()))
                .andExpect(jsonPath("$.reservable").value(true));
    }

    @Test
    void adminCanAddBuildingAndOtherRolesCannot() throws Exception {
        mvc.perform(post("/api/salles/batiments").session(sessionAs(Role.ADMIN)).with(RealCsrf.from(mvc))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nom\":\"Bâtiment B\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom").value("Bâtiment B"));
        mvc.perform(post("/api/salles/batiments").session(sessionAs(Role.STUDENT)).with(RealCsrf.from(mvc))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nom\":\"Bâtiment C\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void validationAndBuildingReferencesReturnBadRequestAndNotFound() throws Exception {
        mvc.perform(post("/api/salles").session(sessionAs(Role.ADMIN)).with(RealCsrf.from(mvc))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"code":"SAL-X","nom":" ","type":"CLASSROOM","capacite":0,"batimentId":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION"))
                .andExpect(jsonPath("$.fields.nom").exists())
                .andExpect(jsonPath("$.fields.capacite").exists());

        mvc.perform(post("/api/salles").session(sessionAs(Role.ADMIN)).with(RealCsrf.from(mvc))
                        .contentType(MediaType.APPLICATION_JSON).content(createBody("SAL-X", 999L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void duplicateCodeReturnsConflict() throws Exception {
        save("SAL-A101", "Salle A101", StatutEspace.ACTIVE, false);
        mvc.perform(post("/api/salles").session(sessionAs(Role.ADMIN)).with(RealCsrf.from(mvc))
                        .contentType(MediaType.APPLICATION_JSON).content(createBody("sal-a101")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    void adminUpdatesSpaceAndAllowsItsUnchangedCode() throws Exception {
        Espace espace = save("SAL-A101", "Salle A101", StatutEspace.ACTIVE, false);
        mvc.perform(put("/api/salles/{id}", espace.getId()).session(sessionAs(Role.ADMIN)).with(RealCsrf.from(mvc))
                        .contentType(MediaType.APPLICATION_JSON).content(updateBody("SAL-A101", false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Salle A101 rénovée"));
    }

    @Test
    void catalogueFiltersByTypeBuildingStatusAndTextAndHidesArchivedSpaces() throws Exception {
        save("SAL-A101", "Salle A101", StatutEspace.ACTIVE, false);
        save("LAB-02", "Laboratoire informatique", StatutEspace.MAINTENANCE, false, TypeEspace.COMPUTER_LAB);
        save("OLD-01", "Ancienne salle", StatutEspace.ACTIVE, true);

        mvc.perform(get("/api/salles").session(sessionAs(Role.STUDENT))
                        .param("type", "COMPUTER_LAB").param("batimentId", batiment.getId().toString())
                        .param("statut", "MAINTENANCE").param("search", "labora"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].code").value("LAB-02"))
                .andExpect(jsonPath("$.content[0].reservable").value(false));

        mvc.perform(get("/api/salles").session(sessionAs(Role.TEACHER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void archivedOrMissingSpaceIsNotAvailableInCatalogDetails() throws Exception {
        Espace archived = save("OLD-01", "Ancienne salle", StatutEspace.ACTIVE, true);
        mvc.perform(get("/api/salles/{id}", archived.getId()).session(sessionAs(Role.STUDENT)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/salles/99999").session(sessionAs(Role.STUDENT)))
                .andExpect(status().isNotFound());
    }

    private Espace save(String code, String nom, StatutEspace statut, boolean archive) {
        return save(code, nom, statut, archive, TypeEspace.CLASSROOM);
    }

    private Espace save(String code, String nom, StatutEspace statut, boolean archive, TypeEspace type) {
        return espaces.saveAndFlush(new Espace(code, nom, type, 30, batiment, 1, null, statut, archive));
    }

    private String createBody(String code) {
        return createBody(code, batiment.getId());
    }

    private static String createBody(String code, Long buildingId) {
        return """ 
                {"code":"%s","nom":"Salle A101","type":"CLASSROOM","capacite":40,"batimentId":%d}
                """.formatted(code, buildingId);
    }

    private String updateBody(String code, boolean archived) {
        return """
                {"code":"%s","nom":"Salle A101 rénovée","type":"CLASSROOM","capacite":42,
                 "batimentId":%d,"etage":2,"description":"Mise à jour","statut":"ACTIVE","archive":%s}
                """.formatted(code, batiment.getId(), archived);
    }

    private static MockHttpSession sessionAs(Role role) {
        String email = switch (role) {
            case ADMIN -> "admin@ensakh.ma";
            case TEACHER -> "prof@usms.ma";
            case STUDENT -> "etudiant@usms.ac.ma";
        };
        var principal = new AuthenticatedUser(1L, email, role);
        var authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null,
                List.of(new SimpleGrantedAuthority(role.authority())));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(authentication));
        return session;
    }
}
