package io.github.phunguy65.zms.presentation.auth.forgotpassword;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.google.android.material.appbar.MaterialToolbar;
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
 * Fragment for the reset password screen where users enter the OTP and new password.
 *
 * <p>Features:
 * <ul>
 *   <li>OTP input with validation</li>
 *   <li>New password and confirmation fields</li>
 *   <li>60-second resend cooldown with visual timer</li>
 *   <li>Navigation back to login on success</li>
 * </ul>
 */
@AndroidEntryPoint
public class ResetPasswordFragment extends Fragment {

    private ResetPasswordViewModel viewModel;
    private String email;

    // Views
    private MaterialToolbar toolbar;
    private TextView tvSubtitle;
    private TextInputLayout tilOtp, tilNewPassword, tilConfirmPassword;
    private TextInputEditText edtOtp, edtNewPassword, edtConfirmPassword;
    private TextView tvResend;
    private ProgressBar progressResend;
    private MaterialButton btnResetPassword;
    private ProgressBar progressReset;

    @Nullable @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reset_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ResetPasswordFragmentArgs args = ResetPasswordFragmentArgs.fromBundle(requireArguments());
        email = args.getEmail();
        boolean showCodeSentMessage = args.getShowCodeSentMessage();

        viewModel = new ViewModelProvider(this).get(ResetPasswordViewModel.class);

        initViews(view);
        setupToolbar();
        setupSubtitle();
        setupListeners();
        observeState();

