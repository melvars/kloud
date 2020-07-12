import type { Group, Context } from "https://deno.land/x/abc@v1/mod.ts";
import * as handlers from "../handler/user.ts";

export default function (g: Group) {
  g.get("/:name", handlers.index);
}
