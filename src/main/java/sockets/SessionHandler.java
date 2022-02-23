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
    public SessionHandler() throws SQLException, ClassNotFoundException {}
    private void sendToSession(Session session, JsonObject message) {
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
            System.out.println("Theres an Exception while sending a packet to the Session");
        }
    }
    private void initialNotificationList(Session session, String userId) {
        StringBuilder notificationQuery = new StringBuilder();
        notificationQuery.append("SELECT * from public.\"NOTIFICATION\" where \"RECIPIENT_ID\"='");
        notificationQuery.append(userId);
        notificationQuery.append("' or \"SENDER_ID\"='");
        notificationQuery.append(userId);
        notificationQuery.append("'");
        ResultSet notificationResultSet = null;
        try {
            notificationResultSet = db.dql(notificationQuery.toString());
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Theres an exception while querying the notification");
        }
        assert notificationResultSet != null;
        try {
            while (notificationResultSet.next()) {
                String sender;
                String receiver;
                String activityType;
                String time;
                boolean seen;
                receiver = notificationResultSet.getString(1);
                sender = notificationResultSet.getString(2);
                if (notificationResultSet.getString(3).equals("friendRequest")) {
                    if (userId.equals(notificationResultSet.getString(2))) {
                        activityType = "friendRequestSent";
                    } else {
                        activityType = "friendRequestReceived";
                    }
                } else if (notificationResultSet.getString(3).equals("friends")) {
                    activityType = notificationResultSet.getString(3);
                    if (userId.equals(notificationResultSet.getString(1))) {
                        receiver = notificationResultSet.getString(2);
                        sender = notificationResultSet.getString(2);
                    } else {
                        receiver = notificationResultSet.getString(1);
                        sender = notificationResultSet.getString(1);
                    }
                } else {
                    activityType = notificationResultSet.getString(3);
                }
                time = notificationResultSet.getString(4);
                seen = notificationResultSet.getBoolean(5);
                try {
                    sendToSession(session, createANotification(sender, receiver, activityType, time, seen));
                }catch (Exception e){
                    e.printStackTrace();
                    System.out.println("Theres an Exception at your End, Unable perform few Operations");
                }

            }
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Theres an Exception while Iterating the Notification ResultSet");
        }
        try {
            notificationResultSet.close();
        }catch (Exception e1){
            e1.printStackTrace();
            System.out.println("Theres an Exception while closing the Notification ResulSet \n Retrying...");
            try{
                notificationResultSet.close();
            }catch (Exception e2){
                e2.printStackTrace();
                System.out.println("ResultSet Failed to Close");
            }
        }
    }
    private String[] getAsArray(ResultSet friendListResultSet) throws SQLException {
        assert friendListResultSet != null;
        if (!friendListResultSet.next()) return null;
        Array friends;
        String[] friendsArray = new String[0];
        if (!friendListResultSet.getString(1).equals("{}")) {
            friends = friendListResultSet.getArray(1);
            friendsArray = (String[]) friends.getArray();
        }
        return friendsArray;
    }
    private void initialRecentChatList(Session session, String userId) {
        String profile = "";
        String active = "";
        StringBuilder friendsQuery = new StringBuilder();
        friendsQuery.append("SELECT \"FRIENDS\" FROM public.\"USERS\" WHERE \"ID\"='");
        friendsQuery.append(userId);
        friendsQuery.append("'");
        ResultSet friendListResultSet = null;
        try {
            friendListResultSet = db.dql(friendsQuery.toString());
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Exception while querying the ResultSet of the FriendList");
        }
        try {
            while (true) {
                assert friendListResultSet != null;
                String[] friendsArray = getAsArray(friendListResultSet);
                if (friendsArray == null){
                    break;
                }

                for (String friendId : friendsArray) {
                    String profileQuery = "SELECT \"PROFILEPIC\",\"active\" FROM public.\"USERS\" WHERE \"ID\"='" + friendId + "';";
                    ResultSet profileResultSet = db.dql(profileQuery);
                    while (profileResultSet.next()) {
                        profile = profileResultSet.getString(1);
                        active = profileResultSet.getString(2);

                    }
                    try {
                        profileResultSet.close();
                    }catch (Exception e){
                        e.printStackTrace();
                        System.out.println("Theres is problem in Closing the ResultSet \n Retrying...");
                        try{
                            profileResultSet.close();
                        }catch (Exception e1){
                            e.printStackTrace();
                            System.out.println("ResultSet Failed to Close");
                        }
                    }
                    try {
                        sendToSession(session, createARecentChat(friendId, active, profile));
                    }catch (Exception e){
                        e.printStackTrace();
                        System.out.println("Theres an Exception at your End, Unable perform few Operations");
                    }
                }
            }
        }catch (Exception e){
            System.out.println("Theres an Exception while iterating the FriendList ResultSet");
        }
        try {
            friendListResultSet.close();
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Theres an Exception while closing the ResultSet \n Retrying...");
            try{
                friendListResultSet.close();
            }catch (Exception e2){
                e2.printStackTrace();
                System.out.println("Connection Not Closed");
            }
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
    private JsonObject createAddNotificationCount(int notificationCount) {
        return JsonProvider
                .provider()
                .createObjectBuilder()
                .add("action", "notificationCount")
                .add("notificationCount",notificationCount)
                .build();
    }
    private JsonObject createARecentChat(String friendId, String active, String profile){
        return JsonProvider
                .provider()
                .createObjectBuilder()
                .add("action","addRecentChatModel")
                .add("name",friendId)
                .add("active",active)
                .add("profile",profile)
                .build();
    }
    private JsonObject createAddChatMessage(String data, String from, String time, String type){
        return  JsonProvider
                .provider()
                .createObjectBuilder()
                .add("action","viewChat")
                .add("data",data)
                .add("from",from)
                .add("time",time)
                .add("type",type)
                .build();

    }
    public JsonObject createAlreadyAFriendWindowMessage(String friendId, String profile){
        return JsonProvider
                .provider()
                .createObjectBuilder()
                .add("action","chatWindow")
                .add("friendId",friendId)
                .add("profile", profile)
                .build();
    }
    public JsonObject createFriendRequestWindowMessage(String friendId, String profile){
        return JsonProvider
                .provider()
                .createObjectBuilder()
                .add("action","addFriendWindow")
                .add("friendId",friendId)
                .add("profile",profile)
                .build();
    }
    public JsonObject createInvitationWindowMessage(String friendId){
        return JsonProvider
                .provider()
                .createObjectBuilder()
                .add("action","invitationWindow")
                .add("friendId",friendId)
                .build();
    }
    public JsonObject createAlreadyInvitedWindowMessage(String friendId){
        return JsonProvider
                .provider()
                .createObjectBuilder()
                .add("action","alreadyInvited")
                .add("friendId",friendId)
                .build();
    }
    private int getNotificationCount(String userId) {
        StringBuilder notificationQueryCount = new StringBuilder();
        notificationQueryCount.append("SELECT COUNT(*) FROM public.\"NOTIFICATION\" where (\"RECIPIENT_ID\"='");
        notificationQueryCount.append(userId);
        notificationQueryCount.append("' or \"SENDER_ID\"='");
        notificationQueryCount.append(userId);
        notificationQueryCount.append("') and \"IS_READ\"='false'");
        ResultSet notificationCountResultSet = null;
        try {
            notificationCountResultSet = db.dql(notificationQueryCount.toString());
        }catch (Exception e){
            e.printStackTrace();
            System.out.println();
        }
        int notificationCount = 0;
        try {
            while (true) {
                assert notificationCountResultSet != null;
                if (!notificationCountResultSet.next()) break;
                notificationCount = notificationCountResultSet.getInt(1);
            }
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Theres an Exception while Iterating the Notification Count ResultSet");
        }
        try {
            notificationCountResultSet.close();
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Theres an Exception while closing the ResultSet \n Retrying...");
            try{
                notificationCountResultSet.close();
            }catch (Exception e1){
                System.out.println("ResultSet FAiled to Close");
            }
        }
        return notificationCount;
    }
    public void addSession(Session session, String userId) {
        sessions.put(userId, session);
        try {
            sendToSession(session, JsonProvider
                    .provider()
                    .createObjectBuilder()
                    .add("action", "userId")
                    .add("userId", userId)
                    .build());
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Theres an Exception at your End, Unable perform few Operations");
        }

        try {
            String updateUser = "UPDATE public.\"USERS\" SET active='online' WHERE \"ID\"='" + userId + "';";
            db.dml(updateUser);
        } catch (SQLException | ClassNotFoundException e) {
            System.out.println("Exception while updating");
            e.printStackTrace();
        }
        try {
            pingAllFriends(userId, "backOnline");
        }catch (Exception e){
            System.out.println("There's is Exception while pinging all the users");
        }
        try{
            initialRecentChatList(session, userId);
        }catch (Exception e){
            System.out.println("Exception while initiating the ChatList");
            e.printStackTrace();
        }
        try {
            initialNotificationList(session,userId);
        }catch (Exception e){
            System.out.println("Exception ");
            e.printStackTrace();
        }
        try{
            JsonObject addMessage = createAddNotificationCount(getNotificationCount(userId));
            sendToSession(session, addMessage);
        }catch (Exception e){
            System.out.println("Exception while getting the Notification Count");
            e.printStackTrace();
        }
    }
    public void removeSession(Session session, String userId) {
        try {
            pingAllFriends(userId, "wentOffline");
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("There's an Exception while Pinging all the Friends that user has wentOffline");
        }
        StringBuilder updateToOffline = new StringBuilder();
        updateToOffline.append("UPDATE public.\"USERS\" SET active='offline' WHERE \"ID\"='");
        updateToOffline.append(userId);
        updateToOffline.append("';");
        try {
            db.dml(updateToOffline.toString());
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("There's an Exception while updating offline");
        }
        sessions.entrySet().removeIf(entry -> session.equals(entry.getValue()));
    }
    public void addChatList(String userId, String friendId, Session session) {
        StringBuilder messageQuery = new StringBuilder();
        messageQuery.append("SELECT * FROM \"MESSAGES\" WHERE ((\"FROM\"='");
        messageQuery.append(userId);
        messageQuery.append("' OR \"FROM\"='");
        messageQuery.append(friendId);
        messageQuery.append("') AND (\"TO\"='");
        messageQuery.append(friendId);
        messageQuery.append("' OR \"TO\"='");
        messageQuery.append(userId);
        messageQuery.append("'));");
        // SELECT * FROM public."MESSAGES" WHERE (("FROM"='BRUCE' OR "FROM"='ALICE') AND ("TO"='BRUCE' OR "TO"='ALICE'));
        ResultSet chatModelsResultSet = null;
        try{
            chatModelsResultSet = db.dql(messageQuery.toString()); // chat data
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Theres an Exception While Querying the ResultSet");
        }
        try {
            while (true) {
                assert chatModelsResultSet != null;
                if (!chatModelsResultSet.next()) break;
                String data = chatModelsResultSet.getString(4);
                String from;
                if (chatModelsResultSet.getString(1).equals(userId)) {
                    from = "you";
                } else {
                    from = "friend";
                }
                String time = chatModelsResultSet.getString(3);
                String type = chatModelsResultSet.getString(5);
                try {
                    sendToSession(session, createAddChatMessage(data, from, time, type));
                }catch (Exception e){
                    e.printStackTrace();
                    System.out.println("Theres an Exception at your End, Unable perform few Operations");
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("theres an Exception While Iterating through ChatResultSet");
        }
        try {
            chatModelsResultSet.close();
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Theres an Exception while closing the ChatResultSet \n Retrying...");
            try{
                chatModelsResultSet.close();
            }catch (Exception e1){
                e1.printStackTrace();
                System.out.println("ChatResultSet Failed to Close");
            }
        }
    }
    public void sendMessage(String userId, JsonObject jsonObject) {
        String friendId = jsonObject.getString("friendId");
        String message = jsonObject.getString("message");
        String time = jsonObject.getString("time");
        try {
            StringBuilder pushAMessage = new StringBuilder()
                    .append("INSERT INTO \"MESSAGES\" VALUES ('")
                    .append(userId)
                    .append("','")
                    .append(friendId)
                    .append("','")
                    .append("now()")
                    .append("','")
                    .append(message)
                    .append("','chat');");
            db.dml(pushAMessage.toString());
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        sendToSession(sessions.get(userId),createAddChatMessage(message,"you",time, "chat"));
        try {
            sendToSession(sessions.get(friendId), createAddChatMessage(message, "friend", time, "chat"));
        }catch (NullPointerException e){
            System.out.println(friendId + "is Offline");
        }
    }
    public void sendImage(String userId, JsonObject jsonObject) {
        String friendId = jsonObject.getString("friendId");
        String time = jsonObject.getString("time");
        String data = jsonObject.getString("encoded");

        try {
            StringBuilder insertImageMessage = new StringBuilder()
                    .append("INSERT INTO \"MESSAGES\" VALUES ('")
                    .append(userId)
                    .append("','")
                    .append(friendId)
                    .append( "','")
                    .append(time)
                    .append("','")
                    .append(data)
                    .append("','image');");
            db.dml(insertImageMessage.toString());
        }catch (Exception e){
            System.out.println("Db error at the image sending");
        }
        try {
            sendToSession(sessions.get(friendId), createAddChatMessage(data, "friend", time, "image"));
        }catch (NullPointerException e){
            System.out.println(friendId + " is Offline");
        }
        sendToSession(sessions.get(userId),createAddChatMessage(data, "you", time, "image"));
    }
    public void addFriendWindow(String userId, JsonObject jsonObject, Session session) {
        String friendId = jsonObject.getString("friendId");
        StringBuilder friendsQuery = new StringBuilder();
        friendsQuery.append("SELECT \"FRIENDS\" FROM public.\"USERS\" WHERE \"ID\"='");
        friendsQuery.append(userId);
        friendsQuery.append("'");
        if (friendId.length() != 0){
            ResultSet addFriendResultSet = null;
            try{
                addFriendResultSet = db.dql("SELECT \"ID\",\"PROFILEPIC\" FROM public.\"USERS\"");
            }catch (Exception e){
                e.printStackTrace();
                System.out.println("Theres an Exception while querying the Friend Data");
            }
            try {
                while (true) {
                    assert addFriendResultSet != null;
                    if (!addFriendResultSet.next()) break;
                    if (addFriendResultSet.getString(1).equals(friendId)) {
                        String profile = addFriendResultSet.getString(2);
                        ResultSet friendListResultSet = db.dql(friendsQuery.toString());
                        while (friendListResultSet.next()) {
                            Array friends;
                            String[] friendsArray;
                            if (!friendListResultSet.getString(1).equals("{}")) {
                                friends = friendListResultSet.getArray(1);
                                friendsArray = (String[]) friends.getArray();

                            } else {
                                JsonObject addMessage = createFriendRequestWindowMessage(friendId, profile);
                                try {
                                    sendToSession(session, addMessage); // has no friend.
                                }catch (Exception e){
                                    e.printStackTrace();
                                    System.out.println("Theres an Exception at your End, Unable perform few Operations");
                                }
                                return;
                            }
                            for (String friend : friendsArray) {
                                if (friendId.equals(friend)) {
                                    // are fds, chat window
                                    JsonObject addMessage = createAlreadyAFriendWindowMessage(friendId, profile);
                                    try {
                                        sendToSession(session, addMessage);
                                    }catch (Exception e){
                                        e.printStackTrace();
                                        System.out.println("Theres an Exception at your End, Unable perform few Operations");
                                    }
                                    return;
                                }
                            }
                        }
                        friendListResultSet.close();
                        JsonObject addMessage = createFriendRequestWindowMessage(friendId, profile);
                        try {
                            sendToSession(session, addMessage); // not a friend, but a user so sends a req
                        }catch (Exception e){
                            e.printStackTrace();
                            System.out.println("Theres an Exception at your End, Unable perform few Operations");
                        }
                        return;
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
                System.out.println("Theres an Exception while Iterating through Friend Data ResultSet");
            }
            try {
                addFriendResultSet.close();
            }catch (Exception e){
                e.printStackTrace();
                System.out.println("Theres an Exception while Closing the Friend Data ResultSet \n Retrying...");
                try{
                    addFriendResultSet.close();
                }catch (Exception e1){
                    System.out.println("FriendList ResultSet Failed to Close");
                }
            }
            // user does not exist, send Invitation
            StringBuilder queryNotification = new StringBuilder();
            queryNotification.append("SELECT * FROM public.\"NOTIFICATION\" where \"RECIPIENT_ID\" = '");
            queryNotification.append(friendId);
            queryNotification.append("' and \"SENDER_ID\" ='");
            queryNotification.append(userId);
            queryNotification.append("';");
            ResultSet invitationResultSet = null;
            try{
                invitationResultSet = db.dql(queryNotification.toString());
            }catch (Exception e){
                e.printStackTrace();
                System.out.println("theres an Exception while querying the ResultSet");
            }
            try {
                sendToSession(session, createAlreadyInvitedWindowMessage(friendId));
                return;
            }catch (Exception e){
                e.printStackTrace();
                System.out.println("Theres an Exception at your End, Unable perform few Operations");
            }
            try {
                assert invitationResultSet != null;
                invitationResultSet.close();
            }catch (Exception e1){
                e1.printStackTrace();
                System.out.println("There Exception while Closing the ResultSet \n Retrying...");
                try{
                    invitationResultSet.close();
                }catch (Exception e2){
                    e2.printStackTrace();
                    System.out.println("ResultSet Failed to Close");
                }
            }
            JsonObject addMessage = createInvitationWindowMessage(friendId);
            try {
                sendToSession(session, addMessage);
            }catch (Exception e){
                e.printStackTrace();
                System.out.println("Theres an Exception at your End, Unable perform few Operations");
            }
        }else {
            System.out.println("Empty String has been sent in the packet");
        }
    }
    public void sendFriendRequest(String userId, String friendId, Session session)  {
        StringBuilder insertNotification = new StringBuilder();
        insertNotification.append("insert into public.\"NOTIFICATION\" values('");
        insertNotification.append(friendId);
        insertNotification.append("','");
        insertNotification.append(userId);
        insertNotification.append("','friendRequest',now(),FALSE);");
        try {
            db.dml(insertNotification.toString());
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Theres an Exception while Manipulating the DB Data");
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
        try {
            sendToSession(session, createANotification(userId, friendId, "friendRequestSent", "", true));
        }catch (Exception e){
            System.out.println("Theres an Exception at your End, Unable perform few Operations");
        }

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
            StringBuilder updateFriendForUser = new StringBuilder();
            updateFriendForUser.append("UPDATE public.\"USERS\" set \"FRIENDS\" = array_append(\"FRIENDS\", '");
            updateFriendForUser.append(friendId);
            updateFriendForUser.append("') where \"ID\"='");
            updateFriendForUser.append(userId);
            updateFriendForUser.append("';");
            db.dml(updateFriendForUser.toString());

            StringBuilder updateFriendForFriend = new StringBuilder();
            updateFriendForFriend.append("UPDATE public.\"USERS\" set \"FRIENDS\" = array_append(\"FRIENDS\", '");
            updateFriendForFriend.append(userId);
            updateFriendForFriend.append( "') where \"ID\"='");
            updateFriendForFriend.append(friendId);
            updateFriendForFriend.append("';");
            db.dml(updateFriendForFriend.toString());

            StringBuilder updateNotification = new StringBuilder();
            updateNotification.append("UPDATE public.\"NOTIFICATION\" set \"ACTIVITY_TYPE\" = 'friends' where \"RECIPIENT_ID\"='");
            updateNotification.append(userId);
            updateNotification.append("' and \"SENDER_ID\"='");
            updateNotification.append(friendId);
            updateNotification.append("';");
            db.dml(updateNotification.toString());

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
            StringBuilder queryFriendProfile = new StringBuilder();
            queryFriendProfile.append("SELECT \"PROFILEPIC\",\"active\" FROM public.\"USERS\" WHERE \"ID\"='");
            queryFriendProfile.append(friendId);
            queryFriendProfile.append("';");
            ResultSet profileResultSet1 = db.dql(queryFriendProfile.toString());
            while (profileResultSet1.next()) {
                profile = profileResultSet1.getString(1);
                active = profileResultSet1.getString(2);
            }
            try {
                profileResultSet1.close();
            }catch (Exception e){
                e.printStackTrace();
                System.out.println("Theres an Exception While Closing ResultSet");
                try{
                    profileResultSet1.close();
                }catch (Exception e1){
                    e1.printStackTrace();
                    System.out.println("Failed to Close the ResultSet");
                }
            }
            try {
                sendToSession(session, createARecentChat(friendId, active, profile));
            }catch (Exception e){
                e.printStackTrace();
                System.out.println("Theres an Exception at your End, Unable perform few Operations");
            }
            StringBuilder queryUserProfile = new StringBuilder();
            queryUserProfile.append("SELECT \"PROFILEPIC\",\"active\" FROM public.\"USERS\" WHERE \"ID\"='");
            queryUserProfile.append(userId);
            queryUserProfile.append("';");
            ResultSet profileResultSet2 = db.dql(queryUserProfile.toString());
            while (profileResultSet2.next()) {
                profile = profileResultSet2.getString(1);
                active = profileResultSet2.getString(2);
            }
            try {
                profileResultSet2.close();
            }catch (Exception e){
                e.printStackTrace();
                System.out.println("Theres an Exception while Closing the ResultSet \n Retrying...");
                try{
                    profileResultSet2.close();
                }catch (Exception e1){
                    e1.printStackTrace();
                    System.out.println("Failed to Close the ResultSet");
                }
            }
            try {
                sendToSession(sessions.get(friendId), createARecentChat(userId, active, profile));
            }catch (Exception e){
                System.out.println(friendId + "is Offline");
            }

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    public void cancelOutGoingFriendRequest(String userId, JsonObject jsonObject) {
        String friendId = jsonObject.getString("friendId");

        StringBuilder deleteNotification = new StringBuilder();
        deleteNotification.append("DELETE FROM public.\"NOTIFICATION\" WHERE \"SENDER_ID\"='");
        deleteNotification.append(userId);
        deleteNotification.append("' AND \"RECIPIENT_ID\"='");
        deleteNotification.append(friendId);
        deleteNotification.append("' AND \"ACTIVITY_TYPE\"='friendRequest';");
        try {
            db.dml(deleteNotification.toString());
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Theres an Exception while Deleting the Data");
        }
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
    public void cancelIncomingFriendRequest(String userId, JsonObject jsonObject)  {
        String friendId = jsonObject.getString("friendId");
        StringBuilder deleteNotification = new StringBuilder();
        deleteNotification.append("DELETE FROM public.\"NOTIFICATION\" WHERE \"SENDER_ID\"='");
        deleteNotification.append(friendId);
        deleteNotification.append("' AND \"RECIPIENT_ID\"='");
        deleteNotification.append(userId);
        deleteNotification.append("' AND \"ACTIVITY_TYPE\"='friendRequest';");
        try {
            db.dml(deleteNotification.toString());
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Theres an Exception while Deleting the Data");
        }
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
    public void invitationAccepted(String userId, String friendId)  {
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
        StringBuilder queryProfile = new StringBuilder();
        queryProfile.append("SELECT \"PROFILEPIC\",\"active\" FROM public.\"USERS\" WHERE \"ID\"='");
        queryProfile.append(friendId);
        queryProfile.append("';");
        ResultSet profileResultSet1 = null;
        try{
            profileResultSet1 = db.dql(queryProfile.toString());
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Theres an Exception while Querying the Data");
        }
        try {
            while (true) {
                assert profileResultSet1 != null;
                if (!profileResultSet1.next()) break;
                profile = profileResultSet1.getString(1);
                active = profileResultSet1.getString(2);
            }
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Theres an Exception while Iterating through the ResultSet");
        }
        try {
            profileResultSet1.close();
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Theres a trouble Closing the ResultSet \n Retrying...");
            try{
                profileResultSet1.close();
            }catch (Exception e1){
                e1.printStackTrace();
                System.out.println("Failed to Close the ResultSet");
            }
        }
        try {
            sendToSession(sessions.get(userId), createARecentChat(friendId, active, profile));
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Theres an Exception at your End, Unable perform few Operations");
        }
    }
    public void notificationsViewed(String userId)  {
        StringBuilder notificationViewedUpdate = new StringBuilder();
        notificationViewedUpdate.append("UPDATE public.\"NOTIFICATION\" SET \"IS_READ\"='true' WHERE (\"RECIPIENT_ID\"='");
        notificationViewedUpdate.append(userId);
        notificationViewedUpdate.append("' or \"SENDER_ID\"='");
        notificationViewedUpdate.append(userId);
        notificationViewedUpdate.append("') and \"IS_READ\"='false'");
        try {
            db.dml(notificationViewedUpdate.toString());
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Theres an Exception while Manipulating the Data");
        }
    }
    public void addInvitedNotification(String userId, JsonObject jsonObject)  {
        try {
            sendToSession(sessions.get(userId),
                    createANotification(userId,
                            jsonObject.getString("friendId"),
                            "invitation",
                            "",
                            true));
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Theres an Exception at your End, Unable perform few Operations");
        }
    }
    public void pingAllFriends(String userId, String message)  {
        StringBuilder friendList = new StringBuilder();
        friendList.append("SELECT \"FRIENDS\" FROM public.\"USERS\" WHERE \"ID\"='");
        friendList.append(userId);
        friendList.append("'");
        ResultSet friendListResultSet = null;
        try{
            friendListResultSet = db.dql(friendList.toString());
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Theres an Exception while Querying the Data");
        }
        try {
            while (true) {
                assert friendListResultSet != null;
                if (!friendListResultSet.next()) break;
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
                        sendToSession(sessions.get(friendId), addMessage);
                    } catch (Exception e) {
                        System.out.println(friendId + " is Offline");
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Theres an Exception while Iterating the ResultSet");
        }
        try {
            friendListResultSet.close();
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Theres an Exception while closing the ResultSet \n Retrying...");
            try{
                friendListResultSet.close();
            }catch (Exception e1){
                e1.printStackTrace();
                System.out.println("Failed to Close The ResultSet");
            }
        }
    }
    public void closeTheDBConnection() {
        try {
            db.close();
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Unable to Call the JDBC's Close Method ");
        }
    }
}