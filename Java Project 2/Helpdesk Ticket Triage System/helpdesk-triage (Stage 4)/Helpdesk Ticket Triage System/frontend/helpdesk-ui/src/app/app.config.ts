import { ApplicationConfig } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  // Without provideRouter the <router-outlet /> in app.html renders nothing and
  // app.routes.ts is dead code — the tickets page is then reachable only from
  // tests, never from a browser.
  providers: [provideHttpClient(), provideRouter(routes)],
};
