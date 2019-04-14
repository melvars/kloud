/**
 * Drag and drop
 */
const drop = document.getElementById("drop");

drop.addEventListener('dragover', e => {
    e.stopPropagation();
    e.preventDefault();
    e.dataTransfer.dropEffect = 'copy';
    drop.style.background = "rgba(12,99,250,0.3)";
});

drop.addEventListener('dragleave', () =>
    drop.style.background = "white"
);

drop.addEventListener('drop', e => {
    // TODO: Fix directory uploading
    e.stopPropagation();
    e.preventDefault();
    drop.style.background = "white";
    const files = e.dataTransfer.files;

    for (const file of files) {
        const request = new XMLHttpRequest();
        const formData = new FormData();

        // TODO: Consider using current date due to updated lastModified state at upload
        const date = new Date(file.lastModified);

        const row = document.getElementById("table").insertRow(-1);
        row.setAttribute("data-href", file.name);
        row.insertCell(0).innerHTML = file.name;
        row.insertCell(1).innerHTML = bytesToSize(file.size);
        row.insertCell(2).innerHTML = `${date.getMonth() + 1}/${date.getDate()}/${date.getFullYear()} ${date.getHours()}:${date.getMinutes()}:${date.getSeconds()}`;

        //setListeners();

        formData.append("file", file);
        request.open("POST", "/upload/" + path);
        request.send(formData);
    }

    function bytesToSize(bytes) {
        const sizes = ['B', 'KiB', 'MiB', 'GiB', 'TiB'];
        if (bytes === 0) return '0 Byte';
        const i = parseInt(Math.floor(Math.log(bytes) / Math.log(1024)));
        return Math.round(bytes / Math.pow(1024, i), 2) + ' ' + sizes[i];
    }
});

/**
 * Set up listeners
 */
document.querySelectorAll("[data-path]").forEach(element => {
    const images = ["jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "tiff"];
    const videos = ["mp4", "m4v", "mov", "webm", "avi", "wmv", "mpg", "mpv", "mpeg"];
    const audio = ["mp3", "m4a", "wav", "ogg"];

    const filename = element.getAttribute("data-path");
    const extension = /(?:\.([^.]+))?$/.exec(filename)[1].toLowerCase();

    if (images.indexOf(extension) > -1) {
        element.setAttribute("data-bp", filename);
        element.setAttribute("data-image", "");
    } else if (videos.indexOf(extension) > -1) {
        element.setAttribute("data-src", filename);
        element.setAttribute("data-video", "");
    } else if (audio.indexOf(extension) > -1) {
        element.setAttribute("data-src", filename);
        element.setAttribute("data-audio", "");
    }

    element.addEventListener("click", () => {
        if (images.indexOf(extension) === -1 && videos.indexOf(extension) === -1 && audio.indexOf(extension) === -1)
            window.location = filename; // download binary files
    });
});

// images
document.querySelectorAll("[data-image]").forEach(element => {
    element.addEventListener("click", image => {
        BigPicture({
            el: image.currentTarget,
            gallery: document.querySelectorAll("[data-image]")
        })
    });
});

// videos // TODO: Fix timeout exception and scrubbing issues with chromium based browsers
document.querySelectorAll("[data-video]").forEach(element => {
    element.addEventListener("click", video => {
        BigPicture({
            el: video.currentTarget,
            vidSrc: video.currentTarget.getAttribute("data-src")
        })
    });
});

//audio // TODO: Fix IOException and scrubbing issues with chromium based browsers
document.querySelectorAll("[data-audio]").forEach(element => {
    element.addEventListener("click", audio => {
        BigPicture({
            el: audio.currentTarget,
            audio: audio.currentTarget.getAttribute("data-src")
        });
    });
});

// normal files
document.querySelectorAll("[data-href]").forEach(element => {
    element.addEventListener("click", () => {
        window.location = element.getAttribute("data-href");
    })
});
