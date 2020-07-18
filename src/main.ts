import "https://deno.land/x/dotenv/load.ts";
import { Application } from "https://deno.land/x/abc@master/mod.ts";
import type { Context } from "https://deno.land/x/abc@master/mod.ts";
import { renderFile } from "https://deno.land/x/dejs/mod.ts";
import * as groups from "./groups/index.ts";
import { handlePath } from "./handler/fileView.ts";
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

app.static("/public/", "./src/public/"); // Manage static files
app.get("/", async (c: Context) => await c.render("./src/views/index.html")); // Render index on /

app.get("/files/*", handlePath);

// Load groups dynamically
// deno-lint-ignore ban-ts-comment
// @ts-ignore
for (let groupName in groups) groups[groupName](app.group(groupName));

app.start({ port });
console.log(`Server listening on http://localhost:${port}`);
