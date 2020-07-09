import { Client } from "https://deno.land/x/mysql/mod.ts";

export default class Connector {
  async connect(): Promise<Client> {
    try {
      return await new Client().connect({
        hostname: Deno.env.get("DBHost"),
        username: Deno.env.get("DBUser"),
        db: Deno.env.get("DBName"),
        password: Deno.env.get("DBPassword"),
      });
    } catch (e) {
      console.error("Could not connect to database!");
      throw e;
    }
  }
}
