package ghostybank.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import ghostybank.GhostyBank;
import ghostybank.data.BankData;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

/**
 * MySQL-basierter Storage mit HikariCP Connection Pool.
 * Aktivierbar in config.yml unter storage.type: mysql
 *
 * Tabelle: ghostybank_players
 *   uuid           VARCHAR(36)  PRIMARY KEY
 *   name           VARCHAR(16)  NOT NULL
 *   balance        DOUBLE       DEFAULT 0
 *   total_interest DOUBLE       DEFAULT 0
 *   level          INT          DEFAULT 1
 */
public class MySQLStorage implements IStorage {

    private final GhostyBank plugin;
    private HikariDataSource dataSource;

    private static final String TABLE = "ghostybank_players";

    public MySQLStorage(GhostyBank plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        ConfigurationSection cfg = plugin.getConfig().getConfigurationSection("storage.mysql");
        if (cfg == null) {
            plugin.getLogger().severe("[MySQLStorage] Fehlende mysql-Konfiguration in config.yml!");
            return;
        }

        HikariConfig hikari = new HikariConfig();
        String host     = cfg.getString("host", "localhost");
        int    port     = cfg.getInt("port", 3306);
        String database = cfg.getString("database", "ghostybank");
        String user     = cfg.getString("username", "root");
        String password = cfg.getString("password", "");
        boolean ssl     = cfg.getBoolean("use-ssl", false);

        hikari.setJdbcUrl(String.format(
            "jdbc:mysql://%s:%d/%s?useSSL=%s&allowPublicKeyRetrieval=true&autoReconnect=true&characterEncoding=utf8",
            host, port, database, ssl
        ));
        hikari.setUsername(user);
        hikari.setPassword(password);
        hikari.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // Pool-Einstellungen
        hikari.setMaximumPoolSize(cfg.getInt("pool-size", 10));
        hikari.setMinimumIdle(2);
        hikari.setConnectionTimeout(30_000);
        hikari.setIdleTimeout(600_000);
        hikari.setMaxLifetime(1_800_000);
        hikari.setPoolName("GhostyBank-Pool");

        hikari.addDataSourceProperty("cachePrepStmts", "true");
        hikari.addDataSourceProperty("prepStmtCacheSize", "250");
        hikari.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        try {
            dataSource = new HikariDataSource(hikari);
            createTable();
            plugin.getLogger().info("[Storage] MySQL-Storage verbunden mit " + host + ":" + port + "/" + database);
        } catch (Exception e) {
            plugin.getLogger().severe("[MySQLStorage] Verbindung fehlgeschlagen: " + e.getMessage());
            plugin.getLogger().severe("[MySQLStorage] Fallback auf YAML wird empfohlen.");
        }
    }

    private void createTable() throws SQLException {
        try (Connection con = getConnection();
             Statement stmt = con.createStatement()) {
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
                "  uuid           VARCHAR(36)  NOT NULL PRIMARY KEY," +
                "  name           VARCHAR(16)  NOT NULL," +
                "  balance        DOUBLE       NOT NULL DEFAULT 0," +
                "  total_interest DOUBLE       NOT NULL DEFAULT 0," +
                "  level_interest DOUBLE       NOT NULL DEFAULT 0," +
                "  level          INT          NOT NULL DEFAULT 1" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
            );
        }
    }

    @Override
    public Collection<BankData> loadAll() {
        Collection<BankData> result = new ArrayList<>();
        if (!isConnected()) return result;

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM " + TABLE);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                UUID   uuid   = UUID.fromString(rs.getString("uuid"));
                String name   = rs.getString("name");
                double bal        = rs.getDouble("balance");
                double inter      = rs.getDouble("total_interest");
                double levelInter = rs.getDouble("level_interest");
                int    level      = rs.getInt("level");
                result.add(new BankData(uuid, name, bal, inter, levelInter, level));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[MySQLStorage] Ladefehler: " + e.getMessage());
        }
        return result;
    }

    @Override
    public void save(BankData data) {
        if (!isConnected()) return;
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(
                "INSERT INTO " + TABLE + " (uuid, name, balance, total_interest, level_interest, level) VALUES (?,?,?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE name=VALUES(name), balance=VALUES(balance), " +
                "total_interest=VALUES(total_interest), level_interest=VALUES(level_interest), level=VALUES(level)"
             )) {
            ps.setString(1, data.getUuid().toString());
            ps.setString(2, data.getPlayerName());
            ps.setDouble(3, data.getBalance());
            ps.setDouble(4, data.getTotalInterestEarned());
            ps.setDouble(5, data.getLevelInterestEarned());
            ps.setInt(6, data.getLevel());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[MySQLStorage] Speicherfehler: " + e.getMessage());
        }
    }

    @Override
    public void saveAll(Collection<BankData> all) {
        if (!isConnected()) return;
        String sql = "INSERT INTO " + TABLE + " (uuid, name, balance, total_interest, level_interest, level) VALUES (?,?,?,?,?,?) " +
                     "ON DUPLICATE KEY UPDATE name=VALUES(name), balance=VALUES(balance), " +
                     "total_interest=VALUES(total_interest), level_interest=VALUES(level_interest), level=VALUES(level)";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            con.setAutoCommit(false);
            for (BankData data : all) {
                ps.setString(1, data.getUuid().toString());
                ps.setString(2, data.getPlayerName());
                ps.setDouble(3, data.getBalance());
                ps.setDouble(4, data.getTotalInterestEarned());
                ps.setDouble(5, data.getLevelInterestEarned());
                ps.setInt(6, data.getLevel());
                ps.addBatch();
            }
            ps.executeBatch();
            con.commit();
            con.setAutoCommit(true);
        } catch (SQLException e) {
            plugin.getLogger().severe("[MySQLStorage] Batch-Speicherfehler: " + e.getMessage());
        }
    }

    @Override
    public void remove(UUID uuid) {
        if (!isConnected()) return;
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(
                "DELETE FROM " + TABLE + " WHERE uuid = ?"
             )) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[MySQLStorage] Löschfehler: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("[MySQLStorage] Verbindung geschlossen.");
        }
    }

    private Connection getConnection() throws SQLException {
        if (dataSource == null) throw new SQLException("DataSource ist null!");
        return dataSource.getConnection();
    }

    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }
}
