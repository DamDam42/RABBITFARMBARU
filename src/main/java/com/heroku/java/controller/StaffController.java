package com.heroku.java.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.security.auth.login.LoginException;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.heroku.java.model.Staff;

import jakarta.servlet.http.HttpSession;

@Controller
public class StaffController {
    private final DataSource dataSource;
    

    @Autowired
    public StaffController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/staffRegister")
    public String staffRegister(){
        return "Staff/StaffRegister";
    }

    @PostMapping("/staffRegisters")
    public String staffRegisters(@ModelAttribute("staffRegisters") Staff staff) {

        try {
            Connection conn = dataSource.getConnection();
            String sql = "INSERT INTO public.staff (staffname,staffemail,staffphonenum,staffaddress,staffpassword,managerid) VALUES (?,?,?,?,?,?)";
            PreparedStatement statement = conn.prepareStatement(sql);
			
			statement.setString(1, staff.getStaffName());
			statement.setString(2, staff.getStaffEmail());
			statement.setString(3, staff.getStaffPhoneNum());
			statement.setString(4, staff.getStaffAddress());
			statement.setString(5, staff.getStaffPassword());
			 if (staff.getManagerId() != null) {
	                statement.setInt(6, staff.getManagerId());
	            } else {
	                statement.setNull(6, java.sql.Types.INTEGER);
	            }
			
			statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return "redirect:/errorStaff";
            
        }
        return "redirect:/createStaffSuccess";
    }

    @GetMapping("/errorStaff")
    public String errorStaff(){
        return "Staff/CreateError";
    }

    @GetMapping("/createStaffSuccess")
    public String createstaffSuccess() {
        return "Staff/CreateStaffSuccess";
    }

    @GetMapping("/staffLogin")
    public String staffLogin() {
        return "Staff/StaffLogin";
    }

