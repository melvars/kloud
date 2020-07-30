import type { Group } from "https://deno.land/x/abc@master/mod.ts";
import * as handlers from "../handler/user.ts";

export default function (g: Group) {
    g.get("/login", handlers.renderLogin);
    g.post("/register", handlers.register);
    g.post("/login", handlers.login);
    g.any("/logout", handlers.logout);
    g.put("/theme", handlers.changeTheme);
    g.put("/password", handlers.updatePassword);
}
