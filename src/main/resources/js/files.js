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
    e.stopPropagation();
    e.preventDefault();
    drop.style.background = "white";
    const items = e.dataTransfer.items;
    const uploadedFiles = [];

    for (let i = 0; i < items.length; i++) {
        const item = items[i].webkitGetAsEntry();
        const file = items[i].getAsFile();
        const date = new Date();

        const row = document.getElementById("table").insertRow(-1);
        row.setAttribute("data-href", file.name);
        // TODO: Differentiate between file and directory upload in frontend
        row.insertCell(0).innerHTML = "<td><i class='icon ion-md-document'></i></td>";
        row.insertCell(1).innerHTML = file.name;
        row.insertCell(2).innerHTML = bytesToSize(file.size);
        row.insertCell(3).innerHTML = `${date.getMonth() + 1}/${date.getDate()}/${date.getFullYear()} ${date.getHours()}:${date.getMinutes()}:${date.getSeconds()}`;
        row.insertCell(4).innerHTML = "<td><button class='share'><i class='icon ion-md-share'></i></button></td>";
        row.insertCell(5).innerHTML = "<button class='downloadButton'><a class='download' href='" + file.name + "' download='" + file.name + "'><i class='icon ion-md-download'></i></a></button>";
        row.insertCell(6).innerHTML = "<td><button class='delete'><i class='icon ion-md-trash'></i></button></td>";

        setListeners();

        // TODO: Add empty directory upload support
        const iterateFiles = subItem => {
            if (subItem.isDirectory) {
                let directoryReader = subItem.createReader();
                directoryReader.readEntries(entries => {
                    entries.forEach(entry => {
                        iterateFiles(entry);
                    });
                });
            } else {
                subItem.file(subFile => {
                    // TODO: Add support for nested directory upload with more than 1 layer - via webkitRelativePath on firefox?
                    if (!uploadedFiles.includes(`/${path}/${file.name}/${subFile.name}`.clean())) {
                        const formData = new FormData();
                        const request = new XMLHttpRequest();

                        request.upload.onprogress = e => {
                            if (e.lengthComputable) {
                                console.log(`${subFile.name}: ${e.loaded / e.total * 100}%`)
                            }
                        };

                        uploadedFiles.push(`/${path}/${file.name}/${subFile.name}`.clean());
                        formData.append("file", subFile);
                        if (subFile.webkitRelativePath === "") request.open("POST", `/upload/${path}/${file.name}`.clean());
                        else request.open("POST", `/upload/${path}`.clean());
                        request.send(formData);
                    }
                })
            }
        };

        if (item.isDirectory) {
            iterateFiles(item);
        } else {
            if (!uploadedFiles.includes(`/${path}/${file.name}`.clean())) {
                const formData = new FormData();
                const request = new XMLHttpRequest();

                request.upload.onprogress = e => {
                    if (e.lengthComputable) {
                        console.log(`${file.name}: ${e.loaded / e.total * 100}%`)
                    }
                };

                formData.append("file", file);
                request.open("POST", `/upload/${path}`.clean());
                request.send(formData);
            }
        }
    }

    function bytesToSize(bytes) {
        const sizes = ['B', 'KiB', 'MiB', 'GiB', 'TiB'];
        if (bytes === 0) return '0 Byte';
        const i = parseInt(Math.floor(Math.log(bytes) / Math.log(1024)));
        return Math.round(bytes / Math.pow(1024, i), 2) + ' ' + sizes[i];
    }
}, false);

/**
 * Set up listeners
 */
function setListeners() {
    if (isShared === "true") {
        const accessId = location.pathname === '/shared' ? location.search.split('=')[1] : undefined;
        document.querySelectorAll('[data-path], [data-href]').forEach(element => {
            element.addEventListener('click', () => {
                const filename = '/' + (element.getAttribute('data-path') || element.getAttribute('data-href'));
                if (filename !== '/../') {
                    const request = new XMLHttpRequest();
                    const formData = new FormData();
                    formData.append('accessId', accessId);
                    formData.append('filename', filename);
                    request.open('POST', '/share', true);
                    request.onload = () => {
                        if (request.status === 200 && request.readyState === 4) {
                            if (request.responseText)
                                window.location = `/shared?id=${request.responseText}`;
                            else alert('File not found!');
                        }
                    };
                    request.send(formData)
                } else {
                    window.location = '../'
                }
            });
        });
    } else {
        document.querySelectorAll("[data-path]").forEach(element => {
            const images = ["jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "tiff"];
            const videos = ["mp4", "m4v", "mov", "webm", "avi", "wmv", "mpg", "mpv", "mpeg", "ogv"];
            const audio = ["mp3", "m4a", "wav", "ogg"];

            const filename = element.getAttribute("data-path");
            const extension = /(?:\.([^.]+))?$/.exec(filename)[1].toLowerCase();

            if (images.includes(extension)) {
                element.setAttribute("data-bp", filename);
                element.setAttribute("data-image", "");
            } else if (videos.includes(extension)) {
                element.setAttribute("data-src", filename);
                element.setAttribute("data-video", "");
            } else if (audio.includes(extension)) {
                element.setAttribute("data-src", filename);
                element.setAttribute("data-audio", "");
            }

            element.addEventListener("click", () => {
                if (images.indexOf(extension) === -1 && videos.indexOf(extension) === -1 && audio.indexOf(extension) === -1)
                    window.location = `/files/${path}/${filename}`.clean(); // download binary files
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

        // audio // TODO: Fix IOException and scrubbing issues with chromium based browsers
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
                window.location = `/files/${path}/${element.getAttribute("data-href")}`.clean();
            })
        });
    }

    // deletion button
    document.querySelectorAll(".delete").forEach(element => {
        element.addEventListener("click", e => {
            e.stopPropagation();
            const request = new XMLHttpRequest();
            const parent = e.target.closest("tr");
            const filename = parent.getAttribute("data-href") || parent.getAttribute("data-path");
            if (confirm(`Do you really want to delete: ${filename}?`)) {
                request.open("POST", `/delete/${path}/${filename}`.clean(), true);
                request.send();
                parent.remove();
            } else console.log("File not deleted!")
        })
    });

    // download button
    document.querySelectorAll(".download").forEach(element => {
        element.addEventListener("click", e => {
            e.stopPropagation();
        })
    });
    document.querySelectorAll(".downloadButton").forEach(element => {
        element.addEventListener("click", e => {
            console.log(e);
            e.stopPropagation();
            e.target.children[0].click()
        })
    });

    // share button
    document.querySelectorAll(".share").forEach(element => {
        element.addEventListener("click", e => {
            e.stopPropagation();
            const request = new XMLHttpRequest();
            const parent = e.target.closest("tr");
            const filename = parent.getAttribute("data-href") || parent.getAttribute("data-path");
            const type = filename.endsWith('/') ? 'dir' : 'file';

            request.open("POST", `/share/${path}/${filename}?type=${type}`.clean());
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

/**
 * Cleans the string (in this case the url)
 * @returns {String}
 */
String.prototype.clean = function () {
    return this.replace(/\/\/+/g, '/')
};
