package com.johndoan.bookmarks.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Landing page for the OAuth2 redirect URI (registered as
 * http://localhost:8080/authorized). When Bruno drives the login flow it
 * intercepts this redirect to grab the authorization code, so this page is
 * mostly for the case where you complete the flow in a browser — it just shows
 * that the redirect arrived.
 */
@RestController
public class AuthorizedController {

    /**
     * {@code produces = TEXT_PLAIN_VALUE} is load-bearing, not decoration.
     *
     * Without it this method returns a bare String and Spring picks the content
     * type by NEGOTIATION — a browser sending {@code Accept: text/html} gets the
     * reply labelled {@code text/html}, so anything reflected from the query
     * string is parsed as markup and executes on this origin. That is a
     * reflected XSS on a public, permitAll URL. Pinning text/plain means the
     * browser renders it as text; the values are escaped as well, so neither
     * mistake alone is enough to reintroduce the hole.
     */
    @GetMapping(value = "/authorized", produces = MediaType.TEXT_PLAIN_VALUE)
    public String authorized(@RequestParam(required = false) String code,
                             @RequestParam(required = false) String error) {
        if (error != null) {
            return "Authorization failed: " + sanitize(error);
        }
        String shown = Optional.ofNullable(code)
                .map(AuthorizedController::sanitize)
                .map(c -> c.length() > 12 ? c.substring(0, 12) + "..." : c)
                .orElse("(none)");
        return "Authorization code received (" + shown + "). "
                + "Your client now exchanges it for a token at /oauth2/token. "
                + "You can close this tab.";
    }

    /**
     * Strips the characters that let a reflected value break out into markup.
     * Belt-and-braces alongside the text/plain content type above: an OAuth
     * {@code code} or {@code error} never legitimately contains these.
     */
    private static String sanitize(String value) {
        return value.replaceAll("[<>&\"'`\\r\\n]", "");
    }
}
