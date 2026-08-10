package com.johndoan.bookmarks.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * A custom sign-in page, replacing Spring Security's generated one.
 *
 * The generated page is fine while developing — you already know the password.
 * On a PUBLIC demo it is a dead end: a visitor is asked for credentials with no
 * way to discover them. This page states the demo accounts plainly and can fill
 * them in, so anyone can get past the door.
 *
 * Rendered as a plain string because the project has no template engine; the
 * CSRF token is read from the request attribute Spring Security populates.
 */
@RestController
public class LoginController {

    @GetMapping(value = "/login", produces = MediaType.TEXT_HTML_VALUE)
    public String login(HttpServletRequest request,
                        @RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout) {

        CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        String csrfField = (csrf == null) ? "" :
                "<input type=\"hidden\" name=\"" + csrf.getParameterName()
                        + "\" value=\"" + csrf.getToken() + "\">";

        String banner = "";
        if (error != null) {
            banner = "<p class=\"note bad\">That username and password did not match. "
                    + "Try one of the demo accounts below.</p>";
        } else if (logout != null) {
            banner = "<p class=\"note ok\">Signed out.</p>";
        }

        return """
               <!doctype html>
               <html lang="en"><head><meta charset="utf-8">
               <meta name="viewport" content="width=device-width, initial-scale=1">
               <title>Sign in · Bookmark API</title>
               <style>
                 :root { color-scheme: light dark; }
                 body { font-family: system-ui, -apple-system, "Segoe UI", sans-serif;
                        max-width: 26rem; margin: 3.5rem auto; padding: 0 1.25rem;
                        line-height: 1.55; }
                 h1 { margin: 0 0 .25rem; font-size: 1.6rem; }
                 .sub { opacity: .7; font-size: .95rem; margin: 0 0 1.5rem; }
                 label { display: block; font-size: .85rem; font-weight: 600;
                         margin: .9rem 0 .3rem; }
                 input[type=text], input[type=password] {
                        width: 100%%; padding: .6rem .7rem; font-size: 1rem;
                        border: 1px solid rgba(127,127,127,.5); border-radius: 8px;
                        background: transparent; color: inherit; }
                 button[type=submit] { width: 100%%; margin-top: 1.2rem; padding: .65rem;
                        font-size: 1rem; font-weight: 600; border: 0; border-radius: 8px;
                        background: #2b6cb0; color: #fff; cursor: pointer; }
                 button[type=submit]:hover { background: #24527d; }
                 .demo { margin-top: 1.75rem; padding: .9rem 1rem;
                         border: 1px dashed rgba(127,127,127,.55); border-radius: 10px; }
                 .demo h2 { margin: 0 0 .4rem; font-size: .8rem; text-transform: uppercase;
                            letter-spacing: .06em; opacity: .75; }
                 .demo p { margin: .2rem 0; font-size: .92rem; }
                 code { background: rgba(127,127,127,.18); padding: .1em .35em;
                        border-radius: 4px; }
                 .fill { background: none; border: 0; padding: 0; color: #2b6cb0;
                         font: inherit; font-weight: 600; cursor: pointer;
                         text-decoration: underline; }
                 .note { padding: .6rem .8rem; border-radius: 8px; font-size: .92rem; }
                 .note.bad { background: rgba(200,60,60,.15); }
                 .note.ok  { background: rgba(60,160,90,.15); }
                 .back { display: inline-block; margin-top: 1.25rem; font-size: .9rem;
                         opacity: .8; }
               </style></head><body>
                 <h1>Sign in</h1>
                 <p class="sub">Bookmark API — a public demo. No real data.</p>
                 %s
                 <form method="post" action="/login">
                   %s
                   <label for="username">Username</label>
                   <input type="text" id="username" name="username" autocomplete="username" autofocus>
                   <label for="password">Password</label>
                   <input type="password" id="password" name="password" autocomplete="current-password">
                   <button type="submit">Sign in</button>
                 </form>

                 <div class="demo">
                   <h2>Demo accounts</h2>
                   <p><code>john</code> / <code>password</code>
                      — <button class="fill" type="button" onclick="fill('john')">use</button></p>
                   <p><code>jane</code> / <code>password</code>
                      — <button class="fill" type="button" onclick="fill('jane')">use</button></p>
                   <p style="opacity:.7;font-size:.85rem;margin-top:.5rem">
                      Each account sees only its own bookmarks.</p>
                 </div>

                 <a class="back" href="/">&larr; Back to the overview</a>

                 <script>
                   function fill(user) {
                     document.getElementById('username').value = user;
                     document.getElementById('password').value = 'password';
                     document.getElementById('password').focus();
                   }
                 </script>
               </body></html>
               """.formatted(banner, csrfField);
    }
}
