package com.example.mpproject.presentation.view.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.mpproject.R;
import com.example.mpproject.data.repository.AuthRepositoryImpl;
import com.example.mpproject.databinding.FragmentRegisterBinding;
import com.example.mpproject.presentation.viewmodel.AuthViewModel;
import com.example.mpproject.presentation.viewmodel.ViewModelFactory;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

// [View] Collects registration details, validates them locally, then delegates to AuthViewModel.
public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private AuthViewModel authViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewModelFactory factory = new ViewModelFactory(new AuthRepositoryImpl());
        authViewModel = new ViewModelProvider(this, factory).get(AuthViewModel.class);

        binding.registerButton.setOnClickListener(v -> performRegistration());
        binding.loginLink.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_registerFragment_to_loginFragment));

        clearErrorOnType(binding.firstNameLayout,       binding.firstNameEditText);
        clearErrorOnType(binding.lastNameLayout,        binding.lastNameEditText);
        clearErrorOnType(binding.usernameLayout,        binding.usernameEditText);
        clearErrorOnType(binding.schoolLayout,          binding.schoolEditText);
        clearErrorOnType(binding.emailLayout,           binding.emailEditText);
        clearErrorOnType(binding.passwordLayout,        binding.passwordEditText);
        clearErrorOnType(binding.confirmPasswordLayout, binding.confirmPasswordEditText);

        authViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                binding.authProgressBar.setVisibility(View.GONE);
                binding.registerButton.setEnabled(true);
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void performRegistration() {
        String firstName = binding.firstNameEditText.getText().toString().trim();
        String lastName  = binding.lastNameEditText.getText().toString().trim();
        String username  = binding.usernameEditText.getText().toString().trim();
        String school    = binding.schoolEditText.getText().toString().trim();
        String email     = binding.emailEditText.getText().toString().trim();
        String password  = binding.passwordEditText.getText().toString().trim();
        String confirm   = binding.confirmPasswordEditText.getText().toString().trim();

        // Validate all fields at once so the user sees every error in one go.
        boolean hasError = false;

        if (firstName.isEmpty()) { binding.firstNameLayout.setError("First name required"); hasError = true; }
        if (lastName.isEmpty())  { binding.lastNameLayout.setError("Last name required");   hasError = true; }
        if (username.isEmpty())  { binding.usernameLayout.setError("Username required");    hasError = true; }
        if (school.isEmpty())    { binding.schoolLayout.setError("School required");        hasError = true; }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.setError("Valid email required");
            hasError = true;
        }

        String passwordError = isPasswordStrong(password);
        if (passwordError != null) {
            binding.passwordLayout.setError(passwordError);
            hasError = true;
        }

        if (!password.equals(confirm)) {
            binding.confirmPasswordLayout.setError("Passwords do not match");
            hasError = true;
        }

        if (hasError) return;

        // Disable the button so the user can't tap twice and stack observers on userLiveData.
        binding.registerButton.setEnabled(false);
        binding.authProgressBar.setVisibility(View.VISIBLE);

        authViewModel.register(email, password, firstName, lastName, username, school)
                .observe(getViewLifecycleOwner(), user -> {
                    if (binding == null) return;
                    if (user != null) {
                        binding.authProgressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Account created successfully!", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(requireView())
                                .navigate(R.id.action_registerFragment_to_homeFragment);
                    }
                });
    }

    // Returns an error message if the password fails any rule, or null if it passes.
    private String isPasswordStrong(String password) {
        if (password.length() < 8) return "Password must be at least 8 characters";

        boolean hasLetter = false, hasDigit = false, hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c))     hasLetter = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else                           hasSpecial = true;
        }

        if (!hasLetter)  return "Password must contain at least one letter";
        if (!hasDigit)   return "Password must contain at least one number";
        if (!hasSpecial) return "Password must contain at least one special character";
        return null;
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
