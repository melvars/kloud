import { walk, ensureDirSync, existsSync } from "https://deno.land/std/fs/mod.ts";
import type { Context } from "https://deno.land/x/abc@master/mod.ts";
import { getUserCookies } from "./user.ts";

export const cleanPath = (path: string, uid: number): string => {
    return (
        "data/" +
        uid +
        "/" +
        path
            .replace(/\/files\/?/, "")
            .replace("../", "") // TODO: Fix relative ../
            .replace("./", "")
            .replace(/([^:]\/)\/+/g, "$1")
    );
}

export const getFiles = async (c: Context) => {
    const path = c.path ? c.path : "";
    const uid = getUserCookies(c).uid;
    createUserDirectory(uid);  // TODO: Consider doing this in db/user/createUser => performance?
    const dataPath: string = cleanPath(path, uid);

    if (!existsSync(dataPath)) return [];

    const files = [];
    for await (const entry of walk(dataPath)) {
        files.push(entry.path);
    }
    return files;
}

export const createUserDirectory = (uid: number) => {
    ensureDirSync("data/" + uid);
    // TODO: Give user access to dir
}
