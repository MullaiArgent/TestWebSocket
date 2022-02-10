package datamanagement;

import java.sql.SQLException;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.Session;

public class InvitationMailer {
    static String host = "smtp.gmail.com";
    static String user = "mullairajan2000@gmail.com";
    JDBC db = new JDBC();
    public void mailer(String userId, String friendId){
    Properties properties = new Properties();
        properties.put("mail.smtp.auth","true");
        properties.put("mail.smtp.starttls.enable","true");
        properties.put("mail.smtp.host","smtp.gmail.com");
        properties.put("mail.smtp.port","587");

    final String domainMail = "mullairajan2000@gmail.com";
    final String password = "ArgentWithSilverBlade";

    Session session = Session.getInstance(properties, new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(domainMail, password);

        }
    });

        String invitationUrl = "http://localhost:8080/TomCatChatter_war/invitation?invited="+ userId;
        Message message = prepareMessage(session, friendId, userId, invitationUrl);

        try {
        assert message != null;
        Transport.send(message);
        System.out.println("Success from mail"); // TODO invited notification - notification
        try {
            db.dml("insert into public.\"NOTIFICATION\" values('"+ friendId +"','"+ userId +"','invitation',now(),FALSE);");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    } catch (MessagingException e) {
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
}
