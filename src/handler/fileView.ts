import type { HandlerFunc, Context } from "https://deno.land/x/abc@master/mod.ts";
import { getFiles } from "../util/files.ts";

export const handlePath: HandlerFunc = async (c: Context) => {
    return await c.render("./src/views/index.html", { files: await getFiles(c.path) });
};
