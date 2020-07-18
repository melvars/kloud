import { ensureDirSync } from "https://deno.land/std/fs/mod.ts";
import { walk } from "https://deno.land/std/fs/mod.ts";

const TEMP_USER_ID = 42; // TODO: FIX

export const cleanPath = (path: string): string => {
    createUserDirectory(TEMP_USER_ID);

    return (
        "data/" +
        TEMP_USER_ID +
        "/" +
        path
            .replace("/files/", "")
            .replace("../", "") // TODO: Fix relative ../
            .replace("./", "")
            .replace(/([^:]\/)\/+/g, "$1")
    );
};

export const getFiles = async (path: string) => {
    const newPath = path ? path : "";

    createUserDirectory(TEMP_USER_ID);
    const dataPath: string = cleanPath(newPath);
    console.log(dataPath);

    const files = [];
    for await (const entry of walk(dataPath)) {
        files.push(entry.path);
    }
    return files;
};

export const createUserDirectory = (uid: number) => {
    ensureDirSync("data/" + uid);
    // TODO: Give user access to dir
};
