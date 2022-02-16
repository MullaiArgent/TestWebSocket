package sockets;

import datamanagement.JDBC;

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
public class SessionHandler {

    JDBC db = new JDBC();
    private static final HashMap<String, Session> sessions = new HashMap<>();
    public void addSession(Session session, String userId) throws SQLException, ClassNotFoundException {
        sessions.put(userId, session);
        System.out.println("Session add to Hash for "+ userId);

        db.dml("UPDATE public.\"USERS\" SET active='online' WHERE \"ID\"='"+userId+"';");
        pingAllFriends(userId, "backOnline");

        initialRecentChatList(session, userId);
        initialNotificationList(session,userId);


        int notificationCount = getNotificationCount(userId);
        JsonObject addMessage = createAddNotificationCount(notificationCount);
        sendToSession(session, addMessage);

    }
    public void removeSession(Session session, String userId) throws SQLException, ClassNotFoundException {
        db.dml("UPDATE public.\"USERS\" SET active='offline' WHERE \"ID\"='"+ userId +"';");
        sessions.entrySet().removeIf(entry -> session.equals(entry.getValue()));
    }
    private void sendToSession(Session session, JsonObject message) throws SQLException, ClassNotFoundException {
        String userid = "";
        try{
            session.getBasicRemote().sendText(message.toString());
        } catch (IOException e) {
            for (Map.Entry<String, Session> set : sessions.entrySet()){
                if (session.equals(set.getValue())){
                    userid = set.getKey();
                    break;
                }
            }
            removeSession(session, userid);
            Logger.getLogger(SessionHandler.class.getName()).log(Level.SEVERE, null, e);
        }
    }
    private void initialNotificationList(Session session, String userId) throws SQLException, ClassNotFoundException {

        ResultSet notificationResultSet = db.dql("SELECT * from public.\"NOTIFICATION\" where \"RECIPIENT_ID\"='"+userId+"' or \"SENDER_ID\"='"+userId+"'");
        while (notificationResultSet.next()){
            String sender = "";
            String receiver = "";
            String activityType = "";
            String time = "";
            boolean seen = false;
            receiver = notificationResultSet.getString(1);
            sender = notificationResultSet.getString(2);
            if (notificationResultSet.getString(3).equals("friendRequest")) {
                if (userId.equals(notificationResultSet.getString(2))) {
                    activityType = "friendRequestSent";
                } else {
                    activityType = "friendRequestReceived";
                }
            } else if (notificationResultSet.getString(3).equals("friends")){
                activityType = notificationResultSet.getString(3);
                if (userId.equals(notificationResultSet.getString(1))) {
                    receiver = notificationResultSet.getString(2);
                    sender = notificationResultSet.getString(2);
                }else {
                    receiver = notificationResultSet.getString(1);
                    sender = notificationResultSet.getString(1);
                }
            }
            else {
                activityType = notificationResultSet.getString(3);
            }
            time = notificationResultSet.getString(4);
            seen = notificationResultSet.getBoolean(5);
            sendToSession(session, createANotification(sender, receiver, activityType, time, seen));
        }
    }
    private JsonObject createANotification(String sender, String receiver, String activityType,String time, boolean seen){
        return JsonProvider
                .provider()
                .createObjectBuilder()
                .add("action","addNotification")
                .add("sender",sender)
                .add("receiver",receiver)
                .add("activity",activityType)
                .add("time",time)
                .add("seen",seen)
                .build();

    }
    private int getNotificationCount(String userId) throws SQLException, ClassNotFoundException {
        ResultSet notificationCountResultSet = db.dql("SELECT COUNT(*) FROM public.\"NOTIFICATION\" where (\"RECIPIENT_ID\"='"+userId+"' or \"SENDER_ID\"='"+userId+"') and \"IS_READ\"='false'");
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
    private void initialRecentChatList(Session session, String userId) throws SQLException, ClassNotFoundException {
        String profile = "";
        String active = "";
        ResultSet friendListResultSet = db.dql("SELECT \"FRIENDS\" FROM public.\"USERS\" WHERE \"ID\"='" + userId + "' ");
        while (friendListResultSet.next()){
            Array friends;
            String[] friendsArray = new String[0];
            if (!friendListResultSet.getString(1).equals("{}")){
                friends = friendListResultSet.getArray(1);
                friendsArray = (String[])friends.getArray();
            }
            for (String friendId : friendsArray) {
                ResultSet profileResultSet = db.dql("SELECT \"PROFILEPIC\",\"active\" FROM public.\"USERS\" WHERE \"ID\"='"+ friendId +"';");
                while (profileResultSet.next()) {
                    profile = profileResultSet.getString(1);
                    active = profileResultSet.getString(2);
                }
                sendToSession(session, createARecentChat(friendId, active, profile));
            }
        }
    }
    private JsonObject createARecentChat(String friendId, String active, String profile){
        return JsonProvider.provider().createObjectBuilder()
                .add("action","addRecentChatModel")
                .add("name",friendId)
                .add("active",active)
                .add("profile",profile)
                .build();
    }
    public void addChatList(String userId, String friendId, Session session) throws SQLException, ClassNotFoundException {
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
            String time = chatModelsResultSet.getString(3);
            String type = chatModelsResultSet.getString(5);
            sendToSession(session, createAddChatMessage(data, from, time, type));
        }
    }
    private JsonObject createAddChatMessage(String data, String from, String time, String type){
        return  JsonProvider.provider().createObjectBuilder()
                .add("action","viewChat")
                .add("data",data)
                .add("from",from)
                .add("time",time)
                .add("type",type)
                .build();

    }
    public void sendMessage(String userId, JsonObject jsonObject) throws SQLException, ClassNotFoundException {
        String friendId = jsonObject.getString("friendId");
        String message = jsonObject.getString("message");
        String time = jsonObject.getString("time");
        try {
            db.dml("INSERT INTO \"MESSAGES\" VALUES ('"+ userId +"','"+ friendId +"','"+time+"','" + message + "','chat');");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        JsonObject addMessage1 = createAddChatMessage(message,"you",time, "chat");
        sendToSession(sessions.get(userId),addMessage1);
        try {
            JsonObject addMessage2 = createAddChatMessage(message, "friend", time, "chat");
            sendToSession(sessions.get(friendId), addMessage2);
        }catch (NullPointerException e){
            System.out.println(friendId + "is Offline");
        }
    }
    public void sendImage(String userId, JsonObject jsonObject) throws SQLException, ClassNotFoundException {
        String friendId = jsonObject.getString("friendId");
        String time = jsonObject.getString("time");
        String data = jsonObject.getString("encoded");

        try {
            db.dml("INSERT INTO \"MESSAGES\" VALUES ('" + userId + "','" + friendId + "','" + time + "','" + data + "','image');");
        }catch (Exception e){
            System.out.println("Db error at the image sending");
        }
        try {
            sendToSession(sessions.get(friendId),
                    createAddChatMessage(
                            data,
                            "friend",
                            time,
                            "image"));
        }catch (NullPointerException e){
            System.out.println(friendId + " is Offline");
        }
        sendToSession(sessions.get(userId),
                createAddChatMessage(
                        data,
                        "you",
                        time,
                        "image"));
    }
    public void addFriendWindow(String userId, JsonObject jsonObject, Session session) throws SQLException, ClassNotFoundException {
        String friendId = jsonObject.getString("friendId");
        if (!friendId.equals("")){
            ResultSet addFriendResultSet = db.dql("SELECT \"ID\",\"PROFILEPIC\" FROM public.\"USERS\"");
        while (addFriendResultSet.next()) {
            if (addFriendResultSet.getString(1).equals(friendId)) {
                String profile = addFriendResultSet.getString(2);
                ResultSet friendListResultSet = db.dql("SELECT \"FRIENDS\" FROM public.\"USERS\" WHERE \"ID\"='" + userId + "' ");
                while (friendListResultSet.next()) {
                    Array friends;
                    String[] friendsArray = new String[0];
                    if (!friendListResultSet.getString(1).equals("{}")) {
                        friends = friendListResultSet.getArray(1);
                        friendsArray = (String[]) friends.getArray();
                    } else {
                        JsonObject addMessage = createFriendRequestWindowMessage(friendId, profile);
                        sendToSession(session, addMessage); // has no friend, fd reqing
                        return;
                    }
                    for (String friend : friendsArray) {
                        if (friendId.equals(friend)) {
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
        ResultSet invitationResultSet = db.dql("SELECT * FROM public.\"NOTIFICATION\" where \"RECIPIENT_ID\" = '" + friendId + "' and \"SENDER_ID\" ='" + userId + "';");
        System.out.println("from " + userId + " to " + friendId);
        while (invitationResultSet.next()) {
            System.out.println("inside the notification query");
            sendToSession(session, createAlreadyInvitedWindowMessage(friendId));
            return;
        }
        JsonObject addMessage = createInvitationWindowMessage(friendId);
        sendToSession(session, addMessage);
    }
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
    public void sendFriendRequest(String userId, String friendId, Session session) throws SQLException, ClassNotFoundException {
        try {
            db.dml("insert into public.\"NOTIFICATION\" values('"+ friendId +"','"+ userId +"','friendRequest',now(),FALSE);");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        JsonProvider provider = JsonProvider.provider();
        JsonObject addMessage1 = provider.createObjectBuilder()
                .add("action","newNotification")
                .add("from", userId)
                .build();
        try {
            sendToSession(sessions.get(friendId), addMessage1);
        }catch (Exception e){
            System.out.println(friendId + "is Offline");
        }
        sendToSession(session, createANotification(userId, friendId, "friendRequestSent", "", true ));

        try {
            sendToSession(sessions.get(friendId), createANotification(userId, friendId, "friendRequestReceived", "", true));
        }catch (Exception e){
            System.out.println(friendId + "is Offline");
        }
    }
    public void confirmFriendRequest(String userId, String friendId, Session session){
        String profile = "";
        String active = "";
        try {
            db.dml("UPDATE public.\"USERS\" set \"FRIENDS\" = array_append(\"FRIENDS\", '" + friendId + "') where \"ID\"='" + userId + "';");
            db.dml("UPDATE public.\"USERS\" set \"FRIENDS\" = array_append(\"FRIENDS\", '" + userId + "') where \"ID\"='" + friendId + "';");
            db.dml("UPDATE public.\"NOTIFICATION\" set \"ACTIVITY_TYPE\" = 'friends' where \"RECIPIENT_ID\"='"+ userId +"' and \"SENDER_ID\"='"+ friendId +"';");

            // this suspicious that all the notification where called again for no reason

            try {
                sendToSession(sessions.get(friendId), JsonProvider.provider().createObjectBuilder()
                        .add("action","removeNotification")
                        .add("friendId",userId)
                        .build());
                sendToSession(sessions.get(friendId), createANotification(friendId, userId, "friends", "", true));
            }catch (Exception e){
                System.out.println(friendId + "is Offline");
            }

            ResultSet profileResultSet1 = db.dql("SELECT \"PROFILEPIC\",\"active\" FROM public.\"USERS\" WHERE \"ID\"='"+ friendId +"';");
            while (profileResultSet1.next()) {
                profile = profileResultSet1.getString(1);
                active = profileResultSet1.getString(2);
            }
            sendToSession(session, createARecentChat(friendId,active, profile));

            ResultSet profileResultSet2 = db.dql("SELECT \"PROFILEPIC\",\"active\" FROM public.\"USERS\" WHERE \"ID\"='"+ userId +"';");
            while (profileResultSet2.next()) {
                profile = profileResultSet2.getString(1);
                active = profileResultSet2.getString(2);
            }
            try {
                sendToSession(sessions.get(friendId), createARecentChat(userId, active, profile));
            }catch (Exception e){
                System.out.println(friendId + "is Offline");
            }

        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }
    public void cancelOutGoingFriendRequest(String userId, JsonObject jsonObject) throws SQLException, ClassNotFoundException {
        String friendId = jsonObject.getString("friendId");
        db.dml("DELETE FROM public.\"NOTIFICATION\" WHERE \"SENDER_ID\"='"+userId+"' AND \"RECIPIENT_ID\"='"+friendId+"' AND \"ACTIVITY_TYPE\"='friendRequest';");
        JsonProvider provider = JsonProvider.provider();
        JsonObject addMessage = provider.createObjectBuilder()
                .add("action","removeNotification")
                .add("friendId",userId)
                .build();
        try{
            sendToSession(sessions.get(friendId), addMessage);
        }catch (Exception e){
            System.out.println(friendId + "is Offline");
        }
    }
    public void cancelIncomingFriendRequest(String userId, JsonObject jsonObject) throws SQLException, ClassNotFoundException {
        String friendId = jsonObject.getString("friendId");
        db.dml("DELETE FROM public.\"NOTIFICATION\" WHERE \"SENDER_ID\"='"+friendId+"' AND \"RECIPIENT_ID\"='"+userId+"' AND \"ACTIVITY_TYPE\"='friendRequest';");
        JsonProvider provider = JsonProvider.provider();
        JsonObject addMessage = provider.createObjectBuilder()
                .add("action","removeNotification")
                .add("friendId",userId)
                .build();
        try{
            sendToSession(sessions.get(friendId), addMessage);
        }catch (Exception e){
            System.out.println(friendId + "is Offile");
        }
    }
    public void invitationAccepted(String userId, String friendId) throws SQLException, ClassNotFoundException {
        String profile = "";
        String active = "";
        JsonProvider provider = JsonProvider.provider();
        JsonObject addMessage = provider.createObjectBuilder()
                .add("action","removeNotification")
                .add("friendId",friendId)
                .build();
        System.out.println(userId + "'s invitation is accepted by" + friendId);
        try {
            sendToSession(sessions.get(userId), addMessage);
        }catch (Exception e){
            System.out.println(userId + "is Offline ");
        }

        JsonObject addMessage2 = provider.createObjectBuilder()
                .add("action","newNotification")
                .build();
        try {
            sendToSession(sessions.get(userId), addMessage2);
            sendToSession(sessions.get(userId), createANotification(userId, friendId, "friends", "", true));
        }catch (Exception e){
            System.out.println(userId + "is Offline ");
        }

        ResultSet profileResultSet1 = db.dql("SELECT \"PROFILEPIC\",\"active\" FROM public.\"USERS\" WHERE \"ID\"='"+ friendId +"';");
        while (profileResultSet1.next()) {
            profile = profileResultSet1.getString(1);
            active = profileResultSet1.getString(2);
        }
        sendToSession(sessions.get(userId), createARecentChat(friendId,active, profile ));
    }
    public void notificationsViewed(String userId) throws SQLException, ClassNotFoundException {
        db.dml(" UPDATE public.\"NOTIFICATION\" SET \"IS_READ\"='true' WHERE (\"RECIPIENT_ID\"='"+userId+"' or \"SENDER_ID\"='"+userId+"') and \"IS_READ\"='false'");
    }
    public void addInvitedNotification(String userId, JsonObject jsonObject) throws SQLException, ClassNotFoundException {

        sendToSession(sessions.get(userId), createANotification(userId, jsonObject.getString("friendId"), "invitation", "", true));
    }
    public void pingAllFriends(String userId, String message) throws SQLException, ClassNotFoundException {
        ResultSet friendListResultSet = db.dql("SELECT \"FRIENDS\" FROM public.\"USERS\" WHERE \"ID\"='" + userId + "' ");
        while (friendListResultSet.next()) {
            Array friends;
            String[] friendsArray = new String[0];
            if (!friendListResultSet.getString(1).equals("{}")) {
                friends = friendListResultSet.getArray(1);
                friendsArray = (String[]) friends.getArray();
            }
            for (String friendId : friendsArray) {
                JsonProvider provider = JsonProvider.provider();
                JsonObject addMessage = provider.createObjectBuilder()
                        .add("action", message)
                        .add("friendId", userId)
                        .build();
                try {
                    System.out.println("Sending ping to " + friendId + " that "+ userId + " is " + message);
                    sendToSession(sessions.get(friendId), addMessage);
                }catch (Exception e){
                    System.out.println(friendId + " is Offline");
                }
            }
        }
    }
}
