import DBController from "../db/DBController.ts";
import * as log from "https://deno.land/std/log/mod.ts";

const controller = new DBController();

export const isSetup = async (): Promise<boolean> => {
    try {
        const users = await controller.query("SELECT id FROM users");
        return users.length > 0
    } catch (e) {
        log.error(e);
        return false;
    }
}
