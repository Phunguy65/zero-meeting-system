package io.github.phunguy65.zms.presentation.videocall;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.card.MaterialCardView;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.domain.model.VideoLayout;
import io.github.phunguy65.zms.frontends.R;

/**
 * Bottom sheet dialog for selecting video layout mode.
 * Allows users to choose between Auto, Tiled, Spotlight, and Sidebar layouts.
 */
@AndroidEntryPoint
public class LayoutPickerBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "LayoutPickerBottomSheet";

    private CallViewModel viewModel;

    private LinearLayout cardAuto;
    private LinearLayout cardTiled;
    private LinearLayout cardSpotlight;
    private LinearLayout cardSidebar;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(CallViewModel.class);
    }

    @NonNull
    @Override
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
            }
        });

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_layout_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupLayoutCards();
        setupListeners();
        setupObservers();
    }

    private void initViews(View view) {
        cardAuto = view.findViewById(R.id.cardAuto);
        cardTiled = view.findViewById(R.id.cardTiled);
        cardSpotlight = view.findViewById(R.id.cardSpotlight);
        cardSidebar = view.findViewById(R.id.cardSidebar);
    }

    private void setupLayoutCards() {
        setupCard(cardAuto, R.drawable.ic_layout_auto, R.string.layout_auto);
        setupCard(cardTiled, R.drawable.ic_layout_tiled, R.string.layout_tiled);
        setupCard(cardSpotlight, R.drawable.ic_layout_spotlight, R.string.layout_spotlight);
        setupCard(cardSidebar, R.drawable.ic_layout_sidebar, R.string.layout_sidebar);
    }

    private void setupCard(LinearLayout card, int iconRes, int labelRes) {
        ImageView icon = card.findViewById(R.id.ivLayoutIcon);
        TextView label = card.findViewById(R.id.tvLayoutLabel);

        icon.setImageResource(iconRes);
        label.setText(labelRes);

        String labelText = getString(labelRes);
        card.setContentDescription(getString(R.string.cd_layout_option, labelText));
    }

    private void setupListeners() {
        cardAuto.setOnClickListener(v -> selectLayout(VideoLayout.AUTO));
        cardTiled.setOnClickListener(v -> selectLayout(VideoLayout.TILED));
        cardSpotlight.setOnClickListener(v -> selectLayout(VideoLayout.SPOTLIGHT));
        cardSidebar.setOnClickListener(v -> selectLayout(VideoLayout.SIDEBAR));
    }

    private void setupObservers() {
        viewModel.getCurrentLayout().observe(getViewLifecycleOwner(), this::updateSelectedState);
    }

    private void selectLayout(VideoLayout layout) {
        viewModel.setCurrentLayout(layout);
        dismiss();
    }

    private void updateSelectedState(VideoLayout currentLayout) {
        updateCardState(cardAuto, currentLayout == VideoLayout.AUTO);
        updateCardState(cardTiled, currentLayout == VideoLayout.TILED);
        updateCardState(cardSpotlight, currentLayout == VideoLayout.SPOTLIGHT);
        updateCardState(cardSidebar, currentLayout == VideoLayout.SIDEBAR);
    }

    private void updateCardState(LinearLayout card, boolean isSelected) {
        MaterialCardView iconCard = card.findViewById(R.id.cardIcon);
        ImageView icon = card.findViewById(R.id.ivLayoutIcon);
        TextView label = card.findViewById(R.id.tvLayoutLabel);

        android.util.TypedValue typedValue = new android.util.TypedValue();

        if (isSelected) {
            requireContext().getTheme().resolveAttribute(
                    androidx.appcompat.R.attr.colorPrimary, typedValue, true);
            int primaryColor = typedValue.data;

            iconCard.setStrokeColor(primaryColor);
            iconCard.setStrokeWidth((int) getResources().getDimension(R.dimen.spacing_xxs));
            icon.setImageTintList(android.content.res.ColorStateList.valueOf(primaryColor));
            card.setContentDescription(getString(R.string.cd_layout_selected, label.getText()));
        } else {
            requireContext().getTheme().resolveAttribute(
                    android.R.attr.textColorPrimary, typedValue, true);
            int onSurfaceColor = typedValue.data;

            iconCard.setStrokeColor(android.graphics.Color.TRANSPARENT);
            iconCard.setStrokeWidth(0);
            icon.setImageTintList(android.content.res.ColorStateList.valueOf(onSurfaceColor));
            card.setContentDescription(getString(R.string.cd_layout_option, label.getText()));
        }
    }
}
