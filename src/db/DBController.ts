import { Client } from "https://deno.land/x/mysql/mod.ts";
import * as log from "https://deno.land/std/log/mod.ts";

export default class DBController {
    private client?: Client;

    async init() {
        await this.connect();
        try {
            const sql = await Deno.readTextFile("./src/db/tables.sql");
            const queries = sql.split(";");
            queries.pop();
            for (const query of queries) await this.execute(query);
            log.info("Tables created");
        } catch (e) {
            log.error("Could not create tables");
            throw e;
        }
    }

    async connect(): Promise<Client> {
        try {
            this.client = await new Client().connect({
                hostname: Deno.env.get("DBHost"),
                username: Deno.env.get("DBUser"),
                db: Deno.env.get("DBName"),
                password: Deno.env.get("DBPassword"),
            });
            return this.client;
        } catch (e) {
            log.error("Could not connect to database");
            throw e;
        }
    }

    async query(query: string, params?: (boolean | number | string)[]) {
        if (!this.client) await this.connect();

        try {
            return await this.client!.query(query, params);
        } catch (e) {
            throw e;
        }
    }

    async execute(query: string, params?: (boolean | number | string)[]) {
        if (!this.client) await this.connect();

        try {
            return await this.client!.execute(query, params);
        } catch (e) {
            throw e;
        }
    }

    async execute_multiple(queries: ((boolean | number | string)[] | string)[][]) {
        if (!this.client) await this.connect();

        try {
            await this.client!.transaction(async (conn) => {
                for (const query of queries) await conn.execute(query[0] as string, query[1] as string[]); // ez
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
