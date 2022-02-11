package controller;

import datamanagement.JDBC;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.crypto.spec.SecretKeySpec;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.security.Key;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;

@WebServlet(urlPatterns = "/createAccount")
public class CreateAccountController extends HttpServlet {
    private String getValue(String jwt, String key){
        StringBuilder val = new StringBuilder();
        for (int i = 0; i < jwt.length();i++){
            if (jwt.charAt(i)==key.charAt(0)){
                if (jwt.startsWith(key, i)){
                    i += key.length()+1;
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

    JDBC db = new JDBC();
    @Override
    public void service(HttpServletRequest req, HttpServletResponse res) throws IOException {

        HttpSession session = req.getSession();
        String token = (String) session.getAttribute("invitationToken");
        session.removeAttribute("invitationToken");
        PrintWriter out = res.getWriter();
        if (token != null) {
            String secret = "asdfSFS34wfsdfsdfSDSD32dfsddDDerQSNCK34SOWEK5354fdgdf4";
            Key hmacKey = new SecretKeySpec(Base64.getDecoder().decode(secret), SignatureAlgorithm.HS256.getJcaName());
            Jws<Claims> jwt = Jwts.parserBuilder()
                    .setSigningKey(hmacKey)
                    .build()
                    .parseClaimsJws(token);
            boolean invited = false;
            String friendIdParameter = req.getParameter("mail_id");
            String userIdJwt = getValue(String.valueOf(jwt), "userId");
            System.out.println(userIdJwt);
            String friendIdJwt = getValue(String.valueOf(jwt), "friendId");
            try {

                ResultSet rs = db.dql("SELECT * FROM public.\"NOTIFICATION\" WHERE \"RECIPIENT_ID\"='" + friendIdParameter + "' AND \"SENDER_ID\"='" + userIdJwt + "';");
                while (rs.next()) {
                    invited = true;
                }
            } catch (SQLException | ClassNotFoundException e) {
                e.printStackTrace();
            }

            if (invited && (friendIdJwt.equals(friendIdParameter))) {
                addUSer(req);
                try {
                    db.dml("UPDATE public.\"USERS\" set \"FRIENDS\" = array_append(\"FRIENDS\", '" + friendIdParameter + "') where \"ID\"='" + userIdJwt + "';");
                    db.dml("UPDATE public.\"USERS\" set \"FRIENDS\" = array_append(\"FRIENDS\", '" + userIdJwt + "') where \"ID\"='" + friendIdParameter + "';");
                    db.dml("UPDATE public.\"NOTIFICATION\" set \"ACTIVITY_TYPE\" = 'friends' where \"RECIPIENT_ID\"='"+ friendIdParameter +"' and \"SENDER_ID\"='"+ userIdJwt +"';");
                    // TODO notify front end
                } catch (ClassNotFoundException | SQLException e) {
                    e.printStackTrace();
                }

            } else {
                out.println("not a valid entry valid");
            }
        } else {
            addUSer(req);
        }
    }
    private void addUSer(HttpServletRequest req){
            String userId;
            String fullName;
            String profilePic;
            String password;
            String re_password;

            userId = req.getParameter("userid");
            fullName = req.getParameter("full_name");
            profilePic = req.getParameter("profile_pic");
            password = req.getParameter("password");
            re_password = req.getParameter("re_password");
            if (password.equals(re_password)) {
                try {
                    db.dml("INSERT INTO public.\"USERS\" VALUES ('" + userId + "','" + fullName + "','" + profilePic + "','{}');");
                    db.addUser("INSERT INTO users VALUES ('" + userId + "','" + password + "')");
                    db.addUser("INSERT INTO users_roles VALUES ('" + userId + "','user')");
                } catch (ClassNotFoundException | SQLException e) {
                    e.printStackTrace();
                }

            }
        }
    }
