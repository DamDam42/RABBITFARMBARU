package com.heroku.java.controller;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.heroku.java.model.ticket;

import jakarta.servlet.http.HttpSession;

@Controller
public class TicketController {
    private final DataSource dataSource;

    @Autowired
    public TicketController(DataSource dataSource) {
        this.dataSource = dataSource;
    }



    @PostMapping("/addTickets")
    public String addTicket(HttpSession session,@ModelAttribute("addTickets") ticket tickets) {
        session.getAttribute("staffid");
        try {
            System.out.println("Ticket Type: " + tickets.getTicketType());
            System.out.println("Ticket Price: " + tickets.getTicketPrice());
    
            try (Connection connection = dataSource.getConnection()) {
                String sql = "INSERT INTO public.ticket(tickettype,ticketprice) VALUES(?,?)";
                final var statement = connection.prepareStatement(sql);
    
                String tickettype = tickets.getTicketType();
                double ticketprice = tickets.getTicketPrice();
    
                statement.setString(1, tickettype);
                statement.setDouble(2, ticketprice);
    
                statement.executeUpdate();
                connection.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/addTicketError";
        }
    
        return "redirect:/addTicketSuccess";
    }

    @GetMapping("/addTicketSuccess")
    public String addTicketSuccess(HttpSession session){
        session.getAttribute("staffid");
        return "Ticket/AddTicketSuccess";
    }

    @GetMapping("/addTicketError")
    public String addTicketError(HttpSession session){
        session.getAttribute("staffid");
        return "Ticket/AddTicketError";
    }


    @GetMapping("/ticketList")
    public String ticketList(HttpSession session,Model model) {
        session.getAttribute("staffid");
        List<ticket> tickets = new ArrayList<>();

        try (Connection connection = dataSource.getConnection()) {
            String sql = "SELECT  ticketid, tickettype, ticketprice FROM public.ticket ORDER BY ticketid";
            final var statement = connection.prepareStatement(sql);
            final var resultSet = statement.executeQuery();

            while (resultSet.next()) {
                Long ticketId = resultSet.getLong("ticketid");
                String ticketType = resultSet.getString("tickettype");
                double ticketPrice = resultSet.getDouble("ticketprice");

                ticket ticket = new ticket();
                ticket.setTicketId(ticketId);
                ticket.setTicketType(ticketType);
                ticket.setTicketPrice(ticketPrice);

                tickets.add(ticket);
            }

            model.addAttribute("tickets", tickets);

        } catch (SQLException e) {
            e.printStackTrace();
            return "error";
        }

        return "Ticket/bookingTicket";
    }


@GetMapping("/updateTicketGet")
public String updateTicket(HttpSession session,@RequestParam("ticketId") Long ticketId, Model model) {
    session.getAttribute("staffid");
    try {
        Connection connection = dataSource.getConnection();
        String sql = "SELECT ticketid,tickettype,ticketprice FROM public.ticket WHERE ticketid=?";
        final var statement= connection.prepareStatement(sql);
        statement.setLong(1,ticketId);
        final var resultSet = statement.executeQuery();

        if (resultSet.next()){
            
            String ticketType = resultSet.getString("tickettype");
            double ticketPrice = resultSet.getDouble("ticketprice");

            ticket ticket = new ticket();
            ticket.setTicketId(ticketId);
            ticket.setTicketPrice(ticketPrice);
            ticket.setTicketType(ticketType);
            model.addAttribute("ticket",ticket);

            connection.close();
        }
    }
    catch(Exception e){
        e.printStackTrace();
    }
    return "Ticket/updateTicket";
}

@PostMapping("/updateTickets")
public String updateTicket(HttpSession session,@ModelAttribute("updateTickets") ticket ticket, Model model){
    session.getAttribute("staffid");
    try {
        Connection connection = dataSource.getConnection();
        String sql = "UPDATE ticket SET tickettype=? , ticketprice=? WHERE ticketid=?";
        final var statement=connection.prepareStatement(sql);
        Long ticketId = ticket.getTicketId();
        String ticketType = ticket.getTicketType();
        double ticketPrice = ticket.getTicketPrice();

        
        statement.setString(1,ticketType);
        statement.setDouble(2,ticketPrice);
        statement.setLong(3, ticketId);

        statement.executeUpdate();

        connection.close();
        

    } catch (Exception e) {
        e.printStackTrace();
        return "redirect:/error";
    }
    return "redirect:/ticketList";
}

@PostMapping("/deleteTicket")
public String deleteTicket(HttpSession session,@RequestParam("ticketId") Long ticketId){
    session.getAttribute("staffid");
    try {
        Connection connection = dataSource.getConnection();
        String sql = "DELETE FROM public.ticket WHERE ticketid=?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setLong(1,ticketId);
        statement.executeUpdate();

        connection.close();

    } catch (Exception e) {
        e.printStackTrace();
        return "redirect:/errorDelete";
    }
    return "redirect:/ticketList";
}
}