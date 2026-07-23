package com.example.mpproject.presentation.view.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import android.content.res.Configuration;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.mpproject.R;
import com.example.mpproject.data.repository.AuthRepositoryImpl;
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

    private void applyAuthLogo(View view) {
        ImageView authLogo = view.findViewById(R.id.auth_logo);

        int currentNightMode = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;

        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            authLogo.setImageResource(R.drawable.img_aw_cat_dark);
        } else {
            authLogo.setImageResource(R.drawable.img_aw_cat_light);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        applyAuthLogo(view);

        ViewModelFactory factory = new ViewModelFactory(new AuthRepositoryImpl(requireContext()));
        authViewModel = new ViewModelProvider(this, factory).get(AuthViewModel.class);

        binding.loginButton.setOnClickListener(v -> performLogin());
        binding.registerLink.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_loginFragment_to_registerFragment));

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

                // Save the "remember me" preference so MainActivity knows whether to keep the session on next launch
                boolean rememberMe = binding.cbRememberMe.isChecked();
                SharedPreferences prefs = requireContext()
                        .getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
                prefs.edit().putBoolean("pref_remember_me", rememberMe).apply();

                Navigation.findNavController(requireView())
                        .navigate(R.id.action_loginFragment_to_homeFragment);
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
