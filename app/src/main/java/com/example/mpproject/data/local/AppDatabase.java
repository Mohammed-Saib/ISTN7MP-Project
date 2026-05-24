package com.example.mpproject.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.mpproject.data.local.dao.AssessmentDao;
import com.example.mpproject.data.local.dao.CalendarEventDao;
import com.example.mpproject.data.local.dao.ModuleDao;
import com.example.mpproject.data.local.dao.ModuleNoteDao;
import com.example.mpproject.data.local.dao.PersonalNoteDao;
import com.example.mpproject.data.local.dao.TodoDao;
import com.example.mpproject.data.local.dao.UserDao;
import com.example.mpproject.data.local.entity.AssessmentEntity;
import com.example.mpproject.data.local.entity.CalendarEventEntity;
import com.example.mpproject.data.local.entity.ModuleEntity;
import com.example.mpproject.data.local.entity.ModuleNoteEntity;
import com.example.mpproject.data.local.entity.PersonalNoteEntity;
import com.example.mpproject.data.local.entity.TodoEntity;
import com.example.mpproject.data.local.entity.UserEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Version bumped to 5 — adds assessments table; adds linkedAssessmentId to calendar_events
@Database(
        entities = {
                UserEntity.class,
                ModuleEntity.class,
                ModuleNoteEntity.class,
                PersonalNoteEntity.class,
                TodoEntity.class,
                CalendarEventEntity.class,
                AssessmentEntity.class
        },
        version = 5,
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDao userDao();
    public abstract ModuleDao moduleDao();
    public abstract ModuleNoteDao moduleNoteDao();
    public abstract PersonalNoteDao personalNoteDao();
    public abstract TodoDao todoDao();
    public abstract CalendarEventDao calendarEventDao();
    public abstract AssessmentDao assessmentDao();

    private static volatile AppDatabase INSTANCE;

    // 4-thread pool keeps DB writes off the main thread without excessive parallelism
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(4);

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "notesapp_database")
                            // Explicit migration preserves user data across schema upgrades.
                            // If a device somehow has a version older than 2, Room will throw
                            // IllegalStateException — the user must reinstall. Data is in Firestore
                            // and will be restored from the cloud on first login.
                            .addMigrations(
                                    DatabaseMigrations.MIGRATION_2_3,
                                    DatabaseMigrations.MIGRATION_3_4,
                                    DatabaseMigrations.MIGRATION_4_5)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
