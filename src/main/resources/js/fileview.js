const preview = document.getElementById("preview");
const content = document.getElementById("content");
const contentCode = document.querySelector("#content > code");

// buttons
const raw = document.getElementById("raw");
const code = document.getElementById("code");
const dark = document.getElementById("dark");
const settings = document.getElementById("settings");

const originalContent = content.innerText;

if (extension === "md" || extension === "html") {
    if (extension === "md")
        preview.innerHTML = marked(originalContent);
    else if (extension === "html")
        preview.innerHTML = marked(originalContent);

    preview.style.display = "block";
    raw.style.display = "block";
    content.style.display = "none";

    raw.addEventListener("click", () => {
        if (preview.style.display === "block") {
            raw.innerText = "Show preview";
            preview.style.display = "none";
            content.style.display = "block";
            settings.style.display = "block";
        } else {
            raw.innerText = "Show raw";
            preview.style.display = "block";
            content.style.display = "none";
            settings.style.display = "none";
        }
    });
} else {
    settings.style.display = "block";
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
