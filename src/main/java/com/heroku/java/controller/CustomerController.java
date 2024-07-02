package com.heroku.java.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.heroku.java.model.Citizen;
import com.heroku.java.model.Customer;
import com.heroku.java.model.NonCitizen;

import jakarta.servlet.http.HttpSession;

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
                    Citizen citizen = new Citizen();
                    citizen.setCustID(custid);
                    citizen.setCustIcNum(custIC);

                    
                    String citizenSql = "INSERT INTO public.citizen(custid,custicnum) VALUES (?, ?)";
                    try (PreparedStatement citizenStatement = connection.prepareStatement(citizenSql)) {
                        citizenStatement.setLong(1, citizen.getCustID());
                        citizenStatement.setString(2, citizen.getCustIcNum());
                        citizenStatement.executeUpdate();
                    }
                    
                } else if ("Non-Citizen".equals(citizenStatus)) {
                    NonCitizen noncitizen = new NonCitizen();
                    noncitizen.setCustID(custid);
                    noncitizen.setCustPassport(passportNumber);
                    String nonCitizenSql = "INSERT INTO public.noncitizen(custid, custpassport) VALUES (?, ?)";
                    try (PreparedStatement nonCitizenStatement = connection.prepareStatement(nonCitizenSql)) {
                        nonCitizenStatement.setLong(1, noncitizen.getCustID());
                        nonCitizenStatement.setString(2, noncitizen.getCustPassport());
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



    @GetMapping("/customerLogin")
        public String customerLogin(){
            
            return "Customer/CustomerLogin";
        }


    @PostMapping("/customerLogins")
        public String customerLogin(HttpSession session,@RequestParam("custEmail") String custemail,@RequestParam("custPassword") String custpassword, Model model,Customer customer){
            try {
                Connection connection = dataSource.getConnection();
                String sql = "SELECT custid,custname,custemail,custphonenum,custaddress,custpassword FROM public.customers WHERE custemail=?";
                final var statement = connection.prepareStatement(sql);
                statement.setString(1,custemail);

                final var resultSet = statement.executeQuery();

                if (resultSet.next()){

                    Long custId = resultSet.getLong("custid");
                    String custName = resultSet.getString("custname");
                    String custEmail = resultSet.getString("custemail");
                    String custPassword = resultSet.getString("custpassword");

                    if (custEmail.equals(custemail)&&custPassword.equals(custpassword)){
                        session.setAttribute("custname", custName);
                        session.setAttribute("custid", custId);

                        return "redirect:/accLogin";
                    }

                   

                }
                 connection.close();
                 return "redirect:/error2";
            } catch (Exception e) {
                e.printStackTrace();
                return "redirect:/error";
            }
        }

    @GetMapping("/customerProfile")
    public String customerProfile(HttpSession session, Model model) {
        Long custId = (Long) session.getAttribute("custid");

        try {
            Connection connection = dataSource.getConnection();
            String sql = "SELECT c.custname, c.custemail, c.custaddress, c.custphonenum, c.custpassword, ct.custicnum, nc.custpassport " +
                     "FROM public.customers c " +
                     "LEFT JOIN public.citizen ct ON c.custid = ct.custid " +
                     "LEFT JOIN public.noncitizen nc ON c.custid = nc.custid " +
                     "WHERE c.custid = ?";

            final var statement= connection.prepareStatement(sql);
            statement.setLong(1, custId);
            final var resultSet = statement.executeQuery();

            if (resultSet.next()){
                
                String custName = resultSet.getString("custname");
                String custEmail = resultSet.getString("custemail");
                String custAddress = resultSet.getString("custaddress");
                String custphonenum= resultSet.getString("custphonenum");
                String custPassword = resultSet.getString("custpassword");
                String custic = resultSet.getString("custicnum");
                String custPassport = resultSet.getString("custpassport");
                
                

                Customer customer = null;

                if (custic != null){

                customer = new Citizen(custName, custEmail, custphonenum, custAddress, custPassword, custId, custic);
               
                } else if (custPassport!= null){
                    customer = new NonCitizen(custName, custEmail, custphonenum, custAddress, custPassword, custId, custPassport);
                }
                
                
                
                model.addAttribute("customer",customer);

                connection.close();

            }

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/error";
        }
        return "Customer/CustomerProfile";
    }
    
    //CUSTOMER UPDATE PROFILE

    @GetMapping("/customerUpdate")
    public String customerUpdate(HttpSession session, Model model) {
        Long custId = (Long) session.getAttribute("custid");

        try {
            Connection connection = dataSource.getConnection();
            String sql = "SELECT c.custname, c.custemail, c.custaddress, c.custphonenum, c.custpassword, ct.custicnum, nc.custpassport " +
                     "FROM public.customers c " +
                     "LEFT JOIN public.citizen ct ON c.custid = ct.custid " +
                     "LEFT JOIN public.noncitizen nc ON c.custid = nc.custid " +
                     "WHERE c.custid = ?";

            final var statement= connection.prepareStatement(sql);
            statement.setLong(1, custId);
            final var resultSet = statement.executeQuery();

            if (resultSet.next()){
                
                String custName = resultSet.getString("custname");
                String custEmail = resultSet.getString("custemail");
                String custAddress = resultSet.getString("custaddress");
                String custphonenum= resultSet.getString("custphonenum");
                String custPassword = resultSet.getString("custpassword");
                String custic = resultSet.getString("custicnum");
                String custPassport = resultSet.getString("custpassport");
                
                

                Customer customer = null;

                if (custic != null){

                customer = new Citizen(custName, custEmail, custphonenum, custAddress, custPassword, custId, custic);
               
                } else if (custPassport!= null){
                    customer = new NonCitizen(custName, custEmail, custphonenum, custAddress, custPassword, custId, custPassport);
                }
                
                
                
                model.addAttribute("customer",customer);

                connection.close();

            }
            }catch (Exception e){
                e.printStackTrace();
            }
            return "Customer/CustomerUpdate";
        }

        @PostMapping("/customerUpdate")
        public String customerUpdate(Model model,Long custid,HttpSession session) {
            Long custId = (Long) session.getAttribute("custid");
            Customer customer = new Customer();
            try {
                Connection conn = dataSource.getConnection();
                String sql = "Update Customer set custname=?,custemail=?,custaddress=?,custphonenum=?,custpassword=? WHERE custid=?";
                PreparedStatement statement = conn.prepareStatement(sql);
                statement.setLong(6, custId);
                statement.setString(1,customer.getCustName());
                statement.setString(2, customer.getCustEmail());
                statement.setString(3, customer.getCustAddress());
                statement.setString(4, customer.getCustPhoneNum());
                statement.setString(5, customer.getCustPassword());
                statement.executeUpdate();
                
                if (customer instanceof Citizen citizen) {
                    sql = "UPDATE citizen SET custicnum = ? WHERE custid = ?";
                    statement = conn.prepareStatement(sql);
                    statement.setString(1, citizen.getCustIcNum());
                    statement.setLong(2, citizen.getCustID());
                    statement.executeUpdate();
                } else if (customer instanceof NonCitizen nonCitizen) {
                    sql = "UPDATE noncitizen SET custpassportnum = ? WHERE custid = ?";
                    statement = conn.prepareStatement(sql);
                    statement.setString(1, nonCitizen.getCustPassport());
                    statement.setLong(2, nonCitizen.getCustID());
                    statement.executeUpdate();
                }

                model.addAttribute("customer",customer);

                conn.close();
                
            }catch(Exception e) {
                e.printStackTrace();
            }
            return "Customer/CustomerUpdate";
        }

    @GetMapping("/accLogin")
    public String index(){
        return "indexLogin";
    }
    
    
}
