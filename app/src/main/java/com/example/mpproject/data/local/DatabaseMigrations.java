package com.example.mpproject.data.local;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

// Explicit Room schema migrations — one constant per version step.
// Using explicit migrations instead of fallbackToDestructiveMigration() means that when the
// schema changes, existing user data is preserved rather than silently wiped.
public class DatabaseMigrations {

    // v2 → v3: adds recurrence support columns to todos and calendar_events.
    // All three columns are nullable so existing rows keep their data and receive NULL for the new fields.
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `todos` ADD COLUMN `recurrencePattern` TEXT");
            db.execSQL("ALTER TABLE `todos` ADD COLUMN `recurrenceGroupId` TEXT");
            db.execSQL("ALTER TABLE `todos` ADD COLUMN `recurrenceEndDate` INTEGER");

            db.execSQL("ALTER TABLE `calendar_events` ADD COLUMN `recurrencePattern` TEXT");
            db.execSQL("ALTER TABLE `calendar_events` ADD COLUMN `recurrenceGroupId` TEXT");
            db.execSQL("ALTER TABLE `calendar_events` ADD COLUMN `recurrenceEndDate` INTEGER");
        }
    };

    // v3 → v4: adds indices on recurrenceGroupId for faster group delete/update queries.
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE INDEX IF NOT EXISTS index_todos_recurrenceGroupId ON todos(recurrenceGroupId)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_calendar_events_recurrenceGroupId ON calendar_events(recurrenceGroupId)");
        }
    };

    // v4 → v5: creates assessments table; adds linkedAssessmentId to calendar_events.
    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS assessments ("
                    + "assessmentId TEXT NOT NULL PRIMARY KEY, "
                    + "moduleId TEXT, "
                    + "userId TEXT, "
                    + "title TEXT, "
                    + "assessmentType TEXT, "
                    + "weightingPercent REAL NOT NULL DEFAULT 0, "
                    + "scoreMode TEXT, "
                    + "scoreAchieved REAL, "
                    + "scoreMaximum REAL, "
                    + "calendarEventId TEXT, "
                    + "dueDate INTEGER, "
                    + "notes TEXT, "
                    + "createdAt INTEGER NOT NULL DEFAULT 0, "
                    + "updatedAt INTEGER NOT NULL DEFAULT 0, "
                    + "FOREIGN KEY(moduleId) REFERENCES modules(moduleId) ON DELETE CASCADE)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_assessments_moduleId ON assessments(moduleId)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_assessments_userId ON assessments(userId)");
            db.execSQL("ALTER TABLE calendar_events ADD COLUMN linkedAssessmentId TEXT");
        }
    };

    private DatabaseMigrations() {}
}