        if (showCodeSentMessage) {
            Snackbar.make(view, R.string.forgot_password_success_message, Snackbar.LENGTH_SHORT)
                    .show();
        }
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        tvSubtitle = view.findViewById(R.id.tvSubtitle);
        tilOtp = view.findViewById(R.id.tilOtp);
        edtOtp = view.findViewById(R.id.edtOtp);
        tilNewPassword = view.findViewById(R.id.tilNewPassword);
        edtNewPassword = view.findViewById(R.id.edtNewPassword);
        tilConfirmPassword = view.findViewById(R.id.tilConfirmPassword);
        edtConfirmPassword = view.findViewById(R.id.edtConfirmPassword);
        tvResend = view.findViewById(R.id.tvResend);
        progressResend = view.findViewById(R.id.progressResend);
        btnResetPassword = view.findViewById(R.id.btnResetPassword);
        progressReset = view.findViewById(R.id.progressReset);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(
                v -> Navigation.findNavController(v).navigateUp());
    }

    private void setupSubtitle() {
        String subtitle = getString(R.string.reset_password_subtitle_format, email);
        tvSubtitle.setText(subtitle);
    }

    private void setupListeners() {
        btnResetPassword.setOnClickListener(v -> {
            clearErrors();
            String otp = edtOtp.getText() != null ? edtOtp.getText().toString().trim() : "";
            String newPassword =
                    edtNewPassword.getText() != null ? edtNewPassword.getText().toString() : "";
            String confirmPassword = edtConfirmPassword.getText() != null
                    ? edtConfirmPassword.getText().toString()
                    : "";
            viewModel.resetPassword(email, otp, newPassword, confirmPassword);
        });

        tvResend.setOnClickListener(v -> viewModel.resendOtp(email));

        addErrorClearingWatcher(edtOtp, tilOtp);
        addErrorClearingWatcher(edtNewPassword, tilNewPassword);
        addErrorClearingWatcher(edtConfirmPassword, tilConfirmPassword);
    }

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
        viewModel.getResetState().observe(getViewLifecycleOwner(), state -> {
            switch (state) {
                case UiState.Idle<Void> ignored -> setResetIdleState();
                case UiState.Loading<Void> ignored -> setResetLoadingState();
                case UiState.Success<Void> success -> handleResetSuccess();
                case UiState.Error<Void> error -> handleResetError(error.error());
            }
        });

        viewModel.getResendState().observe(getViewLifecycleOwner(), state -> {
            switch (state) {
                case UiState.Idle<Void> ignored -> setResendIdleState();
                case UiState.Loading<Void> ignored -> setResendLoadingState();
                case UiState.Success<Void> success -> handleResendSuccess();
                case UiState.Error<Void> error -> handleResendError(error.error());
            }
        });

        viewModel.getResendCooldown().observe(getViewLifecycleOwner(), seconds -> {
            if (seconds > 0) {
                tvResend.setEnabled(false);
                tvResend.setText(getString(R.string.reset_password_resend_countdown, seconds));
                tvResend.setTextColor(getThemeColor(android.R.attr.textColorSecondary));
            } else {
                tvResend.setEnabled(true);
                tvResend.setText(R.string.reset_password_resend);
                tvResend.setTextColor(getThemeColor(androidx.appcompat.R.attr.colorPrimary));
                tvResend.announceForAccessibility(getString(R.string.reset_password_resend));
            }
        });
    }

    private void setResetIdleState() {
        btnResetPassword.setEnabled(true);
        btnResetPassword.setText(R.string.reset_password_btn_reset);
        progressReset.setVisibility(View.GONE);
    }

    private void setResetLoadingState() {
        btnResetPassword.setEnabled(false);
        btnResetPassword.setText("");
        progressReset.setVisibility(View.VISIBLE);
        clearErrors();
    }

    private void handleResetSuccess() {
        setResetIdleState();
        viewModel.resetResetState();

        Snackbar.make(requireView(), R.string.reset_password_success_message, Snackbar.LENGTH_LONG)
                .show();

        Navigation.findNavController(requireView()).navigate(R.id.action_resetPassword_to_login);
    }

    private void handleResetError(UiError error) {
        setResetIdleState();

        boolean isOtpError = false;
        boolean isOtpLocked = false;

        switch (error) {
            case UiError.Fail fail -> {
                String code = fail.code();
                if (code != null) {
                    isOtpError = code.equals("OTP_EXPIRED")
                            || code.equals("OTP_INVALID")
                            || code.equals("OTP_ALREADY_USED")
                            || code.equals("OTP_LOCKED");
                    isOtpLocked = code.equals("OTP_LOCKED");
                }

                for (FieldError fe : fail.fieldErrors()) {
                    String msg = fe.message() != null
                            ? fe.message()
                            : resolveValidationMessage(fe.code());
                    switch (fe.field()) {
                        case "otp" -> tilOtp.setError(msg);
                        case "newPassword" -> tilNewPassword.setError(msg);
                        case "confirmPassword" -> tilConfirmPassword.setError(msg);
                        default -> {}
                    }
                }
                if (fail.fieldErrors().isEmpty() && fail.message() != null) {
                    tilOtp.setError(fail.message());
                }
            }
            case UiError.ServerError s -> tilOtp.setError(getString(R.string.error_server));
            case UiError.NetworkError n -> tilOtp.setError(getString(R.string.error_network));
            case UiError.Unknown u -> tilOtp.setError(getString(R.string.error_unknown));
        }

        if (isOtpError) {
            edtOtp.setText("");
        }

        if (isOtpLocked) {
            Snackbar.make(requireView(), R.string.error_otp_locked, Snackbar.LENGTH_LONG)
                    .show();
            Navigation.findNavController(requireView()).navigateUp();
        }
    }

    private void setResendIdleState() {
        progressResend.setVisibility(View.GONE);
    }

    private void setResendLoadingState() {
        progressResend.setVisibility(View.VISIBLE);
        tvResend.setEnabled(false);
    }

    private void handleResendSuccess() {
        setResendIdleState();
        viewModel.resetResendState();
        Snackbar.make(requireView(), R.string.reset_password_code_resent, Snackbar.LENGTH_SHORT)
                .show();
    }

    private void handleResendError(UiError error) {
        setResendIdleState();
        String message =
                switch (error) {
                    case UiError.Fail fail ->
                        fail.message() != null ? fail.message() : getString(R.string.error_unknown);
                    case UiError.ServerError s -> getString(R.string.error_server);
                    case UiError.NetworkError n -> getString(R.string.error_network);
                    case UiError.Unknown u -> getString(R.string.error_unknown);
                };
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show();
    }

    private String resolveValidationMessage(String code) {
        return switch (code) {
            case "REQUIRED" -> getString(R.string.validation_required);
            case "FORMAT" -> getString(R.string.validation_invalid_format);
            case "MIN_LENGTH" -> getString(R.string.validation_password_min_length);
            case "MISMATCH" -> getString(R.string.validation_passwords_mismatch);
            case "OTP_EXPIRED" -> getString(R.string.error_otp_expired);
            case "OTP_INVALID" -> getString(R.string.error_otp_invalid);
            case "OTP_ALREADY_USED" -> getString(R.string.error_otp_already_used);
            case "OTP_LOCKED" -> getString(R.string.error_otp_locked);
            case "RATE_LIMIT_EXCEEDED" -> getString(R.string.error_rate_limit_exceeded);
            case "GOOGLE_ONLY_ACCOUNT" -> getString(R.string.error_google_only_account);
            default -> getString(R.string.validation_invalid_value);
        };
    }

    private void clearErrors() {
        tilOtp.setError(null);
        tilNewPassword.setError(null);
        tilConfirmPassword.setError(null);
    }

    /**
     * Resolves a color from a theme attribute.
     */
    private int getThemeColor(int attrResId) {
        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(attrResId, typedValue, true);
        return typedValue.data;
    }
}
