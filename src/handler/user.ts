import type { HandlerFunc, Context } from "https://deno.land/x/abc@master/mod.ts";
// import type { HandlerFunc, Context } from "../abc/mod.ts";
import db from "../db/user.ts";

export const index: HandlerFunc = async (c: Context) => c.params.name;
