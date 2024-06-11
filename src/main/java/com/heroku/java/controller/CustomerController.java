package com.heroku.java.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.heroku.java.model.Customer;

@Controller
public class CustomerController {

    private final DataSource dataSource;
    

    @Autowired
    public CustomerController(DataSource dataSource) {
        this.dataSource = dataSource;
    }


    @PostMapping("/customerRegisters")
    public String customerRegister(@ModelAttribute("customerRegisters") Customer customer) {
        
        try (Connection connection = dataSource.getConnection()){

            System.out.println("Received customer details:");
        System.out.println("Name: " + customer.getCustName());
        System.out.println("Password: " + customer.getCustPassword());
        System.out.println("Email: " + customer.getCustEmail());
        System.out.println("Phone Number: " + customer.getCustPhoneNum());
        System.out.println("Address: " + customer.getCustAddress());
            String sql = "INSERT INTO public.customers(custname, custpassword, custemail, custphonenum, custaddress) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, customer.getCustName());
            statement.setString(2, customer.getCustPassword());
            statement.setString(3, customer.getCustEmail());
            statement.setString(4, customer.getCustPhoneNum());
            statement.setString(5, customer.getCustAddress());
            statement.executeUpdate();
            System.out.println("Name: " + customer.getCustName());
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/error";
        }
        return "redirect:/index";
    } 
}
