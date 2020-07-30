import type { HandlerFunc, Context } from "https://deno.land/x/abc@master/mod.ts";
import * as log from "https://deno.land/std/log/mod.ts";
import { isAdmin } from "../util/user.ts";
import { isSetup } from "../util/server.ts";

export const render: HandlerFunc = async (c: Context) => {
    if (await isAdmin(c))
        return await c.render("./src/views/admin.ejs", { initial: false });
    else if (!(await isSetup()))
        return await c.render("./src/views/admin.ejs", { initial: true });
    return c.redirect("/");
}
