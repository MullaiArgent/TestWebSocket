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
    if (message.action === "addFriendWindow"){
        const windowType = document.getElementById("forProfile");

        const profile = document.createElement("img");
        profile.setAttribute("src",message.profile);
        profile.setAttribute("height","100");
        profile.setAttribute("width","100");

        windowType.appendChild(profile);

        const popWindowType = document.getElementById("popWindowType");
        popWindowType.innerHTML = "Send Friend Request";

        const popWindowFriendName = document.getElementById("popWindowFriendName");
        popWindowFriendName.innerHTML = message.friendId;

        const popWindowButton = document.getElementById("popWindowButton");
        popWindowButton.innerHTML = "Add Friend"
        popWindowButton.onclick = function () {
            sendFriendRequest(message.friendId);
            overlayOff();
            console.log("add friend request to "+ message.friendId)
        }
        overlayOn();
    }
    if (message.action === "invitationWindow"){
        const windowType = document.getElementById("forProfile");

        const profile = document.createElement("img");
        profile.setAttribute("src", "https://tinyurl.com/2p8hr77v");
        profile.setAttribute("height","100");
        profile.setAttribute("width","100");

        windowType.appendChild(profile);

        const popWindowType = document.getElementById("popWindowType");
        popWindowType.innerHTML = "Send Invitation";

        const popWindowFriendName = document.getElementById("popWindowFriendName");
        popWindowFriendName.innerHTML = message.friendId;

        const popWindowButton = document.getElementById("popWindowButton");
        popWindowButton.innerHTML = "Invite"
        popWindowButton.onclick = function () {
            sendInvitation(message.friendId);
            console.log("Invitation to "+ message.friendId);
            overlayOff();
        }
        overlayOn();
    }
    if (message.action === "chatWindow"){
        const windowType = document.getElementById("forProfile");

        const profile = document.createElement("img");
        profile.setAttribute("src",message.profile);
        profile.setAttribute("height","100");
        profile.setAttribute("width","100");

        windowType.appendChild(profile);

        const popWindowType = document.getElementById("popWindowType");
        popWindowType.innerHTML = "Already a Friend";

        const popWindowFriendName = document.getElementById("popWindowFriendName");
        popWindowFriendName.innerHTML = message.friendId;

        const popWindowButton = document.getElementById("popWindowButton");
        popWindowButton.innerHTML = "Chat"
        popWindowButton.onclick = function (){
            viewChat(message.friendId);
            overlayOff();
        }
        overlayOn();
    }
    if (message.action === "alreadyInvited"){
        const windowType = document.getElementById("forProfile");

        const profile = document.createElement("img");
        profile.setAttribute("src", "https://tinyurl.com/35zst7ft");
        profile.setAttribute("height","100");
        profile.setAttribute("width","100");

        windowType.appendChild(profile);

        const popWindowType = document.getElementById("popWindowType");
        popWindowType.innerHTML = "Already Invited";

        const popWindowFriendName = document.getElementById("popWindowFriendName");
        popWindowFriendName.innerHTML = message.friendId;

        const popWindowButton = document.getElementById("popWindowButton");
        popWindowButton.innerHTML = "Close"
        popWindowButton.onclick = function () {
            overlayOff();
        }
        overlayOn();
    }
    if (message.action === "newNotification"){
        const badge = document.getElementById("badge");
        badge.innerHTML = parseInt(badge.innerHTML) + 1;
    }
    if (message.action === ""){

    }
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
    if (globalFriendId !== ""){
        const ChatAction = {
            action: "sendMessage",
            friendId: globalFriendId,
            message: message + "",
            time : "" + date.getHours() + ":" + date.getMinutes()
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
function sendInvitation(friendId){
    const ChatAction = {
        action: "invitation",
        friendId: friendId
    };
    socket.send(JSON.stringify(ChatAction));
}
function viewNotification(){
    const ChatAction = {
        action: "viewNotification"
    };
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
    const ChatAction = {
        action: "declineFriendRequest",
        friendId: ""
    };
    socket.send(JSON.stringify(ChatAction));
}
function overlayOn() {
    document.getElementById("overlay").style.display = "block";
}
function overlayOff() {
    document.getElementById("overlay").style.display = "none";
    const popWindowProfile = document.getElementById("forProfile");
    popWindowProfile.innerHTML = "";
}
function addFriend(){
    const friendId = document.getElementById("addFriend").value;
    document.getElementById("addFriend").value = "";
    const ChatAction ={
        action : "addFriend",
        friendId : friendId
    };
    socket.send(JSON.stringify(ChatAction));
}