package com.heroku.java.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
            return "redirect:/errorCustomer";
        }
        return "redirect:/createAccountSuccess";
    } 


    @GetMapping("/errorCustomer")
    public String errorCustomer(){
        return "Customer/CreateError";
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
                    String custName = resultSet.getString("custName");
                    String custEmail = resultSet.getString("custEmail");
                    String custPassword = resultSet.getString("custPassword");

                    if (custEmail.equals(custemail)&&custPassword.equals(custpassword)){
                        session.setAttribute("custname", custName);
                        session.setAttribute("custid", custId);

                        return "redirect:/accLogin";
                    }

                   

                }
                 connection.close();
                 return "redirect:/customerLoginError";
            } catch (Exception e) {
                e.printStackTrace();
                return "redirect:/error";
            }
        }

    @GetMapping("/customerProfile")
    public String customerProfile(HttpSession session, Model model) {
        Long custId = (Long) session.getAttribute("custid");

        if (custId == null) {
            return "redirect:/customerLogin"; // redirect to login if custId is not in session
        }
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
                String customerType = null;
                if (custic != null){

                customer = new Citizen(custName, custEmail, custphonenum, custAddress, custPassword, custId, custic);
                customerType = "Citizen";
                } else if (custPassport!= null){
                    customer = new NonCitizen(custName, custEmail, custphonenum, custAddress, custPassword, custId, custPassport);
                    customerType = "NonCitizen";
                }
                
                
                
                model.addAttribute("customer",customer);
                model.addAttribute("customerType", customerType);
                
            }

            connection.close();

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
                String custic = resultSet.getString("custIcNum");
                String custPassport = resultSet.getString("custPassport");
                
                

                Customer customer = null;
                String customerType = null;
                if (custic != null){
                customer = new Citizen(custName, custEmail, custphonenum, custAddress, custPassword, custId, custic);
                customerType = "Citizen";
                } else if (custPassport!= null){
                    customer = new NonCitizen(custName, custEmail, custphonenum, custAddress, custPassword, custId, custPassport);
                    customerType = "NonCitizen";
                }
                
                
                
                model.addAttribute("customer",customer);
                model.addAttribute("customerType", customerType);
                

            }
            connection.close();
            }catch (Exception e){
                e.printStackTrace();
            }
            return "Customer/CustomerUpdate";
        }

        @PostMapping("/customerUpdate")
        public String customerUpdate(HttpSession session,@ModelAttribute("customerUpdate") Customer customer,Model model) {
            Long custId = (Long) session.getAttribute("custid");
            
            try {
                Connection conn = dataSource.getConnection();
                String sql = "Update public.customers set custname=?,custemail=?,custaddress=?,custphonenum=?,custpassword=? WHERE custid=?";
                PreparedStatement statement = conn.prepareStatement(sql);
                statement.setLong(6, custId);
                statement.setString(1,customer.getCustName());
                statement.setString(2, customer.getCustEmail());
                statement.setString(3, customer.getCustAddress());
                statement.setString(4, customer.getCustPhoneNum());
                statement.setString(5, customer.getCustPassword());
                statement.executeUpdate();
                
                if (customer instanceof Citizen citizen) {
                    sql = "UPDATE public.citizen SET custicnum = ? WHERE custid = ?";
                    statement = conn.prepareStatement(sql);
                    statement.setString(1, citizen.getCustIcNum());
                    statement.setLong(2, citizen.getCustID());
                    statement.executeUpdate();
                } else if (customer instanceof NonCitizen nonCitizen) {
                    sql = "UPDATE public.noncitizen SET custpassportnum = ? WHERE custid = ?";
                    statement = conn.prepareStatement(sql);
                    statement.setString(1, nonCitizen.getCustPassport());
                    statement.setLong(2, nonCitizen.getCustID());
                    statement.executeUpdate();
                }

                

                conn.close();
                
            }catch(Exception e) {
                e.printStackTrace();
            }
            return "redirect:/customerProfile";
        }

        @GetMapping("/customerDelete")
        public String customerDelete(HttpSession session,@RequestParam("customerType") String custType) {

            Long custId = (Long) session.getAttribute("custid");
            try {
                Connection conn= dataSource.getConnection();
                String sql = "Delete From public.customers WHERE custid=?";
                PreparedStatement statementcust = conn.prepareStatement(sql);
                
                
                
                statementcust.setLong(1, custId);
                
                
                if(custType.equalsIgnoreCase("Citizen")) {
                    String sqlD = "DELETE FROM public.citizen WHERE custid=?";
                    PreparedStatement statementcitizen = conn.prepareStatement(sqlD);
                    statementcitizen.setLong(1, custId);
                    statementcitizen.executeUpdate();
                }
        
                else if(custType.equalsIgnoreCase("NonCitizen")) {
                    String sqlDe = "DELETE FROM public.noncitizen WHERE custid=?";
                    PreparedStatement statementnon = conn.prepareStatement(sqlDe);
                    statementnon.setLong(1, custId);
                    statementnon.executeUpdate();
                }
                
                
                statementcust.executeUpdate();
                
                conn.close();
                
            }catch(Exception e) {
                 e.printStackTrace();
                 return "redirect:/deleteAccountFail";
               
            } return "redirect:/deleteAccountSuccess";
        }
                

    @GetMapping("/accLogin")
    public String index(HttpSession session){
        session.getAttribute("custid");
        return "indexLogin";
    }

    @GetMapping("/deleteAccountSuccess")
    public String deleteAccSuccess(){
        return "Customer/DeleteAccountSuccess";
    }    

    @GetMapping("/deleteAccountFail")
    public String deleteAccountFail(HttpSession session){
        session.getAttribute("custid");
        return "Customer/CustomerDeleteFail";

    }

    @GetMapping("/customerLoginError")
    public String customerLoginError(){
        return "Customer/customerLoginError";
    }    

    @GetMapping("createAccountSuccess")
    public String createAccountSuccess(){
        return "Customer/createAccountSuccess";
    }    

    @GetMapping("/staffCustomerList")
    public String customerList(HttpSession session,Model model){
        List<Customer> customers = new ArrayList<>();
        session.getAttribute("staffid");

        try {
            Connection conn = dataSource.getConnection();
            String sql = "SELECT * from public.customers Order By custid";
            PreparedStatement statement = conn.prepareStatement(sql);

            ResultSet resultSet = statement.executeQuery();

            while(resultSet.next()){
                Customer customer = new Customer();

                customer.setCustID(resultSet.getLong("custid"));
                customer.setCustName(resultSet.getString("custname"));
                customer.setCustEmail(resultSet.getString("custemail"));
                customer.setCustPhoneNum(resultSet.getString("custphonenum"));
                customer.setcustAddress(resultSet.getString("custaddress"));
                customer.setCustPassword(resultSet.getString("custpassword"));

                customers.add(customer);
            }
            model.addAttribute("customer",customers);

        } catch (SQLException e) {
        } return "Staff/StaffCustomerList";

    }

    @GetMapping("/staffUpdateCustomer")
    public String staffUpdateCustomer(HttpSession session,@RequestParam("custId") Long custId, Model model){
        session.getAttribute("staffid");

        try {
            Connection conn = dataSource.getConnection();
            Connection connection = dataSource.getConnection();
            String sql = "SELECT c.custid,c.custname, c.custemail, c.custaddress, c.custphonenum, c.custpassword, ct.custicnum, nc.custpassport " +
                     "FROM public.customers c " +
                     "LEFT JOIN public.citizen ct ON c.custid = ct.custid " +
                     "LEFT JOIN public.noncitizen nc ON c.custid = nc.custid " +
                     "WHERE c.custid = ?";

            final var statement= connection.prepareStatement(sql);
            statement.setLong(1, custId);
            ResultSet resultSet = statement.executeQuery();

            if(resultSet.next()){
                String custname = resultSet.getString("custname");
                String custemail = resultSet.getString("custemail");
                String custaddress = resultSet.getString("custaddress");
                String custphonenum = resultSet.getString("custphonenum");
                String custpassword = resultSet.getString("custpassword");
                String custicnum = resultSet.getString("custicnum");
                String custpassport = resultSet.getString("custpassport");
                
                Customer customer = null;
                String customerType = null;
                if (custicnum != null){
                customer = new Citizen(custname, custemail, custphonenum, custaddress, custpassword, custId, custicnum);
                customerType = "Citizen";
                } else if (custpassport!= null){
                    customer = new NonCitizen(custname, custemail, custphonenum, custaddress, custpassword, custId, custpassport);
                    customerType = "NonCitizen";
                }

                model.addAttribute("customer",customer);
                model.addAttribute("customerType",customerType);
                
            }

            conn.close();

        } catch (Exception e) {
        } return "Staff/StaffUpdateCustomer";

    }


    @PostMapping("/staffUpdateCustomers")
    public String  staffUpdateCustomers(HttpSession session,@RequestParam("custId") Long custId, @ModelAttribute("staffUpdateCustomers") Customer customer, Model model){
        session.getAttribute("staffid");

        try {
            Connection conn = dataSource.getConnection();
            String sql ="UPDATE public.customers SET custname=?,custemail=?,custphonenum=?,custaddress=?,custpassword=? WHERE custid=?";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setLong(6, custId);
            statement.setString(1,customer.getCustName());
            statement.setString(2, customer.getCustEmail());
            statement.setString(3, customer.getCustAddress());
            statement.setString(4, customer.getCustPhoneNum());
            statement.setString(5, customer.getCustPassword());
            statement.executeUpdate();

            if(customer instanceof Citizen citizen){
                sql = "UPDATE public.citizen SET custicnum=? Where custid=?";
                statement = conn.prepareStatement(sql);
                statement.setLong(2, citizen.getCustID());
                statement.setString(1, citizen.getCustIcNum());
                statement.executeUpdate();
            } else if (customer instanceof NonCitizen noncitizen){
                sql = "Update public.noncitizen SET custpassportnum=? WHERE custid=?";
                statement = conn.prepareStatement(sql);
                statement.setLong(2, noncitizen.getCustID());
                statement.setString(1, noncitizen.getCustPassport());
                statement.executeUpdate();
            }
            conn.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }return "redirect:/staffCustomerList";

    }

    @GetMapping("/staffDeleteCustomer")
    public String staffDeleteCustomer(HttpSession session,@RequestParam("custId") Long custId,@RequestParam("custType") String custType){
        session.getAttribute("staffid");

        try {
            Connection conn = dataSource.getConnection();
            String sql ="DELETE FROM public.customers WHERE custid=?";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setLong(1, custId);

            if(custType.equalsIgnoreCase("Citizen")){
                String sqlc = "DELETE FROM public.citizen WHERE custid=?";
                PreparedStatement statementC=conn.prepareStatement(sqlc);
                statementC.setLong(1, custId);
                statementC.executeUpdate();
            }else if(custType.equalsIgnoreCase("NonCitizen")){
                String sqlnc= "DELETE FROM public.noncitizen WHERE custid=?";
                PreparedStatement statementnc=conn.prepareStatement(sqlnc);
                statementnc.setLong(1, custId);
                statementnc.executeUpdate();
            }

            statement.executeUpdate();

            conn.close();
        } catch (SQLException e) {
        } return "redirect:/staffDeleteCustSuccess";
    }

    @GetMapping("/staffDeleteCustSuccess")
    public String staffDeleteAccSuccess(HttpSession session){
        session.getAttribute("staffid");
        return "Staff/StaffDeleteCustSuccess";
    }

    @GetMapping("/logoutConfirmation")
    public String logoutConfirmation(HttpSession session){
        session.getAttribute("custid");
        return "Customer/CustomerLogout";
    }   

    @GetMapping("/logout")
    public String logout(HttpSession session){
        session.invalidate();
        return "redirect:/";
    }   


}
