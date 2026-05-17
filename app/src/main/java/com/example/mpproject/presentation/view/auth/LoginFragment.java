package com.example.mpproject.presentation.view.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.mpproject.R;
import com.example.mpproject.data.local.AppDatabase;
import com.example.mpproject.data.repository.AuthRepositoryImpl;
import com.example.mpproject.data.repository.CalendarEventRepositoryImpl;
import com.example.mpproject.data.repository.ModuleRepositoryImpl;
import com.example.mpproject.data.repository.TodoRepositoryImpl;
import com.example.mpproject.databinding.FragmentLoginBinding;
import com.example.mpproject.presentation.viewmodel.AuthViewModel;
import com.example.mpproject.presentation.viewmodel.ViewModelFactory;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

// [View] Collects login credentials and forwards them to AuthViewModel; observes result LiveData.
public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private AuthViewModel authViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewModelFactory factory = new ViewModelFactory(new AuthRepositoryImpl());
        authViewModel = new ViewModelProvider(this, factory).get(AuthViewModel.class);

        binding.loginButton.setOnClickListener(v -> performLogin());
        binding.registerLink.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_loginFragment_to_registerFragment));
        binding.forgotPasswordLink.setOnClickListener(v -> showForgotPasswordDialog());

        clearErrorOnType(binding.emailLayout, binding.emailEditText);
        clearErrorOnType(binding.passwordLayout, binding.passwordEditText);

        authViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                binding.authProgressBar.setVisibility(View.GONE);
                binding.loginButton.setEnabled(true);
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void performLogin() {
        String email    = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();

        boolean hasError = false;

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.setError("Valid email required");
            hasError = true;
        }
        if (password.isEmpty()) {
            binding.passwordLayout.setError("Password required");
            hasError = true;
        }

        if (hasError) return;

        binding.loginButton.setEnabled(false);
        binding.authProgressBar.setVisibility(View.VISIBLE);

        authViewModel.login(email, password).observe(getViewLifecycleOwner(), user -> {
            if (binding == null) return;
            if (user != null) {
                binding.authProgressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Logged in successfully!", Toast.LENGTH_SHORT).show();
                // Restore any data lost from a DB migration or clean reinstall
                AppDatabase db = AppDatabase.getDatabase(requireContext());
                new ModuleRepositoryImpl(db.moduleDao()).syncFromFirestoreIfEmpty(user.getUid());
                new TodoRepositoryImpl(db.todoDao()).syncFromFirestoreIfEmpty(user.getUid());
                new CalendarEventRepositoryImpl(db.calendarEventDao()).syncFromFirestoreIfEmpty(user.getUid());
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_loginFragment_to_homeFragment);
            }
        });
    }

    private void showForgotPasswordDialog() {
        EditText resetEmail = new EditText(requireContext());
        resetEmail.setHint("Enter your email");

        new AlertDialog.Builder(requireContext())
                .setTitle("Reset Password")
                .setMessage("We will send a reset link to your email.")
                .setView(resetEmail)
                .setPositiveButton("Send", (dialog, which) -> {
                    String email = resetEmail.getText().toString().trim();
                    if (!email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        authViewModel.resetPassword(email).observe(getViewLifecycleOwner(), success -> {
                            if (success != null && success) {
                                Toast.makeText(getContext(), "Reset link sent!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(getContext(), "Invalid email", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearErrorOnType(TextInputLayout layout, TextInputEditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
