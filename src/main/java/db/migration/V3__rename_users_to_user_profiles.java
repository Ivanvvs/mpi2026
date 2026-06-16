package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class V3__rename_users_to_user_profiles extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (tableExists(connection, "users") && !tableExists(connection, "user_profiles")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE users RENAME TO user_profiles");
            }
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        return tableExistsWithCase(connection, tableName)
                || tableExistsWithCase(connection, tableName.toUpperCase())
                || tableExistsWithCase(connection, tableName.toLowerCase());
    }

    private boolean tableExistsWithCase(Connection connection, String tableName) throws SQLException {
        try (ResultSet tables = connection.getMetaData().getTables(null, null, tableName, new String[]{"TABLE"})) {
            return tables.next();
        }
    }
}
