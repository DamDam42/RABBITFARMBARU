package com.heroku.java.model;

import java.sql.Blob;

public class Payment {
    private int paymentId;
    private Blob paymentReceipt;
    private double amount;
    private int bookingId; // Assuming each payment is linked to a booking
    private int custId;

    // Constructors
    public Payment() {}

    public Payment(int paymentId, Blob paymentReceipt, double amount, int bookingId,int custId) {
        this.paymentId = paymentId;
        this.paymentReceipt = paymentReceipt;
        this.amount = amount;
        this.bookingId = bookingId;
    }

    // Getters and Setters
    public int getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(int paymentId) {
        this.paymentId = paymentId;
    }

    public Blob getPaymentReceipt() {
        return paymentReceipt;
    }

    public void setPaymentReceipt(Blob paymentReceipt) {
        this.paymentReceipt = paymentReceipt;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

	public int getCustId() {
		return custId;
	}

	public void setCustId(int custId) {
		this.custId = custId;
	}
}
