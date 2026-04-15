package io.github.phunguy65.zms.presentation.common;

import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.xmlpull.v1.XmlPullParser;

/**
 * Material 3 Bottom Sheet for language selection.
 * <p>
 * Parses available languages from locales_config.xml at runtime.
 * Provides smooth selection animation before applying locale change.
 * Reusable across Welcome, Login, Register, and Settings screens.
 * <p>
 * Persists the selected language via {@link LanguagePickerViewModel} for future server sync.
 */
@AndroidEntryPoint
public class LanguagePickerSheet extends BottomSheetDialogFragment {

    private static final String TAG = "LanguagePickerSheet";
    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
    private static final long SELECTION_DELAY_MS = 200;
    private static final long CHECKMARK_ANIM_MS = 150;

    private LanguagePickerViewModel viewModel;
    private RecyclerView rvLanguages;
    private LanguageAdapter adapter;
    private List<LanguageItem> languages;
    private String currentLanguageCode;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingLocaleChange;

    /**
     * Shows the language picker bottom sheet.
     *
     * @param fragmentManager Fragment manager to use (getChildFragmentManager() for fragments,
     *                        getSupportFragmentManager() for activities)
     */
    public static void show(FragmentManager fragmentManager) {
        new LanguagePickerSheet().show(fragmentManager, TAG);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(LanguagePickerViewModel.class);
        languages = parseLocalesConfig();
        currentLanguageCode = getCurrentLanguageCode();
    }

    @Nullable @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_language_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvLanguages = view.findViewById(R.id.rvLanguages);
        adapter = new LanguageAdapter(languages, currentLanguageCode, this::onLanguageSelected);
        rvLanguages.setAdapter(adapter);
    }

    /**
     * Parses locales_config.xml to get available languages.
     * Maps language codes to localized display names using string resources.
     */
    private List<LanguageItem> parseLocalesConfig() {
        List<LanguageItem> items = new ArrayList<>();
        try {
            XmlResourceParser parser = getResources().getXml(R.xml.locales_config);
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && "locale".equals(parser.getName())) {
                    String code = parser.getAttributeValue(ANDROID_NS, "name");
                    if (code != null) {
                        LanguageItem item = createLanguageItem(code);
                        items.add(item);
                    }
                }
                eventType = parser.next();
            }
            parser.close();
        } catch (Exception e) {
            items.add(new LanguageItem(
                    "en",
                    getString(R.string.language_english_native),
                    getString(R.string.language_english_label)));
            items.add(new LanguageItem(
                    "vi",
                    getString(R.string.language_vietnamese_native),
                    getString(R.string.language_vietnamese_label)));
        }
        return items;
    }

    /**
     * Creates a LanguageItem from a language code using string resources.
     * Falls back to Locale API for unknown codes to support future language additions.
     */
    @NonNull private LanguageItem createLanguageItem(String code) {
        switch (code) {
            case "en":
                return new LanguageItem(
                        code,
                        getString(R.string.language_english_native),
                        getString(R.string.language_english_label));
            case "vi":
                return new LanguageItem(
                        code,
                        getString(R.string.language_vietnamese_native),
                        getString(R.string.language_vietnamese_label));
            default:
                Locale locale = Locale.forLanguageTag(code);
                String nativeName = locale.getDisplayLanguage(locale);
                String englishName = locale.getDisplayLanguage(Locale.ENGLISH);
                if (!nativeName.isEmpty()) {
                    nativeName =
                            Character.toUpperCase(nativeName.charAt(0)) + nativeName.substring(1);
                }
                if (!englishName.isEmpty()) {
                    englishName =
                            Character.toUpperCase(englishName.charAt(0)) + englishName.substring(1);
                }
                String label = nativeName.equals(englishName)
                        ? nativeName
                        : nativeName + " (" + englishName + ")";
                return new LanguageItem(code, nativeName, label);
        }
    }

    /**
     * Gets the current app language code.
     */
    private String getCurrentLanguageCode() {
        String langTag = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        if (langTag.isEmpty()) {
            return "en";
        }
        int dashIndex = langTag.indexOf('-');
        return dashIndex > 0 ? langTag.substring(0, dashIndex) : langTag;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pendingLocaleChange != null) {
            handler.removeCallbacks(pendingLocaleChange);
            pendingLocaleChange = null;
        }
    }

    /**
     * Handles language selection with animation.
     * 1. Animate checkmark transition (150ms)
     * 2. Delay to let user see selection (200ms)
     * 3. Apply locale change and dismiss
     */
    private void onLanguageSelected(LanguageItem item, int oldPosition, int newPosition) {
        if (item.getCode().equals(currentLanguageCode)) {
            dismiss();
            return;
        }

        currentLanguageCode = item.getCode();
        adapter.setSelectedCode(currentLanguageCode);
        adapter.notifyItemChanged(oldPosition);
        adapter.notifyItemChanged(newPosition);

        if (pendingLocaleChange != null) {
            handler.removeCallbacks(pendingLocaleChange);
        }

        pendingLocaleChange = () -> {
            // Persist language preference via ViewModel for future server sync
            viewModel.saveLanguage(item.getCode());
            // Apply locale change via AppCompatDelegate
            LocaleListCompat locales = LocaleListCompat.forLanguageTags(item.getCode());
            AppCompatDelegate.setApplicationLocales(locales);
            dismiss();
        };
        handler.postDelayed(pendingLocaleChange, SELECTION_DELAY_MS + CHECKMARK_ANIM_MS);
    }

    /**
     * RecyclerView adapter for language list.
     */
    private static class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.ViewHolder> {

        private final List<LanguageItem> items;
        private String selectedCode;
        private final OnLanguageClickListener listener;

        interface OnLanguageClickListener {
            void onLanguageClick(LanguageItem item, int oldPosition, int newPosition);
        }

        LanguageAdapter(
                List<LanguageItem> items, String selectedCode, OnLanguageClickListener listener) {
            this.items = items;
            this.selectedCode = selectedCode;
            this.listener = listener;
        }

        void setSelectedCode(String code) {
            this.selectedCode = code;
        }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_language, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            LanguageItem item = items.get(position);
            holder.tvLangLabel.setText(item.getLabel());

            boolean isSelected = item.getCode().equals(selectedCode);
            holder.ivCheck.setVisibility(isSelected ? View.VISIBLE : View.GONE);

            if (isSelected) {
                holder.ivCheck.setAlpha(0f);
                holder.ivCheck.animate().alpha(1f).setDuration(150).start();
            }

            holder.itemView.setSelected(isSelected);
            holder.itemView.setContentDescription(item.getLabel()
                    + (isSelected
                            ? ", "
                                    + holder.itemView
                                            .getContext()
                                            .getString(R.string.language_selected)
                            : ""));

            holder.itemView.setOnClickListener(v -> {
                int adapterPosition = holder.getBindingAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION) {
                    return;
                }
                int oldPosition = findPositionByCode(selectedCode);
                listener.onLanguageClick(item, oldPosition, adapterPosition);
            });
        }

        private int findPositionByCode(String code) {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getCode().equals(code)) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final ImageView ivLangIcon;
            final TextView tvLangLabel;
            final ImageView ivCheck;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivLangIcon = itemView.findViewById(R.id.ivLangIcon);
                tvLangLabel = itemView.findViewById(R.id.tvLangLabel);
                ivCheck = itemView.findViewById(R.id.ivCheck);
            }
        }
    }
}
