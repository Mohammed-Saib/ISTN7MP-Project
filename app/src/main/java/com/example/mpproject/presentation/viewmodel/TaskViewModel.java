package com.example.mpproject.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.mpproject.domain.model.Module;
import com.example.mpproject.domain.model.Todo;
import com.example.mpproject.domain.repository.ModuleRepository;
import com.example.mpproject.domain.repository.TodoRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

// [ViewModel] Todo list — filtered LiveData powers the chip group; editingTodo drives the bottom sheet.
public class TaskViewModel extends ViewModel {

    private final TodoRepository todoRepository;
    private final String userId;

    // Active (not completed) todos — the default view
    public final LiveData<List<Todo>> allTodos;
    public final LiveData<List<Todo>> completedTodos;
    public final LiveData<List<Todo>> highPriorityTodos;
    public final LiveData<List<Todo>> mediumPriorityTodos;
    public final LiveData<List<Todo>> lowPriorityTodos;
    public final LiveData<List<Module>> modules;

    private final MutableLiveData<Todo> editingTodo = new MutableLiveData<>(null);

    public TaskViewModel(TodoRepository todoRepository, ModuleRepository moduleRepository) {
        this.todoRepository = todoRepository;
        FirebaseAuth auth = FirebaseAuth.getInstance();
        this.userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";

        allTodos = todoRepository.getAllByUser(userId);

        completedTodos = todoRepository.getCompletedByUser(userId);

        highPriorityTodos = Transformations.map(allTodos, list -> {
            if (list == null) return new ArrayList<>();
            List<Todo> out = new ArrayList<>();
            for (Todo t : list) if ("HIGH".equals(t.getPriority())) out.add(t);
            return out;
        });

        mediumPriorityTodos = Transformations.map(allTodos, list -> {
            if (list == null) return new ArrayList<>();
            List<Todo> out = new ArrayList<>();
            for (Todo t : list) if ("MEDIUM".equals(t.getPriority())) out.add(t);
            return out;
        });

        lowPriorityTodos = Transformations.map(allTodos, list -> {
            if (list == null) return new ArrayList<>();
            List<Todo> out = new ArrayList<>();
            for (Todo t : list) if ("LOW".equals(t.getPriority())) out.add(t);
            return out;
        });

        modules = moduleRepository.getActiveByUser(userId);
    }

    public LiveData<Todo> getEditingTodo() { return editingTodo; }
    public void setEditingTodo(Todo todo) { editingTodo.setValue(todo); }

    public void addTask(String title, String description, String priority,
                        Long dueDate, String moduleId,
                        String recurrencePattern, Long recurrenceEndDate) {
        String groupId = recurrencePattern != null ? UUID.randomUUID().toString() : null;
        Todo todo = new Todo(UUID.randomUUID().toString(), userId, title);
        todo.setDescription(description);
        todo.setPriority(priority != null ? priority : "MEDIUM");
        todo.setDueDate(dueDate);
        todo.setModuleId(moduleId);
        todo.setRecurrencePattern(recurrencePattern);
        todo.setRecurrenceGroupId(groupId);
        todo.setRecurrenceEndDate(recurrenceEndDate);
        todoRepository.insert(todo);
    }

    public void updateTask(Todo todo) {
        todo.setUpdatedAt(System.currentTimeMillis());
        if (todo.getRecurrenceGroupId() != null) {
            // Propagate non-date fields to all occurrences, then update this instance's own row
            todoRepository.updateGroup(todo);
            todoRepository.update(todo);
        } else {
            todoRepository.update(todo);
        }
    }

    // Toggle completion; if recurring and just completed, schedule the next occurrence
    public void toggleTask(Todo todo, boolean checked) {
        todo.setCompleted(checked);
        todo.setCompletedAt(checked ? System.currentTimeMillis() : null);
        todo.setUpdatedAt(System.currentTimeMillis());
        todoRepository.update(todo);

        if (checked && todo.getRecurrencePattern() != null && todo.getRecurrenceGroupId() != null) {
            scheduleNextOccurrence(todo);
        }
    }

    public void deleteTask(Todo todo) {
        todoRepository.delete(todo);
    }

    public void deleteTaskGroup(Todo todo) {
        if (todo.getRecurrenceGroupId() != null) {
            todoRepository.deleteGroup(todo.getRecurrenceGroupId(), userId);
        } else {
            todoRepository.delete(todo);
        }
    }

    // Creates the next occurrence of a recurring task after the given one is completed.
    // Respects recurrenceEndDate and a hard cap of 100 instances (tracked by group).
    private void scheduleNextOccurrence(Todo completed) {
        if (completed.getDueDate() == null) return;

        Calendar next = Calendar.getInstance();
        next.setTimeInMillis(completed.getDueDate());

        switch (completed.getRecurrencePattern()) {
            case "DAILY":   next.add(Calendar.DAY_OF_YEAR, 1); break;
            case "WEEKLY":  next.add(Calendar.WEEK_OF_YEAR, 1); break;
            case "MONTHLY": next.add(Calendar.MONTH, 1); break;
            case "YEARLY":  next.add(Calendar.YEAR, 1); break;
            default: return;
        }

        long nextMs = next.getTimeInMillis();
        if (completed.getRecurrenceEndDate() != null && nextMs > completed.getRecurrenceEndDate()) return;

        Todo nextTodo = new Todo(UUID.randomUUID().toString(), userId, completed.getTitle());
        nextTodo.setDescription(completed.getDescription());
        nextTodo.setPriority(completed.getPriority());
        nextTodo.setDueDate(nextMs);
        nextTodo.setModuleId(completed.getModuleId());
        nextTodo.setRecurrencePattern(completed.getRecurrencePattern());
        nextTodo.setRecurrenceGroupId(completed.getRecurrenceGroupId());
        nextTodo.setRecurrenceEndDate(completed.getRecurrenceEndDate());
        todoRepository.insert(nextTodo);
    }
}
