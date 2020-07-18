import "https://deno.land/x/dotenv/load.ts";
import { Application } from "https://deno.land/x/abc@master/mod.ts";
import type { Context } from "https://deno.land/x/abc@master/mod.ts";
import { renderFile } from "https://deno.land/x/dejs/mod.ts";
import * as groups from "./groups/index.ts";
import DBController from "./db/DBController.ts";

let dbc: DBController;
// Ugly solution
(async () => {
    dbc = new DBController();
    await dbc.init();
})();

const port = parseInt(Deno.env.get("PORT") || "8080");
const app = new Application();

app.renderer = {
    render<T>(name: string, data: T): Promise<Deno.Reader> {
        return renderFile(name, data);
    },
};

app.static("/", "./src/public/"); // Manage static files - TODO: Consider serving css/js files separately
app.get("/", async (c: Context) => await c.render("./src/public/test.html", { name: "test" })); // Render index on /

// Load groups dynamically
// deno-lint-ignore ban-ts-comment
// @ts-ignore
for (let groupName in groups) groups[groupName](app.group(groupName));

app.start({ port });
console.log(`Server listening on http://localhost:${port}`);
