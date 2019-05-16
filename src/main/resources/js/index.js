if (document.querySelector("#toggle") !== null) {
    document.querySelector("#toggle").addEventListener("click", () => {
        const request = new XMLHttpRequest();
        request.open("POST", "/user/theme");
        request.onload = () => location.reload();
        request.send();
    });
}
