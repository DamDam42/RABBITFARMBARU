package com.heroku.java.model;

public class Citizen extends Customer {

    private String custicnum;

    public Citizen() {}

    public Citizen(String custname, String custemail, String custphonenum, String custaddress, String custpassword, Long custid, String custicnum) {
        super(custname, custemail, custphonenum, custaddress, custpassword, custid);
        this.custicnum = custicnum;
    }

    public String getCustIcNum() {
        return this.custicnum;
    }

    public void setCustIcNum(String custicnum) {
        this.custicnum = custicnum;
    }
}
