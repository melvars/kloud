import type { HandlerFunc, Context } from "https://deno.land/x/abc@master/mod.ts";
import { cleanPath } from "../util/files.ts";

export const handlePath: HandlerFunc = async (c: Context) => {
    return await c.render("./src/views/index.html", { path: cleanPath(c.path) });
};
