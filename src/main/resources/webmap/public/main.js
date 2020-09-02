let images = [];
let markers = [];
let scale = 1/8;
let centerBlockX = 0;
let centerBlockY = 0;
let mouseX = 0;
let mouseY = 0;

function reload(canvas, ctx){
    fetch(`${window.location.href}region_files`)
        .then(value => value.json())
        .then(data => {
            if(data && data.regions){
                images = [];
                for(let region of data.regions){
                    let img = new Image();
                    img.src = `${window.location.href}r.${region.regionX}.${region.regionZ}.png`
                    img.onload = () => {
                        images.push({
                            image: img,
                            regionX: region.regionX,
                            regionZ: region.regionZ
                        });
                        redraw(canvas, ctx);
                    }
                }
            }
            fetch(`${window.location.href}marker`)
                .then(value => value.json())
                .then(data => {
                    if(data){
                        markers = data;
                    }
                    redraw(canvas, ctx);
                });
        });
}

function blockToRender(i){
    return i / scale;
}

function renderToBlock(i){
    return i * scale;
}

window.addEventListener("resize", () => {
    let canvas = document.querySelector("#canvas");
    if(canvas){
        canvas.width = document.body.clientWidth;
        canvas.height = document.body.clientHeight;
        const ctx = canvas.getContext('2d');
        ctx.imageSmoothingEnabled = false;
        redraw(canvas, ctx);
    }
});

document.addEventListener("DOMContentLoaded", evt => {
    let canvas = document.querySelector("#canvas");
    let scaleText = document.querySelector("#scale_text");
    scaleText.innerHTML = `Scale: ${scale}`;
    let centerPosition = document.querySelector("#center_pos_text");
    centerPosition.innerHTML = `Center position: x=${Math.round(centerBlockY)} z=${Math.round(centerBlockY)}`;
    let mousePosition = document.querySelector("#mouse_pos_text");
    mousePosition.innerHTML = `Mouse position:  x=${Math.round(mouseX)} z=${Math.round(mouseY)}`;
    if(canvas){
        canvas.width = document.body.clientWidth;
        canvas.height = document.body.clientHeight;
        const ctx = canvas.getContext('2d');
        ctx.imageSmoothingEnabled = false;
        reload(canvas, ctx);

        const eventSource = new EventSource(`${document.location.href}sse`);
        eventSource.addEventListener("reload", msg => {
            reload(canvas, ctx);
        });

        let downButtonX, downButtonY;
        canvas.onmousedown = evt => {
            if(evt.buttons === 1){
                downButtonX = evt.clientX;
                downButtonY = evt.clientY;
            }
        }
        canvas.onmousemove = evt => {
            if(evt.buttons === 1){
                centerBlockX = centerBlockX - ((downButtonX - evt.clientX) / (scale));
                centerBlockY = centerBlockY - ((downButtonY - evt.clientY) / (scale));
                downButtonX = evt.clientX;
                downButtonY = evt.clientY;
                redraw(canvas, ctx);
                centerPosition.innerHTML = `Center position: x=${Math.round(centerBlockX)} z=${Math.round(centerBlockY)}`;
            }
            mouseX = blockToRender(evt.clientX - (canvas.width / 2)) - centerBlockX;
            mouseY = blockToRender(evt.clientY - (canvas.height / 2)) - centerBlockY;
            mousePosition.innerHTML = `Mouse position:  x=${Math.round(mouseX)} z=${Math.round(mouseY)}`;
            redraw(canvas, ctx);
            //console.log(mouseX);
        }
        canvas.onwheel = evt => {
            scale -= (Math.sign(evt.deltaY) * 0.125);
            scale = Math.max(Math.min(scale, 4), 0.125);
            scaleText.innerHTML = `Scale: ${scale}`;
            mouseX = blockToRender(evt.clientX - (canvas.width / 2)) - centerBlockX;
            mouseY = blockToRender(evt.clientY - (canvas.height / 2)) - centerBlockY;
            mousePosition.innerHTML = `Mouse position:  x=${Math.round(mouseX)} z=${Math.round(mouseY)}`;
            redraw(canvas, ctx);
        }
    }
});

function redraw(canvas, ctx) {
    ctx.fillStyle = "black";
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    for(let img of images){
        //console.log(`Centerblock X: ${centerBlockX} Y: ${centerBlockY}`);
        let wh = img.image.width * scale;
        let x = Math.round(centerBlockX * scale + (wh * img.regionX) + (canvas.width / 2));
        let y = Math.round(centerBlockY * scale + (wh * img.regionZ) + (canvas.height / 2));
        ctx.drawImage(img.image, x, y, wh, wh);
    }
    //ctx.strokeStyle = "#ffffff";
    //ctx.strokeRect(Math.round(centerBlockX * scale + (canvas.width / 2)) + (scale), Math.round(centerBlockY * scale + (canvas.height / 2)) + (scale), scale, scale);
    //
    //ctx.strokeRect(Math.round(mouseX * scale + (canvas.width / 2)), Math.round(mouseY * scale + (canvas.height / 2)), 10, 10);
    //ctx.strokeRect(blockToRender(mouseX), blockToRender(mouseY), 10, 10);
    for(let marker of markers){
        ctx.fillStyle = marker.color;
        let width = Math.round((marker.width ? marker.width : 1) * scale);
        let height = Math.round((marker.height ? marker.height : 1) * scale);
        let x = renderToBlock(centerBlockX + marker.x) + (canvas.width / 2);
        let y = renderToBlock(centerBlockY + marker.z) + (canvas.height / 2);
        ctx.fillRect(x, y, width, height);
        if(marker.name){
            ctx.fillStyle = "#000000C0";
            ctx.font = "10px Arial";
            let textWidth = ctx.measureText(marker.name).width;
            if(width * 2 > textWidth || scale >= 1){
                ctx.fillText(marker.name, x + (width / 2) - (textWidth / 2), y + (height / 2) + 5);
            }
        }
    }
}