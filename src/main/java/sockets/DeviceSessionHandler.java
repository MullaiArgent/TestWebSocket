package sockets;

import models.ChatModel;
import models.Device;
import models.RecentChatModel;

import javax.enterprise.context.ApplicationScoped;
import javax.json.JsonObject;
import javax.json.spi.JsonProvider;
import javax.websocket.Session;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


@ApplicationScoped
public class DeviceSessionHandler {

    public DeviceSessionHandler() {}

    private int deviceId = 0;
    private int chatModelId = 0;
    private int recentChatModelId = 0;
    private static final Set<Session> sessions = new HashSet<>();
    private static final Set<Device> devices = new HashSet<>();
    private static final Set<ChatModel> chatModels = new HashSet<>();
    private static final Set<RecentChatModel> recentChatModels = new HashSet<>();

    public void addSession(Session session){
        sessions.add(session);
        for (Device device : devices){
            JsonObject addMessage = createAddMessage(device);
            sendToSession(session, addMessage);
        }
    }

    public void removeSession(Session session){
        sessions.remove(session);
    }

    public String getDevices(){
        return new ArrayList<>(devices).toString();
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
        for (Session session : sessions){
            sendToSession(session, message);
        }

    }

    private void sendToSession(Session session, JsonObject message){
        try{
            session.getBasicRemote().sendText(message.toString());
        } catch (IOException e) {
            sessions.remove(session);
            Logger.getLogger(DeviceSessionHandler.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private void sendToSpecificSession(){
        //TODO send to the specific users
    }
}
