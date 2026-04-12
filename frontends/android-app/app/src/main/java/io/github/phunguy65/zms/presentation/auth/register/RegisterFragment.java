package io.github.phunguy65.zms.presentation.auth.register;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;
import io.github.phunguy65.zms.presentation.common.state.FieldError;
import io.github.phunguy65.zms.presentation.common.state.UiError;
import io.github.phunguy65.zms.presentation.common.state.UiState;

/**
 * Registration screen fragment.
 *
 * <p>Observes {@link RegisterViewModel#getRegisterState()} and handles state transitions.
 * On successful registration, navigates back to {@code LoginFragment}.
 *
 * <p>Validation strategy (per M3 UX best practices):
 * <ul>
 *   <li>fullName, username, email: validate on blur (focus lost)
 *   <li>password, confirmPassword: validate real-time (as user types)
 *   <li>All fields: re-validate on submit
 * </ul>
 */
@AndroidEntryPoint
public class RegisterFragment extends Fragment {

    private RegisterViewModel viewModel;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingNavigation;

    private ImageView btnBack;
    private TextInputLayout tilFullName, tilUsername, tilEmail, tilPassword, tilConfirmPassword;
    private TextInputEditText edtFullName, edtUsername, edtEmail, edtPassword, edtConfirmPassword;
    private MaterialButton btnRegisterSubmit;
    private ProgressBar progressRegister;
    private TextView tvGeneralError, tvHaveAccount;

    @Nullable @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        initViews(view);
        setupListeners();
        observeState();
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        tilFullName = view.findViewById(R.id.tilFullName);
        tilUsername = view.findViewById(R.id.tilUsername);
        tilEmail = view.findViewById(R.id.tilEmail);
        tilPassword = view.findViewById(R.id.tilPassword);
        tilConfirmPassword = view.findViewById(R.id.tilConfirmPassword);
        edtFullName = view.findViewById(R.id.edtFullName);
        edtUsername = view.findViewById(R.id.edtUsername);
        edtEmail = view.findViewById(R.id.edtEmail);
        edtPassword = view.findViewById(R.id.edtPassword);
        edtConfirmPassword = view.findViewById(R.id.edtConfirmPassword);
        btnRegisterSubmit = view.findViewById(R.id.btnRegisterSubmit);
        progressRegister = view.findViewById(R.id.progressRegister);
        tvGeneralError = view.findViewById(R.id.tvGeneralError);
        tvHaveAccount = view.findViewById(R.id.tvHaveAccount);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        btnRegisterSubmit.setOnClickListener(v -> {
            clearErrors();
            String fullName = getTextTrimmed(edtFullName);
            String username = getTextTrimmed(edtUsername);
            String email = getTextTrimmed(edtEmail);
            String password = getTextOrEmpty(edtPassword);
            String confirmPassword = getTextOrEmpty(edtConfirmPassword);

            viewModel.register(fullName, username, email, password, confirmPassword);
        });

        tvHaveAccount.setOnClickListener(
                v -> Navigation.findNavController(v).navigate(R.id.action_register_to_login));

