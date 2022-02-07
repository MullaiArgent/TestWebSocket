window.onload = init;
var socket = new WebSocket("ws://localhost:8080/TestWebSocket_war/actions");
socket.onmessage = onMessage;

function onMessage(event){
    var device = JSON.parse(event.data);
    if (device.action === "add"){
         printDeviceElement(device);
     }
     if (device.action === "remove"){
         document.getElementById(device.id).remove();
     }
     if (device.action === "toggle"){
         var node = document.getElementById(device.id);
         var statusText = node.children[2];
         if (device.status === "On"){
             statusText.innerHTML = "Status: " + device.status + "(<a href=\"#\" onclick=toggleDevice("+ device.id +")>Turn Off</a>)"
         }else if (device.status === "Off"){
             statusText.innerHTML = "Status: " + device.status + "(<a href=\"#\" onclick=toggleDevice("+ device.id +")>Turn on</a>)"
         }
     }
}
function addDevice(name, type, description){
    var DeviceAction = {
        action: "add",
        name: name,
        type: type,
        description: description
    };
    socket.send(JSON.stringify(DeviceAction))
}

function removeDevice(element){
    var DeviceAction = {
        action: "remove",
        id: element
    };
    socket.send(JSON.stringify(DeviceAction))
}

function toggleDevice(element){
    var DeviceAction = {
        action: "toggle",
        id: element
    };
    socket.send(JSON.stringify(DeviceAction));
}

function printDeviceElement(device){
    var content = document.getElementById("content");

    var deviceDiv = document.createElement("div");
    deviceDiv.setAttribute("id",device.id);
    deviceDiv.setAttribute("class","device" + deviceDiv.type);
    content.appendChild(deviceDiv);

    var deviceName = document.createElement("span");
    deviceName.setAttribute("class","deviceName");
    deviceName.innerHTML = device.name;
    deviceDiv.appendChild(deviceName);

    var deviceType = document.createElement("span");
    deviceType.innerHTML = " <b>Type: </b> " + device.type;
    deviceDiv.appendChild(deviceType);

    var deviceStatus = document.createElement("span");
    if (device.status === "On"){
        deviceStatus.innerHTML = "Status: " + device.status + "(<a href= \"#\" onclick=toggleDevice("+ device.id +")>Turn On</a>)"
    }else if (device.status === "Off"){
        deviceStatus.innerHTML = "Status: " + device.status + "(<a href= \"#\" onclick=toggleDevice("+ device.id +")>Turn Off</a>)"
    }
    deviceDiv.appendChild(deviceStatus);

    var deviceDescription = document.createElement("span");
    deviceDescription.innerHTML = "<b>Comments : </b>" + device.description;
    deviceDiv.appendChild(deviceDescription);

    var removeDevice = document.createElement("span");
    removeDevice.setAttribute("class","removeDevice");
    removeDevice.innerHTML = "<a href=\"#\" onclick=removeDevice("+device.id +")>Remove device</a>";
    deviceDiv.appendChild(removeDevice);

}

function showForm(){
    document.getElementById("addDeviceForm").style.display = '';
}
function hideForm(){
    document.getElementById("addDeviceForm").style.display = "none";
}
function formSubmit(){
    var form = document.getElementById("addDeviceForm");
    var name = form.elements["device_name"].value;
    var type = form.elements["device_type"].value;
    var description = form.elements["device_description"].value;
    hideForm();
    document.getElementById("addDeviceForm").reset();
    addDevice(name, type, description);
}
function init(){
    hideForm();
}