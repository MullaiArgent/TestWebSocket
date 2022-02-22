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
    String scheme;
    String serverName;
    String serverPort;

    public WebSocketServer() throws ClassNotFoundException, SQLException {}

    @OnOpen
    public void open(Session session, EndpointConfig config) throws SQLException, ClassNotFoundException {
        httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
        userId = (String) httpSession.getAttribute("userId");
        scheme = (String) httpSession.getAttribute("scheme");
        serverPort = (String) httpSession.getAttribute("serverPort");
        serverName = (String) httpSession.getAttribute("serverName");
        String token = (String) httpSession.getAttribute("invitationToken");
        String userIdJwt = (String) httpSession.getAttribute("userIdJwt");
        String friendIdJwt = (String) httpSession.getAttribute("friendIdJwt");

        sessionHandler.addSession(session, userId);

        if ((token != null) && (userIdJwt != null) && (friendIdJwt != null)) {
            sessionHandler.invitationAccepted(userIdJwt, friendIdJwt);
            httpSession.removeAttribute("invitationToken");
            httpSession.removeAttribute("userIdJwt");
            httpSession.removeAttribute("friendIdJwt");
        }
    }
    @OnClose
    public void close(Session session) {
        sessionHandler.removeSession(session, userId);
        sessionHandler.close();
    }
    @OnError
    public void error(Throwable error){
        Logger.getLogger(WebSocketServer.class.getName()).log(Level.SEVERE, null, error);
        sessionHandler.close();
    }
    @OnMessage(maxMessageSize = 1048576)
    public void handleMessage(String message, Session session){
        try(JsonReader reader = Json.createReader(new StringReader(message))){
            JsonObject jsonObject = reader.readObject();
            if ("viewChat".equals(jsonObject.getString("action"))){
                sessionHandler.addChatList(userId, jsonObject.getString("friendId"), session);
            }
            else if ("sendMessage".equals(jsonObject.getString("action"))){
                sessionHandler.sendMessage(userId, jsonObject);
            }
            else if ("addFriend".equals(jsonObject.getString("action"))){
                sessionHandler.addFriendWindow(userId, jsonObject, session);
            }
            else if ("friendRequest".equals(jsonObject.getString("action"))){
                sessionHandler.sendFriendRequest(userId, jsonObject.getString("friendId"), session);
            }
            else if ("invitation".equals(jsonObject.getString("action"))){
                new InvitationMailer(scheme, serverName, serverPort).mailer(userId, jsonObject);
            }
            else if ("cancelOutGoingFriendRequest".equals(jsonObject.getString("action"))){
                sessionHandler.cancelOutGoingFriendRequest(userId,jsonObject);
            }
            else if ("cancelIncomingFriendRequest".equals(jsonObject.getString("action"))){
                sessionHandler.cancelIncomingFriendRequest(userId,jsonObject);
            }
            else if ("confirmFriendRequest".equals(jsonObject.getString("action"))){
                sessionHandler.confirmFriendRequest(userId, jsonObject.getString("friendId"), session);
            }
            else if ("notificationsViewed".equals(jsonObject.getString("action"))){
                sessionHandler.notificationsViewed(userId);
            }
            else if ("sendImage".equals(jsonObject.getString("action"))){
                sessionHandler.sendImage(userId, jsonObject);
            }else{
                System.out.println("Invalid message from the Client");
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }
}

