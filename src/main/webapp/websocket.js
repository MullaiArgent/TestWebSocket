window.onload = init;
const socket = new WebSocket("ws://localhost:8080/TestWebSocket_war/actions");
socket.onmessage = onMessage;
let globalFriendId = "";
function init(){

}
function printRecentChats(recentChat){

    const recentChatsUl = document.getElementById("recentChatsUl");
    const clearfix = document.createElement("li");
    clearfix.setAttribute("class","clearfix");
    clearfix.onclick = function () {
        viewChat(recentChat.name);
    }
    recentChatsUl.appendChild(clearfix);

    const avatar = document.createElement("img");
    avatar.setAttribute("src","" + recentChat.profile);
    avatar.setAttribute("alt",recentChat.name)
    clearfix.appendChild(avatar);

    const about = document.createElement("div");
    about.setAttribute("class","about");
    clearfix.appendChild(about);

    const name = document.createElement("div");
    name.setAttribute("class","name");
    name.innerHTML = recentChat.name;
    about.appendChild(name);

}
function printChat(chat){
    const chatHistory = document.getElementById("chatList")

    const chatIl = document.createElement("li");
    chatIl.setAttribute("class", "clearfix");
    chatHistory.appendChild(chatIl)

    if (chat.from === "you"){
        const messageData = document.createElement("div");
        messageData.setAttribute("class","message-data text-right");
        chatIl.appendChild(messageData);

        const messageTime = document.createElement("span");
        messageTime.setAttribute("class","message-data-time");
        messageTime.innerHTML = chat.time;
        messageData.appendChild(messageTime);

        const chatData = document.createElement("div");
        chatData.setAttribute("class","message other-message float-right")
        chatData.innerHTML = chat.data;
        chatIl.appendChild(chatData);

    }else{
        const messageData = document.createElement("div");
        messageData.setAttribute("class","message-data");
        chatIl.appendChild(messageData);

        const messageTime = document.createElement("span");
        messageTime.setAttribute("class","message-data-time");
        messageTime.innerHTML = chat.time;
        messageData.appendChild(messageTime);

        const chatData = document.createElement("div");
        chatData.setAttribute("class","message my-message")
        chatData.innerHTML = chat.data;
        chatIl.appendChild(chatData);
    }
}
function removeChat(){
    const chatList = document.getElementById("chatList")
    chatList.innerHTML = "";
}

function onMessage(event){
    const message = JSON.parse(event.data);
    if (message.action === "addRecentChatModel"){
        printRecentChats(message);
    }
    if (message.action === "viewChat"){
        printChat(message);
    }
    if (message.action === "notificationCount"){
        const Count = document.getElementById("badge");
        Count.innerHTML = message.notificationCount;
    }
    // All message from the DeviceWebSocketServer will be process here
}



function viewChat(friendId){
    globalFriendId = friendId;
    const ChatAction = {
        action: "viewChat",
        friendId: friendId
    };
    const ul = document.getElementById("chatList");
    if (document.contains(ul)){
        removeChat();
    }
    socket.send(JSON.stringify(ChatAction));
}

function sendMessage(){
    const message = document.getElementById("messageToSend").value;
    document.getElementById("messageToSend").value = "";
    const date = new Date();
    const ChatAction = {
        action: "sendMessage",
        friendId: globalFriendId,
        message: message + "",
        time : "" + date.getHours() + ":" + date.getMinutes()
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


