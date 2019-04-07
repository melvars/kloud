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

        drop.insertAdjacentHTML('beforeend', `<a class="filename" href="${files[i].name}">${files[i].name}</a><br><hr>`);

        formData.append("file", files[i]);
        request.open("POST", "/upload/" + path);
        request.send(formData);
    }
});
