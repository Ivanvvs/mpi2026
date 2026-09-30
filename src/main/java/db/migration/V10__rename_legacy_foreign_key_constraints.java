package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Renames foreign keys that were generated before Flyway managed the schema.
 * A clean schema already has the target names, so this migration is a no-op there.
 */
public class V10__rename_legacy_foreign_key_constraints extends BaseJavaMigration {

    private static final List<ConstraintRename> RENAMES = List.of(
            new ConstraintRename("exam_attempts", "fkm3w9a2c525i6nn0ljg04jx6pl", "fk_exam_attempt_session"),
            new ConstraintRename("exam_attempts", "fkg2vsf7tmpfsrdprupd8gecl6x", "fk_exam_attempt_student"),
            new ConstraintRename("exam_results", "fkocuw1c7kan2gn41yceen3imak", "fk_exam_result_session"),
            new ConstraintRename("exam_results", "fkt2jcn29o332cpiv7s7h3o877e", "fk_exam_result_user"),
            new ConstraintRename("exam_sessions", "fk41sytq23h3ws1rop7sxdikmss", "fk_exam_sessions_created_by"),
            new ConstraintRename("exam_sessions", "fk71kfjb2fn4ui5h7bhv2c148d4", "fk_exam_sessions_class"),
            new ConstraintRename("privilege_requests", "fkd3cp8yrbaugim9un5e8m6k006", "fk_privilege_requests_requested_by"),
            new ConstraintRename("privilege_requests", "fkcojlmm851j6fsri45bsskeb45", "fk_privilege_requests_class"),
            new ConstraintRename("secret_votings", "fkayql6yc4upxoq18c5wgpnhpou", "fk_secret_votings_created_by"),
            new ConstraintRename("secret_votings", "fkkiklb2ktphghjm9b4x09q9evi", "fk_secret_votings_class"),
            new ConstraintRename("user_profiles", "fkqeaew9eyrchk6yiua5q83kl1w", "fk_users_account"),
            new ConstraintRename("user_profiles", "fknn16x6b0t9rgy795hsj5h8cry", "fk_users_class"),
            new ConstraintRename("voting_options", "fkmdp4ekid9s3mae0k327t2si7t", "fk_voting_options_voting"),
            new ConstraintRename("voting_receipts", "fk1j0lf7g2lrbg8matwkmlvw9qc", "fk_voting_receipt_voting"),
            new ConstraintRename("voting_receipts", "fk110dd36reqlig2phekoenh57c", "fk_voting_receipt_student")
    );

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!connection.getMetaData().getDatabaseProductName().equalsIgnoreCase("PostgreSQL")) {
            return;
        }

        for (ConstraintRename rename : RENAMES) {
            if (constraintExists(connection, rename.tableName(), rename.legacyName())
                    && !constraintExists(connection, rename.tableName(), rename.targetName())) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("ALTER TABLE " + rename.tableName()
                            + " RENAME CONSTRAINT " + rename.legacyName()
                            + " TO " + rename.targetName());
                }
            }
        }
    }

    private boolean constraintExists(Connection connection, String tableName, String constraintName) throws SQLException {
        String query = """
                SELECT 1
                FROM pg_constraint constraint_info
                JOIN pg_class table_info ON table_info.oid = constraint_info.conrelid
                JOIN pg_namespace schema_info ON schema_info.oid = table_info.relnamespace
                WHERE schema_info.nspname = 'public'
                  AND table_info.relname = ?
                  AND constraint_info.conname = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, tableName);
            statement.setString(2, constraintName);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private record ConstraintRename(String tableName, String legacyName, String targetName) {
    }
}
