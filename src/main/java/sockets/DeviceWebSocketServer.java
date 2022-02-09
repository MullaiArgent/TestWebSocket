package sockets;

import models.Device;

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
public class DeviceWebSocketServer {


    private final DeviceSessionHandler sessionHandler = new DeviceSessionHandler();
    String userId;
    @OnOpen
    public void open(Session session, EndpointConfig config) throws SQLException, ClassNotFoundException {
        HttpSession httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
        userId = (String) httpSession.getAttribute("userId");
        sessionHandler.addSession(session, userId);
    }
    @OnClose
    public void close(Session session){
        sessionHandler.removeSession(session);
    }
    @OnError
    public void error(Throwable error){
       Logger.getLogger(DeviceWebSocketServer.class.getName()).log(Level.SEVERE, null, error);
    }
    @OnMessage
    public void handleMessage(String message, Session session){
        try(JsonReader reader = Json.createReader(new StringReader(message))){
            JsonObject jsonObject = reader.readObject();
            if ("add".equals(jsonObject.getString("action"))){
                Device device = new Device();
                device.setName(jsonObject.getString("name"));
                device.setDescription(jsonObject.getString("description"));
                System.out.println(jsonObject.getString("description") + "  description from the front end");
                device.setType(jsonObject.getString("type"));
                device.setStatus("Off");
                sessionHandler.addDevice(device);
            }
            if ("viewChat".equals(jsonObject.getString("action"))){
                    sessionHandler.addChatModels(userId, jsonObject.getString("friendId"), session);
            }
            if ("sendMessage".equals(jsonObject.getString("action"))){
                    sessionHandler.sendMessage(userId, jsonObject);
            }
            if ("viewNotification".equals(jsonObject.getString("action"))){}
            if ("friendRequest".equals(jsonObject.getString("action"))){}
            if ("confirmFriendRequest".equals(jsonObject.getString("action"))){}
            if ("declineFriendRequest".equals(jsonObject.getString("action"))){}

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }
}

