package com.heroku.java.model;




public class Customer{
    
    private String custname;
    private String custemail;
    private String custphonenum;
    private String custaddress;
    private String custpassword;
    private String custid;
    
    public Customer(){}

    public String getCustName(){
        return this.custname;
    }

    public void setCustName(String custname){
        this.custname = custname;
    }

    public String getCustID(){
        return this.custid;
    }

    public void setCustID(String custid){
        this.custid = custid;
    }

    public String getCustEmail(){
        return this.custemail;
    }

    public void setCustEmail(String custemail){
        this.custemail=custemail;
    }

    public String getCustPhoneNum(){
        return this.custphonenum;
    }

    public void setCustPhoneNum(String custphonenum){
        this.custphonenum=custphonenum;
    }

    public String getCustAddress(){
        return this.custaddress;
    }

    public void setcustAddress(String custaddress){
        this.custaddress=custaddress;
    }

    public String getCustPassword(){
        return this.custpassword;
    }

    public void setCustPassword(String custpassword){
        this.custpassword=custpassword;
    }



}