    @PostMapping("/staffLogins")
    public String staffLogins(HttpSession session,@RequestParam("staffEmail") String staffEmail, @RequestParam("staffPassword") String staffPassword,Staff staff) throws LoginException, SQLException{
        try {
            try (Connection conn = dataSource.getConnection()) {
                String sql = "SELECT staffid,staffname,staffemail,staffphonenum,staffaddress,staffpassword,managerid FROM public.staff WHERE staffemail=?";
                PreparedStatement statement = conn.prepareStatement(sql);
                statement.setString(1,staffEmail);
                
                ResultSet resultSet = statement.executeQuery();
                if(resultSet.next()) {
                    staff = new Staff();
                    staff.setStaffId(resultSet.getLong("staffid"));
                    staff.setStaffName(resultSet.getString("staffname"));
                    staff.setStaffEmail(resultSet.getString("staffemail"));
                    staff.setStaffPhoneNum(resultSet.getString("staffphonenum"));
                    staff.setStaffAddress(resultSet.getString("staffaddress"));
                    staff.setStaffPassword(resultSet.getString("staffpassword"));

                    Long staffId = staff.getStaffId();
                    
                    if(staff.getStaffEmail().equals(staffEmail) && staff.getStaffPassword().equals(staffPassword)) {
                   
                    session.setAttribute("staffname", staff.getStaffName());
                    
                    session.setAttribute("staffid", staffId);
                    
                    return "redirect:/indexStaff";
                   }
                   
                }
                     conn.close();
                     return "redirect:/staffLoginError";

                }

             
    }catch(SQLException e){
        return "redirect:/StaffError";
    }
   
}

@GetMapping("/indexStaff")
public String indexStaff(HttpSession session) {
    Long staffId = (Long) session.getAttribute("staffid");
    return "indexStaff";
}

@GetMapping("/staffLoginError")
public String staffLoginError(){
    return "Staff/StaffLoginError";
}

//STAFF VIEW PROFILE

@GetMapping("/staffProfile")
public String staffProfile(HttpSession session,Model model){
    Object staffIdObj = session.getAttribute("staffid");

    if (staffIdObj == null) {
        // Handle the case where staffid is not found in the session
        // Redirect to a login page or show an error message
        return "redirect:/staffLogin"; // Change this to your actual login page
    }

    Long staffId = (Long) staffIdObj;
    
        try {
            
            Connection conn = dataSource.getConnection();
            String sql = "Select staffname,staffemail,staffphonenum,staffaddress,staffpassword,managerid FROM public.staff WHERE staffid=? ";
            
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setLong(1, staffId);
            ResultSet resultSet = statement.executeQuery();
            
            if(resultSet.next()) {
                String staffname = resultSet.getString("staffname");
                String staffemail = resultSet.getString("staffemail");
                String staffphonenum = resultSet.getString("staffphonenum");
                String staffaddress = resultSet.getString("staffaddress");
                String staffpassword = resultSet.getString("staffpassword");
                Integer managerid = resultSet.getInt("managerid");
                
                 if (resultSet.wasNull()) {
                        managerid = null;
                    }

                Staff staff= new Staff();
                
                staff.setStaffName(staffname);
                staff.setStaffEmail(staffemail);
                staff.setStaffPhoneNum(staffphonenum);
                staff.setStaffAddress(staffaddress);
                staff.setStaffPassword(staffpassword);
                staff.setManagerId(managerid);

                 model.addAttribute("staff",staff);
            }

            conn.close();
  
}catch(SQLException e){
} 
return "Staff/StaffProfile";
}

@GetMapping("/staffUpdate")
public String staffUpdate(HttpSession session,Model model){
    session.getAttribute("staffid");
    Long staffId = (Long) session.getAttribute("staffid");

    if (staffId == null) {
            return "redirect:/staffLogin"; // redirect to login if staffId is not in session
        }

        try {
            Connection conn = dataSource.getConnection();
            String sql = "Select staffname,staffemail,staffphonenum,staffaddress,staffpassword,managerid FROM public.staff WHERE staffid=? ";
            
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setLong(1, staffId);
            ResultSet resultSet = statement.executeQuery();
            
            if(resultSet.next()) {
                String staffname = resultSet.getString("staffname");
                String staffemail = resultSet.getString("staffemail");
                String staffphonenum = resultSet.getString("staffphonenum");
                String staffaddress = resultSet.getString("staffaddress");
                String staffpassword = resultSet.getString("staffpassword");
                Integer managerid = resultSet.getInt("managerid");
                
                 if (resultSet.wasNull()) {
                        managerid = null;
                    }

                Staff staff= new Staff();
                
                staff.setStaffName(staffname);
                staff.setStaffEmail(staffemail);
                staff.setStaffPhoneNum(staffphonenum);
                staff.setStaffAddress(staffaddress);
                staff.setStaffPassword(staffpassword);
                staff.setManagerId(managerid);

                 model.addAttribute("staff",staff);
            }

            conn.close();
  
}catch(SQLException e){
} 
return "Staff/StaffUpdate";
}

@PostMapping("/staffUpdate")
public String staffUpdate(HttpSession session,@ModelAttribute("staffUpdate") Staff staff, Model model){
    Long staffId = (Long) session.getAttribute("staffid");

    if (staffId == null) {
            return "redirect:/staffLogin"; // redirect to login if staffId is not in session
        }

        try {
            Connection conn = dataSource.getConnection();
            String sql = "UPDATE public.staff SET staffname=?,staffemail=?,staffphonenum=?,staffaddress=?,staffpassword=?,managerid=? WHERE staffid=?";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setLong(7,staffId);
            statement.setString(1, staff.getStaffName());
            statement.setString(2, staff.getStaffEmail());
            statement.setString(3, staff.getStaffPhoneNum());
            statement.setString(4, staff.getStaffAddress());
            statement.setString(5, staff.getStaffPassword());
            if (staff.getManagerId() != null) {
                statement.setInt(6, staff.getManagerId());
            } else {
                statement.setNull(6, java.sql.Types.INTEGER);
            }
            statement.executeUpdate();
            
            conn.close();

        } catch (SQLException e) {
        } return "redirect:/staffProfile";
}

@GetMapping("/staffDelete")
public String staffDelete(HttpSession session){
    Long custId = (Long) session.getAttribute("staffid");

    try {
        Connection conn= dataSource.getConnection();
        String sql = "Delete From public.staff WHERE staffid=?";
        PreparedStatement statement = conn.prepareStatement(sql);

        statement.setLong(1, custId);

        statement.executeUpdate();
    } catch (SQLException e) {
    } return "redirect:/deleteStaffSuccess";

}

@GetMapping("/deleteStaffSuccess")
public String deleteStaffSuccess(){
    return "Staff/DeleteStaffSuccess";
}

@GetMapping("/logoutConfirmationStaff")
    public String logoutConfirmationStaff(HttpSession session){
        session.getAttribute("staffid");
        return "Staff/StaffLogout";
    }   

}




