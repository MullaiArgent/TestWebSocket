package sockets;

import datamanagement.InvitationMailer;

import javax.enterprise.context.ApplicationScoped;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
@ServerEndpoint(value = "/actions", configurator = HttpSessionConfigurer.class)
public class WebSocketServer {
    private final SessionHandler sessionHandler = new SessionHandler();
    String userId;
    HttpSession httpSession;
    @OnOpen
    public void open(Session session, EndpointConfig config) throws SQLException, ClassNotFoundException {
        httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
        userId = (String) httpSession.getAttribute("userId");
        sessionHandler.addSession(session, userId);
        String token = (String) httpSession.getAttribute("invitationToken");
        String userIdJwt = (String) httpSession.getAttribute("userIdJwt");
        String friendIdJwt = (String) httpSession.getAttribute("friendIdJwt");
        if ((token != null) && (userIdJwt != null) && (friendIdJwt != null)) {
            sessionHandler.invitationAccepted(userIdJwt, friendIdJwt);
            httpSession.removeAttribute("invitationToken");
            httpSession.removeAttribute("userIdJwt");
            httpSession.removeAttribute("friendIdJwt");
        }

    }
    @OnClose
    public void close(Session session) throws SQLException, ClassNotFoundException {
        // TODO Update the db
        sessionHandler.removeSession(session, userId);
    }
    @OnError
    public void error(Throwable error){
        Logger.getLogger(WebSocketServer.class.getName()).log(Level.SEVERE, null, error);
    }
    @OnMessage
    public void handleMessage(String message, Session session){
        try(JsonReader reader = Json.createReader(new StringReader(message))){
            JsonObject jsonObject = reader.readObject();
            if ("viewChat".equals(jsonObject.getString("action"))){
                sessionHandler.addChatModels(userId, jsonObject.getString("friendId"), session);
            }
            if ("sendMessage".equals(jsonObject.getString("action"))){
                sessionHandler.sendMessage(userId, jsonObject);
            }
            if ("addFriend".equals(jsonObject.getString("action"))){
                sessionHandler.addFriendWindow(userId, jsonObject, session);
            }
            if ("friendRequest".equals(jsonObject.getString("action"))){
                sessionHandler.sendFriendRequest(userId, jsonObject.getString("friendId"), session);
            }
            if ("invitation".equals(jsonObject.getString("action"))){
                new InvitationMailer().mailer(userId, jsonObject);
            }
            if ("cancelOutGoingFriendRequest".equals(jsonObject.getString("action"))){
                sessionHandler.cancelOutGoingFriendRequest(userId,jsonObject);
            }
            if ("cancelIncomingFriendRequest".equals(jsonObject.getString("action"))){
                sessionHandler.cancelIncomingFriendRequest(userId,jsonObject);
            }
            if ("confirmFriendRequest".equals(jsonObject.getString("action"))){
                sessionHandler.confirmFriendRequest(userId, jsonObject.getString("friendId"), session);
            }
            if ("notificationsViewed".equals(jsonObject.getString("action"))){
                sessionHandler.notificationsViewed(userId);
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }
}

