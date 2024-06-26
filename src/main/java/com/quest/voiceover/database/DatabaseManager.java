package com.quest.voiceover.database;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;

import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.DriverManager;

@Slf4j
@Singleton
public class DatabaseManager {

    private static final String SQL_PATH_PREFIX = "jdbc:sqlite:";
    private Connection connection;

    public PreparedStatement prepareStatement(String query) throws SQLException {
        return getDatabaseConnection().prepareStatement(query);
    }

    public void closeConnection() throws SQLException {
        connection.close();
    }

    private Connection getDatabaseConnection() {
        if (connection == null) {
            try {
                String databaseSourceUrl = DatabaseFileManager.getDatabaseSourcePath(DatabaseSource.DATABASE_VERSION);
                connection = DriverManager.getConnection(SQL_PATH_PREFIX + databaseSourceUrl);
                log.info("Quest Voiceover plugin established connection to database.");
            } catch (FileNotFoundException e) {
                log.error("Could not get database source path.", e);
            } catch (SQLException e) {
                log.error("Could not connect to database.", e);
            }
        }
        return connection;
    }
}
