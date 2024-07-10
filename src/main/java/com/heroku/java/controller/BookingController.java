package com.heroku.java.controller;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
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
public String checkAvailability(HttpSession session,
                                @RequestParam("bookingDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bookingDate,
                                @RequestParam("ticketQuantity") int ticketQuantity,
                                @RequestParam("ticketType") String ticketType) {
    session.getAttribute("custid");

    try {
        Connection conn = dataSource.getConnection();
        String sql = "SELECT sum(bt.ticketquantity) FROM public.booking_ticket bt "
                   + "JOIN public.booking b ON b.bookingid = bt.bookingid WHERE b.bookingdate = ?";
        PreparedStatement statement = conn.prepareStatement(sql);
        statement.setDate(1, java.sql.Date.valueOf(bookingDate));
        ResultSet resultSet = statement.executeQuery();

        if (resultSet.next()) {
            int numBooking = resultSet.getInt("sum");

            if (numBooking + ticketQuantity < 80) {
                session.setAttribute("bookingDate", bookingDate);
                session.setAttribute("ticketType", ticketType);
                session.setAttribute("ticketQuantity", ticketQuantity);

                return "redirect:/bookingAvailable";
            } else if (numBooking + ticketQuantity >= 80) {
                return "redirect:/bookingNotAvailable";
            }
        }

        conn.close();

    } catch (SQLException e) {
        e.printStackTrace(); // Log the exception
    }

    return "redirect:/error";
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

   

    // other imports
    
    @GetMapping("/customerBooking")
    public String customerBooking(HttpSession session, Model model) {
        Long custid = (Long) session.getAttribute("custid");
        int ticketQuantity = (int) session.getAttribute("ticketQuantity");
        LocalDate bookingDate = (LocalDate) session.getAttribute("bookingDate");
        String ticketType = (String) session.getAttribute("ticketType");
        

        
            //RETRIEVE CUSTOMER TYPE

            try{Connection conn = dataSource.getConnection();
            String sqlType = "SELECT c.custname, c.custemail, c.custaddress, c.custphonenum, c.custpassword, ct.custicnum, nc.custpassport " +
                     "FROM public.customers c " +
                     "LEFT JOIN public.citizen ct ON c.custid = ct.custid " +
                     "LEFT JOIN public.noncitizen nc ON c.custid = nc.custid " +
                     "WHERE c.custid = ?";
            PreparedStatement statementType = conn.prepareStatement(sqlType);
            statementType.setLong(1,custid);
            ResultSet resultSet = statementType.executeQuery();
            
            if(resultSet.next()){
                String custName = resultSet.getString("custname");
                String custEmail = resultSet.getString("custemail");
                String custAddress = resultSet.getString("custaddress");
                String custphonenum= resultSet.getString("custphonenum");
                String custPassword = resultSet.getString("custpassword");
                String custic = resultSet.getString("custicnum");
                String custPassport = resultSet.getString("custpassport");
                Customer customer = null;
                String customerType =null;

                if (custic != null){

                    customer = new Citizen(custName, custEmail, custphonenum, custAddress, custPassword, custid, custic);
                    customerType = "Citizen";
                    } else if (custPassport!= null){
                        customer = new NonCitizen(custName, custEmail, custphonenum, custAddress, custPassword, custid, custPassport);
                        customerType = "NonCitizen";
                    }
                    
                    
                    
                    model.addAttribute("customer",customer);
                    model.addAttribute("customerType", customerType);

            }
             double subtotal = calculateSubTotal(custid, ticketType, ticketQuantity);
        model.addAttribute("subtotal",subtotal);
        model.addAttribute("bookingDate", bookingDate);
        model.addAttribute("ticketQuantity", ticketQuantity);
        model.addAttribute("ticketType", ticketType);
    
        double totalPrice = calculateTotalPrice(custid, ticketType, ticketQuantity);
        model.addAttribute("totalPrice", totalPrice);
        }catch(SQLException e){

        }

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


    //CALCULATE SUBTOTAL

    public double calculateSubTotal(Long custid, String tickettype, int ticketQuantity) {
        double ticketprice = retrieveTicketPrice(tickettype);
        double subtotal = ticketprice * ticketQuantity;
        
        return subtotal;
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
    public String createBooking(HttpSession session, @ModelAttribute("createBooking") Booking booking,
                                @RequestParam("ticketType") String tickettype,
                                @RequestParam("bookingDate") LocalDate bookingDate,
                                @RequestParam("ticketQuantity") int ticketQuantity,Model model) {
    
        Long custid = (Long) session.getAttribute("custid");
        ResultSet resultSet = null;
        PreparedStatement statement = null;
        PreparedStatement statementCreate = null;
        PreparedStatement statementInsert = null;
        PreparedStatement statementUpdate = null;
    
        try {
            // RETRIEVE TICKET ID
            Connection conn = dataSource.getConnection();
            String sqlTicketId = "SELECT ticketid FROM Ticket WHERE tickettype=?";
            statement = conn.prepareStatement(sqlTicketId);
            statement.setString(1, tickettype);
            resultSet = statement.executeQuery();
            int ticketid = -1;
    
            if (resultSet.next()) {
                ticketid = resultSet.getInt("ticketid");
            }

            
            // CREATE BOOKING (INSERT TO DATABASE)
            String sql = "INSERT INTO public.booking(custid, bookingdate, totalprice, bookingstatus) VALUES (?, ?, ?, ?) RETURNING bookingid";
            statementCreate = conn.prepareStatement(sql);
            statementCreate.setLong(1, custid);
            statementCreate.setObject(2, bookingDate);
            statementCreate.setDouble(3, 0.0); // Initial total price, adjust as needed
            statementCreate.setString(4, "Unpaid");
            resultSet = statementCreate.executeQuery();
    
            if (resultSet.next()) {
                int bookingid = resultSet.getInt("bookingid");
    
                // INSERT INTO BRIDGE
                String sqlInsert = "INSERT INTO public.booking_ticket(bookingid, ticketid, ticketquantity) VALUES (?, ?, ?)";
                statementInsert = conn.prepareStatement(sqlInsert);
                statementInsert.setInt(1, bookingid);
                statementInsert.setInt(2, ticketid);
                statementInsert.setInt(3, ticketQuantity);
                statementInsert.executeUpdate();
    
                // Update total price
                
                double totalPrice = calculateTotalPrice(custid, tickettype, ticketQuantity);
                String sqlPrice = "UPDATE public.booking SET totalprice=? WHERE bookingid=?";
                statementUpdate = conn.prepareStatement(sqlPrice);
                statementUpdate.setDouble(1, totalPrice);
                statementUpdate.setInt(2, bookingid);
                statementUpdate.executeUpdate();

                
            }
    
        } catch (SQLException e) {
            // Handle SQL exceptions
            e.printStackTrace();
            return "redirect:/error"; // Redirect to error page or handle error as needed
        } finally {
            // Close resources in finally block
            try {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
                if (statementCreate != null) statementCreate.close();
                if (statementInsert != null) statementInsert.close();
                if (statementUpdate != null) statementUpdate.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    
        return "redirect:/bookingSuccess";
    }

    @GetMapping("/bookingSuccess")
    public String bookingSuccess(HttpSession session){
        session.getAttribute("custid");
        return "Booking/BookingSuccess";
    }

    //VIEW BOOKING BY CUSTID

    @GetMapping("/customerViewBooking")
    public String customerViewBooking(HttpSession session, Model model){
        Long custid = (Long) session.getAttribute("custid");
        List<Booking> bookingCustomer = new ArrayList<>();

        try {
            Connection conn = dataSource.getConnection();
            String sql ="SELECT b.bookingid,b.bookingdate,b.totalprice,b.bookingstatus,bt.ticketquantity,t.tickettype"
            + " FROM public.booking b JOIN public.booking_ticket bt "
            + " ON b.bookingid=bt.bookingid JOIN public.ticket t"
            + " ON bt.ticketid = t.ticketid WHERE custid=?";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setLong(1, custid);
            ResultSet resultSet = statement.executeQuery();

            while(resultSet.next()){
                int bookingid = resultSet.getInt("bookingid");
                Date bookingDate = resultSet.getDate("bookingdate");
                double totalPrice = resultSet.getDouble("totalprice");
                int ticketQuantity = resultSet.getInt("ticketquantity");
                String ticketType = resultSet.getString("tickettype");
                String bookingStatus = resultSet.getString("bookingstatus");

                Booking booking = new Booking();
                booking.setBookingId(bookingid);
                booking.setBookingDate(bookingDate);
                booking.setTotalPrice(totalPrice);
                booking.setTicketQuantity(ticketQuantity);
                booking.setBookingStatus(bookingStatus);
                booking.setTicketType(ticketType);

                bookingCustomer.add(booking);
                
                model.addAttribute("booking", bookingCustomer);
            }
        } catch (Exception e) {
        } return "Booking/CustomerBookingList";
    }



   // CUSTOMER UPDATE BOOKING

    @GetMapping("/customerUpdateBookingMap")
    public String customerUpdate(HttpSession session,Model model,@RequestParam("bookingId") int bookingid){
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
        }
   
    Booking booking = new Booking();
    
		
		try {
			Connection conn= dataSource.getConnection();
			String sql = "SELECT b.bookingid,b.custid, bt.ticketquantity,b.bookingDate,b.bookingstatus,b.totalprice,t.tickettype"
					   + " FROM public.booking b"
					   + " Join public.booking_ticket bt ON bt.bookingid=b.bookingid"
					   + " Join public.ticket t ON bt.ticketid=t.ticketid "
					   + "WHERE b.bookingid=?";
			
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setInt(1, bookingid);
			ResultSet resultSet = statement.executeQuery();
			
			while (resultSet.next()) {
				
				booking.setBookingId(resultSet.getInt("bookingid"));
				booking.setCustId(resultSet.getInt("custid"));
                booking.setTicketQuantity(resultSet.getInt("ticketquantity"));
                booking.setTotalPrice(resultSet.getDouble("totalprice"));
                booking.setBookingDate(resultSet.getDate("bookingDate"));
                booking.setTicketType(resultSet.getString("tickettype"));
                booking.setBookingStatus(resultSet.getString("bookingstatus"));

                String status = resultSet.getString("bookingstatus");

                if(status.equalsIgnoreCase("Unpaid")){
                model.addAttribute("booking", booking);
                session.setAttribute("bookingId", bookingid);
                }else{
                    return "Booking/InvalidUpdateBooking";
                }
			}
			conn.close();
			
		}catch(SQLException e) {
			e.printStackTrace();
		}
    return "Booking/UpdateBooking";
		
	} 



    //CHECK TICKETS AVAILABILITY FOR UPDATE

    @PostMapping("/checkAvailabilityUpdate")
    public String checkAvailabilityUpdate(HttpSession session,
                                @RequestParam("bookingDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bookingDate,
                                @RequestParam("ticketQuantity") int ticketQuantity,
                                @RequestParam("ticketType") String ticketType
                                ) {
    session.getAttribute("custid");
    session.getAttribute("bookingId");

    try {
        Connection conn = dataSource.getConnection();
        String sql = "SELECT sum(bt.ticketquantity) FROM public.booking_ticket bt "
                   + "JOIN public.booking b ON b.bookingid = bt.bookingid WHERE b.bookingdate = ?";
        PreparedStatement statement = conn.prepareStatement(sql);
        statement.setDate(1, java.sql.Date.valueOf(bookingDate));
        ResultSet resultSet = statement.executeQuery();

        if (resultSet.next()) {
            int numBooking = resultSet.getInt("sum");

            if (numBooking + ticketQuantity < 80) {
                session.setAttribute("bookingDate", bookingDate);
                session.setAttribute("ticketType", ticketType);
                session.setAttribute("ticketQuantity", ticketQuantity);

                return "redirect:/updateAvailable";
            } else if (numBooking + ticketQuantity >= 80) {
                return "redirect:/updateNotAvailable";
            }
        }

        conn.close();

    } catch (SQLException e) {
        e.printStackTrace(); // Log the exception
    }

    return "redirect:/error";
}

    @GetMapping("/updateAvailable")
    public String updateAvailable(HttpSession session){
        session.getAttribute("custid");
        session.getAttribute("bookingId");
        return "Booking/UpdateAvailable";

    }

    @GetMapping("/updateNotAvailable")
    public String updateNotAvailable(HttpSession session){
        session.getAttribute("custid");
        session.getAttribute("bookingId");
        return "Booking/UpdateNotAvailable";
    }



    //OPEN CONFIRMATION PAGE

    @GetMapping("/customerBookingUpdate")
    public String customerBookingUpdate(HttpSession session, Model model) {
        Long custid = (Long) session.getAttribute("custid");
        int bookingId = (int) session.getAttribute("bookingId");
        int ticketQuantity = (int) session.getAttribute("ticketQuantity");
        LocalDate bookingDate = (LocalDate) session.getAttribute("bookingDate");
        String ticketType = (String) session.getAttribute("ticketType");
    
        model.addAttribute("bookingDate", bookingDate);
        model.addAttribute("ticketQuantity", ticketQuantity);
        model.addAttribute("ticketType", ticketType);
        model.addAttribute("bookingId", bookingId);
    
        double totalPrice = calculateTotalPrice(custid, ticketType, ticketQuantity);
        model.addAttribute("totalPrice", totalPrice);
    
        return "Booking/UpdateConfirmBooking";
    }

    //PROCESS UPDATE

    @PostMapping("/customerUpdateBooking")
    public String customerUpdateBooking(HttpSession session,@ModelAttribute("customerUpdateBooking") Booking booking,
                                    @RequestParam("ticketType") String ticketType,
                                    @RequestParam("bookingDate") LocalDate bookingDate,
                                    @RequestParam("ticketQuantity") int ticketQuantity,
                                    @RequestParam("bookingId") int bookingId, 
                                    Model model) {

    Long custid = (Long) session.getAttribute("custid");
    bookingId = (int) session.getAttribute("bookingId");

    try (Connection conn = dataSource.getConnection()) {
        // RETRIEVE TICKET ID
        String sqlTicketId = "SELECT ticketid FROM public.ticket WHERE tickettype = ?";
        int ticketId;
        try (PreparedStatement statement = conn.prepareStatement(sqlTicketId)) {
            statement.setString(1, ticketType);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    ticketId = resultSet.getInt("ticketid");
                } else {
                    throw new SQLException("Ticket ID not found for ticket type: " + ticketType);
                }
            }
        }

        // UPDATE BOOKING
        String sqlUpdateBooking = "UPDATE public.booking SET bookingdate = ?, bookingstatus = ? WHERE custid = ? AND bookingid = ?";
        try (PreparedStatement statement = conn.prepareStatement(sqlUpdateBooking)) {
            statement.setObject(1, bookingDate);
            statement.setString(2, "Unpaid");
            statement.setLong(3, custid);
            statement.setInt(4, bookingId);
            statement.executeUpdate();
        }

        // UPDATE BOOKING_TICKET
        String sqlUpdateBookingTicket = "UPDATE public.booking_ticket SET ticketid = ?, ticketquantity = ? WHERE bookingid = ?";
        try (PreparedStatement statement = conn.prepareStatement(sqlUpdateBookingTicket)) {
            statement.setInt(1, ticketId);
            statement.setInt(2, ticketQuantity);
            statement.setInt(3, bookingId);
            statement.executeUpdate();
        }

        // UPDATE TOTAL PRICE
        double totalPrice = calculateTotalPrice(custid, ticketType, ticketQuantity);
        String sqlUpdateTotalPrice = "UPDATE public.booking SET totalprice = ? WHERE bookingid = ?";
        try (PreparedStatement statement = conn.prepareStatement(sqlUpdateTotalPrice)) {
            statement.setDouble(1, totalPrice);
            statement.setInt(2, bookingId);
            statement.executeUpdate();
        }
    } catch (SQLException e) {
        e.printStackTrace();
        return "redirect:/error"; // Redirect to error page or handle error as needed
    }

    return "redirect:/customerUpdateSuccess";
}

@GetMapping("/customerUpdateSuccess")
    public String customerUpdateSuccess(HttpSession session){
        session.getAttribute("custid");
        return "Booking/UpdateSuccess";
    }

    @GetMapping("/customerDeleteBooking")
        public String customerDeleteBooking(HttpSession session, @RequestParam("bookingId") int bookingId){
            session.getAttribute("custid");
            try {
                Connection conn = dataSource.getConnection();
                String sql = "SELECT bookingstatus from public.booking WHERE bookingid=?";
                PreparedStatement statementcheck =conn.prepareStatement(sql);

                statementcheck.setInt(1, bookingId);
                ResultSet resultSet =statementcheck.executeQuery();

                if(resultSet.next()){
                    String status = resultSet.getString("bookingstatus");

                    if(status.equalsIgnoreCase("paid")||status.equalsIgnoreCase("approved")){
                        return "redirect:/deleteNotValid";
                    }else{
                        String sqlBridge = "DELETE FROM public.booking_ticket WHERE bookingid=?";
                        PreparedStatement statementBridge = conn.prepareStatement(sqlBridge);
                        statementBridge.setInt(1, bookingId);
                        statementBridge.executeUpdate();

                        String sqlDelete = "DELETE from public.booking WHERE bookingid=?";
                        PreparedStatement statement = conn.prepareStatement(sqlDelete);
                        statement.setInt(1, bookingId);
                        statement.executeUpdate();
                    }
                }
            } catch (Exception e) {
            } return "redirect:/deleteBookingSuccess";
        }

        @GetMapping("/deleteNotValid")
        public String deleteNotValid(HttpSession session){
            session.getAttribute("custid");
            return "Booking/DeleteNotValid";
        }

        @GetMapping("/deleteBookingSuccess")
        public String deleteBookingSuccess(HttpSession session){
            session.getAttribute("custid");
            return "Booking/DeleteBookingSuccess";
        }

        @GetMapping("/staffBookingList")
        public String staffBookingList(HttpSession session,@RequestParam(value = "status", required = false) String status,Model model){
            
            List<Booking> bookings = new ArrayList<>();

            try {
                Connection conn = dataSource.getConnection();
                String sql ="SELECT b.bookingid,b.bookingdate,b.totalprice,b.bookingstatus,bt.ticketquantity,t.tickettype"
            + " FROM public.booking b JOIN public.booking_ticket bt "
            + " ON b.bookingid=bt.bookingid JOIN public.ticket t"
            + " ON bt.ticketid = t.ticketid";

            // Add status filter if provided
        if (status != null && !status.isEmpty()) {
            sql += " WHERE b.bookingstatus = ?";
        }

        PreparedStatement statement = conn.prepareStatement(sql);

        // Set status parameter if provided
        if (status != null && !status.isEmpty()) {
            statement.setString(1, status);
        }
            ResultSet resultSet = statement.executeQuery();

            while(resultSet.next()){
                int bookingid = resultSet.getInt("bookingid");
                Date bookingDate = resultSet.getDate("bookingdate");
                double totalPrice = resultSet.getDouble("totalprice");
                int ticketQuantity = resultSet.getInt("ticketquantity");
                String ticketType = resultSet.getString("tickettype");
                String bookingStatus = resultSet.getString("bookingstatus");

                Booking booking = new Booking();
                booking.setBookingId(bookingid);
                booking.setBookingDate(bookingDate);
                booking.setTotalPrice(totalPrice);
                booking.setTicketQuantity(ticketQuantity);
                booking.setBookingStatus(bookingStatus);
                booking.setTicketType(ticketType);

                bookings.add(booking);
                model.addAttribute("booking", bookings);

            }

            } catch (SQLException e) {
            }
        return "Booking/StaffBookingList";
        }


    }














