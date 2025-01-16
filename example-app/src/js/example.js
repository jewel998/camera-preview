import { Camera } from '@jewel998/camera-preview';

window.testEcho = () => {
    const inputValue = document.getElementById("echoInput").value;
    Camera.echo({ value: inputValue })
}
