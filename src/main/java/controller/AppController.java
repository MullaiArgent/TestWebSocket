package controller;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(value = "/app")
public class AppController extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        HttpSession session = req.getSession();
        session.setAttribute("userId",req.getRemoteUser());
        session.setAttribute("scheme",req.getScheme());
        session.setAttribute("serverName",req.getServerName());
        session.setAttribute("serverPort",req.getServerPort()+"");

        RequestDispatcher rd = req.getRequestDispatcher("index.html");
        rd.forward(req, res);
    }
}
