if (extension === "md" || extension === "html") {
    if (extension === "md")
        document.getElementById("preview").innerHTML = marked(document.getElementById("content").innerText);
    else if (extension === "html")
        document.getElementById("preview").innerHTML = marked(document.getElementById("content").innerText);

    document.getElementById("preview").style.display = "block";
    document.getElementById("switch").style.display = "block";
    document.getElementById("content").style.display = "none";

    document.getElementById("switch").addEventListener("click", () => {
        if (document.getElementById("preview").style.display === "block") {
            document.getElementById("switch").innerText = "Show preview";
            document.getElementById("preview").style.display = "none";
            document.getElementById("content").style.display = "block";
        } else {
            document.getElementById("switch").innerText = "Show raw";
            document.getElementById("preview").style.display = "block";
            document.getElementById("content").style.display = "none";
        }
    });
}
