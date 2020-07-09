import { Application } from "https://deno.land/x/abc@v1/mod.ts";

const app = new Application();

app.get("/", (c) => {
    return "Hello, world!";
}).start({ port: 8080 });
