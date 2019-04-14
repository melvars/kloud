const drop = document.getElementById("drop");

drop.addEventListener('dragover', e => {
    e.stopPropagation();
    e.preventDefault();
    e.dataTransfer.dropEffect = 'copy';
    drop.style.background = "rgba(12,99,250,0.3)";
});

drop.addEventListener('dragleave', e =>
    drop.style.background = "white"
);

drop.addEventListener('drop', e => {
    // TODO: Fix directory uploading
    e.stopPropagation();
    e.preventDefault();
    drop.style.background = "white";
    const files = e.dataTransfer.files;

    for (let i = 0; i < files.length; i++) {
        let request = new XMLHttpRequest();
        let formData = new FormData();

        // TODO: Consider using current date due to updated lastModified state at upload
        const date = new Date(files[i].lastModified);
        const lastModified = `${date.getMonth() + 1}/${date.getDate()}/${date.getFullYear()} ${date.getHours()}:${date.getMinutes()}:${date.getSeconds()}`;

        const row = document.getElementById("table").insertRow(-1);
        row.insertCell(0).innerHTML = `<a class="filename" href="${files[i].name}">${files[i].name}</a>`;
        row.insertCell(1).innerHTML = `<a class="filename" href="${files[i].name}">${bytesToSize(files[i].size)}</a>`;
        row.insertCell(2).innerHTML = `<a class="filename" href="${files[i].name}">${lastModified}</a>`;

        formData.append("file", files[i]);
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
