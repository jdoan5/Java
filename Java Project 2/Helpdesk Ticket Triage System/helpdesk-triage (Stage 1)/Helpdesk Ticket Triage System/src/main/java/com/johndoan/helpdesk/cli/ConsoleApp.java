package com.johndoan.helpdesk.cli;

import com.johndoan.helpdesk.domain.Priority;
import com.johndoan.helpdesk.domain.Ticket;
import com.johndoan.helpdesk.domain.TicketStatus;
import com.johndoan.helpdesk.service.TicketService;

import java.util.List;
import java.util.Scanner;

public final class ConsoleApp {
    private final TicketService service;
    private final Scanner in;

    public ConsoleApp(TicketService service, Scanner in) {
        this.service = service;
        this.in = in;
    }

    public void run() {
        seedDemoData();

        while (true) {
            printMenu();
            String choice = prompt("Choose option").trim();

            try {
                switch (choice) {
                    case "1" -> createTicket();
                    case "2" -> listTickets(service.listAll());
                    case "3" -> updateStatus();
                    case "4" -> search();
                    case "5" -> deleteTicket();
                    case "0" -> { System.out.println("Goodbye."); return; }
                    default -> System.out.println("Invalid option.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }

            System.out.println();
        }
    }

    private void printMenu() {
        System.out.println("=== Helpdesk Ticket Triage (Stage 1) ===");
        System.out.println("1) Create ticket");
        System.out.println("2) List tickets");
        System.out.println("3) Update ticket status");
        System.out.println("4) Search tickets");
        System.out.println("5) Delete ticket");
        System.out.println("0) Exit");
    }

    private void createTicket() {
        String title = prompt("Title");
        String desc = prompt("Description");
        Priority priority = parsePriority(prompt("Priority (LOW/MEDIUM/HIGH)"));

        Ticket t = service.create(title, desc, priority);
        System.out.println("Created: " + formatTicket(t));
    }

    private void listTickets(List<Ticket> tickets) {
        if (tickets.isEmpty()) {
            System.out.println("(No tickets)");
            return;
        }
        for (Ticket t : tickets) {
            System.out.println(formatTicket(t));
        }
    }

    private void updateStatus() {
        long id = parseLong(prompt("Ticket id"));
        TicketStatus status = parseStatus(prompt("New status (OPEN/IN_PROGRESS/RESOLVED/CLOSED)"));
        Ticket updated = service.updateStatus(id, status);
        System.out.println("Updated: " + formatTicket(updated));
    }

    private void search() {
        String q = prompt("Search query (title/desc/status/priority/id)");
        List<Ticket> results = service.search(q);
        System.out.println("Matches: " + results.size());
        listTickets(results);
    }

    private void deleteTicket() {
        long id = parseLong(prompt("Ticket id"));
        service.delete(id);
        System.out.println("Deleted ticket " + id);
    }

    private String prompt(String label) {
        System.out.print(label + ": ");
        return in.nextLine();
    }

    private static long parseLong(String s) {
        return Long.parseLong(s.trim());
    }

    private static Priority parsePriority(String s) {
        return Priority.valueOf(s.trim().toUpperCase());
    }

    private static TicketStatus parseStatus(String s) {
        return TicketStatus.valueOf(s.trim().toUpperCase());
    }

    private static String formatTicket(Ticket t) {
        return String.format("#%d [%s | %s] %s",
                t.getId(), t.getStatus(), t.getPriority(), t.getTitle());
    }

    private void seedDemoData() {
        service.create("VPN not connecting", "User cannot connect to VPN from home network.", Priority.HIGH);
        service.create("Email sync slow", "Outlook taking a long time to sync.", Priority.MEDIUM);
        service.create("Printer jam", "Office printer jam on 3rd floor.", Priority.LOW);
    }
}