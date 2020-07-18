import "https://deno.land/x/dotenv/load.ts";
// import { Application } from "https://deno.land/x/abc@v1/mod.ts";
// import type { Context } from "https://deno.land/x/abc@v1/mod.ts";
import { Application } from "./abc/mod.ts";
import type { Context } from "./abc/mod.ts";
import { renderFile } from "https://deno.land/x/dejs/mod.ts";
import * as groups from "./groups/index.ts";
import DBController from "./db/DBController.ts";

// Ugly solution
(async () => await new DBController().init())();

const port = parseInt(Deno.env.get("PORT") || "8080");
const app = new Application();

app.renderer = {
    render<T>(name: string, data: T): Promise<Deno.Reader> {
        return renderFile(name, data);
    },
};

app.static("/", "./src/public/"); // Manage static files
app.get("/", async (c: Context) => await c.render("./src/public/test.html", { name: "test" })); // Render index on /

// Load groups dynamically
// deno-lint-ignore ban-ts-comment
// @ts-ignore
for (let groupName in groups) groups[groupName](app.group(groupName));

app.start({ port });
console.log(`Server listening on http://localhost:${port}`);
