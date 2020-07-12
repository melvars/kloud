import { Client } from "https://deno.land/x/mysql/mod.ts";
import tables from "./tables.ts";

export default class DBController {
  async init() {
    const conn = await this.connect();
    try {
      tables.forEach((table) => conn.execute(table));
      console.log("Tables created");
    } catch (e) {
      console.error("Could not create tables");
      throw e;
    } finally {
      conn.close();
    }
  }

  async connect(): Promise<Client> {
    try {
      return await new Client().connect({
        hostname: Deno.env.get("DBHost"),
        username: Deno.env.get("DBUser"),
        db: Deno.env.get("DBName"),
        password: Deno.env.get("DBPassword"),
      });
    } catch (e) {
      console.error("Could not connect to database");
      throw e;
    }
  }
}
