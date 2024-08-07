package com.heroku.java.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.heroku.java.model.Booking;

@RestController
@RequestMapping("/api")
public class ListApiController {

    @Autowired
    private final DataSource dataSource;

    public ListApiController() {
        this.dataSource = null;
    }

    @GetMapping("/staffBookingList")
    public ResponseEntity<?> staffBookingList(@RequestParam(value = "status", required = false) String status) {
        List<Booking> bookings = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT b.bookingid, b.bookingdate, b.totalprice, b.bookingstatus, b.staffid, bt.ticketquantity, t.tickettype"
                    + " FROM public.booking b JOIN public.booking_ticket bt"
                    + " ON b.bookingid = bt.bookingid JOIN public.ticket t"
                    + " ON bt.ticketid = t.ticketid";

            // Add status filter if provided
            if (status != null && !status.isEmpty()) {
                sql += " WHERE b.bookingstatus = ?";
            }

            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                // Set status parameter if provided
                if (status != null && !status.isEmpty()) {
                    statement.setString(1, status);
                }

                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        Booking booking = new Booking();
                        booking.setBookingId(resultSet.getInt("bookingid"));
                        booking.setBookingDate(resultSet.getDate("bookingdate"));
                        booking.setTotalPrice(resultSet.getDouble("totalprice"));
                        booking.setTicketQuantity(resultSet.getInt("ticketquantity"));
                        booking.setBookingStatus(resultSet.getString("bookingstatus"));
                        booking.setTicketType(resultSet.getString("tickettype"));
                        booking.setStaffId(resultSet.getInt("staffid"));

                        bookings.add(booking);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("An error occurred while fetching data");
        }

        return ResponseEntity.ok(bookings);
    }
}