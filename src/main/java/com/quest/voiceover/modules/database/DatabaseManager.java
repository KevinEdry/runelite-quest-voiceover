package com.quest.voiceover.modules.database;

import com.quest.voiceover.modules.database.functions.LevenshteinFunction;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.io.FileNotFoundException;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Singleton
public class DatabaseManager {

    private static final String SQL_PATH_PREFIX = "jdbc:sqlite:";

    private Connection connection;

    public void initializeConnection() {
        getConnection();
    }

    public void closeConnection() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    public PreparedStatement prepareStatement(String query) throws SQLException {
        return getConnection().prepareStatement(query);
    }

    public Set<String> getVoicedQuests() {
        Set<String> voicedQuests = new HashSet<>();

        try (PreparedStatement statement = prepareStatement("SELECT DISTINCT quest FROM dialogs");
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                voicedQuests.add(resultSet.getString("quest"));
            }
        } catch (SQLException e) {
            log.error("Failed to query voiced quests", e);
        }

        return voicedQuests;
    }

    private Connection getConnection() {
        if (connection != null) {
            return connection;
        }

        try {
            Class.forName("org.sqlite.JDBC");
            String databasePath = DatabaseVersionManager.getDatabasePath();
            connection = DriverManager.getConnection(SQL_PATH_PREFIX + databasePath);
            LevenshteinFunction.register(connection);
            log.info("Established connection to voiceover database");
        } catch (FileNotFoundException e) {
            log.error("Database file not found", e);
        } catch (SQLException e) {
            log.error("Failed to connect to database", e);
        } catch (ClassNotFoundException e) {
            log.error("SQLite JDBC driver not found", e);
            throw new RuntimeException(e);
        }

        return connection;
    }
}
