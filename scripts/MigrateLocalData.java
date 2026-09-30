import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.flywaydb.core.Flyway;

/** One-time local H2 snapshot -> empty MySQL techshop. Never replaces existing rows. */
class MigrateLocalData {
    static String quote(String name) {
        if (!name.matches("[a-zA-Z_][a-zA-Z0-9_]*")) throw new IllegalArgumentException("Invalid identifier");
        return "`" + name + "`";
    }
    static long count(Connection c, String table) throws SQLException {
        try (var s = c.createStatement(); var r = s.executeQuery("SELECT COUNT(*) FROM " + quote(table))) { r.next(); return r.getLong(1); }
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Pass an offline H2 snapshot base path");
        if (!Files.isRegularFile(Path.of(args[0] + ".mv.db"))) throw new IllegalArgumentException("Offline H2 snapshot is missing");
        var props = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("data/mysql-local.properties"))) { props.load(reader); }
        String url = props.getProperty("DB_URL"), user = props.getProperty("DB_USERNAME"), password = props.getProperty("DB_PASSWORD");
        if (!url.startsWith("jdbc:mysql://127.0.0.1:3306/techshop?")) throw new IllegalArgumentException("Only local techshop is supported");
        try (var target = DriverManager.getConnection(url,user,password)) {
            try (var tables = target.getMetaData().getTables("techshop", null, "%", new String[]{"TABLE"})) {
                while (tables.next()) if (!tables.getString("TABLE_NAME").equals("flyway_schema_history") && count(target,tables.getString("TABLE_NAME")) != 0)
                    throw new IllegalStateException("Target has data; refusing to import");
            }
            Flyway.configure().dataSource(url,user,password).locations("filesystem:src/main/resources/db/migration").load().migrate();
            try (var source = DriverManager.getConnection("jdbc:h2:file:" + Path.of(args[0]).toAbsolutePath().toString().replace('\\','/') + ";MODE=MySQL;IFEXISTS=TRUE;ACCESS_MODE_DATA=r", "sa", "")) {
                var dependencies = new TreeMap<String,Set<String>>();
                try (var tables = target.getMetaData().getTables("techshop",null,"%",new String[]{"TABLE"})) {
                    while (tables.next()) {
                        String table = tables.getString("TABLE_NAME");
                        if (table.equals("flyway_schema_history")) continue;
                        var parents = new HashSet<String>();
                        try (var keys = target.getMetaData().getImportedKeys("techshop",null,table)) { while(keys.next()) parents.add(keys.getString("PKTABLE_NAME")); }
                        dependencies.put(table,parents);
                    }
                }
                // Abort if a source table would be silently omitted.
                try (var tables = source.getMetaData().getTables(null,"PUBLIC","%",new String[]{"TABLE"})) {
                    while(tables.next()) if (!dependencies.containsKey(tables.getString("TABLE_NAME").toLowerCase(Locale.ROOT)))
                        throw new IllegalStateException("Unmapped source table: " + tables.getString("TABLE_NAME"));
                }
                target.setAutoCommit(false);
                var done = new HashSet<String>();
                try {
                    while (done.size() < dependencies.size()) {
                        boolean progress = false;
                        for (var entry : dependencies.entrySet()) {
                            String table = entry.getKey();
                            if (done.contains(table) || !done.containsAll(entry.getValue())) continue;
                            if (count(target,table) != 0) throw new IllegalStateException("Target is not empty: " + table);
                            copy(source,target,table);
                            done.add(table); progress = true;
                        }
                        if (!progress) throw new IllegalStateException("Circular table dependencies");
                    }
                    target.commit();
                    System.out.println("COMMITTED: " + done.size() + " tables copied and verified; foreign keys stayed enabled.");
                } catch (Exception ex) { target.rollback(); throw ex; }
            }
        }
    }
    static void copy(Connection source,Connection target,String table) throws Exception {
        var columns = new ArrayList<String>();
        try(var r=target.getMetaData().getColumns("techshop",null,table,"%")) { while(r.next()) columns.add(r.getString("COLUMN_NAME")); }
        var sourceColumns = new HashSet<String>();
        try(var r=source.getMetaData().getColumns(null,"PUBLIC",table.toUpperCase(Locale.ROOT),"%")) { while(r.next()) sourceColumns.add(r.getString("COLUMN_NAME").toLowerCase(Locale.ROOT)); }
        if (!sourceColumns.equals(new HashSet<>(columns))) throw new IllegalStateException("Column mismatch: " + table);
        String fields=String.join(",",columns.stream().map(MigrateLocalData::quote).toList());
        String query="SELECT " + fields + " FROM " + quote(table);
        String insert="INSERT INTO " + quote(table) + " (" + fields + ") VALUES (" + String.join(",",Collections.nCopies(columns.size(),"?")) + ")";
        long copied=0;
        try(var s=source.createStatement();var rows=s.executeQuery(query);var write=target.prepareStatement(insert)) {
            while(rows.next()) {
                for(int i=1;i<=columns.size();i++) {
                    Object value=rows.getObject(i);
                    if(value instanceof java.time.OffsetDateTime date) value=Timestamp.from(date.toInstant());
                    if(value instanceof Clob clob) value=clob.getSubString(1,Math.toIntExact(clob.length()));
                    write.setObject(i,value);
                }
                write.executeUpdate();copied++;
            }
        }
        if(count(target,table)!=copied || count(source,table)!=copied) throw new IllegalStateException("Count mismatch: " + table);
        // Compare complete row values (including account hashes and historical snapshots) before commit.
        try(var a=source.createStatement();var b=target.createStatement();var expected=a.executeQuery(query);var actual=b.executeQuery(query)) {
            var types=actual.getMetaData();
            var left=new HashMap<List<String>,Integer>();var right=new HashMap<List<String>,Integer>();
            while(expected.next()) left.merge(row(expected,types),1,Integer::sum);
            while(actual.next()) right.merge(row(actual,types),1,Integer::sum);
            if(!left.equals(right)) throw new IllegalStateException("Value mismatch: " + table);
        }
        System.out.println(table + ": " + copied + " rows verified");
    }
    static List<String> row(ResultSet r,ResultSetMetaData types) throws SQLException {
        var result=new ArrayList<String>();
        for(int i=1;i<=types.getColumnCount();i++) {
            if(r.getObject(i)==null) { result.add(null); continue; }
            result.add(switch(types.getColumnType(i)) {
                case Types.BIT,Types.BOOLEAN -> Boolean.toString(r.getBoolean(i));
                case Types.TIMESTAMP,Types.TIMESTAMP_WITH_TIMEZONE -> r.getTimestamp(i).toInstant().toString();
                case Types.INTEGER,Types.BIGINT,Types.DOUBLE,Types.FLOAT,Types.DECIMAL,Types.NUMERIC,Types.SMALLINT,Types.TINYINT -> r.getBigDecimal(i).stripTrailingZeros().toPlainString();
                default -> r.getString(i);
            });
        }
        return result;
    }
}
