package sockets;

import datamanagement.JDBC;
import models.ChatModel;
import models.Device;
import models.RecentChatModel;

import javax.enterprise.context.ApplicationScoped;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.spi.JsonProvider;
import javax.websocket.Session;
import java.io.IOException;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


@ApplicationScoped
public class DeviceSessionHandler {

    public DeviceSessionHandler() {}

    private int deviceId = 0;
    private int recentChatModelId = 0;
    private static final HashMap<String, Session> sessions = new HashMap<String, Session>();
    private static final Set<Device> devices = new HashSet<>();
    JDBC db = new JDBC();

    public void addSession(Session session, String userId) throws SQLException, ClassNotFoundException {
        sessions.put(userId, session);
        final Set<RecentChatModel> recentChatModels = createRecentChatModels(userId);
        for (RecentChatModel recentChatModel : recentChatModels){
            JsonObject addMessage = createAddRecentChatMessage(recentChatModel);
            sendToSession(session, addMessage);
        }
        int notificationCount = getNotificationCount(userId);
        JsonObject addMessage = createAddNotificationCount(notificationCount);
        sendToSession(session, addMessage);
    }

    private int getNotificationCount(String userId) throws SQLException, ClassNotFoundException {
        ResultSet notificationCountResultSet = db.dql("select count(*) from public.\"NOTIFICATION\" where \"RECIPIENT_ID\"='"+userId+"' or \"SENDER_ID\"='"+userId+"'");
        int notificationCount = 0;
        while (notificationCountResultSet.next()){
            notificationCount = notificationCountResultSet.getInt(1);
        }
        return notificationCount;
    }

    private JsonObject createAddNotificationCount(int notificationCount) {
        System.out.println("Json for notifi" + notificationCount);
        JsonProvider provider = JsonProvider.provider();
        JsonObject addMessage = provider.createObjectBuilder()
                .add("action", "notificationCount")
                .add("notificationCount",notificationCount)
                .build();
        return addMessage;
    }

    private HashSet<RecentChatModel> createRecentChatModels(String userId) throws SQLException, ClassNotFoundException {
        final HashSet<RecentChatModel> recentChatModels = new HashSet<>();
        ResultSet friendListResultSet = db.dql("SELECT \"FRIENDS\" FROM public.\"USERS\" WHERE \"ID\"='" + userId + "' ");
        while (friendListResultSet.next()){
            Array friends;
            String[] friendsArray = new String[0];
            if (!friendListResultSet.getString(1).equals("{}")){
                friends = friendListResultSet.getArray(1);
                friendsArray = (String[])friends.getArray();
            }
            for (String friendId : friendsArray) {
                RecentChatModel recentChatModel = new RecentChatModel();
                recentChatModel.setName(friendId);
                recentChatModel.setId(recentChatModelId);
                recentChatModelId++;
                ResultSet profileResultSet = db.dql("SELECT \"PROFILEPIC\" FROM public.\"USERS\" WHERE \"ID\"='"+ friendId +"';");
                while (profileResultSet.next()) {
                    recentChatModel.setProfile(profileResultSet.getString(1));
                }
                recentChatModels.add(recentChatModel);
            }
        }
        return recentChatModels;
    }

    private JsonObject createAddRecentChatMessage(RecentChatModel recentChatModel){
        JsonProvider provider = JsonProvider.provider();
        JsonObject message = provider.createObjectBuilder()
                .add("action","addRecentChatModel")
                .add("id",recentChatModel.getId())
                .add("name",recentChatModel.getName())
                .add("active",recentChatModel.isActive())
                .add("profile",recentChatModel.getProfile())
                .build();
        return message;
    }

    public void addChatModels(String userId, String friendId, Session session) throws SQLException, ClassNotFoundException {
        final Set<ChatModel> chatModels = createChatModels(userId, friendId);
        for (ChatModel chatModel : chatModels){
            JsonObject addMessage = createAddChatMessage(chatModel);
            sendToSession(session, addMessage);
        }
    }

    private ChatModel createChatModel(String data, String from, String time){
        ChatModel chatModel = new ChatModel();
        chatModel.setData(data);
        chatModel.setFrom(from);
        chatModel.setTime(time);
        return chatModel;
    }

