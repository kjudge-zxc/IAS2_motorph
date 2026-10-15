package DAOs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data access for the security audit log.
 *
 * Append-only by design: this class exposes no update or delete operation.
 * Audit records are written once and are never modified by the application.
 */

public class AuditLogDAO {

    public static final String LOGIN_SUCCESS        = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILED         = "LOGIN_FAILED";
    public static final String LOGOUT               = "LOGOUT";
    public static final String PASSWORD_CHANGED     = "PASSWORD_CHANGED";
    public static final String ACCOUNT_CREATED      = "ACCOUNT_CREATED";
    public static final String ACCOUNT_DEACTIVATED  = "ACCOUNT_DEACTIVATED";
    public static final String ACCOUNT_ACTIVATED    = "ACCOUNT_ACTIVATED";
    public static final String AUTH_ERROR           = "AUTH_ERROR";
    public static final String ACCOUNT_LOCKED       = "ACCOUNT_LOCKED";

    private static final String INSERT_SQL =
        "INSERT INTO audit_log (eventTime, email, employeeId, eventType, outcome, details) " +
        "VALUES (CONVERT_TZ(UTC_TIMESTAMP(), '+00:00', '+08:00'), ?, ?, ?, ?, ?)";

    private static final String RECENT_SQL =
        "SELECT auditId, eventTime, email, employeeId, eventType, outcome, details " +
        "FROM audit_log ORDER BY eventTime DESC, auditId DESC LIMIT ?";

    /**
     * Writes one security event. Returns false on failure but never throws:
     * a logging problem must not block the action being logged.
     */
    public boolean log(String email, Integer employeeId, String eventType,
                       boolean success, String details) {

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL)) {

            if (email == null) stmt.setNull(1, Types.VARCHAR);
            else stmt.setString(1, email);

            if (employeeId == null) stmt.setNull(2, Types.INTEGER);
            else stmt.setInt(2, employeeId);

            stmt.setString(3, eventType);
            stmt.setString(4, success ? "SUCCESS" : "FAILURE");

            if (details == null) stmt.setNull(5, Types.VARCHAR);
            else stmt.setString(5, details.length() > 255
                    ? details.substring(0, 255) : details);

            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            System.err.println("[AUDIT] Failed to write audit record: " + e.getMessage());
            return false;
        }
    }

    /** Most recent events, newest first. Used for verification and reporting. */
    public List<Map<String, Object>> findRecent(int limit) {
        List<Map<String, Object>> rows = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(RECENT_SQL)) {

            stmt.setInt(1, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("auditId",    rs.getInt("auditId"));
                    row.put("eventTime",  rs.getTimestamp("eventTime"));
                    row.put("email",      rs.getString("email"));
                    row.put("employeeId", rs.getObject("employeeId"));
                    row.put("eventType",  rs.getString("eventType"));
                    row.put("outcome",    rs.getString("outcome"));
                    row.put("details",    rs.getString("details"));
                    rows.add(row);
                }
            }
        } catch (Exception e) {
            System.err.println("[AUDIT] Failed to read audit log: " + e.getMessage());
        }
        return rows;
    }
}