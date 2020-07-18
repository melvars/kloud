import "https://deno.land/x/dotenv/load.ts";
import { assertThrowsAsync, assert } from "https://deno.land/std/testing/asserts.ts";
import { Client } from "https://deno.land/x/mysql/mod.ts";
import DBController from "../src/db/DBController.ts";

const controller = new DBController();

Deno.test("database connection", async () => {
    await controller.connect();
});

Deno.test({
    name: "database initialization",
    sanitizeResources: false,
    async fn() {
        await controller.init();
    },
});

Deno.test({
    name: "database table creation",
    sanitizeResources: false,
    async fn() {
        await controller.execute("DROP TABLE IF EXISTS test");
        await controller.execute("CREATE TABLE test (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(16) UNIQUE)");
    },
});

Deno.test({
    name: "database variable arguments",
    sanitizeResources: false,
    async fn() {
        await controller.execute("INSERT INTO test(name) VALUES(?)", ["Melvin"]);
        assertThrowsAsync(
            () => controller.execute("INSERT INTO test(name) VALUES(?)", ["Melvin"]),
            Error,
            "Duplicate entry 'Melvin' for key 'name'"
        );
        await controller.execute("INSERT INTO test(name) VALUES(?)", ["LarsVomMars"]);
    },
});

Deno.test({
    name: "database multiple statements",
    sanitizeResources: false,
    async fn() {
        await controller.execute_multiple([
            ["DELETE FROM test WHERE ?? = ?", ["name", "Melvin"]],
            ["INSERT INTO test(name) VALUES(?)", ["Melvin"]],
        ]);
    },
});

Deno.test({
    name: "database select statements",
    sanitizeResources: false,
    async fn() {
        const element = await controller.query("SELECT ?? FROM ?? WHERE id=?", ["name", "test", "4"]);
        assert(element[0].name == "Melvin");
    },
});

Deno.test({
    name: "database select statements",
    sanitizeResources: false,
    async fn() {
        await controller.execute("DROP TABLE test");
        await controller.close();
    },
});
