package com.example.mpproject.data.local;

import android.content.Context;

import com.example.mpproject.data.local.entity.AssessmentEntity;
import com.example.mpproject.data.local.entity.CalendarEventEntity;
import com.example.mpproject.data.local.entity.ModuleEntity;
import com.example.mpproject.data.local.entity.ModuleNoteEntity;
import com.example.mpproject.data.local.entity.NoteFolderEntity;
import com.example.mpproject.data.local.entity.PersonalNoteAttachmentEntity;
import com.example.mpproject.data.local.entity.PersonalNoteEntity;
import com.example.mpproject.data.local.entity.ResearchPaperEntity;
import com.example.mpproject.data.local.entity.TodoEntity;
import com.example.mpproject.data.local.entity.UserEntity;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Seeds the built-in demo account so the app can be opened and explored without registering
 * and hand-entering a semester's worth of data first. Everything below is fictional.
 *
 * <p>The account belongs to "Dean Winchester", an Information Systems &amp; Technology Honours
 * student at UKZN, and covers every feature the app has: modules with assessments and weightings,
 * a recurring lecture timetable, tasks at each priority, module and personal notes with real files
 * on disk, and a research paper library.
 *
 * <p>Dates are generated relative to the moment of seeding rather than hardcoded, so the dashboard,
 * calendar, and overdue highlighting look current whenever the app is first launched.
 *
 * <p>Seeding runs once: it is skipped when a user with {@link #DEMO_EMAIL} already exists, so a
 * demo account the user has since edited is never overwritten.
 */
public final class DemoDataSeeder {

    public static final String DEMO_EMAIL = "dean.winchester@stu.ukzn.ac.za";
    public static final String DEMO_PASSWORD = "demo1234";

    private static final String USER_ID = "demo-user-dean-winchester";

    // Stable module ids so assessments, notes, tasks, and events can reference them directly.
    private static final String MOD_MOBILE   = "demo-module-istn7mp";
    private static final String MOD_BI       = "demo-module-istn7bi";
    private static final String MOD_SECURITY = "demo-module-istn7is";
    private static final String MOD_RESEARCH = "demo-module-istn7rp";
    private static final String MOD_METHODS  = "demo-module-istn7rm";
    private static final String MOD_ENTSYS   = "demo-module-istn7es";

    private DemoDataSeeder() {}

    /** Populates the demo account on a background thread if it is not already present. */
    public static void seedIfNeeded(final Context context) {
        final Context appContext = context.getApplicationContext();
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(appContext);
            if (db.userDao().getByEmailSync(DEMO_EMAIL) != null) return;
            seed(appContext, db);
        });
    }

    private static void seed(Context context, AppDatabase db) {
        long now = System.currentTimeMillis();

        db.userDao().insert(demoUser(now));

        List<ModuleEntity> modules = modules(now);
        for (ModuleEntity module : modules) db.moduleDao().insert(module);

        for (AssessmentEntity assessment : assessments(now)) db.assessmentDao().insert(assessment);
        for (TodoEntity todo : todos(now)) db.todoDao().insert(todo);
        db.calendarEventDao().insertAll(calendarEvents(now));

        for (NoteFolderEntity folder : noteFolders(now)) db.noteFolderDao().insert(folder);
        for (PersonalNoteEntity note : personalNotes(now)) db.personalNoteDao().insert(note);
        for (ModuleNoteEntity note : moduleNotes(context, now)) db.moduleNoteDao().insert(note);
        for (PersonalNoteAttachmentEntity attachment : attachments(context, now)) {
            db.personalNoteAttachmentDao().insert(attachment);
        }
        for (ResearchPaperEntity paper : researchPapers(context, now)) {
            db.researchPaperDao().insert(paper);
        }
    }

    // ---------------------------------------------------------------- user

    private static UserEntity demoUser(long now) {
        UserEntity user = new UserEntity(
                USER_ID,
                "Dean",
                "Winchester",
                DEMO_EMAIL,
                PasswordHasher.sha256(DEMO_PASSWORD),
                daysFrom(-210, 9, 0)); // registered at the start of the academic year
        user.setSchool("School of Management, IT & Governance, UKZN");
        return user;
    }

    // ------------------------------------------------------------- modules

    private static List<ModuleEntity> modules(long now) {
        List<ModuleEntity> modules = new ArrayList<>();

        modules.add(module(MOD_MOBILE, "Mobile Programming", "ISTN7MP",
                "Prof. R. Singer", "singerr@ukzn.ac.za", "Tue 13:00 - 15:00, MTB Room 4-12",
                "#2E75B6", currentSemester(), false, now));

        modules.add(module(MOD_BI, "Business Intelligence & Analytics", "ISTN7BI",
                "Dr. E. Harvelle", "harvellee@ukzn.ac.za", "Wed 10:00 - 12:00, Shepstone 3-05",
                "#7E57C2", currentSemester(), false, now));

        modules.add(module(MOD_SECURITY, "Information Security Management", "ISTN7IS",
                "Dr. C. Novak", "novakc@ukzn.ac.za", "Thu 11:00 - 12:30, MTB Room 2-08",
                "#E67E22", currentSemester(), false, now));

        modules.add(module(MOD_RESEARCH, "IS&T Honours Research Project", "ISTN7RP",
                "Prof. J. Mills", "millsj@ukzn.ac.za", "By appointment, MTB Room 5-21",
                "#2E9E6B", academicYear() + " Full Year", false, now));

        // Completed last semester - these show up under the Archived tab.
        modules.add(module(MOD_METHODS, "Research Methodology in IS", "ISTN7RM",
                "Dr. M. Moseley", "moseleym@ukzn.ac.za", "Mon 09:00 - 11:00, Shepstone 2-14",
                "#8D6E63", previousSemester(), true, now));

        modules.add(module(MOD_ENTSYS, "Enterprise Systems", "ISTN7ES",
                "Dr. R. Turner", "turnerr@ukzn.ac.za", "Fri 14:00 - 16:00, MTB Room 3-01",
                "#C0392B", previousSemester(), true, now));

        return modules;
    }

    private static ModuleEntity module(String id, String name, String code, String lecturer,
                                       String lecturerEmail, String officeHours, String color,
                                       String semester, boolean archived, long now) {
        ModuleEntity module = new ModuleEntity();
        module.setModuleId(id);
        module.setUserId(USER_ID);
        module.setName(name);
        module.setModuleCode(code);
        module.setLecturerName(lecturer);
        module.setLecturerEmail(lecturerEmail);
        module.setLecturerOfficeHours(officeHours);
        module.setColor(color);
        module.setSemester(semester);
        module.setArchived(archived);
        module.setCreatedAt(archived ? daysFrom(-200, 9, 0) : daysFrom(-40, 9, 0));
        module.setUpdatedAt(now);
        return module;
    }

    // --------------------------------------------------------- assessments

    // Weightings add up to 100% per module, which is what the module detail screen validates against.
    private static List<AssessmentEntity> assessments(long now) {
        List<AssessmentEntity> list = new ArrayList<>();

        // Mobile Programming - two practicals marked, project and exam still ahead.
        list.add(assessment("demo-asm-mp-1", MOD_MOBILE, "Practical 1: Room & MVVM Setup",
                "Practical", 10, "RAW", 84.0, 100.0, daysFrom(-27, 23, 59),
                "Lost marks on the DAO query tests.", now));
        list.add(assessment("demo-asm-mp-2", MOD_MOBILE, "Practical 2: Navigation & Fragments",
                "Practical", 15, "RAW", 78.0, 100.0, daysFrom(-13, 23, 59),
                "Bottom navigation back stack was the tricky part.", now));
        list.add(assessment("demo-asm-mp-3", MOD_MOBILE, "Group Project: Academic Weapon App",
                "Project", 40, "RAW", null, 100.0, daysFrom(9, 23, 59),
                "Demo to Prof. Singer in the Tuesday practical slot. Report due with the APK.", now));
        list.add(assessment("demo-asm-mp-4", MOD_MOBILE, "Final Exam",
                "Exam", 35, "PERCENTAGE", null, null, daysFrom(83, 9, 0),
                "Covers lifecycle, Room, MVVM and Navigation.", now));

        // Business Intelligence - one assignment back, one due this week.
        list.add(assessment("demo-asm-bi-1", MOD_BI, "Assignment 1: Data Warehouse Design",
                "Assignment", 20, "RAW", 88.0, 100.0, daysFrom(-20, 23, 59),
                "Star schema design for a retail case.", now));
        list.add(assessment("demo-asm-bi-2", MOD_BI, "Assignment 2: Power BI Dashboard",
                "Assignment", 20, "RAW", null, 100.0, daysFrom(5, 23, 59),
                "Build a sales dashboard from the provided dataset. Submit .pbix plus a 3 page write-up.", now));
        list.add(assessment("demo-asm-bi-3", MOD_BI, "Case Study Presentation",
                "Presentation", 15, "PERCENTAGE", null, null, daysFrom(19, 10, 0),
                "15 minutes plus questions.", now));
        list.add(assessment("demo-asm-bi-4", MOD_BI, "Final Exam",
                "Exam", 45, "PERCENTAGE", null, null, daysFrom(88, 9, 0), null, now));

        // Information Security - the weakest mark so far, gap analysis due in a few days.
        list.add(assessment("demo-asm-is-1", MOD_SECURITY, "Test 1: Risk & Governance",
                "Test", 25, "RAW", 71.0, 100.0, daysFrom(-16, 23, 59),
                "Need to revise the risk treatment section before the exam.", now));
        list.add(assessment("demo-asm-is-2", MOD_SECURITY, "Assignment: ISO 27001 Gap Analysis",
                "Assignment", 25, "RAW", null, 100.0, daysFrom(3, 23, 59),
                "Pick an organisation and assess it against Annex A controls.", now));
        list.add(assessment("demo-asm-is-3", MOD_SECURITY, "Final Exam",
                "Exam", 50, "PERCENTAGE", null, null, daysFrom(90, 14, 0), null, now));

        // Honours research project - runs the full year.
        list.add(assessment("demo-asm-rp-1", MOD_RESEARCH, "Research Proposal",
                "Proposal", 15, "RAW", 82.0, 100.0, daysFrom(-58, 23, 59),
                "Approved with minor revisions to the sampling strategy.", now));
        list.add(assessment("demo-asm-rp-2", MOD_RESEARCH, "Literature Review Chapter",
                "Chapter", 20, "RAW", null, 100.0, daysFrom(12, 23, 59),
                "Minimum 30 sources, at least 20 peer reviewed.", now));
        list.add(assessment("demo-asm-rp-3", MOD_RESEARCH, "Draft Dissertation",
                "Draft", 25, "PERCENTAGE", null, null, daysFrom(45, 23, 59), null, now));
        list.add(assessment("demo-asm-rp-4", MOD_RESEARCH, "Final Dissertation & Defence",
                "Dissertation", 40, "PERCENTAGE", null, null, daysFrom(79, 23, 59),
                "Hard deadline, no extensions.", now));

        // Archived modules, fully marked.
        list.add(assessment("demo-asm-rm-1", MOD_METHODS, "Critique Assignment",
                "Assignment", 30, "RAW", 79.0, 100.0, daysFrom(-150, 23, 59), null, now));
        list.add(assessment("demo-asm-rm-2", MOD_METHODS, "Research Design Portfolio",
                "Portfolio", 30, "RAW", 85.0, 100.0, daysFrom(-120, 23, 59), null, now));
        list.add(assessment("demo-asm-rm-3", MOD_METHODS, "Final Exam",
                "Exam", 40, "PERCENTAGE", 74.0, null, daysFrom(-95, 9, 0), null, now));

        list.add(assessment("demo-asm-es-1", MOD_ENTSYS, "SAP Configuration Practical",
                "Practical", 25, "RAW", 91.0, 100.0, daysFrom(-155, 23, 59), null, now));
        list.add(assessment("demo-asm-es-2", MOD_ENTSYS, "ERP Implementation Case Study",
                "Assignment", 25, "RAW", 80.0, 100.0, daysFrom(-125, 23, 59), null, now));
        list.add(assessment("demo-asm-es-3", MOD_ENTSYS, "Final Exam",
                "Exam", 50, "PERCENTAGE", 68.0, null, daysFrom(-92, 9, 0),
                "Ran out of time on the last question.", now));

        return list;
    }

    private static AssessmentEntity assessment(String id, String moduleId, String title, String type,
                                               double weighting, String scoreMode, Double achieved,
                                               Double maximum, Long dueDate, String notes, long now) {
        AssessmentEntity assessment = new AssessmentEntity();
        assessment.setAssessmentId(id);
        assessment.setModuleId(moduleId);
        assessment.setUserId(USER_ID);
        assessment.setTitle(title);
        assessment.setAssessmentType(type);
        assessment.setWeightingPercent(weighting);
        assessment.setScoreMode(scoreMode);
        assessment.setScoreAchieved(achieved);
        assessment.setScoreMaximum(maximum);
        assessment.setDueDate(dueDate);
        assessment.setNotes(notes);
        assessment.setCreatedAt(daysFrom(-40, 9, 0));
        assessment.setUpdatedAt(now);
        return assessment;
    }

    // --------------------------------------------------------------- tasks

    private static List<TodoEntity> todos(long now) {
        List<TodoEntity> list = new ArrayList<>();

        list.add(todo("demo-todo-1", MOD_SECURITY, "Finish ISO 27001 gap analysis write-up",
                "Sections 4 and 5 still outstanding. Map each finding back to an Annex A control.",
                daysFrom(2, 20, 0), "HIGH", false, now));
        list.add(todo("demo-todo-2", MOD_BI, "Clean the sales dataset for the Power BI dashboard",
                "Drop the duplicate order lines and fix the date column types before modelling.",
                daysFrom(1, 18, 0), "HIGH", false, now));
        list.add(todo("demo-todo-3", MOD_MOBILE, "Write the Room migration tests",
                "Cover v8 to v9 and v9 to v10 so the group project does not lose data on upgrade.",
                daysFrom(4, 17, 0), "HIGH", false, now));
        list.add(todo("demo-todo-4", MOD_RESEARCH, "Summarise 5 more papers for the literature review",
                "Aim for 30 sources total. Focus on mobile adoption in developing countries.",
                daysFrom(6, 20, 0), "MEDIUM", false, now));
        list.add(todo("demo-todo-5", MOD_MOBILE, "Fix the calendar recurrence bug in the group project",
                "Weekly events skip an occurrence when the month rolls over.",
                daysFrom(3, 16, 0), "MEDIUM", false, now));
        list.add(todo("demo-todo-6", MOD_RESEARCH, "Book supervisor meeting with Prof. Mills",
                "Take the updated chapter outline and the revised timeline.",
                daysFrom(0, 15, 0), "MEDIUM", false, now));
        list.add(todo("demo-todo-7", MOD_BI, "Rewatch the dimensional modelling lecture recording",
                "Week 2, from the 40 minute mark - slowly changing dimensions.",
                daysFrom(7, 19, 0), "LOW", false, now));
        list.add(todo("demo-todo-8", null, "Renew library book: Design Science Research",
                "Due back at the Westville branch.",
                daysFrom(8, 12, 0), "LOW", false, now));

        // Overdue - shows the red highlighting on the task list and dashboard.
        list.add(todo("demo-todo-9", MOD_SECURITY, "Email Dr. Novak about the test 1 memo",
                "Ask for the marking memo for question 3.",
                daysFrom(-2, 12, 0), "MEDIUM", false, now));

        // Recurring weekly study block.
        TodoEntity weekly = todo("demo-todo-10", MOD_RESEARCH, "Weekly research journal entry",
                "One page on what moved forward this week and what is blocked.",
                daysFrom(5, 20, 0), "MEDIUM", false, now);
        weekly.setRecurrencePattern("WEEKLY");
        weekly.setRecurrenceGroupId("demo-todo-group-journal");
        weekly.setRecurrenceEndDate(daysFrom(90, 20, 0));
        list.add(weekly);

        // Completed - populates the Completed filter.
        list.add(todo("demo-todo-11", MOD_MOBILE, "Submit practical 2 on Moodle",
                null, daysFrom(-13, 23, 59), "HIGH", true, now));
        list.add(todo("demo-todo-12", MOD_BI, "Draw the star schema for assignment 1",
                null, daysFrom(-22, 20, 0), "HIGH", true, now));
        list.add(todo("demo-todo-13", MOD_RESEARCH, "Revise proposal sampling strategy",
                "Prof. Mills wanted purposive sampling justified properly.",
                daysFrom(-50, 20, 0), "HIGH", true, now));
        list.add(todo("demo-todo-14", null, "Register for semester 2 modules",
                null, daysFrom(-45, 12, 0), "MEDIUM", true, now));

        return list;
    }

    private static TodoEntity todo(String id, String moduleId, String title, String description,
                                   Long dueDate, String priority, boolean completed, long now) {
        TodoEntity todo = new TodoEntity();
        todo.setTodoId(id);
        todo.setUserId(USER_ID);
        todo.setModuleId(moduleId);
        todo.setTitle(title);
        todo.setDescription(description);
        todo.setDueDate(dueDate);
        todo.setPriority(priority);
        todo.setCompleted(completed);
        if (completed && dueDate != null) todo.setCompletedAt(dueDate);
        todo.setCreatedAt(daysFrom(-30, 9, 0));
        todo.setUpdatedAt(now);
        return todo;
    }

    // ------------------------------------------------------------ calendar

    private static List<CalendarEventEntity> calendarEvents(long now) {
        List<CalendarEventEntity> list = new ArrayList<>();

        // Weekly timetable. Occurrences are stored as individual rows sharing a recurrenceGroupId,
        // which is the same shape CalendarViewModel writes when a user creates a recurring event.
        addWeeklySeries(list, "demo-evt-mp-lecture", MOD_MOBILE, "ISTN7MP Lecture",
                "MTB Room 4-12", "LECTURE", "#2E75B6", Calendar.TUESDAY, 13, 0, 15, 0, now);
        addWeeklySeries(list, "demo-evt-mp-prac", MOD_MOBILE, "ISTN7MP Practical",
                "Computer LAN 2", "LECTURE", "#2E75B6", Calendar.THURSDAY, 14, 0, 16, 0, now);
        addWeeklySeries(list, "demo-evt-bi-lecture", MOD_BI, "ISTN7BI Lecture",
                "Shepstone 3-05", "LECTURE", "#7E57C2", Calendar.WEDNESDAY, 10, 0, 12, 0, now);
        addWeeklySeries(list, "demo-evt-is-lecture", MOD_SECURITY, "ISTN7IS Lecture",
                "MTB Room 2-08", "LECTURE", "#E67E22", Calendar.THURSDAY, 11, 0, 12, 30, now);
        addWeeklySeries(list, "demo-evt-rp-meeting", MOD_RESEARCH, "Supervisor meeting - Prof. Mills",
                "MTB Room 5-21", "PERSONAL", "#2E9E6B", Calendar.FRIDAY, 9, 0, 10, 0, now);

        // Deadlines, linked back to the assessment that owns them.
        list.add(linkedEvent("demo-evt-due-is", MOD_SECURITY, "demo-asm-is-2",
                "ISO 27001 Gap Analysis due", "Submit on Moodle before 23:59.",
                daysFrom(3, 23, 59), "ASSIGNMENT_DUE", "#E67E22", now));
        list.add(linkedEvent("demo-evt-due-bi", MOD_BI, "demo-asm-bi-2",
                "Power BI Dashboard due", "Upload the .pbix file and the write-up.",
                daysFrom(5, 23, 59), "ASSIGNMENT_DUE", "#7E57C2", now));
        list.add(linkedEvent("demo-evt-due-mp", MOD_MOBILE, "demo-asm-mp-3",
                "Group Project demo & submission", "Live demo in the practical slot.",
                daysFrom(9, 23, 59), "ASSIGNMENT_DUE", "#2E75B6", now));
        list.add(linkedEvent("demo-evt-due-rp", MOD_RESEARCH, "demo-asm-rp-2",
                "Literature Review Chapter due", "Email to Prof. Mills and upload to Moodle.",
                daysFrom(12, 23, 59), "ASSIGNMENT_DUE", "#2E9E6B", now));
        list.add(linkedEvent("demo-evt-pres-bi", MOD_BI, "demo-asm-bi-3",
                "BI Case Study Presentation", "15 minutes plus questions.",
                daysFrom(19, 10, 0), "ASSIGNMENT_DUE", "#7E57C2", now));

        // Exams.
        list.add(linkedEvent("demo-evt-exam-mp", MOD_MOBILE, "demo-asm-mp-4",
                "ISTN7MP Final Exam", "Venue to be confirmed.",
                daysFrom(83, 9, 0), "EXAM", "#2E75B6", now));
        list.add(linkedEvent("demo-evt-exam-bi", MOD_BI, "demo-asm-bi-4",
                "ISTN7BI Final Exam", "Venue to be confirmed.",
                daysFrom(88, 9, 0), "EXAM", "#7E57C2", now));
        list.add(linkedEvent("demo-evt-exam-is", MOD_SECURITY, "demo-asm-is-3",
                "ISTN7IS Final Exam", "Venue to be confirmed.",
                daysFrom(90, 14, 0), "EXAM", "#E67E22", now));

        // Personal.
        list.add(personalEvent("demo-evt-study-group", MOD_BI, "BI study group",
                "Meet at the postgrad commons, bring the dataset.",
                daysFrom(2, 16, 0), daysFrom(2, 18, 0), now));
        list.add(personalEvent("demo-evt-honours-seminar", MOD_RESEARCH, "Honours seminar series",
                "Guest talk: research ethics in IS fieldwork.",
                daysFrom(6, 12, 0), daysFrom(6, 13, 0), now));
        list.add(personalEvent("demo-evt-library", null, "Library workshop: Mendeley & referencing",
                "Level 3 training room.",
                daysFrom(4, 15, 0), daysFrom(4, 16, 30), now));

        return list;
    }

    private static void addWeeklySeries(List<CalendarEventEntity> list, String idPrefix,
                                        String moduleId, String title, String description,
                                        String type, String color, int dayOfWeek,
                                        int startHour, int startMinute,
                                        int endHour, int endMinute, long now) {
        final int weeksBack = 5;
        final int weeksAhead = 11;

        Calendar cursor = Calendar.getInstance();
        cursor.set(Calendar.HOUR_OF_DAY, startHour);
        cursor.set(Calendar.MINUTE, startMinute);
        cursor.set(Calendar.SECOND, 0);
        cursor.set(Calendar.MILLISECOND, 0);
        cursor.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        cursor.add(Calendar.WEEK_OF_YEAR, -weeksBack);

        String groupId = idPrefix + "-group";
        List<CalendarEventEntity> series = new ArrayList<>();
        long seriesEnd = 0L;

        for (int week = 0; week <= weeksBack + weeksAhead; week++) {
            Calendar end = (Calendar) cursor.clone();
            end.set(Calendar.HOUR_OF_DAY, endHour);
            end.set(Calendar.MINUTE, endMinute);
            seriesEnd = end.getTimeInMillis();

            CalendarEventEntity event = new CalendarEventEntity();
            event.setEventId(idPrefix + "-" + week);
            event.setUserId(USER_ID);
            event.setModuleId(moduleId);
            event.setTitle(title);
            event.setDescription(description);
            event.setStartTime(cursor.getTimeInMillis());
            event.setEndTime(seriesEnd);
            event.setType(type);
            event.setColor(color);
            event.setRecurrencePattern("WEEKLY");
            event.setRecurrenceGroupId(groupId);
            event.setCreatedAt(daysFrom(-40, 9, 0));
            event.setUpdatedAt(now);
            series.add(event);

            cursor.add(Calendar.WEEK_OF_YEAR, 1);
        }

        // The whole series shares the end date of its last occurrence.
        for (CalendarEventEntity event : series) event.setRecurrenceEndDate(seriesEnd);
        list.addAll(series);
    }

    private static CalendarEventEntity linkedEvent(String id, String moduleId, String assessmentId,
                                                   String title, String description, long start,
                                                   String type, String color, long now) {
        CalendarEventEntity event = new CalendarEventEntity();
        event.setEventId(id);
        event.setUserId(USER_ID);
        event.setModuleId(moduleId);
        event.setTitle(title);
        event.setDescription(description);
        event.setStartTime(start);
        event.setEndTime(start);
        event.setType(type);
        event.setColor(color);
        event.setLinkedAssessmentId(assessmentId);
        event.setCreatedAt(daysFrom(-40, 9, 0));
        event.setUpdatedAt(now);
        return event;
    }

    private static CalendarEventEntity personalEvent(String id, String moduleId, String title,
                                                     String description, long start, long end,
                                                     long now) {
        CalendarEventEntity event = new CalendarEventEntity();
        event.setEventId(id);
        event.setUserId(USER_ID);
        event.setModuleId(moduleId);
        event.setTitle(title);
        event.setDescription(description);
        event.setStartTime(start);
        event.setEndTime(end);
        event.setType("PERSONAL");
        event.setCreatedAt(daysFrom(-20, 9, 0));
        event.setUpdatedAt(now);
        return event;
    }

    // --------------------------------------------------------------- notes

    private static final String FOLDER_RESEARCH = "demo-folder-research";
    private static final String FOLDER_EXAM     = "demo-folder-exam-prep";
    private static final String FOLDER_LECTURE  = "demo-folder-lecture-summaries";
    private static final String FOLDER_ADMIN    = "demo-folder-admin";

    private static List<NoteFolderEntity> noteFolders(long now) {
        List<NoteFolderEntity> list = new ArrayList<>();
        list.add(folder(FOLDER_RESEARCH, "Research Project", now));
        list.add(folder(FOLDER_EXAM, "Exam Prep", now));
        list.add(folder(FOLDER_LECTURE, "Lecture Summaries", now));
        list.add(folder(FOLDER_ADMIN, "Admin", now));
        return list;
    }

    private static NoteFolderEntity folder(String id, String name, long now) {
        NoteFolderEntity folder = new NoteFolderEntity();
        folder.setFolderId(id);
        folder.setUserId(USER_ID);
        folder.setName(name);
        folder.setCreatedAt(daysFrom(-60, 9, 0));
        folder.setUpdatedAt(now);
        return folder;
    }

    private static List<PersonalNoteEntity> personalNotes(long now) {
        List<PersonalNoteEntity> list = new ArrayList<>();

        list.add(personalNote("demo-note-1", MOD_RESEARCH, FOLDER_RESEARCH, true,
                "Proposal feedback from Prof. Mills",
                "Approved with minor revisions.\n\n"
                        + "1. Justify purposive sampling properly - cite Etikan et al.\n"
                        + "2. Research question 2 is too broad, narrow it to a single adoption factor.\n"
                        + "3. Add a paragraph on ethical clearance (HSSREC application goes in next month).\n"
                        + "4. Timeline needs a buffer before the draft deadline.\n\n"
                        + "Next meeting: bring the revised chapter outline.",
                daysFrom(-8, 14, 30)));

        list.add(personalNote("demo-note-2", MOD_RESEARCH, FOLDER_RESEARCH, false,
                "Literature review - working outline",
                "1. Introduction and scope of the review\n"
                        + "2. Technology acceptance models\n"
                        + "   - TAM (Davis, 1989)\n"
                        + "   - UTAUT (Venkatesh et al., 2003)\n"
                        + "   - Criticisms and extensions\n"
                        + "3. Mobile adoption in developing economies\n"
                        + "4. Student productivity tools - what has actually been measured\n"
                        + "5. Gaps in the literature\n"
                        + "6. Conceptual framework\n\n"
                        + "Currently at 24 sources, 18 peer reviewed. Need 6 more.",
                daysFrom(-3, 20, 15)));

        list.add(personalNote("demo-note-3", MOD_MOBILE, FOLDER_EXAM, true,
                "MVVM vs MVC - exam notes",
                "MVC: controller handles input, updates model, selects view. On Android the Activity "
                        + "ends up being both controller and view, which is why it bloats.\n\n"
                        + "MVVM: the ViewModel exposes observable state (LiveData) and knows nothing about "
                        + "the View. The View subscribes. Survives configuration changes because the "
                        + "ViewModel is scoped to the lifecycle owner, not the Activity instance.\n\n"
                        + "Likely exam question: explain why the ViewModel must not hold a reference to a "
                        + "Context or a View - memory leak on rotation.",
                daysFrom(-5, 19, 0)));

        list.add(personalNote("demo-note-4", MOD_BI, FOLDER_EXAM, false,
                "Star schema vs snowflake schema",
                "Star: one fact table, denormalised dimension tables radiating out. Fewer joins, faster "
                        + "reads, more storage and more update anomalies.\n\n"
                        + "Snowflake: dimensions normalised into sub-dimensions. Less redundancy, more "
                        + "joins, slower queries.\n\n"
                        + "Rule of thumb from the lecture: start with a star, snowflake only when a "
                        + "dimension is genuinely large and volatile.\n\n"
                        + "Slowly changing dimensions - type 1 overwrites, type 2 adds a new row with "
                        + "validity dates, type 3 keeps a previous-value column.",
                daysFrom(-6, 21, 40)));

        list.add(personalNote("demo-note-5", MOD_SECURITY, FOLDER_EXAM, false,
                "ISO 27001 Annex A - control families",
                "A.5  Organisational controls\n"
                        + "A.6  People controls\n"
                        + "A.7  Physical controls\n"
                        + "A.8  Technological controls\n\n"
                        + "The 2022 revision collapsed the old 14 clauses into these 4 themes.\n\n"
                        + "For the gap analysis: pick the organisation first, then work through A.5 and "
                        + "A.8 in detail rather than skimming all four.",
                daysFrom(-4, 18, 20)));

        list.add(personalNote("demo-note-6", MOD_MOBILE, FOLDER_LECTURE, false,
                "Room migration gotchas",
                "- Bumping the version without writing a Migration means Room throws at runtime.\n"
                        + "- fallbackToDestructiveMigration wipes the database. Fine in development, never "
                        + "in a submitted project.\n"
                        + "- ALTER TABLE ADD COLUMN cannot add a NOT NULL column without a default.\n"
                        + "- Renaming a column means create-copy-drop-rename, four statements.\n"
                        + "- exportSchema true writes the JSON schema files that migration tests read.",
                daysFrom(-11, 16, 45)));

        list.add(personalNote("demo-note-7", null, FOLDER_ADMIN, false,
                "Semester 2 timetable",
                "Mon  free (research writing day)\n"
                        + "Tue  13:00 - 15:00  ISTN7MP Lecture, MTB 4-12\n"
                        + "Wed  10:00 - 12:00  ISTN7BI Lecture, Shepstone 3-05\n"
                        + "Thu  11:00 - 12:30  ISTN7IS Lecture, MTB 2-08\n"
                        + "Thu  14:00 - 16:00  ISTN7MP Practical, Computer LAN 2\n"
                        + "Fri  09:00 - 10:00  Supervisor meeting, MTB 5-21\n\n"
                        + "Consultation: Prof. Singer Tue after class, Dr. Novak Thu 13:00.",
                daysFrom(-38, 11, 0)));

        return list;
    }

    private static PersonalNoteEntity personalNote(String id, String moduleId, String folderId,
                                                   boolean pinned, String title, String content,
                                                   long updatedAt) {
        PersonalNoteEntity note = new PersonalNoteEntity();
        note.setNoteId(id);
        note.setUserId(USER_ID);
        note.setModuleId(moduleId);
        note.setFolderId(folderId);
        note.setTitle(title);
        note.setContent(content);
        note.setPinned(pinned);
        note.setCreatedAt(updatedAt);
        note.setUpdatedAt(updatedAt);
        return note;
    }

    private static List<ModuleNoteEntity> moduleNotes(Context context, long now) {
        List<ModuleNoteEntity> list = new ArrayList<>();

        list.add(moduleNote(context, "demo-mnote-1", MOD_MOBILE, "Week 1 - Android Fundamentals",
                "istn7mp_week1_fundamentals.pdf",
                "Activity and fragment lifecycle, the manifest, and view binding.", daysFrom(-38, 15, 0)));
        list.add(moduleNote(context, "demo-mnote-2", MOD_MOBILE, "Week 3 - Room & LiveData",
                "istn7mp_week3_room_livedata.pdf",
                "Entities, DAOs, the database class, and observing queries with LiveData.", daysFrom(-24, 15, 0)));
        list.add(moduleNote(context, "demo-mnote-3", MOD_MOBILE, "Practical 2 Brief",
                "istn7mp_practical2_brief.pdf",
                "Navigation component task sheet and the marking rubric.", daysFrom(-20, 9, 30)));
        list.add(moduleNote(context, "demo-mnote-4", MOD_BI, "Week 2 - Dimensional Modelling",
                "istn7bi_week2_dimensional_modelling.pdf",
                "Facts, dimensions, grain, and slowly changing dimensions.", daysFrom(-30, 11, 0)));
        list.add(moduleNote(context, "demo-mnote-5", MOD_BI, "Power BI Lab Guide",
                "istn7bi_powerbi_lab_guide.pdf",
                "Step-by-step guide for the assignment 2 dashboard.", daysFrom(-9, 10, 15)));
        list.add(moduleNote(context, "demo-mnote-6", MOD_SECURITY, "ISO 27001 Overview Slides",
                "istn7is_iso27001_overview.pdf",
                "The ISMS, the Annex A control themes, and the certification cycle.", daysFrom(-26, 12, 0)));
        list.add(moduleNote(context, "demo-mnote-7", MOD_SECURITY, "Week 4 - Threat Modelling",
                "istn7is_week4_threat_modelling.pdf",
                "STRIDE, attack trees, and worked examples.", daysFrom(-12, 12, 0)));
        list.add(moduleNote(context, "demo-mnote-8", MOD_RESEARCH, "Honours Research Handbook",
                "istn7rp_research_handbook.pdf",
                "Submission rules, formatting requirements, and the ethics process.", daysFrom(-55, 8, 45)));

        return list;
    }

    private static ModuleNoteEntity moduleNote(Context context, String id, String moduleId,
                                               String title, String fileName, String blurb,
                                               long createdAt) {
        File target = new File(context.getFilesDir(),
                "module_notes/" + USER_ID + "/" + moduleId + "/" + id + "_" + fileName);
        long size = DemoFileFactory.writePlaceholderPdf(target, title,
                blurb,
                "",
                "Demo document for the Academic Weapon sample account.",
                "This stands in for the real lecture material, which is not distributed with the app.");

        ModuleNoteEntity note = new ModuleNoteEntity();
        note.setNoteId(id);
        note.setModuleId(moduleId);
        note.setUserId(USER_ID);
        note.setTitle(title);
        note.setFileName(fileName);
        note.setFileType("PDF");
        note.setFileSizeBytes(size);
        note.setStorageUri(target.getAbsolutePath());
        note.setUploadStatus("DONE");
        note.setCreatedAt(createdAt);
        return note;
    }

    private static List<PersonalNoteAttachmentEntity> attachments(Context context, long now) {
        List<PersonalNoteAttachmentEntity> list = new ArrayList<>();

        list.add(attachment(context, "demo-attach-1", "demo-note-1",
                "proposal_feedback_prof_mills.pdf", "Proposal feedback",
                "Marked-up proposal returned by the supervisor.", daysFrom(-8, 14, 35)));
        list.add(attachment(context, "demo-attach-2", "demo-note-2",
                "literature_matrix.pdf", "Literature matrix",
                "Source-by-theme matrix for the literature review.", daysFrom(-3, 20, 20)));

        return list;
    }

    private static PersonalNoteAttachmentEntity attachment(Context context, String id, String noteId,
                                                           String fileName, String title,
                                                           String blurb, long createdAt) {
        String relativePath = "personal_note_attachments/" + USER_ID + "/" + noteId + "/"
                + id + "_" + fileName;
        File target = new File(context.getFilesDir(), relativePath);
        long size = DemoFileFactory.writePlaceholderPdf(target, title,
                blurb,
                "",
                "Demo attachment for the Academic Weapon sample account.");

        PersonalNoteAttachmentEntity attachment = new PersonalNoteAttachmentEntity();
        attachment.setAttachmentId(id);
        attachment.setNoteId(noteId);
        attachment.setUserId(USER_ID);
        attachment.setFileName(fileName);
        attachment.setFileType("PDF");
        attachment.setFileSizeBytes(size);
        attachment.setStoragePath(relativePath);
        attachment.setDownloadUrl(target.getAbsolutePath());
        attachment.setUploadStatus("DONE");
        attachment.setCreatedAt(createdAt);
        return attachment;
    }

    // ----------------------------------------------------- research papers

    // Real, widely cited IS papers - the kind of reading list an honours student actually works
    // through. Only the stored PDFs are placeholders; the metadata describes the real papers.
    private static List<ResearchPaperEntity> researchPapers(Context context, long now) {
        List<ResearchPaperEntity> list = new ArrayList<>();

        list.add(paper(context, "demo-paper-1",
                "User Acceptance of Information Technology: Toward a Unified View",
                "Venkatesh, V., Morris, M. G., Davis, G. B. & Davis, F. D.",
                "2003", "Technology Adoption",
                "Reviews eight competing models of technology acceptance and consolidates them into "
                        + "the Unified Theory of Acceptance and Use of Technology (UTAUT).",
                "Performance expectancy, effort expectancy, social influence and facilitating "
                        + "conditions predict intention and use. Gender, age, experience and "
                        + "voluntariness moderate those relationships. UTAUT explained about 70% of "
                        + "variance in intention, well above the individual models.",
                "Longitudinal field study across four organisations, then validated on two more.",
                "The backbone of my theoretical framework. The moderators matter for a student "
                        + "population where use is effectively voluntary.",
                "READ", true, daysFrom(-62, 10, 0)));

        list.add(paper(context, "demo-paper-2",
                "Perceived Usefulness, Perceived Ease of Use, and User Acceptance of Information Technology",
                "Davis, F. D.",
                "1989", "Technology Adoption",
                "Introduces the Technology Acceptance Model and the scales for measuring perceived "
                        + "usefulness and perceived ease of use.",
                "Perceived usefulness is the stronger predictor of use; ease of use acts largely "
                        + "through usefulness. Both scales showed high reliability and validity.",
                "Instrument development across two studies, 152 users and 40 participants in a lab study.",
                "The origin point for everything in chapter 2. Needed for the historical framing "
                        + "before UTAUT.",
                "READ", true, daysFrom(-60, 10, 30)));

        list.add(paper(context, "demo-paper-3",
                "The DeLone and McLean Model of Information Systems Success: A Ten-Year Update",
                "DeLone, W. H. & McLean, E. R.",
                "2003", "IS Success",
                "Revisits the 1992 IS success model after a decade of empirical testing and adds "
                        + "service quality and net benefits.",
                "Six interdependent dimensions: information quality, system quality, service quality, "
                        + "intention to use / use, user satisfaction and net benefits. Success is "
                        + "multidimensional and the dimensions must be measured together.",
                "Conceptual review and synthesis of roughly 100 empirical studies citing the original model.",
                "Useful if I measure outcomes rather than just adoption intention. Decide with Prof. "
                        + "Mills whether this belongs in the framework or just the review.",
                "READING", false, daysFrom(-30, 16, 0)));

        list.add(paper(context, "demo-paper-4",
                "Design Science in Information Systems Research",
                "Hevner, A. R., March, S. T., Park, J. & Ram, S.",
                "2004", "Research Methods",
                "Sets out a conceptual framework and seven guidelines for conducting and evaluating "
                        + "design science research in IS.",
                "Design science produces and evaluates artifacts: constructs, models, methods and "
                        + "instantiations. Relevance comes from the environment, rigour from the "
                        + "knowledge base, and evaluation is mandatory rather than optional.",
                "Conceptual framework development illustrated with worked case examples.",
                "Directly relevant if the project is framed as building and evaluating an artifact "
                        + "rather than surveying users.",
                "READING", true, daysFrom(-21, 9, 20)));

        list.add(paper(context, "demo-paper-5",
                "The Case Research Strategy in Studies of Information Systems",
                "Benbasat, I., Goldstein, D. K. & Mead, M.",
                "1987", "Research Methods",
                "Argues for case research in IS and gives practical criteria for designing and "
                        + "reporting case studies.",
                "Case research suits questions about contemporary phenomena in their natural setting "
                        + "where the researcher cannot control the variables. Site selection, unit of "
                        + "analysis and data collection protocol have to be justified explicitly.",
                "Methodological review of published IS case studies against a set of design criteria.",
                "Backup reading in case the sample size for the survey does not hold up.",
                "UNREAD", false, daysFrom(-14, 13, 10)));

        list.add(paper(context, "demo-paper-6",
                "Big Data: The Management Revolution",
                "McAfee, A. & Brynjolfsson, E.",
                "2012", "Analytics",
                "Argues that data-driven decision making measurably outperforms decisions based on "
                        + "intuition, and describes what has to change managerially to get there.",
                "Firms in the top third of their industry for data-driven decision making were "
                        + "roughly 5% more productive and 6% more profitable than competitors. The "
                        + "binding constraint is management and talent, not technology.",
                "Survey of 330 public North American companies combined with interviews and "
                        + "structured performance data.",
                "Background for the ISTN7BI case study presentation rather than the dissertation.",
                "UNREAD", false, daysFrom(-7, 20, 5)));

        return list;
    }

    private static ResearchPaperEntity paper(Context context, String id, String title, String authors,
                                             String year, String category, String summary,
                                             String keyFindings, String methodology, String relevance,
                                             String status, boolean important, long uploadedAt) {
        String fileName = safeFileName(title) + ".pdf";
        String relativePath = "research_papers/" + USER_ID + "/" + id + "_" + fileName;
        File target = new File(context.getFilesDir(), relativePath);
        DemoFileFactory.writePlaceholderPdf(target, title,
                authors + " (" + year + ")",
                "",
                "Demo record for the Academic Weapon sample account.",
                "The published paper is not distributed with the app - look it up through the",
                "UKZN library to read the full text.");

        return new ResearchPaperEntity(
                id, USER_ID, title, authors, year, category,
                summary, keyFindings, methodology, relevance,
                fileName, target.getAbsolutePath(), relativePath,
                status, important, uploadedAt, uploadedAt);
    }

    private static String safeFileName(String title) {
        String cleaned = title.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
        return cleaned.length() > 48 ? cleaned.substring(0, 48) : cleaned;
    }

    // ------------------------------------------------------------- helpers

    /** Unix ms for a date offset from today, at a specific time of day. */
    private static long daysFrom(int days, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, days);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private static int academicYear() {
        return Calendar.getInstance().get(Calendar.YEAR);
    }

    private static boolean inFirstSemester() {
        return Calendar.getInstance().get(Calendar.MONTH) <= Calendar.JUNE;
    }

    private static String currentSemester() {
        return academicYear() + " Semester " + (inFirstSemester() ? 1 : 2);
    }

    private static String previousSemester() {
        return inFirstSemester()
                ? (academicYear() - 1) + " Semester 2"
                : academicYear() + " Semester 1";
    }
}
