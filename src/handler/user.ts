import type { HandlerFunc, Context } from "https://deno.land/x/abc@v1/mod.ts";
import db from "../db/user.ts";

export const index: HandlerFunc = async (c: Context) => c.params.name;
