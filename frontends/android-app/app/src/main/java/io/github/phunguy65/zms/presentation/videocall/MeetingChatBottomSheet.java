package io.github.phunguy65.zms.presentation.videocall;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;
import io.github.phunguy65.zms.presentation.meeting.chat.MeetingChatViewModel;

/**
 * Bottom sheet dialog for meeting chat.
 * Replaces MeetingChatActivity for use within VideoCallActivity.
 * Does NOT include mini video preview (removed per spec).
 */
@AndroidEntryPoint
public class MeetingChatBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "MeetingChatBottomSheet";

    private MeetingChatViewModel viewModel;

    // Views
    private View btnCloseContainer, btnAttachContainer, btnSendContainer;
    private EditText edtMessage;
    private RecyclerView rvChat;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MeetingChatViewModel.class);
    }

    @NonNull @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
            FrameLayout bottomSheet = bottomSheetDialog.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
                // Set to 70% of screen height
                int screenHeight = getResources().getDisplayMetrics().heightPixels;
                behavior.setPeekHeight((int) (screenHeight * 0.7));
            }
        });

        return dialog;
    }

    @Nullable @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_meeting_chat_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupRecyclerView();
        setupListeners();
    }

    private void initViews(View view) {
        btnCloseContainer = view.findViewById(R.id.btnCloseContainer);
        btnAttachContainer = view.findViewById(R.id.btnAttachContainer);
        btnSendContainer = view.findViewById(R.id.btnSendContainer);
        edtMessage = view.findViewById(R.id.edtMessage);
        rvChat = view.findViewById(R.id.rvChat);
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        rvChat.setLayoutManager(layoutManager);
        // TODO: Set adapter when chat messages are implemented
    }

    private void setupListeners() {
        btnCloseContainer.setOnClickListener(v -> dismiss());

        btnAttachContainer.setOnClickListener(v -> {
            Snackbar.make(
                            requireView(),
                            R.string.call_attachment_coming_soon,
                            Snackbar.LENGTH_SHORT)
                    .show();
        });

        btnSendContainer.setOnClickListener(v -> {
            String message = edtMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                viewModel.sendMessage(message);
                edtMessage.setText("");
                Snackbar.make(requireView(), R.string.call_message_sent, Snackbar.LENGTH_SHORT)
                        .show();
            }
        });
    }
}
