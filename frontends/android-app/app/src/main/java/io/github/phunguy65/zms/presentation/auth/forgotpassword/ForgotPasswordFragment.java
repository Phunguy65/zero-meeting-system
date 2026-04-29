package io.github.phunguy65.zms.presentation.auth.forgotpassword;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;
import io.github.phunguy65.zms.presentation.common.state.FieldError;
import io.github.phunguy65.zms.presentation.common.state.UiError;
import io.github.phunguy65.zms.presentation.common.state.UiState;

/**
 * Fragment for the forgot password screen where users enter their email to receive an OTP.
 *
 * <p>On success, navigates to the reset password screen with the email as an argument.
 */
@AndroidEntryPoint
public class ForgotPasswordFragment extends Fragment {

    private ForgotPasswordViewModel viewModel;

    // Views
    private MaterialToolbar toolbar;
    private TextInputLayout tilEmail;
    private TextInputEditText edtEmail;
    private MaterialButton btnSendCode;
    private ProgressBar progressSend;
    private TextView tvBackToLogin;

    @Nullable @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forgot_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ForgotPasswordViewModel.class);

        initViews(view);
        setupToolbar();
        setupListeners();
        observeState();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        tilEmail = view.findViewById(R.id.tilEmail);
        edtEmail = view.findViewById(R.id.edtEmail);
        btnSendCode = view.findViewById(R.id.btnSendCode);
        progressSend = view.findViewById(R.id.progressSend);
        tvBackToLogin = view.findViewById(R.id.tvBackToLogin);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(
                v -> Navigation.findNavController(v).navigateUp());
    }

    private void setupListeners() {
        btnSendCode.setOnClickListener(v -> {
            clearErrors();
            String email =
                    edtEmail.getText() != null ? edtEmail.getText().toString().trim() : "";
            viewModel.requestPasswordReset(email);
        });

        tvBackToLogin.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        addErrorClearingWatcher(edtEmail, tilEmail);
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
        viewModel.getRequestState().observe(getViewLifecycleOwner(), state -> {
            switch (state) {
                case UiState.Idle<String> ignored -> setIdleState();
                case UiState.Loading<String> ignored -> setLoadingState();
                case UiState.Success<String> success -> handleSuccess(success.data());
                case UiState.Error<String> error -> handleError(error.error());
            }
        });
    }

    private void setIdleState() {
        btnSendCode.setEnabled(true);
        btnSendCode.setText(R.string.forgot_password_btn_send);
        progressSend.setVisibility(View.GONE);
    }

    private void setLoadingState() {
        btnSendCode.setEnabled(false);
        btnSendCode.setText("");
        progressSend.setVisibility(View.VISIBLE);
        clearErrors();
    }

    private void handleSuccess(String email) {
        setIdleState();
        viewModel.resetState();

        NavDirections action = ForgotPasswordFragmentDirections.actionForgotPasswordToResetPassword(
                        email)
                .setShowCodeSentMessage(true);
        Navigation.findNavController(requireView()).navigate(action);
    }

    private void handleError(UiError error) {
        setIdleState();
        switch (error) {
            case UiError.Fail fail -> {
                for (FieldError fe : fail.fieldErrors()) {
                    String msg = fe.message() != null
                            ? fe.message()
                            : resolveValidationMessage(fe.code());
                    if ("email".equals(fe.field())) {
                        tilEmail.setError(msg);
                    }
                }
                if (fail.fieldErrors().isEmpty() && fail.message() != null) {
                    tilEmail.setError(fail.message());
                }
            }
            case UiError.ServerError s -> tilEmail.setError(getString(R.string.error_server));
            case UiError.NetworkError n -> tilEmail.setError(getString(R.string.error_network));
            case UiError.Unknown u -> tilEmail.setError(getString(R.string.error_unknown));
        }
    }

    private String resolveValidationMessage(String code) {
        return switch (code) {
            case "REQUIRED" -> getString(R.string.validation_required);
            case "FORMAT" -> getString(R.string.validation_invalid_format);
            default -> getString(R.string.validation_invalid_value);
        };
    }

    private void clearErrors() {
        tilEmail.setError(null);
    }
}
