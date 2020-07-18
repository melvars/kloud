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

    async query(query: string, params?: string[]) {
        if (!this.client) throw Error("Database isn't initialized yet!");

        try {
            return await this.client.query(query, params);
        } catch (e) {
            throw e;
        }
    }

    async execute(query: string, params?: string[]) {
        if (!this.client) throw Error("Database isn't initialized yet!");

        try {
            await this.client.execute(query, params);
        } catch (e) {
            throw e;
        }
    }

    async execute_multiple(queries: any[][]) {
        if (!this.client) throw Error("Database isn't initialized yet!");

        try {
            await this.client!.transaction(async (conn) => {
                queries.forEach(async (query) => {
                    await conn.execute(query[0], query[1]);
                });
            });
        } catch (e) {
            throw e;
        }
    }

    async close() {
        if (!this.client) throw Error("Database isn't initialized yet!");

        await this.client!.close();
    }
}
