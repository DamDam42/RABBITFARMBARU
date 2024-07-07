package com.heroku.java.controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.heroku.java.model.Booking;
import com.heroku.java.model.Payment;

import jakarta.servlet.http.HttpSession;

@Controller
public class PaymentController {
    private final DataSource dataSource;
    

    @Autowired
    public PaymentController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/makePayment")
    public String makePayment(@RequestParam("selectedBookings")List<Integer> selectedBookings,Model model,HttpSession session){
        session.getAttribute("custid");
        List<Booking> bookingDetails = new ArrayList<>();
        double totalPaymentAmount=0.0;
        
        try {
            Connection conn = dataSource.getConnection();
             String sql = "SELECT bookingid, bookingdate, totalprice, bookingstatus FROM public.booking WHERE bookingid = ?";
        for (int bookingId : selectedBookings){
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setInt(1, bookingId);
            ResultSet resultSet = statement.executeQuery();

            if(resultSet.next()){
                Booking booking = new Booking();
                booking.setBookingId(resultSet.getInt("bookingid"));
                booking.setBookingDate(resultSet.getDate("bookingdate"));
                booking.setTotalPrice(resultSet.getDouble("totalprice"));
                booking.setBookingStatus(resultSet.getString("bookingstatus"));
                
                String bookingStatus = resultSet.getString("bookingstatus");
                if (bookingStatus.equalsIgnoreCase("Paid")||bookingStatus.equalsIgnoreCase("Approved")) {
                    return "Payment/PaymentExistError";
                }
                

                bookingDetails.add(booking);
                totalPaymentAmount+= booking.getTotalPrice();
                
            }


        } 
        conn.close();
    

        model.addAttribute("bookings",bookingDetails);
        model.addAttribute("totalAmount", totalPaymentAmount);

        } catch (SQLException e) {
        } return "Payment/PaymentPage";
    }

    @PostMapping("/makePayment")
    public String makePayment(HttpSession session,@RequestParam("totalPaymentAmount") double totalAmount,
                    @RequestParam("paymentReceipt") MultipartFile paymentReceipt,
                    @RequestParam("selectedBookings") List<Integer> selectedBookings,
                    Model model)
        {
            session.getAttribute("custid");
            byte[] receiptData = null;

            try {
                receiptData = paymentReceipt.getBytes();
            } catch (IOException e) {
            }

            try {
                Connection conn = dataSource.getConnection();
                
                String paymentSql = "INSERT INTO public.payment (paymentamount, paymentreceipt, bookingid) VALUES (?, ?, ?)";
                for (int bookingId : selectedBookings) {

                    PreparedStatement paymentStatement = conn.prepareStatement(paymentSql);
                    paymentStatement.setDouble(1, totalAmount);
                    paymentStatement.setBytes(2, receiptData);
                    paymentStatement.setInt(3, bookingId);
                    paymentStatement.executeUpdate();

                //CHANGE PAYMENT STATUS
                String sqlStatus = "UPDATE public.booking SET bookingStatus=? WHERE bookingid=?";
                PreparedStatement statementStatus = conn.prepareStatement(sqlStatus);
                statementStatus.setInt(2, bookingId);
                statementStatus.setString(1, "Paid");
                statementStatus.executeUpdate();
                    
            } 

            conn.close();
            }catch (SQLException e) { 
                e.printStackTrace();
                return "Payment/PaymentError";
               
            }
                return "Payment/PaymentSuccessful";

        }   

        //STAFF VIEW PAYMENT
        @GetMapping("/viewPayment")
        public String viewPayment(HttpSession session,@RequestParam("bookingId") int bookingId,Model model){
            session.getAttribute("staffid");

            try {
                Connection conn = dataSource.getConnection();
                String sql = "SELECT paymentid,paymentreceipt,bookingid FROM public.payment WHERE bookingid=?";
                PreparedStatement statement = conn.prepareStatement(sql);
                statement.setInt(1, bookingId);
                ResultSet resultSet = statement.executeQuery();

                if(resultSet.next()){
                    Payment payment = new Payment();
                    payment.setPaymentId(resultSet.getInt("paymentid"));
                    payment.setBookingId(resultSet.getInt("bookingid"));
                    payment.setPaymentReceipt(resultSet.getBytes("paymentreceipt"));

                    model.addAttribute("payments", payment);

                }else{
                    return "Payment/PaymentNotMade";
                }

                conn.close();
                
                
            } catch (SQLException e) {
                e.printStackTrace();
            } return "Payment/StaffViewPayment";
        }


        



}
