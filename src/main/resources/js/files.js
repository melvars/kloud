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
        row.insertCell(3).innerHTML = "<td><button class='share'><i class='icon ion-md-share'></i></button></td>";
        row.insertCell(4).innerHTML = "<td><button class='delete'><i class='icon ion-md-trash'></i></button></td>";

        setListeners();

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
function setListeners() {
    document.querySelectorAll("[data-path]").forEach(element => {
        const images = ["jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "tiff"];
        const videos = ["mp4", "m4v", "mov", "webm", "avi", "wmv", "mpg", "mpv", "mpeg", "ogv"];
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

    // deletion button
    document.querySelectorAll(".delete").forEach(element => {
        element.addEventListener("click", e => {
            e.stopPropagation();
            const request = new XMLHttpRequest();
            const parent = e.target.closest("tr");
            const fileName = parent.getAttribute("data-href") || parent.getAttribute("data-path");
            request.open("POST", `/delete/${path}/${fileName}`);
            request.send();
            parent.remove();
        })
    });

    // share button
    document.querySelectorAll(".share").forEach(element => {
        element.addEventListener("click", e => {
            e.stopPropagation();
            const request = new XMLHttpRequest();
            const parent = e.target.closest("tr");
            const fileName = parent.getAttribute("data-href") || parent.getAttribute("data-path");

            request.open("POST", `/share/${path}/${fileName}`);
            request.onload = () => {
                if (request.readyState === 4) {
                    if (request.status === 200) {  // TODO: fix clipboard in Firefox
                        const input = document.createElement('input');
                        input.setAttribute('value', request.responseText);
                        document.body.appendChild(input);
                        input.select();
                        document.execCommand('copy');
                        document.body.removeChild(input);
                        alert(`Copied url to clipboard!\n${request.responseText}`);
                    } else {
                        alert("Something went wrong.");
                    }
                }
            };
            request.send();
        })
    });
}

setListeners();

/**
 * Set up sort features
 */
function sortTable(table, col, ascending) {
    const tb = table.tBodies[0];
    let tr = Array.prototype.slice.call(tb.rows, 0);

    ascending = -((+ascending) || -1);
    tr = tr.sort((a, b) => {
        if (a.cells[col].getAttribute("data-size") !== null)
            return ascending * (Number(a.cells[col].getAttribute("data-size")) > Number(b.cells[col].getAttribute("data-size")) ? 1 : -1);
        else if (a.cells[col].getAttribute("data-date") !== null)
            return ascending * (Number(a.cells[col].getAttribute("data-date")) > Number(b.cells[col].getAttribute("data-date")) ? 1 : -1);
        else
            return ascending * (a.cells[col].textContent.trim().localeCompare(b.cells[col].textContent.trim()))
    });

    for (let i = 0; i < tr.length; ++i) tb.appendChild(tr[i]);
}

document.querySelectorAll("thead tr > th").forEach((header, index) => {
    header.addEventListener("click", () => {
        const ascending = header.getAttribute("data-asc");
        sortTable(document.querySelector("table"), index, (ascending === "true"));
        header.setAttribute("data-asc", (ascending === "false").toString())
    })
});
