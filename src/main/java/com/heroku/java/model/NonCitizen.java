package com.heroku.java.model;

public class NonCitizen extends Customer {

    private String custpassport;

    public NonCitizen() {}

    public NonCitizen(String custname, String custemail, String custphonenum, String custaddress, String custpassword, Long custid, String custpassport) {
        super(custname, custemail, custphonenum, custaddress, custpassword, custid);
        this.custpassport = custpassport;
    }

    public String getCustPassport() {
        return this.custpassport;
    }

    public void setCustPassport(String custpassport) {
        this.custpassport = custpassport;
    }
}
