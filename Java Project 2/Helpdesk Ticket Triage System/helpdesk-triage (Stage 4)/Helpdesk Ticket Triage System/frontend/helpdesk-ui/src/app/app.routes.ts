import { Routes } from '@angular/router';
import { TicketsComponent } from './pages/tickets/tickets.component';

export const routes: Routes = [
  { path: 'tickets', component: TicketsComponent },
  { path: '', redirectTo: 'tickets', pathMatch: 'full' }
];
