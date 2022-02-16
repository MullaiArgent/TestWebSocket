package sockets;

import datamanagement.JDBC;
import models.ChatModel;
import models.NotificationModel;
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
public class SessionHandler {

    JDBC db = new JDBC();
    private int recentChatModelId = 0;
    private int notificationModelId = 0;

    private static final HashMap<String, Session> sessions = new HashMap<>();

    public void addSession(Session session, String userId) throws SQLException, ClassNotFoundException {
        sessions.put(userId, session);
        System.out.println("Session add to Hash for "+ userId);

        db.dml("UPDATE public.\"USERS\" SET active='online' WHERE \"ID\"='"+userId+"';");
        pingAllFriends(userId, "backOnline");

        final List<RecentChatModel> recentChatModels = createRecentChatModels(userId);
        for (RecentChatModel recentChatModel : recentChatModels){
            JsonObject addMessage = createAddRecentChatMessage(recentChatModel);
            sendToSession(session, addMessage);
        }
        final List<NotificationModel> notificationModels = createNotificationModels(userId);
        for (NotificationModel notificationModel : notificationModels) {
            JsonObject addMessage = createAddNotificationMessage(notificationModel);
            sendToSession(session, addMessage);
        }
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



    private List<NotificationModel> createNotificationModels(String userId) throws SQLException, ClassNotFoundException {
        ResultSet notificationResultSet = db.dql("SELECT * from public.\"NOTIFICATION\" where \"RECIPIENT_ID\"='"+userId+"' or \"SENDER_ID\"='"+userId+"'");
        List<NotificationModel> notificationModels = new ArrayList<>();
        while (notificationResultSet.next()){
            NotificationModel notificationModel = new NotificationModel();
            notificationModel.setId(notificationModelId); notificationModelId++;
            notificationModel.setReceiver(notificationResultSet.getString(1));
            notificationModel.setSender(notificationResultSet.getString(2));
            if (notificationResultSet.getString(3).equals("friendRequest")) {
                if (userId.equals(notificationResultSet.getString(2))) {
                    notificationModel.setActivityType("friendRequestSent");
                } else {
                    notificationModel.setActivityType("friendRequestReceived");
                }
            } else if (notificationResultSet.getString(3).equals("friends")){
                notificationModel.setActivityType(notificationResultSet.getString(3));
                if (userId.equals(notificationResultSet.getString(1))) {
                    notificationModel.setReceiver(notificationResultSet.getString(2));
                    notificationModel.setSender(notificationResultSet.getString(2));
                }else {
                    notificationModel.setReceiver(notificationResultSet.getString(1));
                    notificationModel.setSender(notificationResultSet.getString(1));
                }
            }
            else {
                notificationModel.setActivityType(notificationResultSet.getString(3));
            }
            notificationModel.setTime(notificationResultSet.getString(4));
            notificationModel.setSeen(notificationResultSet.getBoolean(5));
            notificationModels.add(notificationModel);
        }
        return notificationModels;
    }
    private JsonObject createAddNotificationMessage(NotificationModel notificationModel){
        JsonProvider provider = JsonProvider.provider();
        JsonObject addMessage = provider.createObjectBuilder()
                .add("action","addNotification")
                .add("id",notificationModel.getId())
                .add("sender",notificationModel.getSender())
                .add("receiver",notificationModel.getReceiver())
                .add("activity",notificationModel.getActivityType())
                .add("time",notificationModel.getTime())
                .add("seen",notificationModel.isSeen())
                .build();
        return addMessage;
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
                ResultSet profileResultSet = db.dql("SELECT \"PROFILEPIC\",\"active\" FROM public.\"USERS\" WHERE \"ID\"='"+ friendId +"';");
                while (profileResultSet.next()) {
                    recentChatModel.setProfile(profileResultSet.getString(1));
                    recentChatModel.setActive(profileResultSet.getString(2));
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
                .add("active",recentChatModel.getActive())
                .add("profile",recentChatModel.getProfile() + "")
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
    private ChatModel createChatModel(String data, String from, String time, String type){
        ChatModel chatModel = new ChatModel();
        chatModel.setData(data);
        chatModel.setFrom(from);
        chatModel.setTime(time);
        chatModel.setType(type);
        return chatModel;
    }
    private List<ChatModel> createChatModels(String userId, String friendId) throws SQLException, ClassNotFoundException {
        List<ChatModel> chatModels = new ArrayList<>();
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
            chatModels.add(createChatModel(data, from, time, type));
        }
        System.out.println();
        return chatModels;
    }
    private JsonObject createAddChatMessage(ChatModel chatModel){
        return  JsonProvider.provider().createObjectBuilder()
                .add("action","viewChat")
                .add("data",chatModel.getData())
                .add("from",chatModel.getFrom())
                .add("time",chatModel.getTime())
                .add("type",chatModel.getType())
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
        JsonObject addMessage1 = createAddChatMessage(createChatModel(message,"you",time, "chat"));
        sendToSession(sessions.get(userId),addMessage1);
        try {
            JsonObject addMessage2 = createAddChatMessage(createChatModel(message, "friend", time, "chat"));
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
                    createAddChatMessage(createChatModel(
                            data,
                            "friend",
                            time,
                            "image")));
        }catch (NullPointerException e){
            System.out.println(friendId + " is Offline");
        }

        sendToSession(sessions.get(userId),
                createAddChatMessage(createChatModel(
                        data,
                        "you",
                        time,
                        "image")));
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

        NotificationModel notificationModel1 = new NotificationModel();
        notificationModel1.setId(notificationModelId); notificationModelId++;
        notificationModel1.setReceiver(friendId);
        notificationModel1.setActivityType("friendRequestSent");
        notificationModel1.setSender(userId);
        JsonObject addMessage3 = createAddNotificationMessage(notificationModel1);
        sendToSession(session, addMessage3);

        NotificationModel notificationModel2 = new NotificationModel();
        notificationModel2.setReceiver(friendId);
        notificationModel2.setActivityType("friendRequestReceived");
        notificationModel2.setSender(userId);
        JsonObject addMessage4 = createAddNotificationMessage(notificationModel2);
        try {
            sendToSession(sessions.get(friendId), addMessage4);
        }catch (Exception e){
            System.out.println(friendId + "is Offline");
        }
    }
    public void confirmFriendRequest(String userId, String friendId, Session session){
        try {
            db.dml("UPDATE public.\"USERS\" set \"FRIENDS\" = array_append(\"FRIENDS\", '" + friendId + "') where \"ID\"='" + userId + "';");
            db.dml("UPDATE public.\"USERS\" set \"FRIENDS\" = array_append(\"FRIENDS\", '" + userId + "') where \"ID\"='" + friendId + "';");
            db.dml("UPDATE public.\"NOTIFICATION\" set \"ACTIVITY_TYPE\" = 'friends' where \"RECIPIENT_ID\"='"+ userId +"' and \"SENDER_ID\"='"+ friendId +"';");
            final List<NotificationModel> userNotificationModels1 = createNotificationModels(userId);
            for (NotificationModel notificationModel : userNotificationModels1) {
                JsonObject addMessage1 = createAddNotificationMessage(notificationModel);
                sendToSession(session, addMessage1);
            }

            JsonProvider provider = JsonProvider.provider();
            JsonObject addMessage3 = provider.createObjectBuilder()
                    .add("action","removeNotification")
                    .add("friendId",userId)
                    .build();

            try {
                sendToSession(sessions.get(friendId), addMessage3);
            }catch (Exception e){
                System.out.println(friendId + "is Offline");
            }

            NotificationModel notificationModel = new NotificationModel();
            notificationModel.setReceiver(userId);
            notificationModel.setActivityType("friends");
            notificationModel.setSender(friendId);
            JsonObject addMessage2 = createAddNotificationMessage(notificationModel);
            try {
                sendToSession(sessions.get(friendId), addMessage3);
                sendToSession(sessions.get(friendId), addMessage2);
            }catch (Exception e){
                System.out.println(friendId + "is Offline");
            }

            RecentChatModel recentChatModel1 = new RecentChatModel();
            recentChatModel1.setName(friendId);
            ResultSet profileResultSet1 = db.dql("SELECT \"PROFILEPIC\",\"active\" FROM public.\"USERS\" WHERE \"ID\"='"+ friendId +"';");
            while (profileResultSet1.next()) {
                recentChatModel1.setProfile(profileResultSet1.getString(1));
                recentChatModel1.setActive(profileResultSet1.getString(2));
            }
            sendToSession(session, createAddRecentChatMessage(recentChatModel1));

            RecentChatModel recentChatModel2 = new RecentChatModel();
            recentChatModel2.setName(userId);
            ResultSet profileResultSet2 = db.dql("SELECT \"PROFILEPIC\",\"active\" FROM public.\"USERS\" WHERE \"ID\"='"+ userId +"';");
            while (profileResultSet2.next()) {
                recentChatModel2.setProfile(profileResultSet2.getString(1));
                recentChatModel2.setActive(profileResultSet2.getString(2));
            }
            try {
                sendToSession(sessions.get(friendId), createAddRecentChatMessage(recentChatModel2));
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
        }catch (Exception e){
            System.out.println(userId + "is Offline ");
        }

        NotificationModel notificationModel = new NotificationModel();
        notificationModel.setReceiver(friendId);
        notificationModel.setActivityType("friends");
        notificationModel.setSender(userId);
        JsonObject addMessage1 = createAddNotificationMessage(notificationModel);
        try {
            sendToSession(sessions.get(userId), addMessage1);
        }catch (Exception e){
            System.out.println(userId + "is Offline ");
        }

        RecentChatModel recentChatModel1 = new RecentChatModel();
        recentChatModel1.setName(friendId);
        ResultSet profileResultSet1 = db.dql("SELECT \"PROFILEPIC\" FROM public.\"USERS\" WHERE \"ID\"='"+ friendId +"';");
        while (profileResultSet1.next()) {
            recentChatModel1.setProfile(profileResultSet1.getString(1));
        }
        sendToSession(sessions.get(userId), createAddRecentChatMessage(recentChatModel1));

    }
    public void notificationsViewed(String userId) throws SQLException, ClassNotFoundException {
        db.dml(" UPDATE public.\"NOTIFICATION\" SET \"IS_READ\"='true' WHERE (\"RECIPIENT_ID\"='"+userId+"' or \"SENDER_ID\"='"+userId+"') and \"IS_READ\"='false'");
    }
    public void addInvitedNotification(String userId, JsonObject jsonObject) throws SQLException, ClassNotFoundException {
        NotificationModel notificationModel1 = new NotificationModel();
        notificationModel1.setId(notificationModelId); notificationModelId++;
        notificationModel1.setReceiver(jsonObject.getString("friendId"));
        notificationModel1.setActivityType("invitation");
        notificationModel1.setSender(userId);
        JsonObject addMessage3 = createAddNotificationMessage(notificationModel1);
        sendToSession(sessions.get(userId), addMessage3);
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
