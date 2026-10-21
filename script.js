const camera = document.getElementById("camera");
const statusText = document.getElementById("status");
const welcomeOverlay = document.getElementById("welcomeOverlay");

async function startCamera() {

    try {

        const stream =
            await navigator.mediaDevices.getUserMedia({
                video: true,
                audio: false
            });

        camera.srcObject = stream;

        camera.onloadedmetadata = () => {
            camera.play();
        };

        statusText.innerText =
            "Camera Started Successfully";

    }
    catch(error) {

        console.error(error);

        statusText.innerText =
            "Camera Permission Denied";

    }
}

function showWelcome() {

    statusText.innerText =
        "Person Detected";

    welcomeOverlay.classList.add("show");
}

function hideWelcome() {

    statusText.innerText =
        "Waiting For Visitor...";

    welcomeOverlay.classList.remove("show");
}

document.addEventListener("keydown", (event) => {

    if(event.key.toLowerCase() === "p") {
        showWelcome();
    }

    if(event.key.toLowerCase() === "o") {
        hideWelcome();
    }

});

startCamera();
