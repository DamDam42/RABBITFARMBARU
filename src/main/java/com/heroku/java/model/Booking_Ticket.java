package com.heroku.java.model;

 public class Booking_Ticket{
	
	private int bookingId;
	private int ticketId;
    private int ticketQuantity;
	
	public Booking_Ticket() {}
	
	public Booking_Ticket(int bookingId,int ticketId,int ticketQuantity) {
		this.bookingId=bookingId;
		this.ticketId=ticketId;
        this.ticketQuantity=ticketQuantity;
	}
	
	
	public int getBookingId() {
		return bookingId;
	}
	public void setBookingId(int bookingId) {
		this.bookingId = bookingId;
	}
	public int getTicketId() {
		return ticketId;
	}
	public void setTicketId(int ticketId) {
		this.ticketId = ticketId;
	}

    public int getTicketQuantity() {
        return ticketQuantity;
    }

    public void setTicketQuantity(int ticketQuantity) {
        this.ticketQuantity = ticketQuantity;
    }

	
	
} 