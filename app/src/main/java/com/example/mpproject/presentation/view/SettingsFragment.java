package com.example.mpproject.presentation.view;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.mpproject.R;
import com.example.mpproject.data.local.entity.UserEntity;
import com.example.mpproject.data.repository.AuthRepositoryImpl;
import com.example.mpproject.databinding.FragmentSettingsBinding;
import com.example.mpproject.presentation.viewmodel.AuthViewModel;
import com.example.mpproject.presentation.viewmodel.ViewModelFactory;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// [View] Settings: profile overview with collapsible edit form, dark/light mode toggle, logout.
public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private AuthViewModel authViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewModelFactory factory = new ViewModelFactory(new AuthRepositoryImpl(requireContext()));
        authViewModel = new ViewModelProvider(this, factory).get(AuthViewModel.class);

        loadUserProfile();

        // Edit Profile toggle — shows/hides the edit fields section
        binding.btnEditProfile.setOnClickListener(v -> {
            boolean isShowing = binding.llEditFields.getVisibility() == View.VISIBLE;
            binding.llEditFields.setVisibility(isShowing ? View.GONE : View.VISIBLE);
            binding.btnEditProfile.setText(isShowing ? "Edit Profile" : "Cancel");
        });

        clearErrorOnType(binding.editFirstNameLayout, binding.editFirstNameText);
        clearErrorOnType(binding.editLastNameLayout,  binding.editLastNameText);
        clearErrorOnType(binding.editEmailLayout,     binding.editEmailText);
        clearErrorOnType(binding.editSchoolLayout,    binding.editSchoolText);

        authViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                binding.profileProgressBar.setVisibility(View.GONE);
                binding.saveProfileButton.setEnabled(true);
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        binding.saveProfileButton.setOnClickListener(v -> saveProfile());

        binding.btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        SharedPreferences themePrefs = requireContext()
                .getSharedPreferences("app_prefs", requireContext().MODE_PRIVATE);
        boolean savedDarkMode = themePrefs.getBoolean("pref_dark_mode", false);
        binding.switchDarkMode.setChecked(savedDarkMode);

        binding.switchDarkMode.setOnCheckedChangeListener((btn, isChecked) -> {
            themePrefs.edit().putBoolean("pref_dark_mode", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        binding.btnBack.setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack());

        binding.btnLogout.setOnClickListener(v -> {
            SharedPreferences prefs = requireContext()
                    .getSharedPreferences("app_prefs", requireContext().MODE_PRIVATE);
            prefs.edit().putBoolean("pref_remember_me", false).apply();
            authViewModel.logout();
            Navigation.findNavController(v).navigate(R.id.action_settingsFragment_to_loginFragment);
        });
    }

    private void loadUserProfile() {
        UserEntity user = authViewModel.getCurrentUser();
        if (user == null) return;

        String firstName = user.getFirstName();
        String lastName  = user.getLastName();
        String email     = user.getEmail();
        String school    = user.getSchool();

        // Populate the read-only overview
        String fullName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
        binding.tvProfileName.setText(fullName.trim());
        binding.userEmailDisplay.setText(email != null ? email : "");

        Long dateJoined = user.getDateJoined();
        if (dateJoined > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
            binding.dateJoinedDisplay.setText("Member since " + sdf.format(new Date(dateJoined)));
        }

        // Pre-fill edit fields
        binding.editFirstNameText.setText(firstName);
        binding.editLastNameText.setText(lastName);
        binding.editEmailText.setText(email);
        binding.editSchoolText.setText(school);
    }

    private void showChangePasswordDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(16), dp(20), 0);

        com.google.android.material.textfield.TextInputLayout currentLayout = new com.google.android.material.textfield.TextInputLayout(requireContext());
        currentLayout.setHint("Current password");
        currentLayout.setBoxBackgroundMode(com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE);
        currentLayout.setBoxCornerRadii(12, 12, 12, 12);
        currentLayout.setPasswordVisibilityToggleEnabled(true);

        com.google.android.material.textfield.TextInputEditText currentInput = new com.google.android.material.textfield.TextInputEditText(requireContext());
        currentInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        currentInput.setMaxLines(1);
        currentLayout.addView(currentInput);

        com.google.android.material.textfield.TextInputLayout newLayout = new com.google.android.material.textfield.TextInputLayout(requireContext());
        newLayout.setHint("New password");
        newLayout.setBoxBackgroundMode(com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE);
        newLayout.setBoxCornerRadii(12, 12, 12, 12);
        newLayout.setPasswordVisibilityToggleEnabled(true);

        com.google.android.material.textfield.TextInputEditText newInput = new com.google.android.material.textfield.TextInputEditText(requireContext());
        newInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        newInput.setMaxLines(1);
        newLayout.addView(newInput);

        com.google.android.material.textfield.TextInputLayout confirmLayout = new com.google.android.material.textfield.TextInputLayout(requireContext());
        confirmLayout.setHint("Confirm new password");
        confirmLayout.setBoxBackgroundMode(com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE);
        confirmLayout.setBoxCornerRadii(12, 12, 12, 12);
        confirmLayout.setPasswordVisibilityToggleEnabled(true);

        com.google.android.material.textfield.TextInputEditText confirmInput = new com.google.android.material.textfield.TextInputEditText(requireContext());
        confirmInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        confirmInput.setMaxLines(1);
        confirmLayout.addView(confirmInput);

        LinearLayout.LayoutParams fieldParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        fieldParams.setMargins(0, 0, 0, dp(12));
        layout.addView(currentLayout, fieldParams);
        layout.addView(newLayout, fieldParams);
        layout.addView(confirmLayout, fieldParams);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Change Password")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String currentPw = currentInput.getText() != null ? currentInput.getText().toString() : "";
                    String newPw = newInput.getText() != null ? newInput.getText().toString() : "";
                    String confirmPw = confirmInput.getText() != null ? confirmInput.getText().toString() : "";

                    if (currentPw.isEmpty()) {
                        Toast.makeText(requireContext(), "Enter your current password", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (newPw.isEmpty()) {
                        Toast.makeText(requireContext(), "Enter a new password", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (newPw.length() < 8) {
                        Toast.makeText(requireContext(), "Password must be at least 8 characters", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!newPw.equals(confirmPw)) {
                        Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    authViewModel.changePassword(currentPw, newPw)
                            .observe(getViewLifecycleOwner(), success -> {
                                if (success != null && success) {
                                    Toast.makeText(requireContext(), "Password changed successfully", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private void saveProfile() {
        String firstName = binding.editFirstNameText.getText().toString().trim();
        String lastName  = binding.editLastNameText.getText().toString().trim();
        String email     = binding.editEmailText.getText().toString().trim();
        String school    = binding.editSchoolText.getText().toString().trim();

        boolean hasError = false;
        if (firstName.isEmpty()) { binding.editFirstNameLayout.setError("First name required"); hasError = true; }
        if (lastName.isEmpty())  { binding.editLastNameLayout.setError("Last name required");   hasError = true; }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editEmailLayout.setError("Valid email required"); hasError = true;
        }
        if (school.isEmpty())    { binding.editSchoolLayout.setError("School required");        hasError = true; }
        if (hasError) return;

        binding.saveProfileButton.setEnabled(false);
        binding.profileProgressBar.setVisibility(View.VISIBLE);

        authViewModel.updateProfile(firstName, lastName, email, school)
                .observe(getViewLifecycleOwner(), success -> {
                    if (binding == null) return;
                    binding.profileProgressBar.setVisibility(View.GONE);
                    binding.saveProfileButton.setEnabled(true);
                    if (success != null && success) {
                        Toast.makeText(getContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                        // Update overview text and collapse edit panel
                        binding.tvProfileName.setText(firstName + " " + lastName);
                        binding.userEmailDisplay.setText(email);
                        binding.llEditFields.setVisibility(View.GONE);
                        binding.btnEditProfile.setText("Edit Profile");
                    }
                });
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
