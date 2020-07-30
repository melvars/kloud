const form = document.getElementById("setup-form");

form.addEventListener('submit', async e => {
    e.preventDefault();
    const username = document.getElementById('username').value;
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    const admin = true;

    const body = JSON.stringify({username, email, password, admin});

    const resp = await fetch("/user/register", {
        method: "POST",
        headers: {'Content-Type': 'application/json'},
        body,
    })
    const res = await resp.json();
    if (res.success) location.replace("/user/login");
    else alert("ASJHDOAISJDLKAJSD");

})