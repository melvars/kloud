import "https://deno.land/x/dotenv/load.ts";
import { Application } from "https://deno.land/x/abc@master/mod.ts";
import type { Context } from "https://deno.land/x/abc@master/mod.ts";
import { renderFile } from "https://deno.land/x/dejs/mod.ts";
import * as groups from "./groups/index.ts";
import * as log from "https://deno.land/std/log/mod.ts";
import { handlePath } from "./handler/fileView.ts";
import { render as renderAdmin } from "./handler/admin.ts";
import DBController from "./db/DBController.ts";
import { getCurrentUser } from "./util/user.ts";

new DBController().init().then();

const port = parseInt(Deno.env.get("PORT") || "8080");
const app = new Application();

app.renderer = {
    render<T>(name: string, data: T): Promise<Deno.Reader> {
        return renderFile(name, data);
    },
};

app.static("/", "./src/public/");
app.get("/", async (c: Context) => await c.render("./src/views/index.ejs", {
    files: undefined,
    user: getCurrentUser(c)
}));

app.get("/files/*", handlePath);
app.get("/files/", handlePath);

app.get("/admin/", renderAdmin);

// Load groups dynamically
for (const groupName in groups) (groups as { [key: string]: Function })[groupName](app.group(groupName));

app.start({ port });
log.info(`Server listening on http://localhost:${port}`);
