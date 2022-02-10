package models;

public class NotificationModel {
    private int id;
    private String sender;
    private String receiver;
    private String activityType;
    private boolean isSeen = false;
    private String time = "time";

    @Override
    public String toString() {
        return "NotificationModel{" +
                "sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", activityType='" + activityType + '\'' +
                ", isSeen=" + isSeen +
                ", time='" + time + '\'' +
                '}';
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public boolean isSeen() {
        return isSeen;
    }

    public void setSeen(boolean seen) {
        isSeen = seen;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}