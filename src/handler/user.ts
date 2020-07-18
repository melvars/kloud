import type { HandlerFunc, Context } from "https://deno.land/x/abc@master/mod.ts";
import db from "../db/user.ts";

export const index: HandlerFunc = async (c: Context) => c.params.name;
export const register: HandlerFunc = async (c: Context) => {
    const { username, email, password } = await c.body();
    await db.createUser(email, username, password);

}
