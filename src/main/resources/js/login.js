const tryAgain = document.getElementById("tryAgain");
const countdown = document.getElementById("counter");

if (tryAgain !== null)
    setInterval(() => {
        if (Number(countdown.innerText) === 0) tryAgain.style.display = "none";
        countdown.innerText = Number(countdown.innerText) - 1;
    }, 1000);