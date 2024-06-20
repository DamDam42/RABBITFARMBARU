package com.heroku.java.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.heroku.java.model.Customer;

@Controller
public class CustomerController {

    private final DataSource dataSource;
    

    @Autowired
    public CustomerController(DataSource dataSource) {
        this.dataSource = dataSource;
    }


    @PostMapping("/customerRegisters")
    public String customerRegister(@ModelAttribute("customerRegisters")  @RequestParam("Citizen") String citizenStatus,
                               @RequestParam(value = "custIcNum", required = false) String custIC,
                               @RequestParam(value = "custPassport", required = false) String passportNumber, 
                               Customer customer) {
        
        try (Connection connection = dataSource.getConnection()){

            System.out.println("Received customer details:");
        System.out.println("Name: " + customer.getCustName());
        System.out.println("Password: " + customer.getCustPassword());
        System.out.println("Email: " + customer.getCustEmail());
        System.out.println("Phone Number: " + customer.getCustPhoneNum());
        System.out.println("Address: " + customer.getCustAddress());
            String custsql = "INSERT INTO public.customers(custname, custpassword, custemail, custphonenum, custaddress) VALUES (?, ?, ?, ?, ?) RETURNING custid";
            
            PreparedStatement statement = connection.prepareStatement(custsql);
            statement.setString(1, customer.getCustName());
            statement.setString(2, customer.getCustPassword());
            statement.setString(3, customer.getCustEmail());
            statement.setString(4, customer.getCustPhoneNum());
            statement.setString(5, customer.getCustAddress());

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()){
                
                Long custid =  resultSet.getLong("custid");

                if ("Citizen".equals(citizenStatus)) {
                    String citizenSql = "INSERT INTO public.citizen(custid,custicnum) VALUES (?, ?)";
                    try (PreparedStatement citizenStatement = connection.prepareStatement(citizenSql)) {
                        citizenStatement.setLong(1, custid);
                        citizenStatement.setString(2, custIC);
                        citizenStatement.executeUpdate();
                    }
                } else if ("Non-Citizen".equals(citizenStatus)) {
                    String nonCitizenSql = "INSERT INTO public.noncitizen(custid, custpassport) VALUES (?, ?)";
                    try (PreparedStatement nonCitizenStatement = connection.prepareStatement(nonCitizenSql)) {
                        nonCitizenStatement.setLong(1, custid);
                        nonCitizenStatement.setString(2, passportNumber);
                        nonCitizenStatement.executeUpdate();
                    }
                }
            }
            System.out.println("Name: " + customer.getCustName());

            

            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/error";
        }
        return "redirect:/index";
    } 


    
}
