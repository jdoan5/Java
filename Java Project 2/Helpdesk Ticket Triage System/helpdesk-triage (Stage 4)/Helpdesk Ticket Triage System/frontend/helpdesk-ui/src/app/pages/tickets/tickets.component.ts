import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TicketService } from '../../services/ticket.service';
import { TicketResponse, CreateTicketRequest, Priority } from '../../models/ticket.model';

@Component({
  selector: 'app-tickets',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './tickets.component.html',
})
export class TicketsComponent implements OnInit {
  tickets: TicketResponse[] = [];
  loading = false;
  error: string | null = null;

  form: CreateTicketRequest = {
    title: '',
    description: '',
    priority: 'MEDIUM'
  };

  priorities: Priority[] = ['LOW', 'MEDIUM', 'HIGH'];

  constructor(private ticketsApi: TicketService) {}

  ngOnInit(): void {
    this.loadTickets();
  }

  loadTickets(): void {
    this.loading = true;
    this.error = null;

    this.ticketsApi.getAll().subscribe({
      next: (data) => {
        this.tickets = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.message ?? 'Failed to load tickets';
        this.loading = false;
      }
    });
  }

  createTicket(): void {
    this.error = null;

    this.ticketsApi.create(this.form).subscribe({
      next: () => {
        this.form = { title: '', description: '', priority: 'MEDIUM' };
        this.loadTickets();
      },
      error: (err) => {
        this.error = err?.error?.message ?? 'Failed to create ticket';
      }
    });
  }
}
