import {
    FaceDetector,
    FilesetResolver
} from "https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@0.10.22/+esm";

const camera = document.getElementById("camera");
const welcomeOverlay = document.getElementById("welcomeOverlay");
const systemStatus = document.getElementById("systemStatus");
const permissionScreen = document.getElementById("permissionScreen");
const permissionMessage = document.getElementById("permissionMessage");
const startButton = document.getElementById("startButton");
const clock = document.getElementById("clock");

const slides = Array.from(
    document.querySelectorAll(".slide")
);

const dots = Array.from(
    document.querySelectorAll(".dot")
);

const DEPARTURE_DELAY = 3000;
const DETECTIONS_NEEDED = 2;
const DETECTION_INTERVAL = 250;
const SLIDE_INTERVAL = 7000;

let detector = null;
let cameraStream = null;

let personPresent = false;
let lastSeen = 0;
let consecutiveDetections = 0;
let lastDetectionRun = 0;
let lastVideoTime = -1;
let currentSlide = 0;
let systemStarted = false;

function setStatus(text) {
    systemStatus.textContent = text;
}

function updateClock() {
    const now = new Date();

    clock.textContent = now.toLocaleString(
        "en-AE",
        {
            weekday: "short",
            hour: "2-digit",
            minute: "2-digit"
        }
    );
}

function showSlide(index) {
    slides.forEach((slide, position) => {
        slide.classList.toggle(
            "active",
            position === index
        );
    });

    dots.forEach((dot, position) => {
        dot.classList.toggle(
            "active",
            position === index
        );
    });
}

function startContentRotation() {
    showSlide(currentSlide);

    window.setInterval(() => {
        currentSlide =
            (currentSlide + 1) % slides.length;

        showSlide(currentSlide);
    }, SLIDE_INTERVAL);
}

async function createFaceDetector() {
    setStatus("Loading person-detection model...");

    const vision = await FilesetResolver.forVisionTasks(
        "https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@0.10.22/wasm"
    );

    detector = await FaceDetector.createFromOptions(
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
            "This browser does not support camera access."
        );
    }

    setStatus("Requesting camera access...");

    cameraStream =
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

    camera.srcObject = cameraStream;

    await camera.play();

    await new Promise((resolve) => {
        if (camera.readyState >= 2) {
            resolve();
            return;
        }

        camera.addEventListener(
            "loadeddata",
            resolve,
            {
                once: true
            }
        );
    });
}

function showWelcome() {
    if (personPresent) {
        return;
    }

    personPresent = true;

    welcomeOverlay.classList.add("show");
    welcomeOverlay.setAttribute(
        "aria-hidden",
        "false"
    );

    setStatus("Visitor detected");
}

function hideWelcome() {
    if (!personPresent) {
        return;
    }

    personPresent = false;

    welcomeOverlay.classList.remove("show");
    welcomeOverlay.setAttribute(
        "aria-hidden",
        "true"
    );

    setStatus("Detection active - waiting for visitor");
}

function updatePresence(faceDetected, currentTime) {
    if (faceDetected) {
        lastSeen = currentTime;
        consecutiveDetections += 1;

        if (
            !personPresent &&
            consecutiveDetections >= DETECTIONS_NEEDED
        ) {
            showWelcome();
        }

        return;
    }

    consecutiveDetections = 0;

    if (
        personPresent &&
        currentTime - lastSeen >= DEPARTURE_DELAY
    ) {
        hideWelcome();
    }
}

function detectionLoop(currentTime) {
    window.requestAnimationFrame(detectionLoop);

    if (
        !detector ||
        camera.readyState < 2 ||
        camera.videoWidth === 0 ||
        camera.videoHeight === 0
    ) {
        return;
    }

    if (
        currentTime - lastDetectionRun <
        DETECTION_INTERVAL
    ) {
        return;
    }

    if (camera.currentTime === lastVideoTime) {
        return;
    }

    lastDetectionRun = currentTime;
    lastVideoTime = camera.currentTime;

    try {
        const result = detector.detectForVideo(
            camera,
            currentTime
        );

        const faceDetected =
            Array.isArray(result.detections) &&
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
    if (systemStarted) {
        return;
    }

    systemStarted = true;

    startButton.disabled = true;
    permissionMessage.textContent = "";

    try {
        await Promise.all([
            createFaceDetector(),
            startCamera()
        ]);

        permissionScreen.classList.remove("show");

        setStatus(
            "Detection active - waiting for visitor"
        );

        window.requestAnimationFrame(
            detectionLoop
        );
    } catch (error) {
        console.error(
            "Startup error:",
            error
        );

        systemStarted = false;
        startButton.disabled = false;

        permissionScreen.classList.add("show");

        permissionMessage.textContent =
            "Camera or detection could not start. Allow camera access, then select Start Experience again.";

        setStatus("Welcome system could not start");
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
            document.visibilityState === "visible" &&
            cameraStream
        ) {
            camera.play().catch(
                error => console.error(
                    "Camera resume error:",
                    error
                )
            );
        }
    }
);

window.addEventListener(
    "beforeunload",
    () => {
        if (cameraStream) {
            cameraStream
                .getTracks()
                .forEach(track => track.stop());
        }

        if (detector) {
            detector.close();
        }
    }
);

updateClock();

window.setInterval(
    updateClock,
    30000
);

startContentRotation();

/*
Try to start automatically.

If the browser requires the user to interact first,
the Start Experience screen appears.
*/
startSystem();
