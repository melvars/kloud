import type { Group, Context } from "https://deno.land/x/abc@master/mod.ts";
// import type { Group, Context } from "../abc/mod.ts";
import * as handlers from "../handler/user.ts";

export default function (g: Group) {
    g.get("/:name", handlers.index);
}