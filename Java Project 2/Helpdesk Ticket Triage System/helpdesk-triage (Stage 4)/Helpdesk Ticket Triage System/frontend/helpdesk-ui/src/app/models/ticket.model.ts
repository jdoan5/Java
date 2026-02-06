
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH';
export type TicketStatus = 'NEW' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';

export interface TicketResponse {
  id: number;
  title: string;
  description: string;
  priority: Priority;
  status: TicketStatus;
}

export interface CreateTicketRequest {
  title: string;
  description: string;
  priority: Priority;
}

export interface UpdateTicketRequest {
  title?: string;
  description?: string;
  priority?: Priority;
  status?: TicketStatus;
}
