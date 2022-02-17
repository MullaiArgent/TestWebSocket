function openNav(){
    document.getElementById("myNav").style.width = "100%"
}
function closeNav(){
    document.getElementById("myNav").style.width = "0%"
}

function drawOnImage(image, dwn) {

    const canvasElement = document.getElementById("canvas1");
    const context = canvasElement.getContext("2d");
    if (image) {
        console.log("image exists")
        const imageWidth = image.width;
        const imageHeight = image.height;
        canvasElement.width = imageWidth;
        canvasElement.height = imageHeight;
        context.drawImage(image, 0, 0, imageWidth, imageHeight);
    }
    let isDrawing;
    canvasElement.onmousedown = (e) => {
        isDrawing = true;
        context.beginPath();
        context.lineWidth = 5;
        context.strokeStyle = "#11111111";
        context.lineJoin = "round";
        context.lineCap = "round";
        context.moveTo(e.clientX - 980, e.clientY - 222);
    };

    canvasElement.onmousemove = (e) => {
        if (isDrawing) {
            context.lineTo(e.clientX - 980, e.clientY - 222);
            context.stroke();
            dwn.setAttribute("href", canvasElement.toDataURL())
        }
    };

    canvasElement.onmouseup = function () {
        isDrawing = false;
        context.closePath();
        dwn.setAttribute("href", canvasElement.toDataURL());
    };

    const clearElement = document.getElementById("clear");
    clearElement.onclick = () => {
        context.clearRect(0, 0, canvasElement.width, canvasElement.height);

        const imageWidth = image.width;
        const imageHeight = image.height;

        canvasElement.width = imageWidth;
        canvasElement.height = imageHeight;

        context.drawImage(image, 0, 0, imageWidth, imageHeight);
    };
}