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
        const verification = User.generateId();
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
        try {
            const { uid, password, verification, darkTheme } = (
                await this.controller.query(
                    "SELECT id uid, password, verification, dark_theme darkTheme FROM users WHERE username = ?",
                    [username]
                )
            )[0];  // Will throw an error if user does not exist => good to go?
            if (!compare(plainTextPassword, password)) return { success: false };
            return {
                success: true,
                uid,
                darkTheme,
                verification,
            };
        } catch (e) {
            throw e;
        }
    }

    /**
     * Returns the user with the uid and verification
     * TODO: Tests
     * @param uid
     * @param userVerification
     */
    async getUserByVerificationId(uid?: number, userVerification?: string): Promise<userData | undefined> {
        try {
            if (!uid || !userVerification || uid < 1 || userVerification.length !== 64) throw new TypeError("Wrong parameters");
            const user = (
                await this.controller.query(
                    "SELECT id, email, username, verification, dark_theme darkTheme, is_admin isAdmin FROM users WHERE id = ? AND verification = ?",
                    [uid, userVerification]
                )
            )[0];
            return user as userData;
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
     * Gets user theme
     * @param uid
     */
    async getUserTheme(uid: number): Promise<boolean> {
        try {
            const users = await this.controller.query("SELECT dark_theme FROM users WHERE id = ?", [uid]);
            if (users.length > 0) return users[0].dark_theme;
            return true;
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
     *
     * @param {number} uid
     * @returns {Promise<boolean>}
     */
    async isAdmin(uid: number): Promise<boolean> {
        try {
            const user = (await this.controller.query("SELECT is_admin FROM users WHERE id = ?", [uid]))[0];
            return user.is_admin;
        } catch (e) {
            throw e;
        }
    }

    /**
     *
     * @param {number} uid
     * @param {string} currentPassword
     * @param {string} newPassword
     * @returns {Promise<void>}
     */
    async changePassword(uid: number, currentPassword: string, newPassword: string) {
        try {
            const userPassword = (await this.controller.query("SELECT password FROM users WHERE id = ?", [uid]))[0];
            if (!compare(currentPassword, userPassword)) throw new Error("Passwords do not match");
            const salt = await genSalt(12);
            const passwordHash = await hash(newPassword, salt);
            await this.controller.execute("UPDATE users SET password = ? WHERE id = ?", [passwordHash, uid]);
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
    static generateId(len = 64): string {
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
