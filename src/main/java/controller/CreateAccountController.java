package controller;

import datamanagement.JDBC;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Key;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Objects;

@WebServlet(urlPatterns = "/createAccount")
public class CreateAccountController extends HttpServlet {
    HttpSession session;
    JDBC db = new JDBC();
    public CreateAccountController() throws ClassNotFoundException, SQLException {
    }

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

    @SuppressWarnings("StringBufferReplaceableByString")
    @Override
    public void service(HttpServletRequest req, HttpServletResponse res) throws IOException {

        PrintWriter out = res.getWriter();
        session = req.getSession();
        String token = (String) session.getAttribute("invitationToken");

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
            String friendIdJwt = getValue(String.valueOf(jwt), "friendId");
            session.setAttribute("userIdJwt",userIdJwt);
            session.setAttribute("friendIdJwt",friendIdJwt);
            ResultSet rs;
            try {
                StringBuilder queryNotification = new StringBuilder();
                queryNotification.append("SELECT * FROM public.\"NOTIFICATION\" WHERE \"RECIPIENT_ID\"='");
                queryNotification.append(friendIdParameter);
                queryNotification.append("' AND \"SENDER_ID\"='");
                queryNotification.append(userIdJwt);
                queryNotification.append("';");

                rs = db.dql(queryNotification.toString());
                while (rs.next()) {
                    invited = true;
                }
                try {
                    rs.close();
                }catch (Exception e){
                    e.printStackTrace();
                    System.out.println("Theres an Exception while Closing the ResultSet\n Retrying...");
                    try{
                        rs.close();
                    }catch (Exception e1){
                        e1.printStackTrace();
                        System.out.println("Failed to Close the ResultSet");
                    }
                }
            } catch (SQLException | ClassNotFoundException e) {
                e.printStackTrace();
            }

            if (invited && (friendIdJwt.equals(friendIdParameter))) {
                addUSer(req);


                try {
                    StringBuilder updateUser = new StringBuilder();
                    updateUser.append("UPDATE public.\"USERS\" set \"FRIENDS\" = array_append(\"FRIENDS\", '");
                    updateUser.append(friendIdParameter);
                    updateUser.append("') where \"ID\"='");
                    updateUser.append(userIdJwt);
                    updateUser.append("';");

                    StringBuilder updateNotification = new StringBuilder();
                    updateNotification.append("UPDATE public.\"NOTIFICATION\" set \"ACTIVITY_TYPE\" = 'friends' where \"RECIPIENT_ID\"='");
                    updateNotification.append(friendIdParameter);
                    updateNotification.append("' and \"SENDER_ID\"='");
                    updateNotification.append(userIdJwt);
                    updateNotification.append("';");

                    db.dml(updateUser.toString());
                    db.dml(updateNotification.toString());
                    db.close();
                    res.sendRedirect("app");

                } catch (ClassNotFoundException | SQLException e) {
                    e.printStackTrace();
                    db.close();
                }

            } else {
                db.close();
                out.println("not a valid entry valid");
            }
        } else {
            addUSer(req);
            db.close();
            res.sendRedirect("app");
        }
    }
    private void addUSer(HttpServletRequest req){
            String userId;
            String fullName;
            String profilePic;
            String password;
            String re_password;
            String mail_id;

            userId = req.getParameter("userid");
            fullName = req.getParameter("full_name");
            profilePic = req.getParameter("profile_pic");
            System.out.println(profilePic + "the profile ");
            if(Objects.equals(profilePic, "")){
                profilePic = "https://www.pngitem.com/pimgs/m/146-1468479_my-profile-icon-blank-profile-picture-circle-hd.png";
            }
            System.out.println(profilePic + "the profile ");
            password = req.getParameter("password");
            re_password = req.getParameter("re_password");
            mail_id = req.getParameter("mail_id");
            if (password.equals(re_password)) {
                StringBuilder insertUser = new StringBuilder();
                insertUser.append("INSERT INTO public.\"USERS\" VALUES ('");
                insertUser.append(userId);
                insertUser.append("','");
                insertUser.append(fullName);
                insertUser.append("','");
                insertUser.append(profilePic);
                insertUser.append("','{");
                insertUser.append(session.getAttribute("userIdJwt"));
                insertUser.append("}', '");
                insertUser.append(mail_id);
                insertUser.append("');");
                try {
                    db.dml(insertUser.toString());
                }catch (Exception e){
                    e.printStackTrace();
                    System.out.println("Theres an Exception while Inserting the Data");
                }

                StringBuilder insertUserName = new StringBuilder();
                insertUserName.append("INSERT INTO users VALUES ('");
                insertUserName.append(userId);
                insertUserName.append("','");
                insertUserName.append(password);
                insertUserName.append("')");
                try {
                    db.dml(insertUserName.toString());
                }catch (Exception e){
                    e.printStackTrace();
                    System.out.println("Theres an Exception while Inserting the Data");
                }

                StringBuilder insertUserRole = new StringBuilder();
                insertUserRole.append("INSERT INTO users_roles VALUES ('");
                insertUserRole.append(userId);
                insertUserRole.append("','user')");
                try {
                    db.dml(insertUserRole.toString());
                }catch (Exception e){
                    e.printStackTrace();
                    System.out.println("Theres an Exception while Inserting the Data");
                }

            }
        }
    }
