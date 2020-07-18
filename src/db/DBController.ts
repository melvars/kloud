import { Client } from "https://deno.land/x/mysql/mod.ts";
import { readFileStr } from "https://deno.land/std/fs/mod.ts";

export default class DBController {
    private client?: Client;

    async init() {
        this.client = await this.connect();
        try {
            const sql = await readFileStr("./src/db/tables.sql");
            const queries = sql.split(";");
            queries.pop();
            queries.forEach(async (query) => await this.execute(query));
            console.log("Tables created");
        } catch (e) {
            console.error("Could not create tables");
            throw e;
        } finally {
            this.client.close();
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

    async execute(query: string) {
        if (this.client) {
            try {
                return await this.client.execute(query);
            } catch (e) {
                throw e;
            }
        } else throw Error("Database isn't initialized yet!");
    }

    async execute_multiple(queries: string[]) {
        await this.client!.transaction(async (conn) => {
            queries.forEach(async (query) => {
                await conn.execute(query);
            });
        });
    }

    async close() {
        await this.client!.close();
    }
}
