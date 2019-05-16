/**
 * Drag and drop
 */
const drop = document.getElementById("drop");
drop.addEventListener('dragover', e => {
    e.stopPropagation();
    e.preventDefault();
    e.dataTransfer.dropEffect = 'copy';
    drop.classList.add("hover");
});

drop.addEventListener('dragleave', () =>
    drop.classList.remove("hover")
);

drop.addEventListener('drop', e => {
    e.stopPropagation();
    e.preventDefault();
    drop.classList.remove("hover");
    const items = e.dataTransfer.items;

    for (let i = 0; i < items.length; i++) {
        const item = items[i].webkitGetAsEntry();
        const file = items[i].getAsFile();
        const date = new Date();

        const row = document.getElementById("table").insertRow(-1);
        row.setAttribute("data-href", file.name);

        if (item.isDirectory) {
            row.insertCell(0).innerHTML = "<td><i class='icon ion-md-folder'></i></td>";
            row.insertCell(1).innerHTML = file.name + "/";
        } else {
            row.insertCell(0).innerHTML = "<td><i class='icon ion-md-document'></i></td>";
            row.insertCell(1).innerHTML = file.name;
        }
        row.insertCell(2).innerHTML = bytesToSize(file.size);
        row.insertCell(3).innerHTML = `${date.getMonth() + 1}/${date.getDate()}/${date.getFullYear()} ${date.getHours()}:${date.getMinutes()}:${date.getSeconds()}`;
        row.insertCell(4).innerHTML = "<td><button class='share'><i class='icon ion-md-share'></i></button></td>";
        row.insertCell(5).innerHTML = "<button class='downloadButton'><a class='download' href='" + file.name + "' download='" + file.name + "'><i class='icon ion-md-download'></i></a></button>";
        row.insertCell(6).innerHTML = "<td><button class='delete'><i class='icon ion-md-trash'></i></button></td>";

        setListeners();

        iterateFiles(item, files => {
            const progress = document.getElementById("progress");
            const request = new XMLHttpRequest();
            const formData = new FormData();

            for (const file of files)
                formData.append('file', file);

            request.upload.onprogress = e => {
                if (e.lengthComputable) {
                    progress.style.display = "block";
                    progress.innerText = `Uploading ${files.length} file(s): ${(e.loaded / e.total * 100).toFixed(2)}%`;
                }
            };

            request.onreadystatechange = () => {
                if (request.readyState === 4) {
                    if (request.status === 200) {
                        progress.innerText = "Finished uploading!";
                        setTimeout(() => progress.style.display = "none", 3000)
                    } else {
                        progress.style.color = "red";
                        progress.innerText = "An error occurred :(";
                    }
                }
            };

            request.open('POST', `/upload/${path}`.clean(), true);
            request.send(formData);
        });
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
            const extension = (/(?:\.([^.]+))?$/.exec(filename)[1] || "").toLowerCase();

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
                    if (request.status === 200) {
                        window.prompt("Copy with Ctrl+C", request.responseText);
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
 * Iterates over files and directories
 * TODO: Add empty directory upload support
 * @param item
 * @param callback
 */
function iterateFiles(item, callback) {
    const files = [];
    console.log(item);
    (function iterate(subItem) {
        if (subItem.isDirectory) {
            const directoryReader = subItem.createReader();
            directoryReader.readEntries(entries => {
                entries.forEach(entry => {
                    entry.name = entry.fullPath;
                    iterate(entry)
                })
            });
        } else
            subItem.file(file => {
                const newName = subItem.fullPath.charAt(0) === '/' ? subItem.fullPath.substr(1) : subItem.fullPath;
                files.push(new File([file], newName, {
                    lastModified: file.lastModified,
                    lastModifiedDate: file.lastModifiedDate,
                    size: file.size,
                    webkitRelativePath: file.webkitRelativePath,
                    type: file.type,
                }));
            }, err => console.error(err));
    })(item);

    // REMEMBER: This is a quite ugly solution but due to the almost inexistent filesystem support in most browsers
    //  we need to use this!
    setTimeout(() => {
        console.log(files);
        callback(files.flat(100))
    }, 100) // max iterate time the pc may need: 100ms
}

/**
 * Set up sort features
 * @param table
 * @param col
 * @param ascending
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
