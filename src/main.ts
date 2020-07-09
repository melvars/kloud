import { Application, Context } from "https://deno.land/x/abc@v1/mod.ts";
import { renderFile } from "https://deno.land/x/dejs/mod.ts";
import "https://deno.land/x/dotenv/load.ts";

const port = parseInt(Deno.env.get("PORT") || "8080");
const app = new Application();

app.renderer = {
  render<T>(name: string, data: T): Promise<Deno.Reader> {
    return renderFile(name, data);
  },
};

app.static("/", "./src/public/"); // Manage static files
app.get("/", async (c: Context) => await c.render("./src/public/index.html")); // Render index on /

app.start({ port });
console.log(`Server listening on http://localhost:${port}`);
