package com.example.mpproject.presentation.view;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.example.mpproject.databinding.FragmentProfileBinding;
import com.example.mpproject.presentation.viewmodel.AuthViewModel;
import com.example.mpproject.presentation.viewmodel.ViewModelFactory;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// [View] Displays and edits the user profile; reads from and writes to AuthViewModel.
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private AuthViewModel authViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Profile screens don't need a TodoRepository — pass null so the DB isn't opened here.
        ViewModelFactory factory = new ViewModelFactory(null, new AuthRepositoryImpl());
        authViewModel = new ViewModelProvider(this, factory).get(AuthViewModel.class);

        loadUserProfile();

        binding.saveProfileButton.setOnClickListener(v -> saveProfile());
        binding.logoutButton.setOnClickListener(v -> logout());
        binding.profileResetPasswordButton.setOnClickListener(v -> resetPassword());

        clearErrorOnType(binding.editFirstNameLayout, binding.editFirstNameText);
        clearErrorOnType(binding.editLastNameLayout,  binding.editLastNameText);
        clearErrorOnType(binding.editUsernameLayout,  binding.editUsernameText);
        clearErrorOnType(binding.editSchoolLayout,    binding.editSchoolText);

        authViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                binding.profileProgressBar.setVisibility(View.GONE);
                binding.saveProfileButton.setEnabled(true);
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadUserProfile() {
        FirebaseUser user = authViewModel.getCurrentUser();
        if (user == null) return;

        binding.profileProgressBar.setVisibility(View.VISIBLE);
        authViewModel.getUserData(user.getUid()).observe(getViewLifecycleOwner(), data -> {
            binding.profileProgressBar.setVisibility(View.GONE);
            if (data != null) {
                String firstName = (String) data.get("firstName");
                String lastName  = (String) data.get("lastName");
                String username  = (String) data.get("username");
                String school    = (String) data.get("school");
                String email     = (String) data.get("email");

                binding.profileName.setText(firstName + " " + lastName);
                binding.profileUsername.setText("@" + username);
                binding.editFirstNameText.setText(firstName);
                binding.editLastNameText.setText(lastName);
                binding.editUsernameText.setText(username);
                binding.editSchoolText.setText(school);
                binding.userEmailDisplay.setText("Email: " + email);

                Long dateJoined = (Long) data.get("dateJoined");
                if (dateJoined != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
                    binding.dateJoinedDisplay.setText("Member since: " + sdf.format(new Date(dateJoined)));
                }
            }
        });
    }

    private void saveProfile() {
        String firstName = binding.editFirstNameText.getText().toString().trim();
        String lastName  = binding.editLastNameText.getText().toString().trim();
        String username  = binding.editUsernameText.getText().toString().trim();
        String school    = binding.editSchoolText.getText().toString().trim();

        boolean hasError = false;

        if (firstName.isEmpty()) { binding.editFirstNameLayout.setError("First name required"); hasError = true; }
        if (lastName.isEmpty())  { binding.editLastNameLayout.setError("Last name required");   hasError = true; }
        if (username.isEmpty())  { binding.editUsernameLayout.setError("Username required");    hasError = true; }
        if (school.isEmpty())    { binding.editSchoolLayout.setError("School required");        hasError = true; }

        if (hasError) return;

        binding.saveProfileButton.setEnabled(false);
        binding.profileProgressBar.setVisibility(View.VISIBLE);

        authViewModel.updateProfile(firstName, lastName, username, school)
                .observe(getViewLifecycleOwner(), success -> {
                    if (binding == null) return;
                    binding.profileProgressBar.setVisibility(View.GONE);
                    binding.saveProfileButton.setEnabled(true);
                    if (success != null && success) {
                        Toast.makeText(getContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                        binding.profileName.setText(firstName + " " + lastName);
                        binding.profileUsername.setText("@" + username);
                    }
                });
    }

    private void logout() {
        authViewModel.logout();
        Navigation.findNavController(requireView()).navigate(R.id.loginFragment);
    }

    private void resetPassword() {
        FirebaseUser user = authViewModel.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            authViewModel.resetPassword(user.getEmail()).observe(getViewLifecycleOwner(), success -> {
                if (success != null && success) {
                    Toast.makeText(getContext(), "Reset link sent to your email!", Toast.LENGTH_SHORT).show();
                }
            });
        }
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
