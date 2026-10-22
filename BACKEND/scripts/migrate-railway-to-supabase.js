import pg from "pg";

const { Client } = pg;
const sourceUrl = process.env.RAILWAY_DATABASE_URL;
const targetUrl = process.env.SUPABASE_DATABASE_URL;
const targetSchema = process.env.DB_SCHEMA || "vybora_backend";

if (!sourceUrl || !targetUrl) throw new Error("Configure RAILWAY_DATABASE_URL and SUPABASE_DATABASE_URL as GitHub Actions secrets.");
if (!/^[a-z_][a-z0-9_]*$/.test(targetSchema)) throw new Error("Invalid DB_SCHEMA.");

const tables = [
  "users",
  "posts",
  "follows",
  "post_likes",
  "comments",
  "messages",
  "notifications",
  "user_files",
  "user_private_profiles",
];

const source = new Client({ connectionString: sourceUrl, ssl: { rejectUnauthorized: true }, application_name: "vybora-migration-source" });
const target = new Client({ connectionString: targetUrl, ssl: { rejectUnauthorized: true }, application_name: "vybora-migration-target" });
const ident = (s) => '"' + String(s).replaceAll('"', '""') + '"';
const qualified = (schema, table) => ident(schema) + "." + ident(table);

try {
  await source.connect();
  await target.connect();

  const sourceVersion = await source.query("SELECT version()");
  const targetVersion = await target.query("SELECT version()");
  console.log("Source and target PostgreSQL connections established.");
  console.log("Source:", sourceVersion.rows[0].version.split(" ").slice(0, 2).join(" "));
  console.log("Target:", targetVersion.rows[0].version.split(" ").slice(0, 2).join(" "));

  const sourceCounts = {};
  for (const table of tables) {
    const exists = await source.query(
      "SELECT 1 FROM information_schema.tables WHERE table_schema='public' AND table_name=$1 AND table_type='BASE TABLE'",
      [table],
    );
    if (!exists.rowCount) throw new Error("Missing source table public." + table + "; stop without changing either database.");
    const count = await source.query("SELECT count(*)::bigint AS n FROM " + qualified("public", table));
    sourceCounts[table] = Number(count.rows[0].n);
  }
  console.log("Source counts:", JSON.stringify(sourceCounts));

  const targetSchemaExists = await target.query("SELECT 1 FROM information_schema.schemata WHERE schema_name=$1", [targetSchema]);
  if (!targetSchemaExists.rowCount) throw new Error("Target schema does not exist: " + targetSchema + ". Run the documented schema migrations first.");

  for (const table of tables) {
    const exists = await target.query(
      "SELECT 1 FROM information_schema.tables WHERE table_schema=$1 AND table_name=$2 AND table_type='BASE TABLE'",
      [targetSchema, table],
    );
    if (!exists.rowCount) throw new Error("Missing target table " + targetSchema + "." + table + "; stop without copying.");
    const count = await target.query("SELECT count(*)::bigint AS n FROM " + qualified(targetSchema, table));
    if (Number(count.rows[0].n) !== 0) {
      throw new Error("Target table " + targetSchema + "." + table + " is not empty. No data was copied; refusing to overwrite or merge.");
    }
  }

  await target.query("BEGIN");
  try {
    for (const table of tables) {
      const sourceColumns = await source.query(
        "SELECT column_name FROM information_schema.columns WHERE table_schema='public' AND table_name=$1 ORDER BY ordinal_position",
        [table],
      );
      const targetColumns = await target.query(
        "SELECT column_name FROM information_schema.columns WHERE table_schema=$1 AND table_name=$2 ORDER BY ordinal_position",
        [targetSchema, table],
      );
      const sourceSet = new Set(sourceColumns.rows.map((r) => r.column_name));
      const targetSet = new Set(targetColumns.rows.map((r) => r.column_name));
      const missingInTarget = [...sourceSet].filter((c) => !targetSet.has(c));
      if (missingInTarget.length) throw new Error("Schema mismatch for " + table + "; target missing: " + missingInTarget.join(", "));
      const columns = sourceColumns.rows.map((r) => r.column_name);
      const select = await source.query("SELECT " + columns.map(ident).join(", ") + " FROM " + qualified("public", table));
      if (select.rows.length) {
        const names = columns.map(ident).join(", ");
        const values = "(" + columns.map((_, i) => "$" + (i + 1)).join(", ") + ")";
        const insert = "INSERT INTO " + qualified(targetSchema, table) + " (" + names + ") VALUES " + values;
        for (const row of select.rows) {
          await target.query(insert, columns.map((c) => row[c]));
        }
      }
      console.log(table + ": copied " + select.rows.length + " rows");
    }

    for (const table of tables) {
      const count = await target.query("SELECT count(*)::bigint AS n FROM " + qualified(targetSchema, table));
      if (Number(count.rows[0].n) !== sourceCounts[table]) {
        throw new Error("Count mismatch in " + table + ": source=" + sourceCounts[table] + ", target=" + count.rows[0].n);
      }
    }
    await target.query("COMMIT");
    console.log("SUCCESS: table counts match; target transaction committed.");
  } catch (error) {
    await target.query("ROLLBACK");
    throw error;
  }
} finally {
  await Promise.allSettled([source.end(), target.end()]);
}
