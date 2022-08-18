window.onload = init;
const socket = new WebSocket("ws://localhost:8080/TestWebSocket_war/actions");
socket.onmessage = onMessage;
let globalFriendId;
let globalUserId;

function init(){

}
function onMessage(event) {
    const message = JSON.parse(event.data);
    if (message.action === "userId"){
        document.getElementById("title").innerHTML = message.userId;
        globalUserId = message.userId;

    }
    if (message.action === "addRecentChatModel") {
        printRecentChats(message);
    }
    if (message.action === "viewChat") {
        if (message.type === "chat") {
            printChat(message);
        }
        else if (message.type === "image"){
            printImage(message);
        }}
    if (message.action === "notificationCount") {
        document.getElementById("badge").innerHTML = message.notificationCount;
    }
    if (message.action === "addFriendWindow") {
        const windowType = document.getElementById("forProfile");

        const profile = document.createElement("img");
        profile.setAttribute("src", message.profile);
        profile.setAttribute("height", "100");
        profile.setAttribute("width", "100");

        windowType.appendChild(profile);

        const popWindowType = document.getElementById("popWindowType");
        popWindowType.innerHTML = "Send Friend Request to " + message.friendId;

        const popWindowButton = document.getElementById("popWindowButton");
        popWindowButton.innerHTML = "Add Friend"
        popWindowButton.onclick = function () {
            sendFriendRequest(message.friendId);
            overlayOff();
        }
        overlayOn();
    }
    if (message.action === "invitationWindow") {
        const windowType = document.getElementById("forProfile");

        const profile = document.createElement("img");
        profile.setAttribute("src", "https://tinyurl.com/2p8hr77v");
        profile.setAttribute("height", "100");
        profile.setAttribute("width", "100");

        windowType.appendChild(profile);

        const popWindowType = document.getElementById("popWindowType");
        popWindowType.innerHTML = "Send Invitation to " + message.friendId;

        const popWindowButton = document.getElementById("popWindowButton");
        popWindowButton.innerHTML = "Invite"
        popWindowButton.onclick = function () {
            sendInvitation(message.friendId);
            overlayOff();
        }
        overlayOn();
    }
    if (message.action === "chatWindow") {
        const windowType = document.getElementById("forProfile");

        const profile = document.createElement("img");
        profile.setAttribute("src", message.profile);
        profile.setAttribute("height", "100");
        profile.setAttribute("width", "100");

        windowType.appendChild(profile);

        const popWindowType = document.getElementById("popWindowType");
        popWindowType.innerHTML = message.friendId + "is Already a Friend";

        const popWindowButton = document.getElementById("popWindowButton");
        popWindowButton.innerHTML = "Chat"
        popWindowButton.onclick = function () {
            viewChat(message.friendId);
            overlayOff();
        }
        overlayOn();
    }
    if (message.action === "alreadyInvited") {
        const windowType = document.getElementById("forProfile");

        const profile = document.createElement("img");
        profile.setAttribute("src", "https://tinyurl.com/35zst7ft");
        profile.setAttribute("height", "100");
        profile.setAttribute("width", "100");

        windowType.appendChild(profile);

        const popWindowType = document.getElementById("popWindowType");
        popWindowType.innerHTML = "Already Invited "  + message.friendId;

        const popWindowButton = document.getElementById("popWindowButton");
        popWindowButton.innerHTML = "Close"
        popWindowButton.onclick = function () {
            overlayOff();
        }
        overlayOn();
    }
    if (message.action === "newNotification") {
        notify()
        console.log("comes here")
        const badge = document.getElementById("badge");
        badge.innerHTML = parseInt(badge.innerHTML) + 1;
    }
    if (message.action === "addNotification") {
        printNotification(message);
    }
    if (message.action === "removeNotification"){
        removeNotification(message);
    }
    if (message.action === "backOnline"){
        document.getElementById("status"+message.friendId).innerHTML = "<i class=\"fa fa-circle online\"></i>&nbsp" + "online";
        const aboutStatus = document.getElementById("aboutStatus"+message.friendId)
        if (aboutStatus !== null) {
            aboutStatus.innerHTML = "<i class=\"fa fa-circle online\"></i>&nbsp" + "online";
        }
    }
    if (message.action === "wentOffline"){
        document.getElementById("status"+message.friendId).innerHTML = "<i class=\"fa fa-circle offline\"></i>&nbsp" + "offline";
        const aboutStatus = document.getElementById("aboutStatus"+message.friendId);
        if (aboutStatus !== null) {
            aboutStatus.innerHTML = "<i class=\"fa fa-circle offline\"></i>&nbsp" + "offline";
        }
    }
    if (message.action === "isTyping"){
        document.getElementById("status"+message.friendId).innerHTML = "<div class=\"typing\">Typing...</div>";
        const aboutStatus = document.getElementById("aboutStatus"+message.friendId);
        if (aboutStatus !== null) {
            aboutStatus.innerHTML = "<div class=\"typing\">Typing...</div>";
        }
    }
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
    avatar.setAttribute("id","avatar"+recentChat.name);
    avatar.setAttribute("alt",recentChat.name)
    clearfix.appendChild(avatar);

    const about = document.createElement("div");
    about.setAttribute("class","about");
    clearfix.appendChild(about);

    const name = document.createElement("div");
    name.setAttribute("class","name");
    name.innerHTML = "&nbsp&nbsp"+recentChat.name;
    about.appendChild(name);

    const status = document.createElement("div");
    status.setAttribute("id","status"+recentChat.name);
    status.setAttribute("class","status");
    about.appendChild(status);
    if (recentChat.active === "online") {
        status.innerHTML = "<i class=\"fa fa-circle online\"></i>&nbsp" + recentChat.active;
    }else {
        status.innerHTML = "<i class=\"fa fa-circle offline\"></i>&nbsp" + recentChat.active;
    }
}
function printChat(chat){
    const chatHistory = document.getElementById("chatList")
    chatHistory.scrollTop = chatHistory.scrollHeight;

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
function printImage(chat) {
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
        chatData.innerHTML = "<img src='" + chat.data  + "' alt=''/>";
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
        chatData.innerHTML = "<img src='" + chat.data  + "' alt=''/>";
        chatIl.appendChild(chatData);
    }
}
function printNotification(message) {
    const n = document.getElementById("notificationDrop");
    const notificationList = document.createElement("div");
    notificationList.setAttribute("class", "noty-manager-list-item noty-manager-list-item-error");
    n.appendChild(notificationList)
    const activityItem = document.createElement("div");
    activityItem.setAttribute("class", "activity-item");
    notificationList.setAttribute("id", message.receiver);
    notificationList.appendChild(activityItem);
    const activity = document.createElement("div");
    activity.setAttribute("class", "activity");
    if (message.activity === "friendRequestSent") {
        activity.innerHTML = "Friend Request Sent to ";
        activityItem.appendChild(activity);
        const a = document.createElement("a");
        a.innerHTML = message.receiver;
        activity.appendChild(a);

        const cancelRequest = document.createElement("button");
        cancelRequest.innerHTML = "Cancel Request"
        cancelRequest.onclick = function () {
            cancelOutGoingFriendRequest(message.receiver);
        }
        activity.appendChild(cancelRequest);
    }
    if (message.activity === "friendRequestReceived") {
        notificationList.setAttribute("id", message.sender);
        activity.innerHTML = "You Have a Friend Request from ";
        activityItem.appendChild(activity);
        const a = document.createElement("a");
        a.innerHTML = message.sender;
        activity.appendChild(a);

        const confirm = document.createElement("button");
        confirm.innerHTML = "Confirm"
        confirm.onclick = function () {
            confirmFriendRequest(message.sender);
        }
        activity.appendChild(confirm);
        const decline = document.createElement("button");
        decline.innerHTML = "Decline"
        decline.onclick = function () {
            cancelIncomingFriendRequest(message.sender);
        }
        activity.appendChild(decline);
    }
    if (message.activity === "invitation") {
        activity.innerHTML = "Invitation has been sent to ";
        activityItem.appendChild(activity);
        const a = document.createElement("a");
        a.innerHTML = message.receiver;
        activity.appendChild(a);
    }
    if (message.activity === "friends") {
        activity.innerHTML = "You are now Friends ";
        activityItem.appendChild(activity);
        const a = document.createElement("a");
        a.innerHTML = message.receiver;
        activity.appendChild(a);

        const chat = document.createElement("button");
        chat.innerHTML = "Chat"
        chat.onclick = function () {
            viewChat(message.receiver);
        }
        activity.appendChild(chat);
    }
}
function removeNotification(message){
    const Count = document.getElementById("badge");
    if (Count.innerHTML > 0){
        Count.innerHTML = parseInt(Count.innerHTML) - 1;
    }
    const node = document.getElementById(message.friendId);
    node.parentNode.removeChild(node)
}
function removeChat(){
    const chatList = document.getElementById("chatList")
    chatList.innerHTML = "";
}
function removeLabel(){
    document.getElementById("about").innerHTML = ""
}
function applyTheLabel(friendId){
    removeLabel();
    const chatAbout = document.getElementById("about");

    const avatar = document.getElementById("avatar" + friendId);
    const clone = avatar.cloneNode(false);
    document.getElementById("about").appendChild(clone);
    const chatAbout1 = document.createElement("div");
    chatAbout1.setAttribute("class","chat-about");
    const name = document.createElement("h6");
    name.setAttribute("class","m-b-0");
    name.innerHTML = friendId;
    chatAbout.appendChild(name);

    const status = document.getElementById("status"+ friendId);
    const aboutStatus = document.createElement("div");
    aboutStatus.setAttribute("id","aboutStatus"+friendId);
    aboutStatus.setAttribute("class","status");
    aboutStatus.innerHTML = status.innerHTML;
    chatAbout.appendChild(aboutStatus);

}
function viewChat(friendId) {
    applyTheLabel(friendId);
    globalFriendId = friendId;
    const ChatAction = {
        action: "viewChat",
        friendId: friendId
    };
    const ul = document.getElementById("chatList");
    if (document.contains(ul)) {
        removeChat();
    }
    socket.send(JSON.stringify(ChatAction));
}
function sendMessage() {
    const message = document.getElementById("messageToSend").value;
    document.getElementById("messageToSend").value = "";
    const date = new Date();
    if (globalFriendId !== "") {

        const ChatAction = {
            action: "sendMessage",
            friendId: globalFriendId,
            message: message + "",
            time: date.getHours() + " : " + date.getMinutes()
        };
        socket.send(JSON.stringify(ChatAction));
    }
}
function sendFriendRequest(friendId){
    const ChatAction = {
        action: "friendRequest",
        friendId: friendId
    };
    socket.send(JSON.stringify(ChatAction));
}
function sendInvitation(friendId) {

    const ChatAction = {
        action: "invitation",
        friendId: friendId,
    };
    socket.send(JSON.stringify(ChatAction));
}
function cancelOutGoingFriendRequest(friendId) {
    const ChatAction = {
        action: "cancelOutGoingFriendRequest",
        friendId: friendId
    };
    removeNotification(ChatAction);
    socket.send(JSON.stringify(ChatAction));
}
function cancelIncomingFriendRequest(friendId) {
    const ChatAction = {
        action: "cancelIncomingFriendRequest",
        friendId: friendId
    };
    removeNotification(ChatAction);
    socket.send(JSON.stringify(ChatAction));
}
function confirmFriendRequest(friendId) {
    const n = document.getElementById("notificationDrop");
    n.parentNode.removeChild(n);
    const ChatAction = {
        action: "confirmFriendRequest",
        friendId: friendId
    };
    socket.send(JSON.stringify(ChatAction));
}
function addFriend() {
    const friendId = document.getElementById("addFriend").value;
    document.getElementById("addFriend").value = "";
    if (friendId !== "" && friendId !== globalUserId) {
        const ChatAction = {
            action: "addFriend",
            friendId: friendId
        };
        socket.send(JSON.stringify(ChatAction));
    }
}
function overlayOn() {
    document.getElementById("overlay").style.display = "block";
}
function overlayOff() {
    document.getElementById("overlay").style.display = "none";
    const popWindowProfile = document.getElementById("forProfile");
    popWindowProfile.innerHTML = "";
}
function viewNotification(){
    const Count = document.getElementById("badge");
    Count.innerHTML = 0;
    const ChatAction = {
        action : "notificationsViewed"
    }
    socket.send(JSON.stringify(ChatAction));
}
function sendImage(item) {
    closeNav();
    const date = new Date();
    if (globalFriendId !== "") {
        const chatAction = {
            action: "sendImage",
            friendId: globalFriendId,
            time: "" + date.getHours() + ":" + date.getMinutes(),
            encoded: item.href.toString()
        };
        socket.send(JSON.stringify(chatAction))
        console.log("sending image to" + globalFriendId)
    }
}
function notify(){
    document.getElementById("notificationAudio").play();
}
function startedTyping(){
    socket.send(JSON.stringify({
        action : "isTyping",
        friendId : globalFriendId
    }))
}
function stoppedTyping(){
    socket.send(JSON.stringify({
        action : "backOnline",
        friendId : globalFriendId
    }))
}