        // ═══════════════════════════════════════════════════════════════════════
        // Blur validation: fullName, username, email
        // ═══════════════════════════════════════════════════════════════════════
        edtFullName.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                FieldError error = viewModel.validateFullName(getTextTrimmed(edtFullName));
                if (error != null) {
                    tilFullName.setError(resolveValidationMessage(error.code()));
                } else {
                    tilFullName.setError(null);
                }
            }
        });

        edtUsername.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                FieldError error = viewModel.validateUsername(getTextTrimmed(edtUsername));
                if (error != null) {
                    tilUsername.setError(resolveValidationMessage(error.code()));
                } else {
                    tilUsername.setError(null);
                    // Restore helper text when valid
                    tilUsername.setHelperText(getString(R.string.register_helper_username));
                }
            }
        });

        edtEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                FieldError error = viewModel.validateEmail(getTextTrimmed(edtEmail));
                if (error != null) {
                    tilEmail.setError(resolveValidationMessage(error.code()));
                } else {
                    tilEmail.setError(null);
                }
            }
        });

        // ═══════════════════════════════════════════════════════════════════════
        // Real-time validation: password, confirmPassword
        // ═══════════════════════════════════════════════════════════════════════
        edtPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Clear general error when typing
                if (tvGeneralError.getVisibility() == View.VISIBLE) {
                    tvGeneralError.setVisibility(View.GONE);
                }

                String password = s.toString();
                FieldError error = viewModel.validatePassword(password);
                if (error != null) {
                    tilPassword.setError(resolveValidationMessage(error.code()));
                } else {
                    tilPassword.setError(null);
                    // Restore helper text when valid
                    tilPassword.setHelperText(getString(R.string.register_helper_password));
                }

                // Also re-validate confirmPassword if it has content
                String confirmPassword = getTextOrEmpty(edtConfirmPassword);
                if (!confirmPassword.isEmpty()) {
                    FieldError confirmError =
                            viewModel.validateConfirmPassword(confirmPassword, password);
                    if (confirmError != null) {
                        tilConfirmPassword.setError(resolveValidationMessage(confirmError.code()));
                    } else {
                        tilConfirmPassword.setError(null);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        edtConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Clear general error when typing
                if (tvGeneralError.getVisibility() == View.VISIBLE) {
                    tvGeneralError.setVisibility(View.GONE);
                }

                String confirmPassword = s.toString();
                String password = getTextOrEmpty(edtPassword);
                FieldError error = viewModel.validateConfirmPassword(confirmPassword, password);
                if (error != null) {
                    tilConfirmPassword.setError(resolveValidationMessage(error.code()));
                } else {
                    tilConfirmPassword.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // ═══════════════════════════════════════════════════════════════════════
        // Error recovery: clear field error when user starts typing (for blur fields)
        // ═══════════════════════════════════════════════════════════════════════
        addErrorClearingWatcher(edtFullName, tilFullName, null);
        addErrorClearingWatcher(edtUsername, tilUsername, R.string.register_helper_username);
        addErrorClearingWatcher(edtEmail, tilEmail, null);
    }

    /**
     * Adds a TextWatcher that clears the field error and hides the general error
     * when the user begins typing. Optionally restores helper text.
     */
    private void addErrorClearingWatcher(
            TextInputEditText editText, TextInputLayout layout, @Nullable Integer helperTextResId) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (layout.getError() != null) {
                    layout.setError(null);
                    if (helperTextResId != null) {
                        layout.setHelperText(getString(helperTextResId));
                    }
                }
                if (tvGeneralError.getVisibility() == View.VISIBLE) {
                    tvGeneralError.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Gets text from EditText with trimming (for fields like fullName, username, email).
     */
    private String getTextTrimmed(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    /**
     * Gets text from EditText without trimming (for password fields).
     */
    private String getTextOrEmpty(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString() : "";
    }

    private void observeState() {
        viewModel.getRegisterState().observe(getViewLifecycleOwner(), state -> {
            switch (state) {
                case UiState.Idle<?> ignored -> setIdleState();
                case UiState.Loading<?> ignored -> setLoadingState();
                case UiState.Success<?> success -> handleRegisterSuccess();
                case UiState.Error<?> error -> handleError(error.error());
            }
        });
    }

    private void setIdleState() {
        btnRegisterSubmit.setEnabled(true);
        btnRegisterSubmit.setText(R.string.register_btn_create);
        progressRegister.setVisibility(View.GONE);
    }

    private void setLoadingState() {
        btnRegisterSubmit.setEnabled(false);
        btnRegisterSubmit.setText("");
        progressRegister.setVisibility(View.VISIBLE);
        clearErrors();
    }

    /**
     * Shows a success Snackbar then navigates to the login screen after a brief delay.
     */
    private void handleRegisterSuccess() {
        progressRegister.setVisibility(View.GONE);
        Snackbar.make(requireView(), R.string.register_success_message, Snackbar.LENGTH_SHORT)
                .show();
        pendingNavigation = this::navigateToLogin;
        mainHandler.postDelayed(pendingNavigation, 800);
    }

    private void navigateToLogin() {
        Navigation.findNavController(requireView()).navigate(R.id.action_register_to_login);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pendingNavigation != null) {
            mainHandler.removeCallbacks(pendingNavigation);
        }
    }

    private void handleError(UiError error) {
        setIdleState();
        switch (error) {
            case UiError.Fail fail -> {
                // Show field-level errors with localized messages
                for (FieldError fe : fail.fieldErrors()) {
                    String msg = fe.message() != null
                            ? fe.message()
                            : resolveValidationMessage(fe.code());
                    switch (fe.field()) {
                        case "fullName" -> tilFullName.setError(msg);
                        case "username" -> tilUsername.setError(msg);
                        case "email" -> tilEmail.setError(msg);
                        case "password" -> tilPassword.setError(msg);
                        case "confirmPassword" -> tilConfirmPassword.setError(msg);
                        default -> {}
                    }
                }
                // Show general error for domain errors (not field validation)
                if (fail.fieldErrors().isEmpty() || !"VALIDATION".equals(fail.code())) {
                    String generalMsg = fail.message() != null
                            ? fail.message()
                            : getString(R.string.error_validation);
                    showGeneralError(generalMsg);
                }
            }
            case UiError.ServerError s -> showGeneralError(getString(R.string.error_server));
            case UiError.NetworkError n -> showGeneralError(getString(R.string.error_network));
            case UiError.Unknown u -> showGeneralError(getString(R.string.error_unknown));
        }
    }

    /**
     * Resolves a localized validation message from a machine-readable code.
     * Used for client-side validation errors where {@link FieldError#message()} is {@code null}.
     */
    private String resolveValidationMessage(String code) {
        if (code == null) {
            return getString(R.string.validation_invalid_value);
        }
        return switch (code) {
            case "REQUIRED" -> getString(R.string.validation_required);
            case "FORMAT" -> getString(R.string.validation_invalid_format);
            case "MISMATCH" -> getString(R.string.validation_passwords_mismatch);
            case "FULLNAME_TOO_LONG" -> getString(R.string.validation_fullname_too_long);
            case "USERNAME_TOO_SHORT" -> getString(R.string.validation_username_too_short);
            case "USERNAME_TOO_LONG" -> getString(R.string.validation_username_too_long);
            case "USERNAME_FORMAT" -> getString(R.string.validation_username_format);
            case "PASSWORD_TOO_SHORT" -> getString(R.string.validation_password_too_short);
            case "PASSWORD_TOO_LONG" -> getString(R.string.validation_password_too_long);
            default -> getString(R.string.validation_invalid_value);
        };
    }

    private void showGeneralError(String message) {
        tvGeneralError.setText(message);
        tvGeneralError.setVisibility(View.VISIBLE);
    }

    private void clearErrors() {
        tilFullName.setError(null);
        tilUsername.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
        tvGeneralError.setVisibility(View.GONE);
    }
}
