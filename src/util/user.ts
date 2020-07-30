import type { userData } from "../db/user.ts";
import db from "../db/user.ts";
import * as log from "https://deno.land/std/log/mod.ts";
import type { Context } from "https://deno.land/x/abc@master/mod.ts";

export const getCurrentUser = async (c: Context): Promise<userData | undefined> => {
    const cookies = getUserCookies(c);
    try {
        return await db.getUserByVerificationId(cookies.uid, cookies.verification) as userData;
    } catch (e) {
        log.error(e);
        return undefined;
    }
}

export const getUserCookies = (c: Context): userCookies => {
    const uid = parseInt(c.cookies["uid"]);
    const verification = c.cookies["verification"];
    return { uid, verification };
}

export const isAdmin = async (c: Context): Promise<boolean> => {
    const user = await getCurrentUser(c);
    return (user && user.isAdmin) as boolean;
}

export interface userCookies {
    uid: number;
    verification: string;
}
