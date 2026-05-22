"use strict";

let sqlite3Ready = false;
let sqlite3;
const databases = new Map();
const statements = new Map();
let nextDbId = 1;
let nextStmtId = 1;
const pendingMessages = [];

importScripts("sqlite3.js");

self.sqlite3InitModule().then((s) => {
    sqlite3 = s;
    sqlite3Ready = true;
    console.log("SQLite worker ready, version:", sqlite3.version.libVersion);
    pendingMessages.forEach(handleMessage);
    pendingMessages.length = 0;
}).catch((e) => {
    console.error("SQLite init failed:", e);
});

self.onmessage = (event) => {
    if (!sqlite3Ready) {
        pendingMessages.push(event);
    } else {
        handleMessage(event);
    }
};

function handleMessage(event) {
    const { id, data } = event.data;

    try {
        let result = null;
        let shouldRespond = true;

        switch (data.cmd) {
            case "open": {
                let db;
                if (data.fileName === ":memory:") {
                    db = new sqlite3.oo1.DB(data.fileName, "ct");
                    console.log("Opened in-memory database");
                } else {
                    if (sqlite3.opfs) {
                        db = new sqlite3.oo1.OpfsDb(data.fileName, "ct");
                        console.log("Opened OPFS database:", data.fileName);
                    } else {
                        console.warn("OPFS not available, falling back to in-memory database. Data will NOT persist.");
                        db = new sqlite3.oo1.DB(data.fileName, "ct");
                    }
                }
                const dbId = nextDbId++;
                databases.set(dbId, db);
                result = { databaseId: dbId };
                break;
            }

            case "prepare": {
                const db = databases.get(data.databaseId);
                const stmt = db.prepare(data.sql);
                const stmtId = nextStmtId++;
                const columnNames = [];
                const colCount = stmt.columnCount;
                for (let i = 0; i < colCount; i++) {
                    columnNames.push(
                        sqlite3.capi.sqlite3_column_name(stmt.pointer, i)
                    );
                }
                statements.set(stmtId, { stmt, db, lastSql: data.sql });
                result = {
                    statementId: stmtId,
                    parameterCount: stmt.parameterCount,
                    columnNames,
                };
                break;
            }

            case "step": {
                const entry = statements.get(data.statementId);
                const stmt = entry.stmt;
                
                if (data.bindings) {
                    stmt.reset();
                    const bindings = data.bindings;
                    for (let i = 0; i < bindings.length; i++) {
                        stmt.bind(i + 1, bindings[i] ?? null);
                    }
                }
                
                const rows = [];
                const columnTypes = [];
                while (stmt.step()) {
                    const row = [];
                    for (let i = 0; i < stmt.columnCount; i++) {
                        row.push(stmt.get(i));
                        if (rows.length === 0) {
                            columnTypes.push(
                                sqlite3.capi.sqlite3_column_type(stmt.pointer, i)
                            );
                        }
                    }
                    rows.push(row);
                }
                result = { rows, columnTypes };
                break;
            }

            case "close": {
                shouldRespond = false;
                if (data.statementId != null) {
                    const entry = statements.get(data.statementId);
                    if (entry) {
                        entry.stmt.finalize();
                        statements.delete(data.statementId);
                    }
                }
                if (data.databaseId != null) {
                    const db = databases.get(data.databaseId);
                    if (db) {
                        db.close();
                        databases.delete(data.databaseId);
                    }
                }
                break;
            }
        }

        if (shouldRespond) {
            self.postMessage({ id, data: result, error: null });
        }

    } catch (e) {
        self.postMessage({ id, data: null, error: e.message ?? String(e) });
    }
}
