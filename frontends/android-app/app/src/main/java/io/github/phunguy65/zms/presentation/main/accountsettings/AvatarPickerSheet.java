package io.github.phunguy65.zms.presentation.main.accountsettings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import io.github.phunguy65.zms.frontends.R;

/**
 * Bottom sheet for avatar picker options.
 *
 * <p>Shows options to:
 * <ul>
 *   <li>Take a photo with camera</li>
 *   <li>Choose from gallery</li>
 *   <li>Remove photo (if user has a custom avatar)</li>
 * </ul>
 *
 * <p>Uses Fragment Result API for lifecycle-safe communication with parent fragment.
 */
public class AvatarPickerSheet extends BottomSheetDialogFragment {

    public static final String TAG = "AvatarPickerSheet";

    // Fragment result keys
    public static final String REQUEST_KEY = "avatar_picker_request";
    public static final String RESULT_KEY = "avatar_picker_result";

    // Result values
    public static final String RESULT_TAKE_PHOTO = "take_photo";
    public static final String RESULT_CHOOSE_GALLERY = "choose_gallery";
    public static final String RESULT_REMOVE_PHOTO = "remove_photo";

    // Argument keys
    private static final String ARG_HAS_CUSTOM_AVATAR = "has_custom_avatar";

    /**
     * Creates a new AvatarPickerSheet with the specified settings.
     *
     * @param hasCustomAvatar whether the user has a custom avatar (shows remove option)
     * @return new AvatarPickerSheet instance
     */
    public static AvatarPickerSheet newInstance(boolean hasCustomAvatar) {
        AvatarPickerSheet sheet = new AvatarPickerSheet();
        Bundle args = new Bundle();
        args.putBoolean(ARG_HAS_CUSTOM_AVATAR, hasCustomAvatar);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_avatar_picker_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvTitle = view.findViewById(R.id.tvTitle);
        if (tvTitle != null) {
            ViewCompat.setAccessibilityHeading(tvTitle, true);
        }

        boolean hasCustomAvatar =
                getArguments() != null && getArguments().getBoolean(ARG_HAS_CUSTOM_AVATAR, false);

        LinearLayout btnTakePhoto = view.findViewById(R.id.btnTakePhoto);
        LinearLayout btnChooseGallery = view.findViewById(R.id.btnChooseGallery);
        LinearLayout btnRemovePhoto = view.findViewById(R.id.btnRemovePhoto);

        btnTakePhoto.setOnClickListener(v -> {
            sendResult(RESULT_TAKE_PHOTO);
            dismiss();
        });

        btnChooseGallery.setOnClickListener(v -> {
            sendResult(RESULT_CHOOSE_GALLERY);
            dismiss();
        });

        btnRemovePhoto.setVisibility(hasCustomAvatar ? View.VISIBLE : View.GONE);
        btnRemovePhoto.setOnClickListener(v -> {
            sendResult(RESULT_REMOVE_PHOTO);
            dismiss();
        });
    }

    private void sendResult(String action) {
        Bundle result = new Bundle();
        result.putString(RESULT_KEY, action);
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
    }

    /**
     * Shows the avatar picker sheet.
     *
     * @param fragmentManager the fragment manager
     * @param hasCustomAvatar whether to show the remove option
     */
    public static void show(FragmentManager fragmentManager, boolean hasCustomAvatar) {
        AvatarPickerSheet sheet = newInstance(hasCustomAvatar);
        sheet.show(fragmentManager, TAG);
    }
}
