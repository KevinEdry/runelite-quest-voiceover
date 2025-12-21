package com.quest.voiceover.modules.database.functions;

import com.quest.voiceover.utility.LevenshteinUtility;
import lombok.extern.slf4j.Slf4j;
import org.sqlite.Function;

import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
public class LevenshteinFunction {

    public static void register(Connection connection) throws SQLException {
        Function.create(connection, "levenshtein", new Function() {
            @Override
            protected void xFunc() throws SQLException {
                String source = value_text(0);
                String target = value_text(1);

                if (source == null || target == null) {
                    result(Integer.MAX_VALUE);
                    return;
                }

                result(LevenshteinUtility.distance(source, target));
            }
        });

        Function.create(connection, "levenshtein_similarity", new Function() {
            @Override
            protected void xFunc() throws SQLException {
                String source = value_text(0);
                String target = value_text(1);

                if (source == null || target == null) {
                    result(0.0);
                    return;
                }

                result(LevenshteinUtility.similarity(source, target));
            }
        });

        log.debug("Registered Levenshtein SQL functions");
    }
}
