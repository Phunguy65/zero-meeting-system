package io.github.phunguy65.zms.presentation.auth.login;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.domain.model.LoginResult;
import io.github.phunguy65.zms.frontends.R;
import io.github.phunguy65.zms.presentation.common.state.FieldError;
import io.github.phunguy65.zms.presentation.common.state.UiError;
import io.github.phunguy65.zms.presentation.common.state.UiState;
import io.github.phunguy65.zms.presentation.dashboard.DashboardActivity;
import java.util.concurrent.Executors;

/**
 * Login screen fragment supporting email/password and Google Sign-In.
 *
 * <p>Observes {@link LoginViewModel#getLoginState()} and reacts to state transitions:
 * Idle, Loading (disable button + show progress), Success (navigate Dashboard),
 * Error (show inline field errors + general error message).
 */
@AndroidEntryPoint
public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";

    private LoginViewModel viewModel;
    private CredentialManager credentialManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingNavigation;

    // Views
    private ImageView btnBack;
    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText edtEmail, edtPassword;
    private MaterialButton btnLoginSubmit, btnGoogle;
    private ProgressBar progressLogin;
    private TextView tvGeneralError, tvNeedAccount, tvForgotPassword;

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
        setupListeners();
        observeState();

        // Dim "Forgot password?" to indicate it is not fully functional yet
        tvForgotPassword.setAlpha(0.5f);
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        tilEmail = view.findViewById(R.id.tilEmail);
        tilPassword = view.findViewById(R.id.tilPassword);
        edtEmail = view.findViewById(R.id.edtEmail);
        edtPassword = view.findViewById(R.id.edtPassword);
        btnLoginSubmit = view.findViewById(R.id.btnLoginSubmit);
        btnGoogle = view.findViewById(R.id.btnGoogle);
        progressLogin = view.findViewById(R.id.progressLogin);
        tvGeneralError = view.findViewById(R.id.tvGeneralError);
        tvNeedAccount = view.findViewById(R.id.tvNeedAccount);
        tvForgotPassword = view.findViewById(R.id.tvForgotPassword);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> requireActivity().finish());

        btnLoginSubmit.setOnClickListener(v -> {
            clearErrors();
            String email =
                    edtEmail.getText() != null ? edtEmail.getText().toString().trim() : "";
            String password = edtPassword.getText() != null
                    ? edtPassword.getText().toString().trim()
                    : "";
            viewModel.loginWithEmail(email, password);
        });

        btnGoogle.setOnClickListener(v -> startGoogleSignIn());

        tvNeedAccount.setOnClickListener(
                v -> Navigation.findNavController(v).navigate(R.id.action_login_to_register));

        tvForgotPassword.setOnClickListener(v -> Snackbar.make(
                        v, R.string.login_forgot_password_coming_soon, Snackbar.LENGTH_SHORT)
                .show());

        // Error recovery: clear field error when user starts typing
        addErrorClearingWatcher(edtEmail, tilEmail);
        addErrorClearingWatcher(edtPassword, tilPassword);
    }

    /**
     * Adds a TextWatcher that clears the field error and hides the general error
     * when the user begins typing.
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
                if (tvGeneralError.getVisibility() == View.VISIBLE) {
                    tvGeneralError.setVisibility(View.GONE);
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
        btnLoginSubmit.setBackgroundTintList(
                ColorStateList.valueOf(getResources().getColor(R.color.md_theme_success, null)));
        pendingNavigation = this::navigateToDashboard;
        mainHandler.postDelayed(pendingNavigation, 400);
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(requireContext(), DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
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
                        case "email" -> tilEmail.setError(msg);
                        case "password" -> tilPassword.setError(msg);
                        default -> {} // ignore unknown fields
                    }
                }
                // Show general error if no field-level errors or it's a domain error
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
        return switch (code) {
            case "REQUIRED" -> getString(R.string.validation_required);
            case "FORMAT" -> getString(R.string.validation_invalid_format);
            case "MISMATCH" -> getString(R.string.validation_passwords_mismatch);
            default -> getString(R.string.validation_invalid_value);
        };
    }

    private void showGeneralError(String message) {
        tvGeneralError.setText(message);
        tvGeneralError.setVisibility(View.VISIBLE);
    }

    private void clearErrors() {
        tilEmail.setError(null);
        tilPassword.setError(null);
        tvGeneralError.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pendingNavigation != null) {
            mainHandler.removeCallbacks(pendingNavigation);
        }
    }

    // ── Google Sign-In via Credential Manager + Firebase Auth ─────────────────

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
                null, // cancellation signal
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        requireActivity().runOnUiThread(() -> handleGoogleCredential(result));
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        // User cancelled or no credentials available — silent dismiss
                        Log.d(TAG, "Google Sign-In cancelled or failed: " + e.getMessage());
                    }
                });
    }

    private void handleGoogleCredential(GetCredentialResponse response) {
        try {
            GoogleIdTokenCredential googleCredential =
                    GoogleIdTokenCredential.createFrom(response.getCredential().getData());

            String googleIdToken = googleCredential.getIdToken();

            // Exchange Google ID token for Firebase ID token
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
                                                    .runOnUiThread(() -> showGeneralError(getString(
                                                            R.string.error_google_signin_failed)));
                                            return;
                                        }
                                        viewModel.loginWithGoogle(firebaseIdToken);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to get Firebase ID token", e);
                                        requireActivity()
                                                .runOnUiThread(() -> showGeneralError(getString(
                                                        R.string.error_google_signin_failed)));
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Firebase signInWithCredential failed", e);
                        requireActivity()
                                .runOnUiThread(() -> showGeneralError(
                                        getString(R.string.error_google_signin_failed)));
                    });
        } catch (Exception e) {
            Log.e(TAG, "Failed to extract Google credential", e);
            requireActivity()
                    .runOnUiThread(
                            () -> showGeneralError(getString(R.string.error_google_signin_failed)));
        }
    }
}
