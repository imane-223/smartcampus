package com.smartcampus.auth.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DeferredCsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartcampus.auth.dto.CsrfResponse;
import com.smartcampus.auth.dto.LoginRequest;
import com.smartcampus.auth.dto.UserResponse;
import com.smartcampus.auth.model.Utilisateur;
import com.smartcampus.auth.security.AuthenticatedUser;
import com.smartcampus.auth.service.AuthService;
import com.smartcampus.auth.service.LoginOutcome;
import com.smartcampus.auth.service.LoginResult;
import com.smartcampus.common.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String CSRF_HEADER = "X-XSRF-TOKEN";

    private final AuthService authService;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionStrategy;
    private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();

    public AuthController(AuthService authService, SecurityContextRepository securityContextRepository,
            CsrfTokenRepository csrfTokenRepository) {
        this.authService = authService;
        this.securityContextRepository = securityContextRepository;
        // Ce que Spring ferait après un formLogin : nouvel identifiant de session (fixation) et nouveau jeton CSRF
        this.sessionStrategy = new CompositeSessionAuthenticationStrategy(List.of(
                new ChangeSessionIdAuthenticationStrategy(),
                new CsrfAuthenticationStrategy(csrfTokenRepository)));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest body, HttpServletRequest request,
            HttpServletResponse response) {
        LoginOutcome outcome = authService.login(body.courriel(), body.motDePasse());
        if (!outcome.isSuccess()) {
            return refused(outcome, request);
        }

        Utilisateur utilisateur = outcome.utilisateur();
        AuthenticatedUser principal = new AuthenticatedUser(
                utilisateur.getId(), utilisateur.getCourriel(), utilisateur.getRole());
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, List.of(new SimpleGrantedAuthority(utilisateur.getRole().authority())));

        sessionStrategy.onAuthentication(authentication, request, response);
        SecurityContext context = contextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        contextHolder.setContext(context);
        // Spring Security 6+ ne sauvegarde plus le contexte tout seul
        securityContextRepository.saveContext(context, request, response);

        // Charge le nouveau jeton CSRF pour que son cookie parte dans cette réponse
        CsrfToken newToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (newToken != null) {
            newToken.getToken();
        }
        return ResponseEntity.ok(UserResponse.from(utilisateur));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal AuthenticatedUser principal, HttpServletRequest request) {
        Optional<Utilisateur> utilisateur = authService.findActiveUser(principal.id());
        if (utilisateur.isEmpty()) {
            // Compte supprimé ou désactivé depuis la connexion : la session ne vaut plus rien
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse.of(401, "UNAUTHORIZED",
                    "Authentification requise.", request.getRequestURI()));
        }
        return ResponseEntity.ok(UserResponse.from(utilisateur.get()));
    }

    /** Amorce le cookie XSRF-TOKEN et renvoie aussi le jeton, pour un frontend qui ne peut pas lire le cookie. */
    @GetMapping("/csrf")
    public CsrfResponse csrf(HttpServletRequest request) {
        // Jeton brut chargé par le CsrfFilter (celui du cookie), à renvoyer tel quel dans l'en-tête.
        // L'attribut CsrfToken, lui, est masqué (BREACH) et ne conviendrait pas pour l'en-tête.
        DeferredCsrfToken token = (DeferredCsrfToken) request.getAttribute(DeferredCsrfToken.class.getName());
        return new CsrfResponse(CSRF_HEADER, token.get().getToken());
    }

    private ResponseEntity<ErrorResponse> refused(LoginOutcome outcome, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.of(401, outcome.result().name(), outcome.message(),
                request.getRequestURI());
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.UNAUTHORIZED);
        if (outcome.result() == LoginResult.ACCOUNT_LOCKED) {
            error = error.withRetryAt(outcome.lockedUntil());
            builder.header(HttpHeaders.RETRY_AFTER, String.valueOf(outcome.retryAfterSeconds()));
        }
        return builder.body(error);
    }
}
