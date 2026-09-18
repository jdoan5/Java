import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';

import { TicketService } from './ticket.service';
import { environment } from '../../environments/environment';
import { TicketResponse } from '../models/ticket.model';

describe('TicketService', () => {
  const baseUrl = `${environment.apiBaseUrl}/api/tickets`;

  let service: TicketService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(TicketService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  // Fails the test if the service fired a request nothing expected, which is how
  // a stray or duplicated call gets caught rather than silently passing.
  afterEach(() => httpMock.verify());

  it('GETs the collection endpoint and passes the tickets through', () => {
    const tickets: TicketResponse[] = [
      {
        id: 1,
        title: 'Printer offline',
        description: 'Third floor',
        priority: 'HIGH',
        status: 'NEW',
      },
    ];

    let received: TicketResponse[] | undefined;
    service.getAll().subscribe((t) => (received = t));

    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('GET');

    req.flush(tickets);
    expect(received).toEqual(tickets);
  });

  it('GETs a single ticket by id', () => {
    service.getById(42).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/42`);
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('POSTs the request body when creating', () => {
    const body = {
      title: 'VPN down',
      description: 'Cannot connect',
      priority: 'MEDIUM' as const,
    };

    service.create(body).subscribe();

    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush({});
  });

  it('PATCHes only the supplied fields on a partial update', () => {
    service.updatePartial(7, { status: 'RESOLVED' }).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/7`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ status: 'RESOLVED' });
    req.flush({});
  });

  it('PUTs the full body when replacing', () => {
    const body = {
      title: 'VPN down',
      description: 'Cannot connect',
      priority: 'HIGH' as const,
      status: 'IN_PROGRESS',
    };

    service.replace(7, body).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/7`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(body);
    req.flush({});
  });

  it('DELETEs by id', () => {
    service.delete(7).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/7`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
