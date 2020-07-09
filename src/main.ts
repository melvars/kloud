import { Application } from "https://deno.land/x/abc@v1/mod.ts";

const app = new Application();

console.log("Started server!");

app.get("/", (c) => {
    return "Hello, world!";
}).start({ port: 8080 });
