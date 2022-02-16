package controller;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = "/logout")
public class LogOut extends HttpServlet {
    @Override
    public void service(HttpServletRequest req, HttpServletResponse res) throws IOException {
        try {
            req.getSession(false).invalidate();

        }catch (Exception e){
            System.out.println("There's "+ e +" while logging out the User");
        }finally {
            res.sendRedirect("app");
        }

    }
}
