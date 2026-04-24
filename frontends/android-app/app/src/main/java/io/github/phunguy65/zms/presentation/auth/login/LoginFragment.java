package io.github.phunguy65.zms.presentation.auth.login;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.domain.model.LoginResult;
import io.github.phunguy65.zms.frontends.R;
import io.github.phunguy65.zms.presentation.common.LanguagePickerSheet;
import io.github.phunguy65.zms.presentation.common.state.FieldError;
import io.github.phunguy65.zms.presentation.common.state.UiError;
import io.github.phunguy65.zms.presentation.common.state.UiState;
import io.github.phunguy65.zms.presentation.main.MainActivity;
import java.util.concurrent.Executors;

/**
 * Login screen fragment supporting email/password and Google Sign-In.
 *
 * <p>Observes {@link LoginViewModel#getLoginState()} and reacts to state transitions:
 * Idle, Loading (disable button + show progress), Success (navigate Dashboard),
 * Error (show inline field errors).
 */
@AndroidEntryPoint
public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";

    private LoginViewModel viewModel;
    private CredentialManager credentialManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingNavigation;

    // Views
    private MaterialToolbar toolbar;
    private MaterialButton btnLanguage;
    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText edtEmail, edtPassword;
    private MaterialCheckBox cbRememberMe;
    private MaterialButton btnLoginSubmit, btnGoogle;
    private ProgressBar progressLogin;
    private TextView tvNeedAccount, tvForgotPassword;

    @Nullable @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        credentialManager = CredentialManager.create(requireContext());

        initViews(view);
        setupToolbar();
        setupAccessibility();
        setupListeners();
        observeState();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        btnLanguage = view.findViewById(R.id.btnLanguage);
        tilEmail = view.findViewById(R.id.tilEmail);
        tilPassword = view.findViewById(R.id.tilPassword);
        edtEmail = view.findViewById(R.id.edtEmail);
        edtPassword = view.findViewById(R.id.edtPassword);
        cbRememberMe = view.findViewById(R.id.cbRememberMe);
        btnLoginSubmit = view.findViewById(R.id.btnLoginSubmit);
        btnGoogle = view.findViewById(R.id.btnGoogle);
        progressLogin = view.findViewById(R.id.progressLogin);
        tvNeedAccount = view.findViewById(R.id.tvNeedAccount);
        tvForgotPassword = view.findViewById(R.id.tvForgotPassword);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> requireActivity().finish());
        btnLanguage.setOnClickListener(v -> LanguagePickerSheet.show(getChildFragmentManager()));
        updateLanguageButton();
    }

    /**
     * Updates the language button text to show current language code (EN/VI).
     * Also sets accessibility content description.
     */
    private void updateLanguageButton() {
        String langTag = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        String code;
        String displayName;

        if (langTag.startsWith("vi")) {
            code = "VI";
            displayName = getString(R.string.language_vietnamese_native);
        } else {
            code = "EN";
            displayName = getString(R.string.language_english_native);
        }

        btnLanguage.setText(code);
        btnLanguage.setContentDescription(getString(R.string.cd_language_button, displayName));
    }

    private void setupAccessibility() {}

    private void setupListeners() {
        btnLoginSubmit.setOnClickListener(v -> {
            clearErrors();
            String email =
                    edtEmail.getText() != null ? edtEmail.getText().toString().trim() : "";
            String password = edtPassword.getText() != null
                    ? edtPassword.getText().toString().trim()
                    : "";
            boolean rememberMe = cbRememberMe.isChecked();
            viewModel.loginWithEmail(email, password, rememberMe);
        });

        btnGoogle.setOnClickListener(v -> startGoogleSignIn());

        tvNeedAccount.setOnClickListener(
                v -> Navigation.findNavController(v).navigate(R.id.action_login_to_register));

        tvForgotPassword.setOnClickListener(
                v -> Navigation.findNavController(v).navigate(R.id.action_login_to_forgotPassword));

        addErrorClearingWatcher(edtEmail, tilEmail);
        addErrorClearingWatcher(edtPassword, tilPassword);
    }

    /**
     * Adds a TextWatcher that clears the field error when the user begins typing.
     */
    private void addErrorClearingWatcher(TextInputEditText editText, TextInputLayout layout) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (layout.getError() != null) {
                    layout.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void observeState() {
        viewModel.getLoginState().observe(getViewLifecycleOwner(), state -> {
            switch (state) {
                case UiState.Idle<LoginResult> ignored -> setIdleState();
                case UiState.Loading<LoginResult> ignored -> setLoadingState();
                case UiState.Success<LoginResult> success -> handleLoginSuccess();
                case UiState.Error<LoginResult> error -> handleError(error.error());
            }
        });
    }

    private void setIdleState() {
        btnLoginSubmit.setEnabled(true);
        btnLoginSubmit.setText(R.string.login_btn_sign_in);
        progressLogin.setVisibility(View.GONE);
        btnGoogle.setEnabled(true);
    }

    private void setLoadingState() {
        btnLoginSubmit.setEnabled(false);
        btnLoginSubmit.setText("");
        progressLogin.setVisibility(View.VISIBLE);
        btnGoogle.setEnabled(false);
        clearErrors();
    }

    /**
     * Shows brief success feedback (checkmark + green tint) before navigating to Dashboard.
     */
    private void handleLoginSuccess() {
        progressLogin.setVisibility(View.GONE);
        btnLoginSubmit.setEnabled(false);
        btnLoginSubmit.setText("\u2713");
        // Use theme-aware success color for dark mode compatibility
        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(R.attr.colorSuccess, typedValue, true);
        btnLoginSubmit.setBackgroundTintList(ColorStateList.valueOf(typedValue.data));
        pendingNavigation = this::navigateToDashboard;
        mainHandler.postDelayed(pendingNavigation, 400);
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void handleError(UiError error) {
        setIdleState();
        switch (error) {
            case UiError.Fail fail -> {
                for (FieldError fe : fail.fieldErrors()) {
                    String msg = fe.message() != null
                            ? fe.message()
                            : resolveValidationMessage(fe.code());
                    switch (fe.field()) {
                        case "email" -> tilEmail.setError(msg);
                        case "password" -> tilPassword.setError(msg);
                        default -> {}
                    }
                }
                if (fail.fieldErrors().isEmpty() && fail.message() != null) {
                    tilPassword.setError(fail.message());
                }
            }
            case UiError.ServerError s -> tilPassword.setError(getString(R.string.error_server));
            case UiError.NetworkError n -> tilPassword.setError(getString(R.string.error_network));
            case UiError.Unknown u -> tilPassword.setError(getString(R.string.error_unknown));
        }
    }

    /**
     * Resolves a localized validation message from a machine-readable code.
     * Used for client-side validation errors where {@link FieldError#message()} is {@code null}.
     */
    private String resolveValidationMessage(String code) {
        return switch (code) {
            case "REQUIRED" -> getString(R.string.validation_required);
            case "FORMAT" -> getString(R.string.validation_invalid_format);
            case "MISMATCH" -> getString(R.string.validation_passwords_mismatch);
            default -> getString(R.string.validation_invalid_value);
        };
    }

    private void clearErrors() {
        tilEmail.setError(null);
        tilPassword.setError(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pendingNavigation != null) {
            mainHandler.removeCallbacks(pendingNavigation);
        }
    }

    private void startGoogleSignIn() {
        clearErrors();

        String serverClientId = requireContext().getString(R.string.default_web_client_id);

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(
                requireContext(),
                request,
                null,
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        requireActivity().runOnUiThread(() -> handleGoogleCredential(result));
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        if (e
                                instanceof
                                androidx.credentials.exceptions
                                        .GetCredentialCancellationException) {
                            Log.d(TAG, "Google Sign-In cancelled by user");
                            return;
                        }
                        Log.e(TAG, "Google Sign-In failed: " + e.getMessage(), e);
                        requireActivity()
                                .runOnUiThread(() -> tilPassword.setError(
                                        getString(R.string.error_google_signin_failed)));
                    }
                });
    }

    private void handleGoogleCredential(GetCredentialResponse response) {
        try {
            GoogleIdTokenCredential googleCredential =
                    GoogleIdTokenCredential.createFrom(response.getCredential().getData());

            String googleIdToken = googleCredential.getIdToken();

            AuthCredential firebaseCredential =
                    GoogleAuthProvider.getCredential(googleIdToken, null);

            FirebaseAuth.getInstance()
                    .signInWithCredential(firebaseCredential)
                    .addOnSuccessListener(authResult -> {
                        var user = authResult.getUser();
                        if (user != null) {
                            user.getIdToken(true)
                                    .addOnSuccessListener(tokenResult -> {
                                        String firebaseIdToken = tokenResult.getToken();
                                        if (firebaseIdToken == null) {
                                            requireActivity()
                                                    .runOnUiThread(() ->
                                                            tilPassword.setError(getString(R.string
                                                                    .error_google_signin_failed)));
                                            return;
                                        }
                                        boolean rememberMe = cbRememberMe.isChecked();
                                        viewModel.loginWithGoogle(firebaseIdToken, rememberMe);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to get Firebase ID token", e);
                                        requireActivity()
                                                .runOnUiThread(() -> tilPassword.setError(getString(
                                                        R.string.error_google_signin_failed)));
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Firebase signInWithCredential failed", e);
                        requireActivity()
                                .runOnUiThread(() -> tilPassword.setError(
                                        getString(R.string.error_google_signin_failed)));
                    });
        } catch (Exception e) {
            Log.e(TAG, "Failed to extract Google credential", e);
            requireActivity()
                    .runOnUiThread(() ->
                            tilPassword.setError(getString(R.string.error_google_signin_failed)));
        }
    }
}
