package io.github.phunguy65.zms.presentation.main.accountsettings;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;
import io.github.phunguy65.zms.presentation.common.util.InitialsDrawable;
import io.github.phunguy65.zms.presentation.welcome.WelcomeActivity;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Fragment for editing user account settings.
 *
 * <p>Allows editing of avatar, full name, and username. Email is read-only.
 */
@AndroidEntryPoint
public class AccountSettingsFragment extends Fragment {

    private AccountSettingsViewModel viewModel;
    private NavController navController;

    // Views
    private ProgressBar progressBar;
    private ImageView imgAvatar;
    private TextView tvChangePhoto;
    private TextInputLayout tilFullName;
    private TextInputEditText etFullName;
    private TextInputLayout tilUsername;
    private TextInputEditText etUsername;
    private TextInputLayout tilEmail;
    private TextInputEditText etEmail;
    private MaterialButton btnSave;
    private MaterialButton btnDeleteAccount;

    private AlertDialog deleteDialog;
    private String lastConfirmText = "";

    private boolean isUpdatingFields = false;

    private Uri pendingCameraPhotoUri;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    viewModel.setNewAvatarUri(uri);
                }
            });

    private final ActivityResultLauncher<Uri> takePicture =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && pendingCameraPhotoUri != null) {
                    viewModel.setNewAvatarUri(pendingCameraPhotoUri);
                }
            });

    private final ActivityResultLauncher<String> requestCameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    launchCamera();
                } else {
                    Snackbar.make(
                                    requireView(),
                                    R.string.permission_camera_required,
                                    Snackbar.LENGTH_LONG)
                            .show();
                }
            });

    @Nullable @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AccountSettingsViewModel.class);
        navController = NavHostFragment.findNavController(this);

        initViews(view);
        setupListeners();
        setupBackPressHandler();
        observeViewModel();
    }

    private void initViews(View view) {
        progressBar = view.findViewById(R.id.progressBar);
        imgAvatar = view.findViewById(R.id.imgAvatar);
        tvChangePhoto = view.findViewById(R.id.tvChangePhoto);
        tilFullName = view.findViewById(R.id.tilFullName);
        etFullName = view.findViewById(R.id.etFullName);
        tilUsername = view.findViewById(R.id.tilUsername);
        etUsername = view.findViewById(R.id.etUsername);
        tilEmail = view.findViewById(R.id.tilEmail);
        etEmail = view.findViewById(R.id.etEmail);
        btnSave = view.findViewById(R.id.btnSave);
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount);

        getChildFragmentManager()
                .setFragmentResultListener(
                        AvatarPickerSheet.REQUEST_KEY,
                        getViewLifecycleOwner(),
                        (requestKey, result) -> {
                            String action = result.getString(AvatarPickerSheet.RESULT_KEY);
                            if (action != null) {
                                switch (action) {
                                    case AvatarPickerSheet.RESULT_TAKE_PHOTO ->
                                        checkCameraPermissionAndLaunch();
                                    case AvatarPickerSheet.RESULT_CHOOSE_GALLERY ->
                                        launchGalleryPicker();
                                    case AvatarPickerSheet.RESULT_REMOVE_PHOTO ->
                                        viewModel.removeAvatar();
                                }
                            }
                        });
    }

    private void setupListeners() {
        View.OnClickListener avatarClickListener = v -> showAvatarPicker();
        imgAvatar.setOnClickListener(avatarClickListener);
        tvChangePhoto.setOnClickListener(avatarClickListener);

        etFullName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!isUpdatingFields) {
                    viewModel.setFullName(s.toString());
                }
            }
        });

        etUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!isUpdatingFields) {
                    viewModel.setUsername(s.toString());
                }
            }
        });

        btnSave.setOnClickListener(v -> viewModel.saveProfile());
        btnDeleteAccount.setOnClickListener(v -> viewModel.requestDeleteAccount());
    }

    private void setupBackPressHandler() {
        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (viewModel.hasChanges()) {
                            showDiscardDialog();
                        } else {
                            setEnabled(false);
                            requireActivity().getOnBackPressedDispatcher().onBackPressed();
                        }
                    }
                });
    }

    private void showDiscardDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.account_settings_discard_title)
                .setMessage(R.string.account_settings_discard_message)
                .setPositiveButton(R.string.account_settings_discard_confirm, (dialog, which) -> {
                    navController.popBackStack();
                })
                .setNegativeButton(R.string.account_settings_discard_cancel, null)
                .show();
    }

    private void showAvatarPicker() {
        AccountSettingsViewModel.AccountSettingsUiState state =
                viewModel.getUiState().getValue();
        boolean hasCustomAvatar =
                state instanceof AccountSettingsViewModel.AccountSettingsUiState.Content content
                        && content.avatarUrl() != null
                        && !content.avatarRemoved();

        AvatarPickerSheet.show(getChildFragmentManager(), hasCustomAvatar);
    }

    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        try {
            File photoFile = createImageFile();
            pendingCameraPhotoUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    photoFile);
            takePicture.launch(pendingCameraPhotoUri);
        } catch (IOException e) {
            Snackbar.make(requireView(), R.string.error_camera_file, Snackbar.LENGTH_SHORT)
                    .show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "AVATAR_" + timeStamp + "_";
        File storageDir = requireContext().getCacheDir();
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void launchGalleryPicker() {
        pickMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), this::handleUiState);
        viewModel.getSaveEvent().observe(getViewLifecycleOwner(), this::handleSaveEvent);
        viewModel.getDeleteUiState().observe(getViewLifecycleOwner(), this::handleDeleteUiState);
    }

    private void handleUiState(AccountSettingsViewModel.AccountSettingsUiState state) {
        switch (state) {
            case AccountSettingsViewModel.AccountSettingsUiState.Loading loading -> showLoading();
            case AccountSettingsViewModel.AccountSettingsUiState.Content content ->
                showContent(content);
            case AccountSettingsViewModel.AccountSettingsUiState.Error error -> showError(error);
        }
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);
    }

    private void showContent(AccountSettingsViewModel.AccountSettingsUiState.Content content) {
        progressBar.setVisibility(View.GONE);

        isUpdatingFields = true;
        if (!etFullName.getText().toString().equals(content.fullName())) {
            etFullName.setText(content.fullName());
        }
        if (!etUsername.getText().toString().equals(content.username())) {
            etUsername.setText(content.username());
        }
        if (!etEmail.getText().toString().equals(content.email())) {
            etEmail.setText(content.email());
        }
        isUpdatingFields = false;

        tilFullName.setError(content.fullNameError());
        tilUsername.setError(content.usernameError());

        btnSave.setEnabled(content.hasChanges() && !content.isSaving());
        btnSave.setText(
                content.isSaving()
                        ? R.string.account_settings_saving
                        : R.string.account_settings_save);

        boolean canInteract = !content.isSaving();
        imgAvatar.setEnabled(canInteract);
        imgAvatar.setClickable(canInteract);
        tvChangePhoto.setEnabled(canInteract);
        tvChangePhoto.setClickable(canInteract);
        etFullName.setEnabled(canInteract);
        etUsername.setEnabled(canInteract);

        if (content.isSaving()) {
            imgAvatar.setContentDescription(getString(R.string.account_settings_saving));
        } else {
            imgAvatar.setContentDescription(getString(R.string.cd_avatar_tap_to_change));
        }

        loadAvatar(content);
    }

    private void loadAvatar(AccountSettingsViewModel.AccountSettingsUiState.Content content) {
        InitialsDrawable fallbackDrawable = new InitialsDrawable(content.fullName(), null);

        if (content.pendingAvatarUri() != null) {
            Glide.with(this)
                    .load(content.pendingAvatarUri())
                    .transform(new CircleCrop())
                    .placeholder(fallbackDrawable)
                    .error(fallbackDrawable)
                    .into(imgAvatar);
        } else if (content.avatarRemoved()) {
            imgAvatar.setImageDrawable(fallbackDrawable);
        } else if (content.avatarUrl() != null && !content.avatarUrl().isEmpty()) {
            Glide.with(this)
                    .load(content.avatarUrl())
                    .transform(new CircleCrop())
                    .placeholder(fallbackDrawable)
                    .error(fallbackDrawable)
                    .into(imgAvatar);
        } else {
            imgAvatar.setImageDrawable(fallbackDrawable);
        }
    }

    private void showError(AccountSettingsViewModel.AccountSettingsUiState.Error error) {
        progressBar.setVisibility(View.GONE);
        btnSave.setEnabled(false);

        Snackbar.make(requireView(), error.message(), Snackbar.LENGTH_LONG)
                .setAction(R.string.retry, v -> viewModel.loadProfile())
                .show();
    }

    private void handleSaveEvent(AccountSettingsViewModel.SaveEvent event) {
        if (event == null) return;

        switch (event) {
            case AccountSettingsViewModel.SaveEvent.Success success -> {
                Snackbar.make(requireView(), R.string.account_settings_saved, Snackbar.LENGTH_SHORT)
                        .show();
                navController.popBackStack();
            }
            case AccountSettingsViewModel.SaveEvent.Error error -> {
                Snackbar.make(requireView(), error.message(), Snackbar.LENGTH_LONG)
                        .setAction(R.string.retry, v -> viewModel.saveProfile())
                        .show();
            }
        }
    }

    private void handleDeleteUiState(AccountSettingsViewModel.DeleteUiState state) {
        switch (state) {
            case AccountSettingsViewModel.DeleteUiState.Idle idle -> dismissDeleteDialog();
            case AccountSettingsViewModel.DeleteUiState.Confirming confirming ->
                    showDeleteConfirmationDialog();
            case AccountSettingsViewModel.DeleteUiState.Deleting deleting ->
                    updateDeleteDialogDeleting();
            case AccountSettingsViewModel.DeleteUiState.Error error -> {
                dismissDeleteDialog();
                Snackbar.make(
                                requireView(),
                                R.string.account_settings_delete_error,
                                Snackbar.LENGTH_LONG)
                        .setAction(
                                R.string.account_settings_delete_retry,
                                v -> viewModel.confirmDeleteAccount(lastConfirmText))
                        .show();
            }
            case AccountSettingsViewModel.DeleteUiState.Deleted deleted -> {
                dismissDeleteDialog();
                navigateToWelcome();
            }
        }
    }

    private void showDeleteConfirmationDialog() {
        if (deleteDialog != null && deleteDialog.isShowing()) {
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_delete_account, null);

        TextInputEditText etConfirm = dialogView.findViewById(R.id.etDeleteConfirm);

        deleteDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.account_settings_delete_dialog_title)
                .setMessage(R.string.account_settings_delete_dialog_message)
                .setView(dialogView)
                .setPositiveButton(R.string.account_settings_delete_confirm_button, null)
                .setNegativeButton(R.string.account_settings_delete_cancel_button,
                        (dialog, which) -> viewModel.cancelDelete())
                .setOnCancelListener(dialog -> viewModel.cancelDelete())
                .create();

        deleteDialog.show();

        Button positiveButton = deleteDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setEnabled(false);

        etConfirm.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                lastConfirmText = s.toString();
                positiveButton.setEnabled("DELETE".equals(lastConfirmText));
            }
        });

        positiveButton.setOnClickListener(
                v -> viewModel.confirmDeleteAccount(lastConfirmText));
    }

    private void updateDeleteDialogDeleting() {
        if (deleteDialog == null || !deleteDialog.isShowing()) {
            return;
        }

        Button positiveButton = deleteDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = deleteDialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        positiveButton.setEnabled(false);
        positiveButton.setText(R.string.account_settings_deleting);
        negativeButton.setEnabled(false);
        deleteDialog.setCancelable(false);

        TextInputEditText etConfirm = deleteDialog.findViewById(R.id.etDeleteConfirm);
        if (etConfirm != null) {
            etConfirm.setEnabled(false);
        }
    }

    private void dismissDeleteDialog() {
        if (deleteDialog != null && deleteDialog.isShowing()) {
            deleteDialog.dismiss();
        }
        deleteDialog = null;
    }

    private void navigateToWelcome() {
        Intent intent = new Intent(requireContext(), WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
