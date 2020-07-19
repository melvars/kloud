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
     * Returns the user with the uid and verification
     * TODO: Tests
     * @param uid
     * @param userVerification
     */
    async getUserByVerificationId(uid: number, userVerification: string): Promise<userData | undefined> {
        try {
            const user = (
                await this.controller.query(
                    "SELECT id, email, username, verification, dark_theme darkTheme, is_admin isAdmin FROM users WHERE id = ? AND verification = ?",
                    [uid, userVerification]
                )
            )[0];
            if (user) return user as userData;
        } catch (e) {
            throw e;
        }
    }

    /**
     * Changes the user theme
     * @param uid
     */
    async changeTheme(uid: number) {
        try {
            await this.controller.execute("UPDATE users SET dark_theme = NOT dark_theme WHERE id = ?", [uid]);
        } catch (e) {
            throw e;
        }
    }

    /**
     * Sets admin status of a user
     * @param uid
     * @param isAdmin
     */
    async setAdminState(uid: number, isAdmin: boolean) {
        try {
            await this.controller.execute("UPDATE users SET is_admin = ? WHERE id = ?", [isAdmin, uid]);
        } catch (e) {
            throw e;
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
    darkTheme?: boolean;
}

export interface userData {
    id: number;
    email: string;
    username: string;
    verification: string;
    darkTheme: boolean;
    isAdmin: boolean;
}
