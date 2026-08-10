package com.johndoan.bookmarks.web;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A landing page at "/".
 *
 * Without this, Spring Security's form login succeeds and then redirects to "/",
 * which has no mapping — so a successful login lands the visitor on the
 * Whitelabel 404 error page and looks broken. That matters here because this
 * app is a public demo: "/" is the first thing anyone sees.
 *
 * Deliberately plain HTML with no template engine — the point of the project is
 * OAuth2 and deployment, not a UI.
 */
@RestController
public class HomeController {

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String home(Authentication authentication) {
        String who = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName()
                : null;

        String greeting = (who == null)
                ? """
                  <p class="muted">You are not signed in.</p>
                  <p><a class="btn" href="/login">Sign in</a>
                     <span class="muted">demo accounts: <code>john</code> / <code>password</code>
                     &nbsp;·&nbsp; <code>jane</code> / <code>password</code></span></p>
                  """
                : """
                  <p>Signed in as <strong>%s</strong>. <a href="/logout">Sign out</a></p>
                  <p class="muted">A browser session on its own does <em>not</em> open the API:
                     <code>/api/bookmarks</code> is guarded by OAuth2 <em>scopes</em>, which only
                     come from a token. That separation is the point — the login you just
                     completed is what the authorization-code flow uses to mint one.</p>
                  """.formatted(escape(who));

        return """
               <!doctype html>
               <html lang="en"><head><meta charset="utf-8">
               <meta name="viewport" content="width=device-width, initial-scale=1">
               <title>Bookmark API</title>
               <style>
                 :root { color-scheme: light dark; }
                 body { font-family: system-ui, -apple-system, "Segoe UI", sans-serif;
                        max-width: 44rem; margin: 3rem auto; padding: 0 1.25rem;
                        line-height: 1.6; }
                 h1 { margin-bottom: .25rem; }
                 code { background: rgba(127,127,127,.18); padding: .1em .35em;
                        border-radius: 4px; font-size: .92em; }
                 .muted { opacity: .75; font-size: .95rem; }
                 .btn { display: inline-block; padding: .45rem .9rem; border-radius: 8px;
                        background: #2b6cb0; color: #fff; text-decoration: none;
                        font-weight: 600; }
                 .btn:hover { background: #24527d; }
                 table { border-collapse: collapse; margin-top: .5rem; width: 100%%; }
                 td, th { text-align: left; padding: .35rem .6rem;
                          border-bottom: 1px solid rgba(127,127,127,.25); }
                 pre { background: rgba(127,127,127,.14); padding: .85rem;
                       border-radius: 8px; overflow-x: auto; font-size: .86rem; }
               </style></head><body>
                 <h1>Bookmark API</h1>
                 <p class="muted">A Spring Boot service that issues and validates its own JWTs —
                    running on Cloud Run.</p>
                 %s
                 <h2>Endpoints</h2>
                 <table>
                   <tr><th>Path</th><th>Access</th></tr>
                   <tr><td><code>/login</code></td><td>browser form login</td></tr>
                   <tr><td><code>/api/bookmarks</code></td><td>requires a Bearer token (scope <code>bookmark.read</code>)</td></tr>
                   <tr><td><code>/oauth2/token</code></td><td>issues JWTs</td></tr>
                   <tr><td><code>/actuator/health</code></td><td>public</td></tr>
                   <tr><td><code>/.well-known/oauth-authorization-server</code></td><td>public</td></tr>
                 </table>
                 <h2>Get a token from the command line</h2>
                 <pre>curl -u bruno-client:bruno-secret -X POST \\
                 /oauth2/token \\
                 -d grant_type=client_credentials -d scope=bookmark.read</pre>
                 <p class="muted">Demo data only. The database is in memory, so it resets
                    whenever the service scales to zero.</p>
               </body></html>
               """.formatted(greeting);
    }

    /** Usernames come from authentication, but never trust a value into HTML unescaped. */
    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
