import type { HandlerFunc, Context } from "https://deno.land/x/abc@master/mod.ts";
import db, { loginData } from "../db/user.ts";
import * as log from "https://deno.land/std/log/mod.ts";
import { getCurrentUser, isAdmin } from "../util/user.ts";
import { isSetup } from "../util/server.ts";
import { deleteCookie } from "https://deno.land/std/http/cookie.ts";


export const register: HandlerFunc = async (c: Context) => {
    if (!(await isAdmin(c)) && await isSetup()) return { success: false }; // I'm tired: not sure if this works
    // TODO: How to handle register
    const { username, email, password, admin } = await c.body();
    try {
        const success = await db.createUser(email, username, password, admin !== undefined ? admin : false);
        return { success };
    } catch (e) {
        return { success: false };
    }
}
export const renderLogin: HandlerFunc = async (c: Context) => {
    if (await getCurrentUser(c)) return c.redirect("/");
    return await c.render("./src/views/login.ejs");
}
export const login: HandlerFunc = async (c: Context) => {
    const { username, password } = await c.body();
    try {
        const data: loginData = await db.login(username, password);
        if (data.success) {
            c.setCookie({
                name: "uid",
                value: data.uid!.toString(),
                path: "/",
            });
            c.setCookie({
                name: "verification",
                value: data.verification!,
                path: "/",
            });
        }
        return { success: data.success };
    } catch (e) {
        log.error(e);
        return { success: false };
    }
}
export const logout: HandlerFunc = async (c: Context) => {
    deleteCookie(c.response, "uid");
    deleteCookie(c.response, "verification");
    c.redirect("/");
}
export const changeTheme: HandlerFunc = async (c: Context) => {
    try {
        const currentUser = await getCurrentUser(c);
        if (!currentUser) return { success: false };
        await db.changeTheme(currentUser.id);
        return { success: true };
    } catch (e) {
        log.error(e);
        return { success: false };
    }
}
export const setAdmin: HandlerFunc = async (c: Context) => {
    const { uid, state } = await c.body();
    try {
        const currentUser = await getCurrentUser(c);
        if (!(currentUser && currentUser.isAdmin)) return { success: false };
        await db.setAdminState(uid, state);
        return { success: true };
    } catch (e) {
        log.error(e);
        return { success: false };
    }
}
export const updatePassword: HandlerFunc = async (c: Context) => {
    const currentUser = await getCurrentUser(c);
    if (!currentUser) return { success: false };
    const { currentPassword, newPassword } = await c.body();
    try {
        await db.changePassword(currentUser.id, currentPassword, newPassword);
        return { success: true };
    } catch (e) {
        log.error(e);
        return { success: false };
    }
}
