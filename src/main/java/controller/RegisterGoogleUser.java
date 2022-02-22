package controller;

import datamanagement.JDBC;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet(urlPatterns = "/registerGoogleUser")
public class RegisterGoogleUser extends HttpServlet {

    JDBC db = new JDBC();

    public RegisterGoogleUser() throws ClassNotFoundException, SQLException {
    }

    @Override
    public void service(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String userId;
        String fullName;
        String profilePic;

        userId = req.getParameter("email");
        if (userId == null){
            res.sendRedirect("app");
        }
        fullName = req.getParameter("name");
        profilePic = req.getParameter("picture");

        try {
            StringBuilder insertGoogleUser = new StringBuilder();
            insertGoogleUser.append("INSERT INTO public.\"USERS\" VALUES ('");
            insertGoogleUser.append(userId);
            insertGoogleUser.append("','");
            insertGoogleUser.append(fullName);
            insertGoogleUser.append("','");
            insertGoogleUser.append(profilePic);
            insertGoogleUser.append("','{}','");
            insertGoogleUser.append(userId);
            insertGoogleUser.append("');");

            db.dml(insertGoogleUser.toString());

            res.sendRedirect("app");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
