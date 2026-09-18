import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';

import { TicketsComponent } from './tickets.component';
import { environment } from '../../../environments/environment';
import { TicketResponse } from '../../models/ticket.model';

describe('TicketsComponent', () => {
  const baseUrl = `${environment.apiBaseUrl}/api/tickets`;

  const ticket: TicketResponse = {
    id: 1,
    title: 'Printer offline',
    description: 'Third floor',
    priority: 'HIGH',
    status: 'NEW',
  };

  let fixture: ComponentFixture<TicketsComponent>;
  let component: TicketsComponent;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TicketsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(TicketsComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('loads tickets on init and clears the loading flag', () => {
    fixture.detectChanges(); // triggers ngOnInit

    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('GET');
    expect(component.loading).toBeTrue();

    req.flush([ticket]);

    expect(component.tickets).toEqual([ticket]);
    expect(component.loading).toBeFalse();
    expect(component.error).toBeNull();
  });

  it('surfaces the backend message when loading fails', () => {
    fixture.detectChanges();

    httpMock
      .expectOne(baseUrl)
      .flush(
        { message: 'Database unavailable' },
        { status: 500, statusText: 'Server Error' },
      );

    expect(component.error).toBe('Database unavailable');
    expect(component.loading).toBeFalse();
  });

  it('falls back to a generic message when the error carries no body', () => {
    fixture.detectChanges();

    httpMock
      .expectOne(baseUrl)
      .flush(null, { status: 500, statusText: 'Server Error' });

    expect(component.error).toBe('Failed to load tickets');
  });

  it('resets the form and reloads the list after a successful create', () => {
    fixture.detectChanges();
    httpMock.expectOne(baseUrl).flush([]);

    component.form = {
      title: 'VPN down',
      description: 'Cannot connect',
      priority: 'HIGH',
    };
    component.createTicket();

    const post = httpMock.expectOne(baseUrl);
    expect(post.request.method).toBe('POST');
    expect(post.request.body.title).toBe('VPN down');
    post.flush(ticket);

    // createTicket() calls loadTickets() again on success.
    httpMock.expectOne(baseUrl).flush([ticket]);

    expect(component.form).toEqual({
      title: '',
      description: '',
      priority: 'MEDIUM',
    });
    expect(component.tickets).toEqual([ticket]);
  });

  it('keeps the form intact when the create fails', () => {
    fixture.detectChanges();
    httpMock.expectOne(baseUrl).flush([]);

    component.form = {
      title: 'VPN down',
      description: 'Cannot connect',
      priority: 'HIGH',
    };
    component.createTicket();

    httpMock
      .expectOne(baseUrl)
      .flush(
        { message: 'Title already exists' },
        { status: 409, statusText: 'Conflict' },
      );

    expect(component.error).toBe('Title already exists');
    expect(component.form.title).toBe('VPN down');
  });
});
