const preview = document.getElementById("preview");
const modify = document.getElementById("modify");
const content = document.getElementById("content");
const html = document.getElementsByTagName("html")[0];
const body = document.body;

// buttons
const raw = document.getElementById("raw");
const code = document.getElementById("code");
const dark = document.getElementById("dark");
const settings = document.getElementById("settings");

const originalContent = content.innerText;

if (extension === "html") {
    preview.src = "data:text/html;charset=utf-8," + encodeURI(originalContent);

    preview.style.display = "block";
    raw.style.display = "block";
    content.style.display = "none";

    raw.addEventListener("click", () => {
        if (preview.style.display === "block") {
            html.style.overflow = "visible";
            body.style.overflow = "visible";
            raw.innerText = "Show preview";
            preview.style.display = "none";
            content.style.display = "block";
            settings.style.display = "block";
        } else {
            html.style.overflow = "hidden";
            body.style.overflow = "hidden";
            raw.innerText = "Show raw";
            preview.style.display = "block";
            content.style.display = "none";
            settings.style.display = "none";
        }
    });
} else if (extension === "md") {
    const simplemde = new SimpleMDE({
        element: modify,
        hideIcons: ["fullscreen", "preview", "guide"],
        autoDownloadFontAwesome: false
    });
    simplemde.value(originalContent);
    content.style.display = "none";
    html.style.overflow = "visible";
    body.style.overflow = "visible";
} else {
    settings.style.display = "block";
    html.style.overflow = "visible";
    body.style.overflow = "visible";
}

code.addEventListener("change", () => {
    if (code.checked) {
        content.classList.remove("prettyprinted");
        content.innerHTML = "";
        content.classList.add("linenums");
        content.innerText = originalContent;
        PR.prettyPrint();
    } else {
        content.classList.remove("prettyprinted");
        content.innerHTML = "";
        content.classList.remove("linenums");
        content.innerText = originalContent;
        PR.prettyPrint();
    }
});

dark.addEventListener("change", () => {
    if (dark.checked) {
        document.getElementsByTagName("head")[0]
            .insertAdjacentHTML("beforeend", '<link id="darkTheme" href="/css/darkTheme.css" rel="stylesheet" />');
        document.getElementById("lightTheme").outerHTML = "";
    } else {
        document.getElementsByTagName("head")[0]
            .insertAdjacentHTML("beforeend", '<link id="lightTheme" href="/css/lightTheme.css" rel="stylesheet" />');
        document.getElementById("darkTheme").outerHTML = "";
    }
});
