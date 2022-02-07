window.onload = init;
var socket = new WebSocket("ws://localhost:8080/TestWebSocket_war/actions");
socket.onmessage = onMessage;

function onMessage(event){
    var device = JSON.parse(event.data);
    if (device.action === "add"){
        printDeviceElement(device);
    }
    // All mesage from the DeviceWebSocketServer will be prrocess here
}

function init(){
    // TODO list of friends and the notification count
}

function toChat(){
    var ChatAction = {
        action : "toChat",
        friendId : ""
    };
    socket.send(JSON.stringify(ChatAction));
}

function sendMessage(){
    var ChatAction = {
        action : "sendMessage",
        friendId: "",
        message : ""
    };
    socket.send(JSON.stringify(ChatAction));
}

function viewNotification(){
    var ChatAction = {
        action : "viewNotification"
    };
    socket.send(JSON.stringify(ChatAction));
}

function friendRequest(){
    var ChatAction  = {
        action : "friendRequest",
        friendId : ""
    };                                   // TODO pop up the invitation or request
    socket.send(JSON.stringify(ChatAction));
}

function confirmFriendRequest(){
    var ChatAction = {
        action : "confirmFriendRequest",
        friendId : ""
    };
    socket.send(JSON.stringify(ChatAction));
}

function declineFriendRequest(){
    var ChatAction = {
        action : "declineFriendRequest",
        friendId : ""
    };
    socket.send(JSON.stringify(ChatAction));
}

