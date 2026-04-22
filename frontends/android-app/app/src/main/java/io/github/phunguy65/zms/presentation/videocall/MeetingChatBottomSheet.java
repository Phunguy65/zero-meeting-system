package io.github.phunguy65.zms.presentation.videocall;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;
import io.github.phunguy65.zms.presentation.meeting.chat.ChatMessageAdapter;
import io.github.phunguy65.zms.presentation.meeting.chat.MeetingChatViewModel;

/**
 * Bottom sheet dialog for in-meeting chat.
 * Displays chat history, supports sending text messages, and renders
 * real-time incoming messages without leaving the active call.
 */
@AndroidEntryPoint
public class MeetingChatBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "MeetingChatBottomSheet";
    private static final String ARG_ROOM_ID = "roomId";

    private MeetingChatViewModel viewModel;
    private CallViewModel callViewModel;
    private ChatMessageAdapter adapter;

    private View btnCloseContainer;
    private View btnAttachContainer;
    private View btnSendContainer;
    private EditText edtMessage;
    private RecyclerView rvChat;
    private ProgressBar progressLoading;
    private TextView tvEmpty;
    private LinearLayout layoutError;
    private TextView tvError;
    private MaterialButton btnRetry;

    /**
     * Creates a new instance with the given room ID.
     */
    public static MeetingChatBottomSheet newInstance(String roomId) {
        MeetingChatBottomSheet sheet = new MeetingChatBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_ROOM_ID, roomId);
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MeetingChatViewModel.class);
        callViewModel = new ViewModelProvider(requireActivity()).get(CallViewModel.class);
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
        setupObservers();
        initializeChat();
    }

    private void initViews(View view) {
        btnCloseContainer = view.findViewById(R.id.btnCloseContainer);
        btnAttachContainer = view.findViewById(R.id.btnAttachContainer);
        btnSendContainer = view.findViewById(R.id.btnSendContainer);
        edtMessage = view.findViewById(R.id.edtMessage);
        rvChat = view.findViewById(R.id.rvChat);
        progressLoading = view.findViewById(R.id.progressLoading);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        layoutError = view.findViewById(R.id.layoutError);
        tvError = view.findViewById(R.id.tvError);
        btnRetry = view.findViewById(R.id.btnRetry);
    }

    private void setupRecyclerView() {
        adapter = new ChatMessageAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        rvChat.setLayoutManager(layoutManager);
        rvChat.setAdapter(adapter);
    }

    private void setupListeners() {
        btnCloseContainer.setOnClickListener(v -> dismiss());

        btnAttachContainer.setOnClickListener(v -> Snackbar.make(
                        requireView(), R.string.call_attachment_coming_soon, Snackbar.LENGTH_SHORT)
                .show());

        btnSendContainer.setOnClickListener(v -> {
            String message = edtMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                viewModel.sendMessage(message);
            }
        });

        btnRetry.setOnClickListener(v -> viewModel.loadHistory());
    }

    private void setupObservers() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            switch (state) {
                case MeetingChatViewModel.ChatUiState.Loading ignored -> showLoading();
                case MeetingChatViewModel.ChatUiState.Empty ignored -> showEmpty();
                case MeetingChatViewModel.ChatUiState.Content content -> showContent(content);
                case MeetingChatViewModel.ChatUiState.Error error -> showError(error);
            }
        });

        viewModel.isSending().observe(getViewLifecycleOwner(), sending -> {
            btnSendContainer.setEnabled(!sending);
            btnSendContainer.setAlpha(sending ? 0.5f : 1.0f);
        });

        viewModel.getSendError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Snackbar.make(requireView(), R.string.chat_send_failed, Snackbar.LENGTH_SHORT)
                        .show();
                viewModel.clearSendError();
            }
        });

        viewModel.getSendSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                edtMessage.setText("");
                viewModel.clearSendSuccess();
            }
        });
    }

    private void initializeChat() {
        adapter.setCurrentUserId(viewModel.getCurrentUserId());

        String roomId = null;
        if (getArguments() != null) {
            roomId = getArguments().getString(ARG_ROOM_ID);
        }
        if (roomId == null || roomId.isEmpty()) {
            String meetingId = callViewModel.getMeetingId().getValue();
            roomId = meetingId;
        }

        if (roomId != null && !roomId.isEmpty()) {
            viewModel.initialize(roomId);
        } else {
            showError(new MeetingChatViewModel.ChatUiState.Error(
                    getString(R.string.chat_meeting_not_active)));
        }
    }

    private void showLoading() {
        progressLoading.setVisibility(View.VISIBLE);
        rvChat.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
    }

    private void showEmpty() {
        progressLoading.setVisibility(View.GONE);
        rvChat.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
        layoutError.setVisibility(View.GONE);
    }

    private void showContent(MeetingChatViewModel.ChatUiState.Content content) {
        progressLoading.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        rvChat.setVisibility(View.VISIBLE);

        int previousSize = adapter.getCurrentList().size();
        adapter.submitList(content.messages(), () -> {
            if (content.messages().size() > previousSize) {
                rvChat.scrollToPosition(content.messages().size() - 1);
            }
        });
    }

    private void showError(MeetingChatViewModel.ChatUiState.Error error) {
        progressLoading.setVisibility(View.GONE);
        rvChat.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);
        layoutError.setVisibility(View.VISIBLE);
        if (error.messageResId() != 0) {
            tvError.setText(error.messageResId());
        } else if (error.message() != null) {
            tvError.setText(error.message());
        } else {
            tvError.setText(R.string.error_unknown);
        }
    }
}
