package com.heroku.java.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.heroku.java.model.Booking;
import com.heroku.java.model.Citizen;
import com.heroku.java.model.Customer;
import com.heroku.java.model.NonCitizen;
import com.heroku.java.model.ticket;

import jakarta.servlet.http.HttpSession;

@Controller
public class BookingController {
    private final DataSource dataSource;
    

    @Autowired
    public BookingController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    //Put all Ticket Type/Price on Booking/Availability Page
    @GetMapping("/bookingTicketList")
    public String bookingTicketList(HttpSession session, Model model){
        session.getAttribute("custid");
        List<ticket> tickets = new ArrayList<>();

        try {
            Connection conn = dataSource.getConnection();
            String sql = "SELECT tickettype,ticketprice from public.ticket";
            PreparedStatement statement = conn.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()){
                String ticketType = resultSet.getString("tickettype");
                double ticketPrice = resultSet.getDouble("ticketprice");
                ticket ticket = new ticket();
                ticket.setTicketType(ticketType);
                ticket.setTicketPrice(ticketPrice);
                tickets.add(ticket);
                
            }
            model.addAttribute("ticket", tickets);
            
        } catch (SQLException e) {
        } return "Booking/CustomerBooking";
    }


    //CHECK AVAILABILITY OF TICKET ON THE DATE
    @PostMapping("/checkAvailability")
    public String checkAvailability(HttpSession session,@RequestParam("bookingDate") Date bookingDate,
    @RequestParam("ticketQuantity")int ticketQuantity,
    @RequestParam("ticketType") String ticketType){

        session.getAttribute("custid");
            
        try {
            Connection conn = dataSource.getConnection();
            String sql = "SELECT sum(bt.ticketquantity) FROM public.booking_ticket bt"
            + "JOIN public.booking b ON b.bookingid=bt.bookingid WHERE b.bookingdate=?";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setDate(1, (java.sql.Date) bookingDate);
            ResultSet resultSet = statement.executeQuery();

            if(resultSet.next()){
                int numBooking=resultSet.getInt("sum(ticketquantity)");

                if(numBooking + ticketQuantity < 80){
                    session.setAttribute("bookingDate", bookingDate);
                    session.setAttribute("tickettype", ticketType);
                    session.setAttribute("ticketQuantity", ticketQuantity);

                    return "redirect:/bookingAvailable";
                    
                }else if (numBooking + ticketQuantity >= 80){
                    return "redirect:/bookingNotAvailable";
                }
            }

            conn.close();

        } catch (SQLException e) {
        } return "redirect:/error";
    }

    @GetMapping("/bookingAvailable")
    public String bookingAvailable(HttpSession session){
        session.getAttribute("custid");
        return "Booking/BookingAvailable";

    }

    @GetMapping("/bookingNotAvailable")
    public String bookingNotAvailable(HttpSession session){
        session.getAttribute("custid");
        return "Booking/BookingNotAvailable";
    }

    @GetMapping("/customerBooking")
    public String customerBooking(HttpSession session,Model model){
        Long custid = (Long) session.getAttribute("custid");
        int ticketQuantity = (int) session.getAttribute("ticketQuantity");
        Date bookingDate = (Date) session.getAttribute("bookingDate");
        String ticketType = (String) session.getAttribute("ticketQuantity");


        model.addAttribute("bookingDate", bookingDate);
        model.addAttribute("ticketQuantity", ticketQuantity);
        model.addAttribute("ticketType", ticketType);

        double totalPrice= calculateTotalPrice(custid,ticketType, ticketQuantity);
        model.addAttribute("totalPrice", totalPrice);

        return "Booking/CreateBooking";
    }


    //retrieve price from ticket table
    public double retrieveTicketPrice(String tickettype){
        double ticketprice=0.0;
        try {
            Connection conn = dataSource.getConnection();
            String sql = "Select ticketprice from public.ticket WHERE tickettype=?";
            PreparedStatement statement = conn.prepareStatement(sql);

            statement.setString(1, tickettype);
            ResultSet resultSet =statement.executeQuery();

            if(resultSet.next()){
                 ticketprice = resultSet.getDouble("ticketprice");
            }

            conn.close();

        } catch (SQLException e) {
        }
        return ticketprice;
    }

    public Customer retrieveCustomerById(Long custId) throws Exception {
    Customer customer = null;

    try (Connection conn = dataSource.getConnection()) {
        String sql = "SELECT c.custid, c.custname, c.custemail, c.custaddress, c.custphonenum, c.custpassword, ct.custicnum, nc.custpassport " +
                     "FROM public.customers c " +
                     "LEFT JOIN public.citizen ct ON c.custid = ct.custid " +
                     "LEFT JOIN public.noncitizen nc ON c.custid = nc.custid " +
                     "WHERE c.custid = ?";
        PreparedStatement statement = conn.prepareStatement(sql);
        statement.setLong(1, custId);
        ResultSet resultSet = statement.executeQuery();

        if (resultSet.next()) {
            String custname = resultSet.getString("custname");
            String custemail = resultSet.getString("custemail");
            String custaddress = resultSet.getString("custaddress");
            String custphonenum = resultSet.getString("custphonenum");
            String custpassword = resultSet.getString("custpassword");
            String custicnum = resultSet.getString("custicnum");
            String custpassport = resultSet.getString("custpassport");

            if (custicnum != null) {
                customer = new Citizen(custname, custemail, custphonenum, custaddress, custpassword,custId, custicnum);
            } else if (custpassport != null) {
                customer = new NonCitizen(custname, custemail, custphonenum, custaddress, custpassword,custId,custpassport);
            }
        }

        resultSet.close();
        statement.close();

    } catch (Exception e) {
        e.printStackTrace(); // Better to log the exception
        throw new Exception("Error retrieving customer with ID: " + custId, e);
    }

    if (customer == null) {
        throw new Exception("Customer not found for ID: " + custId);
    }

    return customer;
}



    //calculate totalPrice
    public double calculateTotalPrice(Long custid, String tickettype, int ticketQuantity) {
        double ticketprice = retrieveTicketPrice(tickettype);
        double subtotal = ticketprice * ticketQuantity;
        double total = 0.00;
    
        try {
            Customer customer = retrieveCustomerById((long) custid);
    
            if (customer instanceof Citizen) {
                total = subtotal * 0.95; // Apply 5% discount for citizens
            } else {
                total = subtotal; // No discount for non-citizens
            }
    
        } catch (Exception e) {
            e.printStackTrace(); // Better to log the exception
        }
    
        return total;
    }
    
    
    @PostMapping("/createBooking")
    public String createBooking(HttpSession session,@ModelAttribute("createBooking") Booking booking,
    @RequestParam("ticketType")String tickettype,@RequestParam("bookingDate") Date bookingDate,
    @RequestParam("ticketQuantity") int ticketQuantity, Long custid){

        custid = (Long) session.getAttribute("custid");
        ResultSet resultSet = null;
		PreparedStatement statement = null;
		PreparedStatement statementCreate = null;
		PreparedStatement statementInsert = null;
        PreparedStatement statementUpdate = null;
        try {


            //RETRIEVE TICKET ID
            Connection conn = dataSource.getConnection();
            String sqlTicketId = "SELECT ticketid From Ticket Where tickettype=?";
			 statement = conn.prepareStatement(sqlTicketId);
			
			statement.setString(1, tickettype);
			resultSet = statement.executeQuery();
			int ticketid = -1;
			
			if(resultSet.next()) {
				ticketid = resultSet.getInt("ticketid");
			}

            //CREATE BOOKING (INSERT TO DATABASE)
            String sql = "INSERT INTO public.booking(custid,bookingdate,totalprice,bookingstatus) VALUES (?,?,?) returning bookingid";
            statementCreate = conn.prepareStatement(sql);
            statementCreate.setLong(1, custid);
            statementCreate.setDate(2, (java.sql.Date) bookingDate);
            statementCreate.setDouble(3, 0.0);
            statementCreate.setString(4, "Unpaid");

            resultSet= statementCreate.executeQuery();

            if (resultSet.next()){

                int bookingid = resultSet.getInt("bookingid");
            //INSERT INTO BRIDGE

            String sqlInsert ="INSERT INTO public.booking_ticket(bookingid,ticketid,ticketquantity)";
            statementInsert = conn.prepareStatement(sqlInsert);
            statementInsert.setInt(1, bookingid);
            statementInsert.setLong(2, ticketid);
            statementInsert.setInt(3, ticketQuantity);
            statementInsert.executeUpdate();

            //update totalprice

            double totalPrice = calculateTotalPrice(custid, tickettype, ticketQuantity);
            String sqlPrice ="UPDATE public.booking SET totalprice=?";
            statementUpdate = conn.prepareStatement(sqlPrice);
            statementUpdate.setDouble(1, totalPrice);
            statementUpdate.executeUpdate();
            


        }
        } catch (SQLException e) {
        } return "redirect:/bookingSuccess";
    }

    @GetMapping("/bookingSuccess")
    public String bookingSuccess(HttpSession session){
        session.getAttribute("custid");
        return "Booking/BookingSuccess";
    }

}



