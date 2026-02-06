import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  TicketResponse,
  CreateTicketRequest,
  UpdateTicketRequest
} from '../models/ticket.model';

@Injectable({ providedIn: 'root' })
export class TicketService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/tickets`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<TicketResponse[]> {
    return this.http.get<TicketResponse[]>(this.baseUrl);
  }

  getById(id: number): Observable<TicketResponse> {
    return this.http.get<TicketResponse>(`${this.baseUrl}/${id}`);
  }

  create(req: CreateTicketRequest): Observable<TicketResponse> {
    return this.http.post<TicketResponse>(this.baseUrl, req);
  }

  // Use PUT if your backend expects full replace, PATCH for partial update
  updatePartial(id: number, req: UpdateTicketRequest): Observable<TicketResponse> {
    return this.http.patch<TicketResponse>(`${this.baseUrl}/${id}`, req);
  }

  replace(id: number, req: CreateTicketRequest & { status?: any }): Observable<TicketResponse> {
    return this.http.put<TicketResponse>(`${this.baseUrl}/${id}`, req);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
