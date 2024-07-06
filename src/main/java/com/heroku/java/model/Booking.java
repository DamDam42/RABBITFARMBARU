package com.heroku.java.model;



import java.sql.Date;

public class Booking{
	
	private int bookingId;
	private int custId;
	private int staffId;
	
	private double totalPrice;
	private Date bookingDate;
    private String bookingStatus;
	private String ticketType;
    private int ticketQuantity;
	
	public Booking() {}
	
	public Booking(int bookingId,int custId,int staffId,double totalPrice,Date bookingDate,String ticketType,int ticketQuantity) {
		
		this.bookingId=bookingId;
		this.custId=custId;
		this.staffId=staffId;
		this.totalPrice=totalPrice;
		this.bookingDate=bookingDate;
		this.ticketType=ticketType;
        this.ticketQuantity=ticketQuantity;
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
	public int getStaffId() {
		return staffId;
	}
	public void setStaffId(int staffId) {
		this.staffId = staffId;
	}
	public double getTotalPrice() {
		return totalPrice;
	}
	public void setTotalPrice(double totalPrice) {
		this.totalPrice = totalPrice;
	}

	public Date getBookingDate() {
		return bookingDate;
	}

	public void setBookingDate(Date bookingDate) {
		this.bookingDate = bookingDate;
	}

	public String getTicketType() {
		return ticketType;
	}

	public void setTicketType(String ticketType) {
		this.ticketType = ticketType;
	}

    public String getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public int getTicketQuantity() {
        return ticketQuantity;
    }

    public void setTicketQuantity(int ticketQuantity) {
        this.ticketQuantity = ticketQuantity;
    }
	
	
} 