package com.johndoan.bookmarks.web;

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

    @GetMapping("/authorized")
    public String authorized(@RequestParam(required = false) String code,
                             @RequestParam(required = false) String error) {
        if (error != null) {
            return "Authorization failed: " + error;
        }
        String shown = Optional.ofNullable(code)
                .map(c -> c.length() > 12 ? c.substring(0, 12) + "..." : c)
                .orElse("(none)");
        return "Authorization code received (" + shown + "). "
                + "Your client now exchanges it for a token at /oauth2/token. "
                + "You can close this tab.";
    }
}
