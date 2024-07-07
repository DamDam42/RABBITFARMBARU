package com.heroku.java.model;

import java.util.Base64;

public class Payment {
    private int paymentId;
    private byte[] paymentReceipt;
    private double amount;
    private int bookingId; // Assuming each payment is linked to a booking
    private int custId;

    // Constructors
    public Payment() {}

    public Payment(int paymentId, byte[] paymentReceipt, double amount, int bookingId,int custId) {
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

    public byte[] getPaymentReceipt() {
        return paymentReceipt;
    }

    public void setPaymentReceipt(byte[] paymentReceipt) {
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

    public String getPaymentReceiptBase64() {
        if (paymentReceipt == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(paymentReceipt);
    }
}
