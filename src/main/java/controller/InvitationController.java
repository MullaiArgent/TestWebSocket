package controller;

import datamanagement.JDBC;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.RequestDispatcher;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Key;
import java.sql.SQLException;
import java.util.Base64;

@WebServlet(urlPatterns = "/invitation")
public class InvitationController extends HttpServlet {
    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {

        JDBC db = null;
        try {
            db = new JDBC();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        String token = request.getParameter("token");
        PrintWriter out = response.getWriter();
        HttpSession session = request.getSession();
        session.setAttribute("invitationToken",token);


        if (token != null) {
            String secret = "asdfSFS34wfsdfsdfSDSD32dfsddDDerQSNCK34SOWEK5354fdgdf4";
            Key hmacKey = new SecretKeySpec(Base64.getDecoder().decode(secret), SignatureAlgorithm.HS256.getJcaName());

            try {
                Jws<Claims> jwt = Jwts.parserBuilder()
                        .setSigningKey(hmacKey)
                        .build()
                        .parseClaimsJws(token);
                //ResultSet rs = db.dql("select * from public.\"USERS\" where \"ID\" = '"+getValue(String.valueOf(jwt))+"';");
                // TODO
                RequestDispatcher rd = request.getRequestDispatcher("register");
                rd.forward(request, response);

            }catch (Exception e){
                out.println("The Invitation is either broken or invalid");
            }

        }
    }
    private String getValue(String jwt){
        StringBuilder val = new StringBuilder();
        for (int i = 0; i < jwt.length();i++){
            if (jwt.charAt(i)== "friendId".charAt(0)){
                if (jwt.startsWith("friendId", i)){
                    i += "friendId".length()+1;
                    while(jwt.charAt(i)!=','){
                        val.append(jwt.charAt(i));
                        i++;
                    }
                    break;
                }
            }
        }
        return val.toString();
    }
}

