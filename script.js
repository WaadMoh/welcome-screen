const camera = document.getElementById("camera");
const welcomeOverlay = document.getElementById("welcomeOverlay");

let personPresent = false;
let faceTimer;

async function startCamera() {

    try {

        const stream = await navigator.mediaDevices.getUserMedia({
            video: true,
            audio: false
        });

        camera.srcObject = stream;

    } catch (error) {

        console.error("Camera access denied", error);

    }
}

function showWelcome() {

    if (personPresent) return;

    personPresent = true;

    welcomeOverlay.classList.add("show");
}

function hideWelcome() {

    personPresent = false;

    welcomeOverlay.classList.remove("show");
}

/*
Temporary demo detection

Press P = Person detected
Press O = Person left

Later we will replace this with real face detection.
*/

document.addEventListener("keydown", (event) => {

    if (event.key.toLowerCase() === "p") {

        showWelcome();

    }

    if (event.key.toLowerCase() === "o") {

        clearTimeout(faceTimer);

        faceTimer = setTimeout(() => {

            hideWelcome();

        }, 3000);

    }

});

startCamera();
