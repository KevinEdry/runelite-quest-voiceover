package com.quest.voiceover.database;

import lombok.Getter;

/**
 * We have to use a hard coded version control of the database source name because this file contains every quest's dialog
 * with it's FTS counterpart.
 */
@Getter
public enum DatabaseSource {
    DATABASE_VERSION("quest_voiceover_v1.db");

    private final String resourceName;

    DatabaseSource(String resourceName) {
        this.resourceName = resourceName;
    }

}
