package com.example.mpproject.data.local;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

// Explicit Room schema migrations — one constant per version step.
// Using explicit migrations instead of fallbackToDestructiveMigration() means that when the
// schema changes, existing user data is preserved rather than silently wiped.
public class DatabaseMigrations {

    // v2 → v3: adds recurrence support columns to todos and calendar_events.
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
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
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE INDEX IF NOT EXISTS index_todos_recurrenceGroupId ON todos(recurrenceGroupId)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_calendar_events_recurrenceGroupId ON calendar_events(recurrenceGroupId)");
        }
    };

    // v4 → v5: creates assessments table; adds linkedAssessmentId to calendar_events.
    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
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

    // v5 → v6: creates personal note attachments table.
    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `personal_note_attachments` ("
                    + "`attachmentId` TEXT NOT NULL, "
                    + "`noteId` TEXT, "
                    + "`userId` TEXT, "
                    + "`fileName` TEXT, "
                    + "`fileType` TEXT, "
                    + "`fileSizeBytes` INTEGER NOT NULL, "
                    + "`storagePath` TEXT, "
                    + "`downloadUrl` TEXT, "
                    + "`uploadStatus` TEXT, "
                    + "`createdAt` INTEGER NOT NULL, "
                    + "PRIMARY KEY(`attachmentId`), "
                    + "FOREIGN KEY(`noteId`) REFERENCES `personal_notes`(`noteId`) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE)");

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_personal_note_attachments_noteId` "
                    + "ON `personal_note_attachments` (`noteId`)");

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_personal_note_attachments_userId` "
                    + "ON `personal_note_attachments` (`userId`)");
        }
    };

    // v6 → v7: adds note folders and optional folder links on personal/module notes.
    public static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `note_folders` ("
                    + "`folderId` TEXT NOT NULL, "
                    + "`userId` TEXT, "
                    + "`name` TEXT, "
                    + "`createdAt` INTEGER NOT NULL, "
                    + "`updatedAt` INTEGER NOT NULL, "
                    + "PRIMARY KEY(`folderId`))");

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_folders_userId` "
                    + "ON `note_folders` (`userId`)");

            db.execSQL("ALTER TABLE `personal_notes` ADD COLUMN `folderId` TEXT");

            db.execSQL("ALTER TABLE `module_notes` ADD COLUMN `folderId` TEXT");

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_personal_notes_folderId` "
                    + "ON `personal_notes` (`folderId`)");

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_module_notes_folderId` "
                    + "ON `module_notes` (`folderId`)");
        }
    };

    // v7 → v8: creates research papers table for the Research Organiser feature.
    public static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `research_papers` ("
                            + "`paperId` TEXT NOT NULL, "
                            + "`userId` TEXT, "
                            + "`title` TEXT, "
                            + "`authors` TEXT, "
                            + "`year` TEXT, "
                            + "`category` TEXT, "
                            + "`summary` TEXT, "
                            + "`keyFindings` TEXT, "
                            + "`methodology` TEXT, "
                            + "`relevance` TEXT, "
                            + "`fileName` TEXT, "
                            + "`fileUrl` TEXT, "
                            + "`storagePath` TEXT, "
                            + "`status` TEXT, "
                            + "`important` INTEGER NOT NULL, "
                            + "`uploadedAt` INTEGER NOT NULL, "
                            + "`updatedAt` INTEGER NOT NULL, "
                            + "PRIMARY KEY(`paperId`))"
            );

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_research_papers_userId` "
                    + "ON `research_papers` (`userId`)");

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_research_papers_status` "
                    + "ON `research_papers` (`status`)");

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_research_papers_category` "
                    + "ON `research_papers` (`category`)");

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_research_papers_authors` "
                    + "ON `research_papers` (`authors`)");

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_research_papers_year` "
                    + "ON `research_papers` (`year`)");
        }
    };

    private DatabaseMigrations() {}
}