    private HashSet<ChatModel> createChatModels(String userId, String friendId) throws SQLException, ClassNotFoundException {
        HashSet<ChatModel> chatModels = new HashSet<>();
        ResultSet chatModelsResultSet = db.dql("SELECT * FROM \"MESSAGES\" WHERE ((\"FROM\"='"+ userId +"' OR \"FROM\"='"+ friendId +"') AND (\"TO\"='"+ friendId +"' OR \"TO\"='"+ userId +"'));"); // chat data
        // SELECT * FROM public."MESSAGES" WHERE (("FROM"='BRUCE' OR "FROM"='ALICE') AND ("TO"='BRUCE' OR "TO"='ALICE'));
        while (chatModelsResultSet.next()){
            String data = chatModelsResultSet.getString(4);
            String from;
            if (chatModelsResultSet.getString(1).equals(userId)){
                from = "you";
            }else{
                from = "friend";
            }
            String time = chatModelsResultSet.getString(3).substring(0,5);
            chatModels.add(createChatModel(data, from, time));
        }
        return chatModels;
    }

    private JsonObject createAddChatMessage(ChatModel chatModel){
        JsonProvider provider = JsonProvider.provider();
        JsonObject message = provider.createObjectBuilder()
                .add("action","viewChat")
                .add("data",chatModel.getData())
                .add("from",chatModel.getFrom())
                .add("time",chatModel.getTime())
                .build();
        return message;
    }

    private void sendToSession(Session session, JsonObject message){
        try{
            session.getBasicRemote().sendText(message.toString());
        } catch (IOException e) {
            removeSession(session);
            Logger.getLogger(DeviceSessionHandler.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public void removeSession(Session session){
        System.out.println("the session is to be removed");
        sessions.entrySet().removeIf(entry -> session.equals(entry.getValue()));
    }

    public JsonObject createAddMessage(Device device){
        JsonProvider provider = JsonProvider.provider();
        JsonObject addMessage = provider.createObjectBuilder()
                .add("action","add")
                .add("id",device.getId())
                .add("name",device.getName())
                .add("type",device.getType())
                .add("status",device.getStatus())
                .add("description",device.getDescription())
                .build();
        return addMessage;
    }

    public void addDevice(Device device){
        device.setId(deviceId);
        devices.add(device);
        deviceId++;
        JsonObject addMessage = createAddMessage(device);
        sendToAllConnectedSession(addMessage);
    }

    public void removeDevice(int id){
        Device device = getDeviceById(id);
        if(device != null){
            devices.remove(device);
            JsonProvider provider = JsonProvider.provider();
            JsonObject removeMessage =  provider.createObjectBuilder()
                    .add("action","remove")
                    .add("id",id)
                    .build();
            sendToAllConnectedSession(removeMessage);
        }
    }

    public void toggleDevice(int id){
        JsonProvider provider = JsonProvider.provider();
        Device device = getDeviceById(id);
        if (device != null){
            if ("On".equals(device.getStatus())){
                device.setStatus("Off");
            } else {
                device.setStatus("On");
            }
            JsonObject updateDevMessage = provider.createObjectBuilder()
                    .add("action","toggle")
                    .add("id",device.getId())
                    .add("status", device.getStatus())
                    .build();
            sendToAllConnectedSession(updateDevMessage);
        }
    }

    public Device getDeviceById(int id){
        for (Device device : devices){
            if (device.getId() == id){
                return device;
            }
        }
        return null;
    }

    public void sendToAllConnectedSession(JsonObject message){
        for (Map.Entry<String, Session> session : sessions.entrySet()){
            sendToSession(session.getValue(), message);
        }
    }

    public void sendMessage(String userId, JsonObject jsonObject){

        JsonObject addMessage1 = createAddChatMessage(createChatModel(jsonObject.getString("message"),"you",jsonObject.getString("time")));
        sendToSession(sessions.get(userId),addMessage1);
        JsonObject addMessage2 = createAddChatMessage(createChatModel(jsonObject.getString("message"),"friend",jsonObject.getString("time")));
        sendToSession(sessions.get(jsonObject.getString("friendId")),addMessage2);
    }
}
