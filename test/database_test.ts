import "https://deno.land/x/dotenv/load.ts";
import { assertThrowsAsync } from "https://deno.land/std/testing/asserts.ts";
import { Client } from "https://deno.land/x/mysql/mod.ts";
import DBController from "../src/db/DBController.ts";

const controller = new DBController();

Deno.test("database connection", async () => {
    await controller.connect();
});

Deno.test({
    name: "database initialization",
    sanitizeOps: false, // TODO: Find async leak in controller.execute!
    async fn() {
        await controller.init();
    },
});

Deno.test({
    name: "database table creation",
    sanitizeOps: false, // TODO: Previous bug!
    sanitizeResources: false, // TODO: Previous bug!
    async fn() {
        await controller.execute("DROP TABLE IF EXISTS test");
        await controller.execute("CREATE TABLE test (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(16) UNIQUE)");
    },
});

Deno.test({
    name: "database variable arguments",
    sanitizeOps: false, // TODO: Previous bug!
    sanitizeResources: false, // TODO: Previous bug!
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
    sanitizeOps: false, // TODO: Previous bug!
    sanitizeResources: false, // TODO: Previous bug!
    async fn() {
        await controller.execute_multiple([
            ["DELETE FROM test WHERE ?? = ?", ["name", "Melvin"]],
            ["INSERT INTO test(name) VALUES(?)", ["Melvin"]],
        ]);
    },
});

Deno.test({
    name: "database select statements",
    sanitizeOps: false, // TODO: Previous bug!
    sanitizeResources: false, // TODO: Previous bug!
    async fn() {
        const count = await controller.query("SELECT ?? FROM ?? WHERE gd=4", ["name", "test"]);
        // TODO: WHY DOESN'T THIS WORK?
        console.log(count);
    },
});

Deno.test("database close", async () => {
    await controller.execute("DROP TABLE test");
    await controller.close(); // TODO: Fix 'Bad resource ID'!
});
