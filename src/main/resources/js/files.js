const drop = document.getElementById("drop");

setListeners();

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

function setListeners() {
    // binary files
    document.querySelectorAll("[data-path]").forEach(element => {
        const images = ["jpg", "jpeg", "png"]; // TODO: Add other file types

        const filename = element.getAttribute("data-path");
        const extension = /(?:\.([^.]+))?$/.exec(filename)[1].toLowerCase();

        if (images.indexOf(extension) > -1) {
            element.setAttribute("data-bp", filename);
            element.setAttribute("data-image", "");
        }

        element.addEventListener("click", () => {
            if (images.indexOf(extension) === -1) window.location = filename;
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

    // normal files
    document.querySelectorAll("[data-href]").forEach(element => {
        element.addEventListener("click", () => {
            window.location = element.getAttribute("data-href");
        })
    });
}
