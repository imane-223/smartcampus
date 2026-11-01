package com.smartcampus.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartcampus.auth.model.Role;
import com.smartcampus.auth.security.AuthenticatedUser;

/**
 * Le motif d'autorisation (SC1-51), éprouvé sur un contrôleur de démonstration qui n'existe
 * que dans ce test : règles par préfixe dans SecurityConfig, puis @PreAuthorize par méthode.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuthorizationTest.PingController.class)
class AuthorizationTest {

    @RestController
    static class PingController {

        @GetMapping("/api/admin/ping")
        @PreAuthorize("hasRole('ADMIN')")
        String adminPing() {
            return "admin";
        }

        @GetMapping("/api/salles/ping")
        @PreAuthorize("isAuthenticated()")
        String sallesPing() {
            return "salles";
        }

        @GetMapping("/api/salles/creer")
        @PreAuthorize("hasRole('ADMIN')")
        String sallesAdminOnly() {
            return "creee";
        }

        @GetMapping("/api/reservations/moi")
        @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
        String whoAmI(@AuthenticationPrincipal AuthenticatedUser user) {
            return user.courriel();
        }
    }

    @Autowired
    private MockMvc mvc;

    /** Session telle que la crée AuthController après une connexion réussie. */
    private static MockHttpSession sessionAs(Role role, String courriel) {
        AuthenticatedUser principal = new AuthenticatedUser(1L, courriel, role);
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, List.of(new SimpleGrantedAuthority(role.authority())));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(authentication));
        return session;
    }

    private static final MockHttpSession STUDENT = sessionAs(Role.STUDENT, "etudiant@usms.ac.ma");

    // --- Non authentifié : 401 ---

    @Test
    void anonymousGets401OnEveryProtectedPrefix() throws Exception {
        for (String url : new String[] { "/api/admin/ping", "/api/salles/ping", "/api/reservations/moi" }) {
            mvc.perform(get(url))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        }
    }

    // --- Mauvais rôle : 403 ---

    @Test
    void studentGets403OnAdminPrefix() throws Exception {
        mvc.perform(get("/api/admin/ping").session(STUDENT))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Vous n'avez pas les droits nécessaires."));
    }

    @Test
    void preAuthorizeRefusesAStudentInsideAnAuthenticatedPrefix() throws Exception {
        mvc.perform(get("/api/salles/creer").session(STUDENT))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void preAuthorizeRefusesAnAdminWhereOnlyStudentsAndTeachersAreAllowed() throws Exception {
        mvc.perform(get("/api/reservations/moi").session(sessionAs(Role.ADMIN, "admin@ensakh.ma")))
                .andExpect(status().isForbidden());
    }

    // --- Bon rôle : 200 ---

    @Test
    void adminReachesAdminEndpoints() throws Exception {
        MockHttpSession admin = sessionAs(Role.ADMIN, "admin@ensakh.ma");
        mvc.perform(get("/api/admin/ping").session(admin)).andExpect(status().isOk());
        mvc.perform(get("/api/salles/creer").session(admin)).andExpect(status().isOk());
    }

    @Test
    void anyAuthenticatedUserReachesSalles() throws Exception {
        mvc.perform(get("/api/salles/ping").session(STUDENT)).andExpect(status().isOk());
        mvc.perform(get("/api/salles/ping").session(sessionAs(Role.TEACHER, "prof@usms.ma")))
                .andExpect(status().isOk());
    }

    @Test
    void currentUserIsInjectedWithAuthenticationPrincipal() throws Exception {
        mvc.perform(get("/api/reservations/moi").session(sessionAs(Role.TEACHER, "prof@usms.ma")))
                .andExpect(status().isOk())
                .andExpect(content().string("prof@usms.ma"));
    }
}
