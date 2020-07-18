import DBController from "./DBController.ts";
import { hash, compare, genSalt } from "https://deno.land/x/bcrypt/mod.ts";

class User {
    private controller: DBController;
    constructor() {
        this.controller = new DBController();
    }

    /**
     * Creates new user
     * @param email
     * @param username
     * @param password
     * @param isAdmin
     */
    async createUser(email: string, username: string, password: string, isAdmin = false): Promise<boolean> {
        const salt = await genSalt(12);
        const passwordHash = await hash(password, salt);
        const verification = this.generateId();
        try {
            await this.controller.execute(
                "INSERT INTO users (email, username, password, verification, is_admin) VALUE (?, ?, ?, ?, ?)",
                [email, username, passwordHash, verification, isAdmin]
            );
            return true;
        } catch (e) {
            throw e;
        }
    }

    /**
     * Checks if the user provided password is correct
     * @param username
     * @param plainTextPassword
     */
    async login(username: string, plainTextPassword: string): Promise<loginData> {
        const { uid, password, verification, darkTheme } = (
            await this.controller.query(
                "SELECT id as uid, password, verification, dark_theme as darkTheme FROM users WHERE username = ?",
                [username]
            )
        )[0];
        if (compare(plainTextPassword, password)) {
            return {
                success: true,
                uid,
                darkTheme,
                verification,
            };
        } else {
            return { success: false };
        }
    }

    /**
     * Generate random id
     * @param len
     * @private
     */
    // TODO: Improve
    private generateId(len = 64): string {
        const values = new Uint8Array(len / 2);
        crypto.getRandomValues(values);
        return Array.from(values, (dec) => ("0" + dec.toString(36)).substr(-2)).join("");
    }
}

export default new User();

export interface loginData {
    success: boolean;
    uid?: number;
    verification?: string;
    darkTheme?: string;
}
