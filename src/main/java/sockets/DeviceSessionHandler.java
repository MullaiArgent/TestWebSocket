package sockets;

import datamanagement.JDBC;
import models.ChatModel;
import models.RecentChatModel;
import javax.enterprise.context.ApplicationScoped;
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

    private int recentChatModelId = 0;
    private static final HashMap<String, Session> sessions = new HashMap<>();
    JDBC db = new JDBC();

    public void addSession(Session session, String userId) throws SQLException, ClassNotFoundException {
        sessions.put(userId, session);
        final List<RecentChatModel> recentChatModels = createRecentChatModels(userId);
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
        JsonProvider provider = JsonProvider.provider();
        JsonObject addMessage = provider.createObjectBuilder()
                .add("action", "notificationCount")
                .add("notificationCount",notificationCount)
                .build();
        return addMessage;
    }

    private List<RecentChatModel> createRecentChatModels(String userId) throws SQLException, ClassNotFoundException {
        final List<RecentChatModel> recentChatModels = new ArrayList<>();
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
        final List<ChatModel> chatModels = createChatModels(userId, friendId);
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

    private List<ChatModel> createChatModels(String userId, String friendId) throws SQLException, ClassNotFoundException {
        List<ChatModel> chatModels = new ArrayList<>();
        ResultSet chatModelsResultSet = db.dql("SELECT * FROM \"MESSAGES\" WHERE ((\"FROM\"='"+ userId +"' OR \"FROM\"='"+ friendId +"') AND (\"TO\"='"+ friendId +"' OR \"TO\"='"+ userId +"'));"); // chat data
        // SELECT * FROM public."MESSAGES" WHERE (("FROM"='BRUCE' OR "FROM"='ALICE') AND ("TO"='BRUCE' OR "TO"='ALICE'));
        while (chatModelsResultSet.next()){
            String data = chatModelsResultSet.getString(4);
            System.out.println(data);
            String from;
            if (chatModelsResultSet.getString(1).equals(userId)){
                from = "you";
            }else{
                from = "friend";
            }
            String time = chatModelsResultSet.getString(3);
            chatModels.add(createChatModel(data, from, time));
        }
        System.out.println();
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
        System.out.println("the session is being removed");
        sessions.entrySet().removeIf(entry -> session.equals(entry.getValue()));
    }

    public void sendMessage(String userId, JsonObject jsonObject){
        String friendId = jsonObject.getString("friendId");
        String message = jsonObject.getString("message");
        String time = jsonObject.getString("time");
        try {
            db.dml("INSERT INTO \"MESSAGES\" VALUES ('"+ userId +"','"+ friendId +"','"+time+"','" + message + "','chat');");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        JsonObject addMessage1 = createAddChatMessage(createChatModel(message,"you",time));
        sendToSession(sessions.get(userId),addMessage1);
        try {
            JsonObject addMessage2 = createAddChatMessage(createChatModel(message, "friend", time));
            sendToSession(sessions.get(friendId), addMessage2);
        }catch (NullPointerException e){
            System.out.println(friendId + "is Offline");
        }
    }

    public void addFriendWindow(String userId, JsonObject jsonObject, Session session) throws SQLException, ClassNotFoundException {
        String friendId = jsonObject.getString("friendId");
        ResultSet addFriendResultSet = db.dql("SELECT \"ID\",\"PROFILEPIC\" FROM public.\"USERS\"");
        while (addFriendResultSet.next()){
            if (addFriendResultSet.getString(1).equals(friendId)){
                String profile = addFriendResultSet.getString(2);
                ResultSet friendListResultSet = db.dql("SELECT \"FRIENDS\" FROM public.\"USERS\" WHERE \"ID\"='" + userId + "' ");
                while (friendListResultSet.next()){
                    Array friends;
                    String[] friendsArray = new String[0];
                    if (!friendListResultSet.getString(1).equals("{}")){
                        friends = friendListResultSet.getArray(1);
                        friendsArray = (String[])friends.getArray();
                    }else {
                        JsonObject addMessage = createFriendRequestWindowMessage(friendId, profile);
                        sendToSession(session, addMessage); // has no friend, fd reqing
                        return;
                    }
                    for (String friend : friendsArray) {
                        if (friendId.equals(friend)){
                            // are fds, chat window
                            JsonObject addMessage = createAlreadyAFriendWindowMessage(friendId, profile);
                            sendToSession(session, addMessage);
                            return;
                        }
                    }
                }
                JsonObject addMessage = createFriendRequestWindowMessage(friendId, profile);
                sendToSession(session, addMessage); // not a friend, but a user so sends a req
                return;
            }
        }
        // user does not exist, send Invitation
        ResultSet invitationResultSet = db.dql("SELECT * FROM public.\"NOTIFICATION\" where \"RECIPIENT_ID\" = '"+friendId+"' and \"SENDER_ID\" ='"+userId+"';");
        System.out.println("from " + userId + " to " + friendId);
        while (invitationResultSet.next()){
            System.out.println("inside the notification query");
            sendToSession(session, createAlreadyInvitedWindowMessage(friendId));
            return;
        }
        JsonObject addMessage = createInvitationWindowMessage(friendId);
        sendToSession(session, addMessage);
    }

    public JsonObject createAlreadyAFriendWindowMessage(String friendId, String profile){
        JsonProvider provider = JsonProvider.provider();
        JsonObject message = provider.createObjectBuilder()
                .add("action","chatWindow")
                .add("friendId",friendId)
                .add("profile", profile)
                .build();
        return message;
    }

    public JsonObject createFriendRequestWindowMessage(String friendId, String profile){
        JsonProvider provider = JsonProvider.provider();
        JsonObject message = provider.createObjectBuilder()
                .add("action","addFriendWindow")
                .add("friendId",friendId)
                .add("profile",profile)
                .build();
        return message;
    }

    public JsonObject createInvitationWindowMessage(String friendId){
        JsonProvider provider = JsonProvider.provider();
        JsonObject message = provider.createObjectBuilder()
                .add("action","invitationWindow")
                .add("friendId",friendId)
                .build();
        return message;
    }

    public JsonObject createAlreadyInvitedWindowMessage(String friendId){
        JsonProvider provider = JsonProvider.provider();
        JsonObject message = provider.createObjectBuilder()
                .add("action","alreadyInvited")
                .add("friendId",friendId)
                .build();
        return message;
    }

    public void sendFriendRequest(String userId, String friendId) {
        try {
            db.dml("insert into public.\"NOTIFICATION\" values('"+ friendId +"','"+ userId +"','friend_request',now(),FALSE);");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        JsonProvider provider = JsonProvider.provider();
        JsonObject addMessage = provider.createObjectBuilder()
                .add("action","newNotification")
                .add("from", userId)
                .build();
        sendToSession(sessions.get(friendId),addMessage);
    }
}
