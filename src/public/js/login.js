const form = document.getElementById("login-form");

form.addEventListener('submit', async e => {
    e.preventDefault();
    const username = document.getElementById("username").value;
    const password = document.getElementById("password").value;

    const body = JSON.stringify({username, password});

    const resp = await fetch("/user/login", {
        method: "POST",
        headers: {'Content-Type': 'application/json'},
        body,
    });
    const res = await resp.json();
    if (res.success) location.replace("/");
    else alert("ASJHDOAISJDLKAJSD");
})