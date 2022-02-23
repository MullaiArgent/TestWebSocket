package datamanagement;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import sockets.SessionHandler;
import javax.crypto.spec.SecretKeySpec;
import javax.json.JsonObject;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.security.Key;
import java.sql.Date;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Base64;
import java.util.Properties;
import java.util.UUID;

public class InvitationMailer {
    String scheme;
    String serverName;
    String serverPort;
    JDBC db = new JDBC();

    public InvitationMailer(String scheme, String serverName, String serverPort) throws ClassNotFoundException, SQLException {
        this.scheme =scheme;
        this.serverPort=serverPort;
        this.serverName=serverName;
    }

    public void mailer(String userId, JsonObject jsonObject){
    Properties properties = new Properties();
        properties.put("mail.smtp.auth","true");
        properties.put("mail.smtp.starttls.enable","true");
        properties.put("mail.smtp.host","smtp.gmail.com");
        properties.put("mail.smtp.port","587");

    final String domainMail = "mullairajan2000@gmail.com";
    final String password = "ArgentWithSilverBlade";
    final String friendId = jsonObject.getString("friendId");

    Session session = Session.getInstance(properties, new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(domainMail, password);

        }
    });
    StringBuilder url = new StringBuilder();
    url.append(scheme);
    url.append("://");
    url.append(serverName);
    url.append(":");
    url.append(serverPort);
    url.append("/TestWebSocket_war/invitation?token=");
    url.append(generateInvitationToken(userId, friendId));

        //String invitationUrl = "http://localhost:8080/TestWebSocket_war/invitation?token="+ generateInvitationToken(userId, friendId);
        Message message = prepareMessage(session, friendId, userId, url.toString());

        try {
        assert message != null;
        Transport.send(message);
        SessionHandler sessionHandler = new SessionHandler();

        sessionHandler.addInvitedNotification(userId, jsonObject);

        try {
            StringBuilder insertNotification = new StringBuilder();
            insertNotification.append("insert into public.\"NOTIFICATION\" values('");
            insertNotification.append(friendId);
            insertNotification.append("','");
            insertNotification.append(userId);
            insertNotification.append("','invitation',now(),FALSE);");
            db.dml(insertNotification.toString());
            // To avoid the reUsability of the Token
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    } catch (MessagingException | SQLException | ClassNotFoundException e) {
        e.printStackTrace();
    }
    }
    private static Message prepareMessage(Session session, String recipient, String sender,  String invitationUrl){
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("mullairajan2000@gmail.com"));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            message.setSubject(sender + " has sent you a Invitation to Use this App");
            message.setText(invitationUrl);
            return message;
        }catch (Exception ex){
            System.out.println("Error");
        }
        return null;
    }

    public String generateInvitationToken(String userId, String friendId){
        String secret = "asdfSFS34wfsdfsdfSDSD32dfsddDDerQSNCK34SOWEK5354fdgdf4";
        Key hmacKey = new SecretKeySpec(Base64.getDecoder().decode(secret), SignatureAlgorithm.HS256.getJcaName());
        Instant now = Instant.now();
        String jwtToken = Jwts.builder()
                .claim("friendId", friendId)
                .claim("userId", userId)
                .setSubject("Subject")
                .setIssuedAt(Date.from(now))
                .setId(UUID.randomUUID().toString())
                //.setExpiration(java.util.Date.from(now.plus(51, ChronoUnit.MINUTES)))
                .signWith(hmacKey)
                .compact();
        return jwtToken;
    }
}
