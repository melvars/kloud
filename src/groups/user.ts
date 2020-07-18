import type { Group, Context } from "https://deno.land/x/abc@master/mod.ts";
import * as handlers from "../handler/user.ts";

export default function (g: Group) {
    g.get("/:name", handlers.index);
    g.post("/register", handlers.register);
    g.post("/login", handlers.login);
}
