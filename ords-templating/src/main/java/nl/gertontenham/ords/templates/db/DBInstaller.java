package nl.gertontenham.ords.templates.db;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import oracle.dbtools.plugin.api.di.annotations.Provides;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Installing Database objects in context db schema
 */
@Provides
public class DBInstaller {

    public static void migrate(Connection conn) throws SQLException {
        Statement stmt = null;
        try {
            URL path = Resources.getResource("db/install-types.sql");
            String script = Resources.toString(path, Charsets.UTF_8);
            final String[] statements = script.split("\\$\\$");

            stmt = conn.createStatement();
            for (String sql: statements) {
                String[] sqlLines = sql.trim()
                                       .split("\n");
                StringBuffer sb = new StringBuffer();
                for (String line: sqlLines) {
                    sb.append(line + "\n ");
                }
                stmt.addBatch(sb.toString());
            }
            stmt.executeBatch();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (stmt != null) { stmt.close(); }
        }


    }
}
