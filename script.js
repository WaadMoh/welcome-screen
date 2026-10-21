const camera = document.getElementById("camera");
const screen = document.getElementById("screen");

async function startCamera() {

    try {

        const stream =
            await navigator.mediaDevices.getUserMedia({
                video:true,
                audio:false
            });

        camera.srcObject = stream;

    } catch(error) {

        console.error(error);

        screen.innerText =
            "Camera Permission Denied";
    }
}

function personDetected() {

    screen.innerText = "Hello";
}

function personLeft() {

    screen.innerText =
        "Waiting for someone...";
}

/*
FOR TESTING ONLY

Press P = Person detected

Press O = Person left
*/

document.addEventListener("keydown",(event)=>{

    if(event.key.toLowerCase()==="p"){
        personDetected();
    }

    if(event.key.toLowerCase()==="o"){
        personLeft();
    }

});

startCamera();
