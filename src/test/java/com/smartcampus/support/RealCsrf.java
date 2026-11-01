package com.smartcampus.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import jakarta.servlet.http.Cookie;

/**
 * Jeton CSRF obtenu comme le ferait le frontend : GET /api/auth/csrf, puis cookie XSRF-TOKEN
 * renvoyé avec l'en-tête X-XSRF-TOKEN.
 * <p>
 * N'utilisez pas csrf() de spring-security-test : il remplace durablement le dépôt du filtre
 * CSRF (cookie) par un dépôt en session, ce qui fausse les tests suivants du même contexte.
 */
public final class RealCsrf {

    private RealCsrf() {
    }

    public static RequestPostProcessor from(MockMvc mvc) throws Exception {
        Cookie cookie = mvc.perform(get("/api/auth/csrf")).andReturn().getResponse().getCookie("XSRF-TOKEN");
        return request -> {
            request.setCookies(cookie);
            request.addHeader("X-XSRF-TOKEN", cookie.getValue());
            return request;
        };
    }
}
