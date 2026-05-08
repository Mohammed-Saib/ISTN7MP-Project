package com.example.mpproject.presentation.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mpproject.R;
import com.example.mpproject.data.local.AppDatabase;
import com.example.mpproject.data.repository.AuthRepositoryImpl;
import com.example.mpproject.data.repository.TodoRepositoryImpl;
import com.example.mpproject.domain.model.Todo;
import com.example.mpproject.presentation.adapter.TaskAdapter;
import com.example.mpproject.presentation.viewmodel.TaskViewModel;
import com.example.mpproject.presentation.viewmodel.ViewModelFactory;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

// [View] Observes LiveData from TaskViewModel; forwards user actions back to the ViewModel.
public class TasksFragment extends Fragment implements TaskAdapter.OnTaskClickListener {

    private TaskViewModel taskViewModel;
    private TaskAdapter taskAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.tasks_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        taskAdapter = new TaskAdapter(this);
        recyclerView.setAdapter(taskAdapter);

        FloatingActionButton fab = view.findViewById(R.id.fab_add_task);
        fab.setOnClickListener(v -> taskViewModel.addTask());

        AppDatabase db = AppDatabase.getDatabase(requireContext());
        ViewModelFactory factory = new ViewModelFactory(
                new TodoRepositoryImpl(db.todoDao()),
                new AuthRepositoryImpl());
        taskViewModel = new ViewModelProvider(this, factory).get(TaskViewModel.class);

        // Observe the active todo list and push updates to the adapter
        taskViewModel.getAllTasks().observe(getViewLifecycleOwner(), todos ->
                taskAdapter.submitList(todos));

        return view;
    }

    @Override
    public void onTaskClick(Todo todo) {
        // Detail / edit screen — to be implemented
    }

    @Override
    public void onTaskCheckedChange(Todo todo, boolean isChecked) {
        todo.setCompleted(isChecked);
        // Set completedAt explicitly so the stored timestamp is preserved correctly
        todo.setCompletedAt(isChecked ? System.currentTimeMillis() : null);
        taskViewModel.updateTask(todo);
    }
}
