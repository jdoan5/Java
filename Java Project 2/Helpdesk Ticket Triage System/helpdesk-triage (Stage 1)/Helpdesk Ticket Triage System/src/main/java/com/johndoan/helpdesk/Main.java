package com.johndoan.helpdesk;

import com.johndoan.helpdesk.repo.InMemoryTicketRepository;
import com.johndoan.helpdesk.service.TicketService;
import com.johndoan.helpdesk.cli.ConsoleApp;

import java.util.Scanner;

public final class Main {
    public static void main(String[] args) {
        var repo = new InMemoryTicketRepository();
        var service = new TicketService(repo);
        var app = new ConsoleApp(service, new Scanner(System.in));
        app.run();
    }
}