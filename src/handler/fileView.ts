import type { HandlerFunc, Context } from "https://deno.land/x/abc@master/mod.ts";
import { getFiles } from "../util/files.ts";
import { getCurrentUser } from "../util/user.ts";

export const handlePath: HandlerFunc = async (c: Context) => {
    if (!(await getCurrentUser(c))) return c.redirect("/user/login");  // TODO: Handle shared files
    return await c.render("./src/views/index.ejs", { files: await getFiles(c) });
}
