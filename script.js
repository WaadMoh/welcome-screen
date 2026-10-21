import {
    FaceDetector,
    FilesetResolver
} from "https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@0.10.22/+esm";

const camera = document.getElementById("camera");
const mainMessage = document.getElementById("mainMessage");
const statusMessage = document.getElementById("statusMessage");
const card = document.querySelector(".card");

const permissionScreen =
    document.getElementById("permissionScreen");

const startButton =
    document.getElementById("startButton");

const errorMessage =
    document.getElementById("errorMessage");

const DEPARTURE_DELAY = 3000;
const DETECTIONS_REQUIRED = 2;
const DETECTION_INTERVAL = 250;

let detector = null;
let stream = null;

let personPresent = false;
let consecutiveDetections = 0;
let lastPersonSeen = 0;
let lastDetectionRun = 0;
let lastCameraTime = -1;
let systemStarting = false;

function showWaiting() {
    personPresent = false;

    mainMessage.textContent =
        "Waiting for someone...";

    statusMessage.textContent =
        "Detection active";

    card.classList.remove("detected");
}

function showHello() {
    if (personPresent) {
        return;
    }

    personPresent = true;

    mainMessage.textContent = "Hello";

    statusMessage.textContent =
        "Person detected";

    card.classList.add("detected");
}

function updatePresence(
    faceDetected,
    currentTime
) {
    if (faceDetected) {
        lastPersonSeen = currentTime;
        consecutiveDetections += 1;

        if (
            !personPresent &&
            consecutiveDetections >=
                DETECTIONS_REQUIRED
        ) {
            showHello();
        }

        return;
    }

    consecutiveDetections = 0;

    if (
        personPresent &&
        currentTime - lastPersonSeen >=
            DEPARTURE_DELAY
    ) {
        showWaiting();
    }
}

async function loadFaceDetector() {
    statusMessage.textContent =
        "Loading detection model...";

    const vision =
        await FilesetResolver.forVisionTasks(
            "https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@0.10.22/wasm"
        );

    detector =
        await FaceDetector.createFromOptions(
            vision,
            {
                baseOptions: {
                    modelAssetPath:
                        "https://storage.googleapis.com/mediapipe-models/face_detector/blaze_face_short_range/float16/1/blaze_face_short_range.tflite",

                    delegate: "CPU"
                },

                runningMode: "VIDEO",

                minDetectionConfidence: 0.55,

                minSuppressionThreshold: 0.3
            }
        );
}

async function startCamera() {
    if (
        !navigator.mediaDevices ||
        !navigator.mediaDevices.getUserMedia
    ) {
        throw new Error(
            "Camera access is not supported by this browser."
        );
    }

    statusMessage.textContent =
        "Requesting camera access...";

    stream =
        await navigator.mediaDevices.getUserMedia({
            video: {
                facingMode: "user",

                width: {
                    ideal: 640
                },

                height: {
                    ideal: 480
                }
            },

            audio: false
        });

    camera.srcObject = stream;

    await camera.play();

    if (camera.readyState < 2) {
        await new Promise(resolve => {
            camera.addEventListener(
                "loadeddata",
                resolve,
                {
                    once: true
                }
            );
        });
    }
}

function detectionLoop(timestamp) {
    requestAnimationFrame(detectionLoop);

    if (!detector) {
        return;
    }

    if (camera.readyState < 2) {
        return;
    }

    if (
        camera.videoWidth === 0 ||
        camera.videoHeight === 0
    ) {
        return;
    }

    if (
        timestamp - lastDetectionRun <
        DETECTION_INTERVAL
    ) {
        return;
    }

    if (
        camera.currentTime ===
        lastCameraTime
    ) {
        return;
    }

    lastDetectionRun = timestamp;
    lastCameraTime = camera.currentTime;

    try {
        const result =
            detector.detectForVideo(
                camera,
                timestamp
            );

        const faceDetected =
            result.detections &&
            result.detections.length > 0;

        updatePresence(
            faceDetected,
            Date.now()
        );
    } catch (error) {
        console.error(
            "Detection error:",
            error
        );
    }
}

async function startSystem() {
    if (systemStarting) {
        return;
    }

    systemStarting = true;

    startButton.disabled = true;
    errorMessage.textContent = "";

    try {
        await loadFaceDetector();

        await startCamera();

        permissionScreen.classList.remove(
            "show"
        );

        showWaiting();

        requestAnimationFrame(
            detectionLoop
        );
    } catch (error) {
        console.error(
            "System startup error:",
            error
        );

        systemStarting = false;
        startButton.disabled = false;

        permissionScreen.classList.add(
            "show"
        );

        errorMessage.textContent =
            "The camera or detection model could not start. Allow camera access and select Start Experience again.";

        statusMessage.textContent =
            "Detection could not start";
    }
}

startButton.addEventListener(
    "click",
    startSystem
);

document.addEventListener(
    "visibilitychange",
    () => {
        if (
            document.visibilityState ===
                "visible" &&
            stream
        ) {
            camera.play().catch(error => {
                console.error(
                    "Camera restart error:",
                    error
                );
            });
        }
    }
);

window.addEventListener(
    "beforeunload",
    () => {
        if (stream) {
            stream
                .getTracks()
                .forEach(track => {
                    track.stop();
                });
        }

        if (detector) {
            detector.close();
        }
    }
);

startSystem();
