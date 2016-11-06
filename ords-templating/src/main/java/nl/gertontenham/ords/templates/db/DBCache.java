package nl.gertontenham.ords.templates.db;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import oracle.dbtools.plugin.api.di.annotations.Provides;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Caching DB related data
 */
@Provides
public class DBCache {

    private static final long MAX_SIZE = 10;
    private static LoadingCache<String, String> schemaCache =
            CacheBuilder.newBuilder().maximumSize( MAX_SIZE ).build(new CacheLoader<String, String>() {
                @Override
                public String load(String key) throws Exception {
                    return fetchParsingSchema(key);
                }
            });

    private static Connection conn;
    private static Logger logger;

    @Inject
    public DBCache(Logger logger) {
        DBCache.logger = logger;
    }

    public static String currentSchema( String pattern, Connection conn ) {
        DBCache.conn = conn;
        return schemaCache.getUnchecked( pattern );
    }

    private static String fetchParsingSchema(String pattern) {

        String owner = "";
        final String statement = "select parsing_schema " +
                "from user_ords_schemas " +
                "where type = 'BASE_PATH' " +
                "and pattern = ?";


        try {
            PreparedStatement ps = conn.prepareStatement(statement);
            ps.setString(1, pattern);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                owner = rs.getString(1);
            }
            rs.close();
            ps.close();

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error during fetch parsing schema ");
        }

        return owner;
    }
}
