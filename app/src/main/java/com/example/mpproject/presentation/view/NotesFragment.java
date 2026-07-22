package com.example.mpproject.presentation.view;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.mpproject.R;
import com.example.mpproject.data.local.AppDatabase;
import com.example.mpproject.data.repository.ModuleNoteRepositoryImpl;
import com.example.mpproject.data.repository.ModuleRepositoryImpl;
import com.example.mpproject.data.repository.PersonalNoteAttachmentRepositoryImpl;
import com.example.mpproject.data.repository.PersonalNoteRepositoryImpl;
import com.example.mpproject.databinding.FragmentNotesBinding;
import com.example.mpproject.domain.model.Module;
import com.example.mpproject.domain.model.ModuleNote;
import com.example.mpproject.domain.model.PersonalNote;
import com.example.mpproject.domain.model.PersonalNoteAttachment;
import com.example.mpproject.presentation.adapter.NotesAdapter;
import com.example.mpproject.presentation.model.NoteListItem;
import com.example.mpproject.presentation.viewmodel.NotesViewModel;
import com.example.mpproject.presentation.viewmodel.NotesViewModelFactory;
import com.example.mpproject.data.repository.NoteFolderRepositoryImpl;
import com.example.mpproject.domain.model.NoteFolder;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.example.mpproject.data.local.LocalSessionManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import android.content.res.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
public class NotesFragment extends Fragment {
    private FragmentNotesBinding binding;
    private NotesViewModel viewModel;
    private NotesAdapter adapter;
    private List<Module> currentModules = new ArrayList<>();
    private List<NoteFolder> currentFolders = new ArrayList<>();
    private String pendingUploadTitle;
    private String pendingUploadModuleId;
    private String pendingUploadFolderId;
    private boolean populatingModuleFilter = false;
    private boolean populatingFolderFilter = false;
    private String selectedModuleFilterId = null;
    private String selectedFolderFilterId = null;
    private String incomingModuleFilterId = null;
    private boolean incomingModuleFilterApplied = false;
    private ActivityResultLauncher<String[]> moduleFilePickerLauncher;
    private ActivityResultLauncher<String[]> personalAttachmentPickerLauncher;
    private String activeAttachmentNoteId;
    private boolean activeAttachmentBelongsToUnsavedDraft = false;
    private final List<PersonalNoteAttachment> temporaryAttachments = new ArrayList<>();
    private NoteListItem temporaryDraftItem = null;
    private boolean activeBold = false;
    private boolean activeItalic = false;
    private boolean activeUnderline = false;
    private boolean activeHighlight = false;
    private float activeTextSizeScale = 1.0f;
    private boolean applyingTypingFormat = false;
    private int lastTypingStart = -1;
    private int lastTypingCount = 0;
    // Track the current active alignment so it can be carried to new paragraphs
    private android.text.Layout.Alignment activeAlignment = android.text.Layout.Alignment.ALIGN_NORMAL;
    private static final int HIGHLIGHT_COLOR = 0xFFFFFF66;
    private static final int SCREEN_BG = 0xFFEAF5FF;
    private static final int DARK_BLUE = 0xFF08123D;
    private static final int PRIMARY_BLUE = 0xFF2563EB;
    private static final int BORDER_BLUE = 0xFFDDE7FF;
    private static final int MUTED_TEXT = 0xFF6B7280;
    private static final int TOP_SKY_BLUE = 0xFFE3F2FF;
    private static final int EDIT_BUTTON = 0xFF4F6F8F;
    private static final int VIEW_BUTTON = 0xFF6B5B95;
    private static final long MAX_UPLOAD_BYTES = 25L * 1024L * 1024L; // 25 MB safety limit
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNotesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        moduleFilePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri == null) return;
                    persistReadPermission(uri);
                    uploadSelectedModuleFile(uri);
                }
        );
        personalAttachmentPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri == null) return;
                    persistReadPermission(uri);
                    uploadSelectedPersonalAttachment(uri);
                }
        );
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String userId = LocalSessionManager.getCurrentUserId(requireContext());
        if (userId == null) {
            Toast.makeText(requireContext(), "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        NotesViewModelFactory factory = new NotesViewModelFactory(
                userId,
                new ModuleRepositoryImpl(db.moduleDao()),
                new ModuleNoteRepositoryImpl(db.moduleNoteDao()),
                new PersonalNoteRepositoryImpl(db.personalNoteDao()),
                new PersonalNoteAttachmentRepositoryImpl(db.personalNoteAttachmentDao()),
                new NoteFolderRepositoryImpl(db.noteFolderDao())
        );
        viewModel = new ViewModelProvider(this, factory).get(NotesViewModel.class);

        readIncomingModuleFilterArgument();

        setupRecyclerView();
        setupButtons();
        observeData();

        if (incomingModuleFilterId != null) {
            viewModel.setFilter(NotesViewModel.FILTER_ALL);
            viewModel.setSelectedModuleId(incomingModuleFilterId);
            updateFilterUi(R.id.chip_all);

            if (binding.chipGroupFilter != null) {
                binding.chipGroupFilter.check(R.id.chip_all);
            }
        }
    }
    private void persistReadPermission(Uri uri) {
        if (uri == null || getContext() == null) return;

        try {
            requireContext().getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (SecurityException | IllegalArgumentException e) {
            safeToast("Could not keep long-term access to this file. Upload will still be attempted.",
                    Toast.LENGTH_LONG);
        }
    }
    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
    private boolean isDarkMode() {
        int nightMode = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;

        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }
    private GradientDrawable roundedBg(int color, int strokeColor, int strokeWidthDp, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeWidthDp > 0) drawable.setStroke(dp(strokeWidthDp), strokeColor);
        return drawable;
    }
    private MaterialButton makeCleanButton(String text, int bgColor, int textColor) {
        MaterialButton button = new MaterialButton(requireContext());
        button.setText(text);
        button.setTextSize(15);
        button.setAllCaps(false);
        button.setTextColor(textColor);
        button.setCornerRadius(dp(16));
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(18), dp(10), dp(18), dp(10));
        button.setBackgroundTintList(ColorStateList.valueOf(bgColor));
        return button;
    }
    private MaterialButton makeTopBarButton(String text) {
        MaterialButton button = new MaterialButton(requireContext());
        button.setText(text);
        button.setTextSize(text.length() <= 2 ? 20 : 14);
        button.setAllCaps(false);
        button.setTextColor(DARK_BLUE);
        button.setCornerRadius(dp(15));
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(14), dp(6), dp(14), dp(6));
        button.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
        button.setStrokeColor(ColorStateList.valueOf(PRIMARY_BLUE));
        button.setStrokeWidth(dp(1));
        addButtonHint(button, text.equals("←") ? "Back" : text);
        return button;
    }
    private void addButtonHint(View button, String hint) {
        button.setTooltipText(hint);
        button.setContentDescription(hint);
        button.setFocusable(true);
        button.setOnLongClickListener(v -> {
            Toast.makeText(requireContext(), hint, Toast.LENGTH_SHORT).show();
            return true;
        });
        button.setOnHoverListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_HOVER_ENTER) {
                Toast.makeText(requireContext(), hint, Toast.LENGTH_SHORT).show();
            }
            return false;
        });
    }
    private void resetFormattingState() {
        activeBold = false;
        activeItalic = false;
        activeUnderline = false;
        activeHighlight = false;
        activeTextSizeScale = 1.0f;
        activeAlignment = android.text.Layout.Alignment.ALIGN_NORMAL;
        applyingTypingFormat = false;
        lastTypingStart = -1;
        lastTypingCount = 0;
    }
    private ArrayAdapter<String> createModuleSpinnerAdapter(List<String> moduleNames) {
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                moduleNames
        ) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);

                view.setTextSize(14);
                view.setSingleLine(true);
                view.setPadding(dp(12), 0, dp(8), 0);

                if (isDarkMode()) {
                    view.setTextColor(Color.WHITE);
                    view.setBackgroundColor(Color.TRANSPARENT);
                } else {
                    view.setTextColor(DARK_BLUE);
                    view.setBackgroundColor(Color.TRANSPARENT);
                }

                return view;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);

                view.setTextSize(14);
                view.setPadding(dp(14), dp(10), dp(14), dp(10));

                if (isDarkMode()) {
                    view.setTextColor(Color.WHITE);
                    view.setBackgroundColor(0xFF111827);
                } else {
                    view.setTextColor(DARK_BLUE);
                    view.setBackgroundColor(Color.WHITE);
                }

                return view;
            }
        };

        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return spinnerAdapter;
    }
    private LinearLayout createTopBar(MaterialButton btnBack,
                                      TextView topTitle,
                                      MaterialButton primaryButton,
                                      @Nullable MaterialButton secondaryButton,
                                      @Nullable View underRightView) {
        LinearLayout topBar = new LinearLayout(requireContext());
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.TOP);
        topBar.setPadding(dp(18), dp(14), dp(18), dp(4));
        topBar.setBackgroundColor(TOP_SKY_BLUE);
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(dp(48), dp(42));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, dp(42), 1);
        titleParams.setMargins(dp(10), 0, dp(8), 0);
        LinearLayout rightColumn = new LinearLayout(requireContext());
        rightColumn.setOrientation(LinearLayout.VERTICAL);
        rightColumn.setGravity(Gravity.END);
        LinearLayout buttonRow = new LinearLayout(requireContext());
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setGravity(Gravity.END);
        if (secondaryButton != null) {
            LinearLayout.LayoutParams secondaryParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, dp(42));
            secondaryParams.setMargins(0, 0, dp(8), 0);
            buttonRow.addView(secondaryButton, secondaryParams);
        }
        buttonRow.addView(primaryButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(42)));
        rightColumn.addView(buttonRow);
        if (underRightView != null) {
            LinearLayout.LayoutParams underParams = new LinearLayout.LayoutParams(dp(170), dp(38));
            underParams.setMargins(0, dp(6), 0, 0);
            rightColumn.addView(underRightView, underParams);
        }
        topBar.addView(btnBack, backParams);
        topBar.addView(topTitle, titleParams);
        topBar.addView(rightColumn);
        return topBar;
    }
    private TextView makeTopTitle(String text) {
        TextView topTitle = new TextView(requireContext());
        topTitle.setText(text);
        topTitle.setTextSize(18);
        topTitle.setTypeface(Typeface.DEFAULT_BOLD);
        topTitle.setTextColor(DARK_BLUE);
        topTitle.setGravity(Gravity.CENTER_VERTICAL);
        topTitle.setSingleLine(true);
        return topTitle;
    }
    private EditText makeTitleInput(String hint) {
        EditText titleInput = new EditText(requireContext());
        titleInput.setHint(hint);
        titleInput.setSingleLine(true);
        titleInput.setTextSize(26);
        titleInput.setTypeface(Typeface.DEFAULT_BOLD);
        titleInput.setTextColor(DARK_BLUE);
        titleInput.setHintTextColor(0xFF6B7280);
        titleInput.setBackgroundColor(Color.TRANSPARENT);
        titleInput.setPadding(0, dp(8), 0, dp(8));
        titleInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        return titleInput;
    }
    private EditText makeContentInput() {
        EditText contentInput = new EditText(requireContext());
        contentInput.setHint("Start writing...");
        contentInput.setTextSize(18);
        contentInput.setTextColor(DARK_BLUE);
        contentInput.setHintTextColor(0xFF9CA3AF);
        contentInput.setGravity(Gravity.TOP | Gravity.START);
        contentInput.setBackgroundColor(Color.TRANSPARENT);
        contentInput.setPadding(0, dp(8), 0, dp(8));
        contentInput.setMinLines(18);
        contentInput.setMinHeight(dp(390));
        contentInput.setSingleLine(false);
        contentInput.setVerticalScrollBarEnabled(false);
        contentInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                        InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );
        return contentInput;
    }
    private LinearLayout createNoteCard(EditText contentInput, @Nullable TextView charCountTarget) {
        LinearLayout noteCard = new LinearLayout(requireContext());
        noteCard.setOrientation(LinearLayout.VERTICAL);
        noteCard.setPadding(dp(18), dp(12), dp(18), dp(10));
        noteCard.setMinimumHeight(dp(520));
        noteCard.setBackground(roundedBg(Color.WHITE, BORDER_BLUE, 1, 16));
        TextView charCount = charCountTarget != null ? charCountTarget : new TextView(requireContext());
        charCount.setText("Characters: " + contentInput.getText().length());
        charCount.setTextSize(13);
        charCount.setTextColor(MUTED_TEXT);
        charCount.setGravity(Gravity.END);
        contentInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                charCount.setText("Characters: " + s.length());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        noteCard.addView(contentInput, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1
        ));
        noteCard.addView(charCount, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        return noteCard;
    }
    private void styleModuleSpinner(Spinner moduleSpinner) {
        moduleSpinner.setBackground(roundedBg(Color.WHITE, BORDER_BLUE, 1, 14));
        moduleSpinner.setPadding(dp(10), 0, dp(10), 0);
    }
    private LinearLayout createAttachmentControlRow(TextView attachmentStatus, @Nullable Runnable onAttachClicked) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(22), dp(8), dp(22), dp(8));
        row.setBackgroundColor(SCREEN_BG);
        TextView label = attachmentStatus;
        label.setPadding(0, 0, dp(10), 0);
        MaterialButton pin = makeFormatButton("📎", "Attach file");
        pin.setTextSize(18);
        pin.setPadding(dp(6), dp(4), dp(6), dp(4));
        LinearLayout.LayoutParams pinParams = new LinearLayout.LayoutParams(dp(48), dp(42));
        pinParams.setMargins(0, 0, 0, 0);
        pin.setLayoutParams(pinParams);
        pin.setOnClickListener(v -> {
            if (onAttachClicked == null) {
                Toast.makeText(requireContext(), "Attachments are only for personal notes", Toast.LENGTH_SHORT).show();
                return;
            }
            onAttachClicked.run();
        });
        row.addView(label, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(pin);
        return row;
    }
    private void setupRecyclerView() {
        adapter = new NotesAdapter(new NotesAdapter.OnNoteActionListener() {
            @Override
            public void onOpen(NoteListItem item) {
                handleNoteClick(item);
            }

            @Override
            public void onRename(NoteListItem item) {
                showRenameNoteDialog(item);
            }

            @Override
            public void onMoveToFolder(NoteListItem item) {
                showMoveToFolderDialog(item);
            }

            @Override
            public void onDelete(NoteListItem item) {
                showDeleteNoteDialog(item);
            }
        });

        binding.rvNotes.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvNotes.setAdapter(adapter);
    }
    private void readIncomingModuleFilterArgument() {
        Bundle args = getArguments();

        if (args == null) {
            incomingModuleFilterId = null;
            return;
        }

        String moduleId = args.getString("moduleId");

        if (moduleId == null || moduleId.trim().isEmpty()) {
            incomingModuleFilterId = null;
            return;
        }

        incomingModuleFilterId = moduleId.trim();
        selectedModuleFilterId = incomingModuleFilterId;
    }


    private void applyIncomingModuleFilterIfPossible() {
        if (binding == null || viewModel == null) return;

        if (incomingModuleFilterId == null || incomingModuleFilterId.trim().isEmpty()) return;

        if (incomingModuleFilterApplied) return;

        boolean moduleExists = false;

        for (Module module : currentModules) {
            if (incomingModuleFilterId.equals(module.getModuleId())) {
                moduleExists = true;
                break;
            }
        }

        if (!moduleExists) return;

        incomingModuleFilterApplied = true;
        selectedModuleFilterId = incomingModuleFilterId;

        // Important: ALL means module files + personal notes.
        viewModel.setFilter(NotesViewModel.FILTER_ALL);
        viewModel.setSelectedModuleId(incomingModuleFilterId);

        updateFilterUi(R.id.chip_all);

        if (binding.chipGroupFilter != null) {
            binding.chipGroupFilter.check(R.id.chip_all);
        }

        int position = getModuleFilterPosition(incomingModuleFilterId);

        if (binding.spinnerModuleFilter != null) {
            binding.spinnerModuleFilter.setSelection(position, false);
        }
    }
    private void setupButtons() {
        binding.btnSettings.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_notesFragment_to_settingsFragment));

        binding.fabAddNote.setOnClickListener(this::showAddMenu);

        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setSearchQuery(s == null ? "" : s.toString());
            }

            @Override public void afterTextChanged(Editable s) {}
        });

        // Default selected state when the screen opens
        updateFilterUi(R.id.chip_all);

        binding.chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;

            int checkedId = checkedIds.get(0);

            if (checkedId == R.id.chip_modules) {
                viewModel.setFilter(NotesViewModel.FILTER_MODULE);
            } else if (checkedId == R.id.chip_personal) {
                viewModel.setFilter(NotesViewModel.FILTER_PERSONAL);
            } else {
                viewModel.setFilter(NotesViewModel.FILTER_ALL);
            }

            updateFilterUi(checkedId);
        });

        binding.spinnerModuleFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (populatingModuleFilter) return;

                if (position <= 0) {
                    selectedModuleFilterId = null;
                    viewModel.setSelectedModuleId(null);
                    return;
                }

                int moduleIndex = position - 1;

                if (moduleIndex >= 0 && moduleIndex < currentModules.size()) {
                    selectedModuleFilterId = currentModules.get(moduleIndex).getModuleId();
                    viewModel.setSelectedModuleId(selectedModuleFilterId);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (populatingModuleFilter) return;
                selectedModuleFilterId = null;
                viewModel.setSelectedModuleId(null);
            }
        });

        binding.spinnerFolderFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (populatingFolderFilter) return;

                if (position <= 0) {
                    selectedFolderFilterId = null;
                    viewModel.setSelectedFolderId(null);
                    return;
                }

                int folderIndex = position - 1;

                if (folderIndex >= 0 && folderIndex < currentFolders.size()) {
                    selectedFolderFilterId = currentFolders.get(folderIndex).getFolderId();
                    viewModel.setSelectedFolderId(selectedFolderFilterId);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (populatingFolderFilter) return;
                selectedFolderFilterId = null;
                viewModel.setSelectedFolderId(null);
            }
        });
    }

    private void updateFilterUi(int checkedId) {
        if (binding == null) return;

        boolean allSelected = checkedId == R.id.chip_all;
        boolean moduleSelected = checkedId == R.id.chip_modules;
        boolean personalSelected = checkedId == R.id.chip_personal;

        styleFilterChip(binding.chipAll, allSelected);
        styleFilterChip(binding.chipModules, moduleSelected);
        styleFilterChip(binding.chipPersonal, personalSelected);

        if (moduleSelected) {
            binding.txtSectionHeading.setText("Module Notes");
        } else if (personalSelected) {
            binding.txtSectionHeading.setText("Personal Notes");
        } else {
            binding.txtSectionHeading.setText("All Notes");
        }

        // Keep this visible for all filters, including Personal Notes.
        binding.folderFilterCard.setVisibility(View.VISIBLE);
        binding.moduleFilterCard.setVisibility(View.VISIBLE);
    }

    private void styleFilterChip(com.google.android.material.chip.Chip chip, boolean selected) {
        if (chip == null) return;

        int selectedBackground = 0xFF2F9E8F;
        int selectedText = Color.WHITE;

        int normalBackground = isDarkMode() ? 0xFFFFFFFF : Color.WHITE;
        int normalText = Color.BLACK;

        chip.setChipBackgroundColor(ColorStateList.valueOf(
                selected ? selectedBackground : normalBackground
        ));

        chip.setTextColor(selected ? selectedText : normalText);

        chip.setChipStrokeColor(ColorStateList.valueOf(
                isDarkMode() ? 0xFF374151 : Color.BLACK
        ));

        chip.setChipStrokeWidth(dp(1));
    }
    private void observeData() {
        viewModel.getDisplayNotes().observe(getViewLifecycleOwner(), notes -> {
            adapter.submitList(notes);
            int count = notes == null ? 0 : notes.size();
            binding.emptyState.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
            binding.rvNotes.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
            binding.txtNotesSummary.setText(count == 1 ? "1 note found" : count + " notes found");
        });
        viewModel.getModules().observe(getViewLifecycleOwner(), modules -> {
            currentModules = modules != null ? modules : new ArrayList<>();
            populateModuleFilter();
        });
        viewModel.getFolders().observe(getViewLifecycleOwner(), folders -> {
            currentFolders = folders != null ? folders : new ArrayList<>();
            populateFolderFilter();
        });
    }
    private void populateModuleFilter() {
        if (binding == null) return;

        List<String> moduleNames = new ArrayList<>();
        moduleNames.add("All modules and personal");

        for (Module module : currentModules) {
            String label = module.getModuleCode() != null && !module.getModuleCode().isEmpty()
                    ? module.getModuleCode() + " - " + module.getName()
                    : module.getName();
            moduleNames.add(label);
        }

        populatingModuleFilter = true;

        ArrayAdapter<String> moduleAdapter = createModuleSpinnerAdapter(moduleNames);
        binding.spinnerModuleFilter.setAdapter(moduleAdapter);

        int safePosition = getModuleFilterPosition(selectedModuleFilterId);
        binding.spinnerModuleFilter.setSelection(safePosition, false);

        String safeModuleId = getModuleIdForFilterPosition(safePosition);
        if ((selectedModuleFilterId == null && safeModuleId != null)
                || (selectedModuleFilterId != null && !selectedModuleFilterId.equals(safeModuleId))) {
            selectedModuleFilterId = safeModuleId;
            viewModel.setSelectedModuleId(safeModuleId);
        }

        binding.spinnerModuleFilter.post(() -> {
            populatingModuleFilter = false;
            applyIncomingModuleFilterIfPossible();
        });
    }

    private void populateFolderFilter() {
        if (binding == null) return;

        List<String> folderNames = new ArrayList<>();
        folderNames.add("All folders");

        for (NoteFolder folder : currentFolders) {
            String name = folder.getName() == null || folder.getName().trim().isEmpty()
                    ? "Untitled folder"
                    : folder.getName();

            folderNames.add("📁 " + name);
        }

        populatingFolderFilter = true;

        ArrayAdapter<String> folderAdapter = createModuleSpinnerAdapter(folderNames);
        binding.spinnerFolderFilter.setAdapter(folderAdapter);

        int safePosition = getFolderFilterPosition(selectedFolderFilterId);
        binding.spinnerFolderFilter.setSelection(safePosition, false);

        String safeFolderId = getFolderIdForFilterPosition(safePosition);
        if ((selectedFolderFilterId == null && safeFolderId != null)
                || (selectedFolderFilterId != null && !selectedFolderFilterId.equals(safeFolderId))) {
            selectedFolderFilterId = safeFolderId;
            viewModel.setSelectedFolderId(safeFolderId);
        }

        binding.spinnerFolderFilter.post(() -> populatingFolderFilter = false);
    }

    private int getModuleFilterPosition(@Nullable String moduleId) {
        if (moduleId == null || moduleId.trim().isEmpty()) return 0;

        for (int i = 0; i < currentModules.size(); i++) {
            Module module = currentModules.get(i);
            if (moduleId.equals(module.getModuleId())) {
                return i + 1;
            }
        }

        return 0;
    }

    @Nullable
    private String getModuleIdForFilterPosition(int position) {
        if (position <= 0) return null;

        int moduleIndex = position - 1;
        if (moduleIndex >= 0 && moduleIndex < currentModules.size()) {
            return currentModules.get(moduleIndex).getModuleId();
        }

        return null;
    }

    private int getFolderFilterPosition(@Nullable String folderId) {
        if (folderId == null || folderId.trim().isEmpty()) return 0;

        for (int i = 0; i < currentFolders.size(); i++) {
            NoteFolder folder = currentFolders.get(i);
            if (folderId.equals(folder.getFolderId())) {
                return i + 1;
            }
        }

        return 0;
    }

    @Nullable
    private String getFolderIdForFilterPosition(int position) {
        if (position <= 0) return null;

        int folderIndex = position - 1;
        if (folderIndex >= 0 && folderIndex < currentFolders.size()) {
            return currentFolders.get(folderIndex).getFolderId();
        }

        return null;
    }

    private List<String> buildFolderAssignmentNames() {
        List<String> folderNames = new ArrayList<>();
        folderNames.add("No folder");

        for (NoteFolder folder : currentFolders) {
            String name = folder.getName() == null || folder.getName().trim().isEmpty()
                    ? "Untitled folder"
                    : folder.getName();

            folderNames.add("📁 " + name);
        }

        return folderNames;
    }

    @Nullable
    private String getSelectedFolderIdFromSpinner(Spinner folderSpinner) {
        if (folderSpinner == null) return null;

        int selectedIndex = folderSpinner.getSelectedItemPosition();

        if (selectedIndex <= 0) return null;

        int folderIndex = selectedIndex - 1;

        if (folderIndex >= 0 && folderIndex < currentFolders.size()) {
            return currentFolders.get(folderIndex).getFolderId();
        }

        return null;
    }

    private int getFolderSpinnerPosition(@Nullable String folderId) {
        if (folderId == null || folderId.trim().isEmpty()) {
            return 0;
        }

        for (int i = 0; i < currentFolders.size(); i++) {
            NoteFolder folder = currentFolders.get(i);

            if (folderId.equals(folder.getFolderId())) {
                return i + 1;
            }
        }

        return 0;
    }

    private Spinner createFolderAssignmentSpinner() {
        Spinner folderSpinner = new Spinner(requireContext());
        styleModuleSpinner(folderSpinner);
        folderSpinner.setAdapter(createModuleSpinnerAdapter(buildFolderAssignmentNames()));
        return folderSpinner;
    }

    private MaterialButton createFolderIconDropdownButton(Spinner folderSpinner) {
        MaterialButton folderButton = new MaterialButton(requireContext());
        folderButton.setText("📁");
        folderButton.setTextSize(16);
        folderButton.setAllCaps(false);
        folderButton.setMinWidth(0);
        folderButton.setMinimumWidth(0);
        folderButton.setPadding(dp(8), dp(4), dp(8), dp(4));
        folderButton.setCornerRadius(dp(14));
        folderButton.setStrokeWidth(dp(1));
        addButtonHint(folderButton, "Choose folder");

        Runnable updateFolderButtonState = () -> {
            boolean hasFolder = folderSpinner != null && folderSpinner.getSelectedItemPosition() > 0;
            folderButton.setText(hasFolder ? "📂" : "📁");
            folderButton.setTextColor(hasFolder ? Color.WHITE : DARK_BLUE);
            folderButton.setBackgroundTintList(ColorStateList.valueOf(
                    hasFolder ? PRIMARY_BLUE : Color.WHITE
            ));
            folderButton.setStrokeColor(ColorStateList.valueOf(
                    hasFolder ? PRIMARY_BLUE : BORDER_BLUE
            ));
        };

        updateFolderButtonState.run();

        folderButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), folderButton);
            List<String> folders = buildFolderAssignmentNames();

            for (int i = 0; i < folders.size(); i++) {
                String label = folders.get(i);
                if (i == folderSpinner.getSelectedItemPosition()) {
                    label = "✓ " + label;
                }
                popup.getMenu().add(0, i, i, label);
            }

            popup.setOnMenuItemClickListener(item -> {
                int selectedIndex = item.getItemId();
                if (selectedIndex >= 0 && selectedIndex < folders.size()) {
                    folderSpinner.setSelection(selectedIndex);
                    updateFolderButtonState.run();

                    if (selectedIndex <= 0) {
                        Toast.makeText(requireContext(), "No folder selected", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(),
                                folders.get(selectedIndex).replace("📁 ", "") + " selected",
                                Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            });

            popup.show();
        });

        return folderButton;
    }

    private LinearLayout createFolderModulePickerRow(Spinner folderSpinner, Spinner moduleSpinner) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(4));
        row.setBackgroundColor(SCREEN_BG);

        MaterialButton folderButton = createFolderIconDropdownButton(folderSpinner);

        LinearLayout.LayoutParams folderParams = new LinearLayout.LayoutParams(dp(48), dp(38));
        folderParams.setMargins(0, 0, dp(12), 0);

        LinearLayout.LayoutParams moduleParams = new LinearLayout.LayoutParams(0, dp(38), 1);

        row.addView(folderButton, folderParams);
        row.addView(moduleSpinner, moduleParams);

        return row;
    }
    private void showRenameNoteDialog(NoteListItem item) {
        EditText titleInput = new EditText(requireContext());
        titleInput.setSingleLine(true);
        titleInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        titleInput.setText(item.getTitle());
        titleInput.setSelection(titleInput.getText().length());
        titleInput.setHint("Note title");
        int padding = dp(20);
        titleInput.setPadding(padding, padding / 2, padding, padding / 2);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Rename note")
                .setMessage("Give this note a clear title so it is easy to find later.")
                .setView(titleInput)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newTitle = titleInput.getText().toString().trim();
                    if (newTitle.isEmpty()) {
                        Toast.makeText(requireContext(), "Title cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    viewModel.renameNote(item, newTitle);
                    Toast.makeText(requireContext(), "Note renamed", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showMoveToFolderDialog(NoteListItem item) {
        if (item == null) return;

        if (currentFolders.isEmpty()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("No folders yet")
                    .setMessage("Create a folder first, then you can move notes into it.")
                    .setPositiveButton("Create folder", (dialog, which) -> showCreateFolderDialog())
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(40), dp(20), dp(40), 0);

        TextView label = new TextView(requireContext());
        label.setText("Choose where to move this note/file:");
        label.setTextSize(14);
        label.setTextColor(DARK_BLUE);
        label.setPadding(0, 0, 0, dp(10));

        Spinner folderSpinner = createFolderAssignmentSpinner();
        folderSpinner.setSelection(getFolderSpinnerPosition(item.getFolderId()), false);

        layout.addView(label);
        layout.addView(folderSpinner);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Move to folder")
                .setView(layout)
                .setPositiveButton("Move", (dialog, which) -> {
                    String folderId = getSelectedFolderIdFromSpinner(folderSpinner);
                    viewModel.moveNoteToFolder(item, folderId);

                    if (folderId == null) {
                        Toast.makeText(requireContext(), "Removed from folder", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Moved to folder", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void showCreateFolderDialog() {
        EditText folderInput = new EditText(requireContext());
        folderInput.setSingleLine(true);
        folderInput.setHint("Folder name");
        folderInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        int padding = dp(20);
        folderInput.setPadding(padding, padding / 2, padding, padding / 2);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Create folder")
                .setMessage("Create a folder to organise your personal notes and uploaded module files.")
                .setView(folderInput)
                .setPositiveButton("Create", (dialog, which) -> {
                    String folderName = folderInput.getText().toString().trim();

                    if (folderName.isEmpty()) {
                        Toast.makeText(requireContext(), "Folder name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    viewModel.addFolder(folderName);
                    Toast.makeText(requireContext(), "Folder created", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void showDeleteNoteDialog(NoteListItem item) {
        String type = item.getType() == NoteListItem.TYPE_MODULE_FILE ? "module file" : "personal note";
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete note?")
                .setMessage("This will remove \"" + item.getTitle() + "\" from your " + type + " list.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    viewModel.deleteNote(item);
                    Toast.makeText(requireContext(), "Note deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void showAddMenu(View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);

        menu.getMenu().add("Upload module file");
        menu.getMenu().add("Write personal note");
        menu.getMenu().add("Create folder");
        menu.getMenu().add("Manage folders");

        menu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();

            if (title.equals("Upload module file")) {
                showUploadDialog();
            } else if (title.equals("Write personal note")) {
                showPersonalNoteDialog();
            } else if (title.equals("Create folder")) {
                showCreateFolderDialog();
            } else if (title.equals("Manage folders")) {
                showManageFoldersDialog();
            }

            return true;
        });

        menu.show();
    }
    private void showManageFoldersDialog() {
        if (currentFolders == null || currentFolders.isEmpty()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("No folders")
                    .setMessage("You have not created any folders yet.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        List<String> folderNames = new ArrayList<>();

        for (NoteFolder folder : currentFolders) {
            String name = folder.getName() == null || folder.getName().trim().isEmpty()
                    ? "Untitled folder"
                    : folder.getName();

            folderNames.add("📁 " + name);
        }

        String[] folderArray = folderNames.toArray(new String[0]);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Manage folders")
                .setItems(folderArray, (dialog, which) -> {
                    if (which >= 0 && which < currentFolders.size()) {
                        showFolderActionsDialog(currentFolders.get(which));
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void showFolderActionsDialog(NoteFolder folder) {
        if (folder == null) return;

        String folderName = folder.getName() == null || folder.getName().trim().isEmpty()
                ? "Untitled folder"
                : folder.getName();

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(folderName)
                .setItems(new String[]{"Rename folder", "Delete folder"}, (dialog, which) -> {
                    if (which == 0) {
                        showRenameFolderDialog(folder);
                    } else if (which == 1) {
                        showConfirmDeleteFolderDialog(folder);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void showRenameFolderDialog(NoteFolder folder) {
        if (folder == null) return;

        EditText folderInput = new EditText(requireContext());
        folderInput.setSingleLine(true);
        folderInput.setHint("Folder name");
        folderInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        String currentName = folder.getName() == null ? "" : folder.getName();
        folderInput.setText(currentName);
        folderInput.setSelection(folderInput.getText().length());

        int padding = dp(20);
        folderInput.setPadding(padding, padding / 2, padding, padding / 2);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Rename folder")
                .setMessage("Enter a new name for this folder.")
                .setView(folderInput)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = folderInput.getText().toString().trim();

                    if (newName.isEmpty()) {
                        Toast.makeText(requireContext(), "Folder name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    viewModel.renameFolder(folder, newName);
                    Toast.makeText(requireContext(), "Folder renamed", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void showConfirmDeleteFolderDialog(NoteFolder folder) {
        if (folder == null) return;

        String folderName = folder.getName() == null || folder.getName().trim().isEmpty()
                ? "Untitled folder"
                : folder.getName();

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete folder?")
                .setMessage("This will delete the folder \"" + folderName + "\". Notes inside it will not be deleted; they will just be removed from the folder.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    viewModel.deleteFolder(folder);
                    Toast.makeText(requireContext(), "Folder deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void showUploadDialog() {
        if (currentModules.isEmpty()) {
            Toast.makeText(requireContext(), "Create a module first before uploading notes.", Toast.LENGTH_LONG).show();
            return;
        }

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(40), dp(20), dp(40), 0);

        EditText titleInput = new EditText(requireContext());
        titleInput.setHint("Note title e.g. Week 3 Slides");
        titleInput.setSingleLine(true);
        titleInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        Spinner moduleSpinner = new Spinner(requireContext());
        styleModuleSpinner(moduleSpinner);

        List<String> moduleNames = new ArrayList<>();
        for (Module module : currentModules) {
            String label = module.getModuleCode() != null && !module.getModuleCode().isEmpty()
                    ? module.getModuleCode() + " - " + module.getName()
                    : module.getName();
            moduleNames.add(label);
        }

        moduleSpinner.setAdapter(createModuleSpinnerAdapter(moduleNames));

        TextView moduleLabel = new TextView(requireContext());
        moduleLabel.setText("Module");
        moduleLabel.setTextSize(13);
        moduleLabel.setTextColor(DARK_BLUE);
        moduleLabel.setPadding(0, dp(12), 0, dp(4));

        TextView folderLabel = new TextView(requireContext());
        folderLabel.setText("Folder");
        folderLabel.setTextSize(13);
        folderLabel.setTextColor(DARK_BLUE);
        folderLabel.setPadding(0, dp(12), 0, dp(4));

        Spinner folderSpinner = createFolderAssignmentSpinner();

        layout.addView(titleInput);
        layout.addView(moduleLabel);
        layout.addView(moduleSpinner);
        layout.addView(folderLabel);
        layout.addView(folderSpinner);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Upload module note")
                .setMessage("Choose the module, optional folder, and title first. Then select a PDF, PowerPoint, Word document or image.")
                .setView(layout)
                .setPositiveButton("Choose file", (dialog, which) -> {
                    String title = titleInput.getText().toString().trim();

                    if (title.isEmpty()) {
                        Toast.makeText(requireContext(), "Title is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int selectedIndex = moduleSpinner.getSelectedItemPosition();

                    pendingUploadTitle = title;
                    pendingUploadModuleId = currentModules.get(selectedIndex).getModuleId();
                    pendingUploadFolderId = getSelectedFolderIdFromSpinner(folderSpinner);

                    try {
                        moduleFilePickerLauncher.launch(new String[]{
                                "application/pdf",
                                "application/vnd.ms-powerpoint",
                                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                "application/msword",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "image/*"
                        });
                    } catch (Exception e) {
                        pendingUploadTitle = null;
                        pendingUploadModuleId = null;
                        pendingUploadFolderId = null;
                        safeToast("Could not open file picker. Please try again.", Toast.LENGTH_LONG);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void showPersonalNoteDialog() {
        resetFormattingState();
        temporaryAttachments.clear();
        temporaryDraftItem = null;
        activeAttachmentNoteId = null;
        activeAttachmentBelongsToUnsavedDraft = true;
        final boolean[] noteSaved = {false};
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout screen = new LinearLayout(requireContext());
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(SCREEN_BG);
        MaterialButton btnBack = makeTopBarButton("←");
        MaterialButton btnSave = makeTopBarButton("Save");
        addButtonHint(btnSave, "Save note");
        TextView topTitle = makeTopTitle("New note");
        Spinner moduleSpinner = new Spinner(requireContext());
        styleModuleSpinner(moduleSpinner);
        moduleSpinner.setAdapter(createModuleSpinnerAdapter(buildPersonalModuleNames()));
        Spinner folderSpinner = createFolderAssignmentSpinner();
        LinearLayout topBar = createTopBar(btnBack, topTitle, btnSave, null, null);
        LinearLayout editorLayout = new LinearLayout(requireContext());
        editorLayout.setOrientation(LinearLayout.VERTICAL);
        editorLayout.setPadding(dp(22), 0, dp(22), dp(0));
        editorLayout.setBackgroundColor(SCREEN_BG);
        EditText titleInput = makeTitleInput("Title goes here");
        EditText contentInput = makeContentInput();
        TextView attachmentStatus = makeAttachmentStatusText();
        final boolean[] observingDraftAttachments = {false};
        LinearLayout noteCard = createNoteCard(contentInput, null);

        LinearLayout folderModuleRow = createFolderModulePickerRow(folderSpinner, moduleSpinner);
        editorLayout.addView(folderModuleRow);

        editorLayout.addView(titleInput);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, dp(14), 0, 0);
        editorLayout.addView(noteCard, cardParams);
        Runnable attachAction = () -> {
            ensureDraftNoteExists(titleInput, contentInput, moduleSpinner, folderSpinner, true);
            if (!observingDraftAttachments[0] && activeAttachmentNoteId != null) {
                observeAttachmentCount(activeAttachmentNoteId, attachmentStatus);
                observingDraftAttachments[0] = true;
            }
            launchPersonalAttachmentPicker();
        };
        LinearLayout attachmentRow = createAttachmentControlRow(attachmentStatus, attachAction);
        HorizontalScrollView formatScroll = createFormattingToolbar(contentInput, null);
        attachmentStatus.setOnClickListener(v -> {
            if (activeAttachmentNoteId == null || activeAttachmentNoteId.trim().isEmpty()) {
                Toast.makeText(requireContext(), "Attach a file first", Toast.LENGTH_SHORT).show();
                return;
            }
            boolean saved = savePersonalNoteFromEditor(titleInput, contentInput, moduleSpinner, folderSpinner);
            if (saved) {
                String title = titleInput.getText().toString().trim();
                String htmlContent = getCleanHtmlFromEditor(contentInput);
                String moduleName = getSelectedPersonalModuleId(moduleSpinner) == null ? "Personal" : "Module";
                String selectedFolderId = getSelectedFolderIdFromSpinner(folderSpinner);
                String selectedModuleId = getSelectedPersonalModuleId(moduleSpinner);
                NoteListItem viewItem = new NoteListItem(
                        NoteListItem.TYPE_PERSONAL_NOTE,
                        activeAttachmentNoteId,
                        title,
                        moduleName,
                        htmlContent,
                        null,
                        selectedFolderId,
                        selectedModuleId
                );
                noteSaved[0] = true;
                activeAttachmentBelongsToUnsavedDraft = false;
                temporaryAttachments.clear();
                dialog.dismiss();
                showPersonalNoteViewer(viewItem);
            }
        });
        ScrollView editorScroll = new ScrollView(requireContext());
        editorScroll.setFillViewport(true);
        editorScroll.setBackgroundColor(SCREEN_BG);
        editorScroll.addView(editorLayout, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));
        screen.addView(topBar);
        screen.addView(editorScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1
        ));
        screen.addView(attachmentRow);
        screen.addView(formatScroll);
        dialog.setContentView(screen);
        addLiveFormattingWatcher(contentInput);
        btnBack.setOnClickListener(v -> {
            if (noteSaved[0]) {
                dialog.dismiss();
                return;
            }
            confirmSaveBeforeLeaving(dialog, titleInput, contentInput, moduleSpinner, folderSpinner, noteSaved);
        });
        dialog.setOnKeyListener((dialogInterface, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                if (noteSaved[0]) dialog.dismiss();
                else confirmSaveBeforeLeaving(dialog, titleInput, contentInput, moduleSpinner, folderSpinner, noteSaved);
                return true;
            }
            return false;
        });
        btnSave.setOnClickListener(v -> {
            boolean saved = savePersonalNoteFromEditor(titleInput, contentInput, moduleSpinner, folderSpinner);
            if (saved) {
                noteSaved[0] = true;
                activeAttachmentBelongsToUnsavedDraft = false;
                temporaryAttachments.clear();
                dialog.dismiss();
            }
        });
        dialog.setOnDismissListener(d -> {
            if (!noteSaved[0]) {
                cleanupTemporaryAttachments();
                cleanupTemporaryDraftNote();
            }
        });
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(SCREEN_BG));
        }
        titleInput.requestFocus();
        InputMethodManager imm = (InputMethodManager) requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(titleInput, InputMethodManager.SHOW_IMPLICIT);
    }
    private List<String> buildPersonalModuleNames() {
        List<String> moduleNames = new ArrayList<>();
        moduleNames.add("Personal");
        for (Module module : currentModules) {
            String label = module.getModuleCode() != null && !module.getModuleCode().isEmpty()
                    ? module.getModuleCode() + " - " + module.getName()
                    : module.getName();
            moduleNames.add(label);
        }
        return moduleNames;
    }
    private int getPersonalModuleSpinnerPosition(@Nullable String moduleName) {
        if (moduleName == null || moduleName.trim().isEmpty()) {
            return 0;
        }

        String cleanModuleName = moduleName.trim();

        if (cleanModuleName.equalsIgnoreCase("Personal")) {
            return 0;
        }

        for (int i = 0; i < currentModules.size(); i++) {
            Module module = currentModules.get(i);

            String fullLabel = module.getModuleCode() != null && !module.getModuleCode().isEmpty()
                    ? module.getModuleCode() + " - " + module.getName()
                    : module.getName();

            if (cleanModuleName.equalsIgnoreCase(fullLabel)
                    || cleanModuleName.equalsIgnoreCase(module.getName())
                    || cleanModuleName.equalsIgnoreCase(module.getModuleCode())) {
                return i + 1;
            }
        }

        return 0;
    }
    private int getPersonalModuleSpinnerPositionById(@Nullable String moduleId) {
        if (moduleId == null || moduleId.trim().isEmpty()) {
            return 0;
        }

        for (int i = 0; i < currentModules.size(); i++) {
            Module module = currentModules.get(i);
            if (moduleId.equals(module.getModuleId())) {
                return i + 1;
            }
        }

        return 0;
    }

    private TextView makeAttachmentStatusText() {
        TextView text = new TextView(requireContext());
        setAttachmentCountLabel(text, 0);
        text.setTextSize(13);
        text.setTextColor(PRIMARY_BLUE);
        text.setPadding(0, dp(10), 0, dp(8));
        text.setPaintFlags(text.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        text.setClickable(true);
        text.setFocusable(true);
        addButtonHint(text, "View attachments");
        return text;
    }
    private void setAttachmentCountLabel(TextView text, int count) {
        String label = count == 1 ? "1 attachment" : count + " attachments";
        text.setText(label);
    }
    private void showPersonalNoteViewer(NoteListItem item) {
        resetFormattingState();
        temporaryAttachments.clear();
        temporaryDraftItem = null;
        activeAttachmentNoteId = item.getId();
        activeAttachmentBelongsToUnsavedDraft = false;
        final boolean[] isEditMode = {false};
        final boolean[] hasUnsavedChanges = {false};
        final String[] savedTitle = {item.getTitle() == null ? "Untitled note" : item.getTitle()};
        final String[] savedHtml = {item.getContentPreview() == null ? "" : item.getContentPreview()};
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout screen = new LinearLayout(requireContext());
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(SCREEN_BG);
        MaterialButton btnBack = makeTopBarButton("←");
        MaterialButton btnMode = makeTopBarButton("Edit");
        MaterialButton btnSave = makeTopBarButton("Save");
        btnSave.setVisibility(View.GONE);
        TextView topTitle = makeTopTitle(savedTitle[0]);

        Spinner moduleSpinner = new Spinner(requireContext());
        styleModuleSpinner(moduleSpinner);
        moduleSpinner.setAdapter(createModuleSpinnerAdapter(buildPersonalModuleNames()));
        moduleSpinner.setSelection(getPersonalModuleSpinnerPositionById(item.getModuleId()), false);

        Spinner folderSpinner = createFolderAssignmentSpinner();
        folderSpinner.setSelection(getFolderSpinnerPosition(item.getFolderId()), false);

        LinearLayout folderModuleRow = createFolderModulePickerRow(folderSpinner, moduleSpinner);
        folderModuleRow.setVisibility(View.GONE);

        final boolean[] folderSelectionTouched = {false};
        final boolean[] folderSpinnerReady = {false};

        folderSpinner.post(() -> folderSpinnerReady[0] = true);

        folderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (folderSpinnerReady[0]) {
                    folderSelectionTouched[0] = true;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        LinearLayout topBar = createTopBar(btnBack, topTitle, btnMode, btnSave, null);
        LinearLayout contentHolder = new LinearLayout(requireContext());
        contentHolder.setOrientation(LinearLayout.VERTICAL);
        contentHolder.setPadding(dp(22), dp(14), dp(22), dp(10));
        contentHolder.setBackgroundColor(SCREEN_BG);
        TextView titleView = new TextView(requireContext());
        titleView.setText(savedTitle[0]);
        titleView.setTextSize(26);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setTextColor(DARK_BLUE);
        TextView bodyView = new TextView(requireContext());
        bodyView.setText(Html.fromHtml(savedHtml[0], Html.FROM_HTML_MODE_LEGACY));
        bodyView.setTextSize(18);
        bodyView.setTextColor(DARK_BLUE);
        bodyView.setPadding(0, dp(12), 0, dp(16));
        LinearLayout viewCard = new LinearLayout(requireContext());
        viewCard.setOrientation(LinearLayout.VERTICAL);
        viewCard.setPadding(dp(18), dp(12), dp(18), dp(10));
        viewCard.setBackground(roundedBg(Color.WHITE, BORDER_BLUE, 1, 16));
        viewCard.addView(bodyView);
        EditText titleInput = makeTitleInput("Title goes here");
        titleInput.setText(savedTitle[0]);
        EditText contentInput = makeContentInput();
        contentInput.setText(Html.fromHtml(savedHtml[0], Html.FROM_HTML_MODE_LEGACY));
        LinearLayout editCard = createNoteCard(contentInput, null);
        editCard.setVisibility(View.GONE);
        titleInput.setVisibility(View.GONE);
        TextView attachmentHeader = makeAttachmentStatusText();
        LinearLayout attachmentList = new LinearLayout(requireContext());
        attachmentList.setOrientation(LinearLayout.VERTICAL);
        attachmentList.setVisibility(View.VISIBLE);
        Runnable attachAction = () -> {
            activeAttachmentNoteId = item.getId();
            activeAttachmentBelongsToUnsavedDraft = false;
            launchPersonalAttachmentPicker();
        };
        LinearLayout attachmentRow = createAttachmentControlRow(attachmentHeader, attachAction);
        contentHolder.addView(folderModuleRow);
        contentHolder.addView(titleView);
        contentHolder.addView(titleInput);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, dp(14), 0, 0);
        contentHolder.addView(viewCard, cardParams);
        contentHolder.addView(editCard, cardParams);
        contentHolder.addView(attachmentList);
        HorizontalScrollView formatScroll = createFormattingToolbar(contentInput, null);
        formatScroll.setVisibility(View.GONE);
        ScrollView scroll = new ScrollView(requireContext());
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(SCREEN_BG);
        scroll.addView(contentHolder);
        screen.addView(topBar);
        screen.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        screen.addView(attachmentRow);
        screen.addView(formatScroll);
        dialog.setContentView(screen);
        addLiveFormattingWatcher(contentInput);
        observeAttachments(item.getId(), attachmentList, attachmentHeader);
        attachmentHeader.setOnClickListener(v -> {
            if (isEditMode[0]) {
                String newTitle = titleInput.getText().toString().trim();
                if (newTitle.isEmpty()) {
                    Toast.makeText(requireContext(), "Title cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                String newHtml = getCleanHtmlFromEditor(contentInput);
                String selectedModuleId = getSelectedPersonalModuleId(moduleSpinner);
                String selectedFolderId = getSelectedFolderIdFromSpinner(folderSpinner);
                viewModel.updatePersonalNote(item, newTitle, newHtml, selectedModuleId, selectedFolderId);
                folderSelectionTouched[0] = false;

                savedTitle[0] = newTitle;
                savedHtml[0] = newHtml;
                topTitle.setText(newTitle);
                titleView.setText(newTitle);
                bodyView.setText(Html.fromHtml(newHtml, Html.FROM_HTML_MODE_LEGACY));
                hasUnsavedChanges[0] = false;
                isEditMode[0] = false;
                attachmentList.setVisibility(View.VISIBLE);
                switchToViewMode(btnMode, btnSave, titleView, viewCard, titleInput, editCard, formatScroll, folderModuleRow);
                Toast.makeText(requireContext(), "Saved and showing attachments", Toast.LENGTH_SHORT).show();
            }
        });
        TextWatcher dirtyWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                hasUnsavedChanges[0] = true;
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        titleInput.addTextChangedListener(dirtyWatcher);
        contentInput.addTextChangedListener(dirtyWatcher);
        btnMode.setOnClickListener(v -> {
            isEditMode[0] = !isEditMode[0];
            if (isEditMode[0]) {
                btnMode.setText("View");
                btnMode.setBackgroundTintList(ColorStateList.valueOf(VIEW_BUTTON));
                btnMode.setTextColor(Color.WHITE);
                btnSave.setVisibility(View.VISIBLE);

                folderModuleRow.setVisibility(View.VISIBLE);

                titleView.setVisibility(View.GONE);
                viewCard.setVisibility(View.GONE);
                titleInput.setVisibility(View.VISIBLE);
                editCard.setVisibility(View.VISIBLE);
                formatScroll.setVisibility(View.VISIBLE);
                attachmentList.setVisibility(View.GONE);
                titleInput.requestFocus();
            } else {
                if (hasUnsavedChanges[0]) {
                    confirmDiscardEditMode(() -> {
                        titleInput.setText(savedTitle[0]);
                        contentInput.setText(Html.fromHtml(savedHtml[0], Html.FROM_HTML_MODE_LEGACY));
                        hasUnsavedChanges[0] = false;
                        isEditMode[0] = false;
                        attachmentList.setVisibility(View.VISIBLE);
                        switchToViewMode(btnMode, btnSave, titleView, viewCard, titleInput, editCard, formatScroll, folderModuleRow);
                    });
                } else {
                    isEditMode[0] = false;
                    attachmentList.setVisibility(View.VISIBLE);
                    switchToViewMode(btnMode, btnSave, titleView, viewCard, titleInput, editCard, formatScroll, folderModuleRow);
                }
            }
        });
        btnSave.setOnClickListener(v -> {
            String newTitle = titleInput.getText().toString().trim();
            if (newTitle.isEmpty()) {
                Toast.makeText(requireContext(), "Title cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            String newHtml = getCleanHtmlFromEditor(contentInput);
            String selectedModuleId = getSelectedPersonalModuleId(moduleSpinner);
            String selectedFolderId = getSelectedFolderIdFromSpinner(folderSpinner);
            viewModel.updatePersonalNote(item, newTitle, newHtml, selectedModuleId, selectedFolderId);
            folderSelectionTouched[0] = false;

            savedTitle[0] = newTitle;
            savedHtml[0] = newHtml;
            topTitle.setText(newTitle);
            titleView.setText(newTitle);
            bodyView.setText(Html.fromHtml(newHtml, Html.FROM_HTML_MODE_LEGACY));
            hasUnsavedChanges[0] = false;
            isEditMode[0] = false;
            attachmentList.setVisibility(View.VISIBLE);
            switchToViewMode(btnMode, btnSave, titleView, viewCard, titleInput, editCard, formatScroll, folderModuleRow);
            Toast.makeText(requireContext(), "Note saved", Toast.LENGTH_SHORT).show();
        });
        btnBack.setOnClickListener(v -> {
            if (isEditMode[0] && hasUnsavedChanges[0]) {
                confirmDiscardEditMode(dialog::dismiss);
            } else {
                dialog.dismiss();
            }
        });
        dialog.setOnKeyListener((dialogInterface, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                if (isEditMode[0] && hasUnsavedChanges[0]) confirmDiscardEditMode(dialog::dismiss);
                else dialog.dismiss();
                return true;
            }
            return false;
        });
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(SCREEN_BG));
        }
    }
    private void switchToViewMode(MaterialButton btnMode,
                                  MaterialButton btnSave,
                                  TextView titleView,
                                  LinearLayout viewCard,
                                  EditText titleInput,
                                  LinearLayout editCard,
                                  HorizontalScrollView formatScroll,
                                  @Nullable LinearLayout folderModuleRow) {
        btnMode.setText("Edit");
        btnMode.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
        btnMode.setTextColor(DARK_BLUE);
        btnSave.setVisibility(View.GONE);

        if (folderModuleRow != null) {
            folderModuleRow.setVisibility(View.GONE);
        }

        titleView.setVisibility(View.VISIBLE);
        viewCard.setVisibility(View.VISIBLE);
        titleInput.setVisibility(View.GONE);
        editCard.setVisibility(View.GONE);
        formatScroll.setVisibility(View.GONE);
    }
    private void confirmDiscardEditMode(Runnable onDiscard) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Discard changes?")
                .setMessage("Your unsaved edits will be lost.")
                .setPositiveButton("Discard", (dialog, which) -> onDiscard.run())
                .setNegativeButton("Keep editing", null)
                .show();
    }
    private void confirmSaveBeforeLeaving(Dialog dialog,
                                          EditText titleInput,
                                          EditText contentInput,
                                          Spinner moduleSpinner,
                                          Spinner folderSpinner,
                                          boolean[] noteSaved) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Save note?")
                .setMessage("Do you want to save this note before leaving?")
                .setPositiveButton("Save", (d, which) -> {
                    boolean saved = savePersonalNoteFromEditor(titleInput, contentInput, moduleSpinner, folderSpinner);
                    if (saved) {
                        noteSaved[0] = true;
                        activeAttachmentBelongsToUnsavedDraft = false;
                        temporaryAttachments.clear();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Discard", (d, which) -> {
                    cleanupTemporaryAttachments();
                    cleanupTemporaryDraftNote();
                    noteSaved[0] = false;
                    dialog.dismiss();
                })
                .setNeutralButton("Cancel", null)
                .show();
    }
    private boolean savePersonalNoteFromEditor(EditText titleInput,
                                               EditText contentInput,
                                               Spinner moduleSpinner,
                                               Spinner folderSpinner) {
        String title = titleInput.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Title is required", Toast.LENGTH_SHORT).show();
            return false;
        }

        String htmlContent = getCleanHtmlFromEditor(contentInput);

        String moduleId = getSelectedPersonalModuleId(moduleSpinner);
        String folderId = getSelectedFolderIdFromSpinner(folderSpinner);

        if (temporaryDraftItem != null) {
            viewModel.updatePersonalNote(temporaryDraftItem, title, htmlContent, moduleId, folderId);
            return true;
        }


        PersonalNote note = viewModel.addPersonalNote(title, htmlContent, moduleId, folderId);

        temporaryDraftItem = makeTemporaryItem(note, htmlContent);
        activeAttachmentNoteId = note.getNoteId();

        return true;
    }
    private void ensureDraftNoteExists(EditText titleInput,
                                       EditText contentInput,
                                       Spinner moduleSpinner,
                                       Spinner folderSpinner,
                                       boolean showMessage) {
        if (activeAttachmentNoteId != null && !activeAttachmentNoteId.trim().isEmpty()) return;

        String title = titleInput.getText().toString().trim();
        if (title.isEmpty()) title = "Untitled note";

        String htmlContent = getCleanHtmlFromEditor(contentInput);
        String moduleId = getSelectedPersonalModuleId(moduleSpinner);
        String folderId = getSelectedFolderIdFromSpinner(folderSpinner);

        PersonalNote note = viewModel.addPersonalNote(title, htmlContent, moduleId, folderId);

        temporaryDraftItem = makeTemporaryItem(note, htmlContent);
        activeAttachmentNoteId = note.getNoteId();
        activeAttachmentBelongsToUnsavedDraft = true;

        if (showMessage) {
            Toast.makeText(requireContext(),
                    "Temporary note created so the attachment can upload. Discard will remove it.",
                    Toast.LENGTH_LONG).show();
        }
    }
    private NoteListItem makeTemporaryItem(PersonalNote note, String contentPreview) {
        String moduleName = note.getModuleId() == null || note.getModuleId().trim().isEmpty()
                ? "Personal"
                : "Module";
        return new NoteListItem(
                NoteListItem.TYPE_PERSONAL_NOTE,
                note.getNoteId(),
                note.getTitle(),
                moduleName,
                contentPreview,
                null,
                note.getFolderId(),
                note.getModuleId()
        );
    }
    private String getSelectedPersonalModuleId(Spinner moduleSpinner) {
        int selectedIndex = moduleSpinner.getSelectedItemPosition();
        if (selectedIndex <= 0) return null;
        int moduleIndex = selectedIndex - 1;
        if (moduleIndex >= 0 && moduleIndex < currentModules.size()) {
            return currentModules.get(moduleIndex).getModuleId();
        }
        return null;
    }
    private HorizontalScrollView createFormattingToolbar(EditText contentInput,
                                                         @Nullable Runnable onAttachClicked) {
        HorizontalScrollView scroll = new HorizontalScrollView(requireContext());
        scroll.setFillViewport(true);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setBackgroundColor(Color.WHITE);
        scroll.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        MaterialButton bold = makeFormatButton("B", "Bold");
        MaterialButton italic = makeFormatButton("I", "Italic");
        MaterialButton underline = makeFormatButton("U", "Underline");
        MaterialButton highlight = makeFormatButton("✎", "Highlight");
        MaterialButton sizeDown = makeFormatButton("A-", "Decrease text size");
        MaterialButton sizeUp = makeFormatButton("A+", "Increase text size");
        MaterialButton bullet = makeFormatButton("•", "Add bullet point");
        MaterialButton alignLeft = makeFormatButton("≡", "Align left");
        MaterialButton more = makeFormatButton("⋯", "More formatting options");
        bold.setTypeface(Typeface.DEFAULT_BOLD);
        italic.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
        underline.setPaintFlags(underline.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        bold.setOnClickListener(v -> {
            activeBold = !activeBold;
            if (hasTextSelection(contentInput)) {
                applyStyleToSelection(contentInput, new StyleSpan(Typeface.BOLD));
            } else {
                Toast.makeText(requireContext(), "Bold enabled for new text", Toast.LENGTH_SHORT).show();
            }
            setFormatButtonActive(bold, activeBold);
        });
        italic.setOnClickListener(v -> {
            activeItalic = !activeItalic;
            if (hasTextSelection(contentInput)) {
                applyStyleToSelection(contentInput, new StyleSpan(Typeface.ITALIC));
            } else {
                Toast.makeText(requireContext(), "Italic enabled for new text", Toast.LENGTH_SHORT).show();
            }
            setFormatButtonActive(italic, activeItalic);
        });
        underline.setOnClickListener(v -> {
            activeUnderline = !activeUnderline;
            if (hasTextSelection(contentInput)) {
                applyStyleToSelection(contentInput, new UnderlineSpan());
            } else {
                Toast.makeText(requireContext(), "Underline enabled for new text", Toast.LENGTH_SHORT).show();
            }
            setFormatButtonActive(underline, activeUnderline);
        });
        highlight.setOnClickListener(v -> {
            activeHighlight = !activeHighlight;
            if (hasTextSelection(contentInput)) {
                applyStyleToSelection(contentInput, new BackgroundColorSpan(HIGHLIGHT_COLOR));
            } else {
                Toast.makeText(requireContext(), "Highlight enabled for new text", Toast.LENGTH_SHORT).show();
            }
            setFormatButtonActive(highlight, activeHighlight);
        });
        sizeDown.setOnClickListener(v -> {
            if (activeTextSizeScale <= 0.50f) {
                activeTextSizeScale = 0.50f;
                Toast.makeText(requireContext(), "Text has reached smallest size limit", Toast.LENGTH_SHORT).show();
                return;
            }
            activeTextSizeScale -= 0.10f;
            if (activeTextSizeScale < 0.50f) {
                activeTextSizeScale = 0.50f;
            }
            if (hasTextSelection(contentInput)) {
                applyTextSizeToSelection(contentInput, activeTextSizeScale);
            }
            Toast.makeText(requireContext(), "Text size decreased", Toast.LENGTH_SHORT).show();
            setFormatButtonActive(sizeDown, activeTextSizeScale < 1.0f);
            setFormatButtonActive(sizeUp, activeTextSizeScale > 1.0f);
        });
        sizeUp.setOnClickListener(v -> {
            if (activeTextSizeScale >= 2.0f) {
                activeTextSizeScale = 2.0f;
                Toast.makeText(requireContext(), "Text has reached biggest size limit", Toast.LENGTH_SHORT).show();
                return;
            }
            activeTextSizeScale += 0.15f;
            if (activeTextSizeScale > 2.0f) {
                activeTextSizeScale = 2.0f;
            }
            if (hasTextSelection(contentInput)) {
                applyTextSizeToSelection(contentInput, activeTextSizeScale);
            }
            Toast.makeText(requireContext(), "Text size increased", Toast.LENGTH_SHORT).show();
            setFormatButtonActive(sizeDown, activeTextSizeScale < 1.0f);
            setFormatButtonActive(sizeUp, activeTextSizeScale > 1.0f);
        });
        bullet.setOnClickListener(v -> insertBulletPoint(contentInput));
        alignLeft.setOnClickListener(v -> {
            activeAlignment = android.text.Layout.Alignment.ALIGN_NORMAL;
            applyParagraphAlignment(contentInput, android.text.Layout.Alignment.ALIGN_NORMAL);
        });
        more.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), v);
            popup.getMenu().add("Align center");
            popup.getMenu().add("Align right");
            popup.getMenu().add("Normal text size");
            popup.setOnMenuItemClickListener(item -> {
                String selected = item.getTitle().toString();
                if (selected.equals("Align center")) {
                    activeAlignment = android.text.Layout.Alignment.ALIGN_CENTER;
                    applyParagraphAlignment(contentInput, android.text.Layout.Alignment.ALIGN_CENTER);
                } else if (selected.equals("Align right")) {
                    activeAlignment = android.text.Layout.Alignment.ALIGN_OPPOSITE;
                    applyParagraphAlignment(contentInput, android.text.Layout.Alignment.ALIGN_OPPOSITE);
                } else if (selected.equals("Normal text size")) {
                    activeTextSizeScale = 1.0f;
                    if (hasTextSelection(contentInput)) {
                        applyTextSizeToSelection(contentInput, 1.0f);
                    }
                    setFormatButtonActive(sizeDown, false);
                    setFormatButtonActive(sizeUp, false);
                    Toast.makeText(requireContext(), "Normal text size", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
            popup.show();
        });
        row.addView(bold);
        row.addView(italic);
        row.addView(underline);
        row.addView(highlight);
        row.addView(sizeDown);
        row.addView(sizeUp);
        row.addView(bullet);
        row.addView(alignLeft);
        row.addView(more);
        scroll.addView(row, new HorizontalScrollView.LayoutParams(
                HorizontalScrollView.LayoutParams.WRAP_CONTENT,
                HorizontalScrollView.LayoutParams.WRAP_CONTENT
        ));
        return scroll;
    }
    private MaterialButton makeFormatButton(String text, String hint) {
        MaterialButton button = new MaterialButton(requireContext());
        button.setText(text);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setTextColor(DARK_BLUE);
        button.setCornerRadius(dp(14));
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(8), dp(6), dp(8), dp(6));
        button.setBackgroundTintList(ColorStateList.valueOf(0xFFF3F7FF));
        button.setStrokeColor(ColorStateList.valueOf(BORDER_BLUE));
        button.setStrokeWidth(dp(1));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(48), dp(42));
        params.setMargins(0, 0, dp(8), 0);
        button.setLayoutParams(params);
        addButtonHint(button, hint);
        return button;
    }
    private void setFormatButtonActive(MaterialButton button, boolean active) {
        button.setBackgroundTintList(ColorStateList.valueOf(active ? PRIMARY_BLUE : 0xFFF3F7FF));
        button.setTextColor(active ? Color.WHITE : DARK_BLUE);
    }
    private void applyStyleToSelection(EditText editText, Object span) {
        Editable editable = editText.getText();
        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();
        if (start < 0 || end < 0) return;
        if (start == end) {
            Toast.makeText(requireContext(), "Select text first, or keep typing to use this style.", Toast.LENGTH_SHORT).show();
            return;
        }
        int safeStart = Math.min(start, end);
        int safeEnd = Math.max(start, end);
        editable.setSpan(span, safeStart, safeEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    private boolean hasTextSelection(EditText editText) {
        return editText.getSelectionStart() != editText.getSelectionEnd();
    }
    private void applyTextSizeToSelection(EditText editText, float scale) {
        Editable editable = editText.getText();
        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();
        if (start < 0 || end < 0) return;
        if (start == end) {
            Toast.makeText(requireContext(), "Select text first, or keep typing to use this size.", Toast.LENGTH_SHORT).show();
            return;
        }
        int safeStart = Math.min(start, end);
        int safeEnd = Math.max(start, end);
        RelativeSizeSpan[] oldSizeSpans = editable.getSpans(safeStart, safeEnd, RelativeSizeSpan.class);
        for (RelativeSizeSpan span : oldSizeSpans) {
            editable.removeSpan(span);
        }
        if (scale != 1.0f) {
            editable.setSpan(
                    new RelativeSizeSpan(scale),
                    safeStart,
                    safeEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }
    private void insertBulletPoint(EditText editText) {
        int cursorPosition = editText.getSelectionStart();
        if (cursorPosition < 0) {
            cursorPosition = editText.getText().length();
        }
        String textBeforeCursor = editText.getText().toString().substring(0, cursorPosition);
        if (textBeforeCursor.endsWith("\n") || cursorPosition == 0) {
            editText.getText().insert(cursorPosition, "• ");
        } else {
            editText.getText().insert(cursorPosition, "\n• ");
        }
    }
    private String getCleanHtmlFromEditor(EditText editText) {
        return Html.toHtml(
                editText.getText(),
                Html.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL
        );
    }
    /**
     * Returns the start index of the paragraph (line) that contains the given position.
     * A paragraph is delimited by newline characters.
     */
    private int getParagraphStart(CharSequence text, int pos) {
        if (pos <= 0) return 0;
        int start = pos;
        while (start > 0 && text.charAt(start - 1) != '\n') {
            start--;
        }
        return start;
    }

    /**
     * Returns the end index (exclusive) of the paragraph containing pos.
     * Does NOT include the trailing newline — the span should end just before it so
     * that the newline character itself is never swallowed into the span.  This is
     * critical: if the span includes the '\n', Android's SPAN_EXCLUSIVE_EXCLUSIVE
     * logic moves the span end forward when more text is inserted after the newline,
     * which is exactly the corruption we are trying to prevent.
     */
    private int getParagraphEnd(CharSequence text, int pos) {
        int end = pos;
        while (end < text.length() && text.charAt(end) != '\n') {
            end++;
        }
        // Do NOT include the '\n' — stop just before it.
        return end;
    }

    /**
     * Stamps an AlignmentSpan.Standard onto the paragraph that contains the cursor
     * (or every paragraph covered by the current selection).
     *
     * We use SPAN_EXCLUSIVE_EXCLUSIVE and stop the span just before the trailing '\n'.
     * This keeps the span boundaries stable: inserting characters in the middle of the
     * paragraph keeps them inside the span, while the '\n' delimiter is never absorbed.
     * Alignment for new paragraphs is then handled in addLiveFormattingWatcher.
     */
    private void applyParagraphAlignment(EditText editText, android.text.Layout.Alignment alignment) {
        Editable editable = editText.getText();
        if (editable == null || editable.length() == 0) {
            Toast.makeText(requireContext(), "Start typing first, then apply alignment.", Toast.LENGTH_SHORT).show();
            return;
        }
        int selStart = editText.getSelectionStart();
        int selEnd   = editText.getSelectionEnd();
        if (selStart < 0) selStart = 0;
        if (selEnd   < 0) selEnd   = editable.length();
        if (selStart > selEnd) { int t = selStart; selStart = selEnd; selEnd = t; }

        int paraStart = getParagraphStart(editable, selStart);
        // For a multi-line selection, walk to the paragraph that contains selEnd.
        int paraEnd   = getParagraphEnd(editable, selEnd);

        // Wipe every existing alignment span that overlaps this range.
        AlignmentSpan[] old = editable.getSpans(paraStart, paraEnd, AlignmentSpan.class);
        for (AlignmentSpan s : old) editable.removeSpan(s);

        editable.setSpan(
                new AlignmentSpan.Standard(alignment),
                paraStart,
                paraEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        editText.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        editText.invalidate();
    }

    private void addLiveFormattingWatcher(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            private String insertedText = "";

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                lastTypingStart = start;
                lastTypingCount = count;
                if (count > 0 && start + count <= s.length()) {
                    insertedText = s.subSequence(start, start + count).toString();
                } else {
                    insertedText = "";
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (applyingTypingFormat) return;
                if (lastTypingStart < 0 || lastTypingCount <= 0) return;

                int start = lastTypingStart;
                int end   = Math.min(editable.length(), start + lastTypingCount);
                if (start > end) return;

                applyingTypingFormat = true;

                // ── 1. Inline formatting for the newly inserted characters ──────────
                if (start < end) {
                    if (activeBold)
                        editable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    if (activeItalic)
                        editable.setSpan(new StyleSpan(Typeface.ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    if (activeUnderline)
                        editable.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    if (activeHighlight)
                        editable.setSpan(new BackgroundColorSpan(HIGHLIGHT_COLOR), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    if (activeTextSizeScale != 1.0f)
                        editable.setSpan(new RelativeSizeSpan(activeTextSizeScale), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }


                if (!insertedText.contains("\n")) {
                    // ── Normal keystroke: re-pin the current paragraph's alignment ──
                    int paraStart = getParagraphStart(editable, start);
                    int paraEnd   = getParagraphEnd(editable, start);

                    AlignmentSpan[] spans = editable.getSpans(paraStart, paraEnd, AlignmentSpan.class);

                    android.text.Layout.Alignment alignmentToApply = activeAlignment;

                    if (spans.length > 0) {
                        // If this paragraph already has alignment, preserve that paragraph's alignment.
                        alignmentToApply = spans[spans.length - 1].getAlignment();

                        // Remove old spans to avoid duplicates.
                        for (AlignmentSpan s : spans) {
                            editable.removeSpan(s);
                        }
                    }

                    // IMPORTANT FIX:
                    // If the paragraph had no span because it was empty when alignment was selected,
                    // use activeAlignment and stamp the span now that the first character exists.
                    if (paraEnd > paraStart) {
                        editable.setSpan(
                                new AlignmentSpan.Standard(alignmentToApply),
                                paraStart,
                                paraEnd,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        );
                    }
                } else {
                    // ── Enter pressed: re-pin previous paragraph + stamp new paragraph ──
                    for (int i = start; i < end; i++) {
                        if (i >= editable.length()) break;
                        if (editable.charAt(i) != '\n') continue;

                        int prevParaStart = getParagraphStart(editable, i);
                        int prevParaEnd   = i; // stop just before the '\n'

                        int newParaStart  = i + 1;
                        int newParaEnd    = getParagraphEnd(editable, newParaStart);

                        // Discover the alignment that was on the previous paragraph.
                        AlignmentSpan[] prevSpans = editable.getSpans(
                                prevParaStart, Math.max(prevParaStart + 1, prevParaEnd),
                                AlignmentSpan.class);

                        android.text.Layout.Alignment inherited =
                                android.text.Layout.Alignment.ALIGN_NORMAL;
                        if (prevSpans.length > 0) {
                            inherited = prevSpans[prevSpans.length - 1].getAlignment();
                        }

                        // Re-pin the previous paragraph's span (boundaries changed).
                        for (AlignmentSpan s : prevSpans) editable.removeSpan(s);
                        if (prevParaEnd > prevParaStart) {
                            editable.setSpan(
                                    new AlignmentSpan.Standard(inherited),
                                    prevParaStart,
                                    prevParaEnd,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            );
                        }

                        // Propagate alignment to the brand-new paragraph.
                        if (inherited != android.text.Layout.Alignment.ALIGN_NORMAL
                                && newParaStart <= editable.length()) {
                            AlignmentSpan[] newSpans = editable.getSpans(
                                    newParaStart, newParaEnd, AlignmentSpan.class);
                            for (AlignmentSpan s : newSpans) editable.removeSpan(s);
                            if (newParaEnd >= newParaStart) {
                                editable.setSpan(
                                        new AlignmentSpan.Standard(inherited),
                                        newParaStart,
                                        newParaEnd,
                                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                );
                            }
                        }
                    }
                }

                applyingTypingFormat = false;
            }
        });
    }

    private void launchPersonalAttachmentPicker() {
        if (activeAttachmentNoteId == null || activeAttachmentNoteId.trim().isEmpty()) {
            safeToast("Save the note first before attaching files.", Toast.LENGTH_SHORT);
            return;
        }

        try {
            personalAttachmentPickerLauncher.launch(new String[]{
                    "application/pdf",
                    "application/vnd.ms-powerpoint",
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "image/*",
                    "text/*"
            });
        } catch (Exception e) {
            safeToast("Could not open file picker. Please try again.", Toast.LENGTH_LONG);
        }
    }

    //safe file handling for uploading
    private void safeToast(String message, int duration) {
        if (!isAdded() || getContext() == null) return;
        Toast.makeText(requireContext(), message, duration).show();
    }

    private boolean canReadSelectedFile(Uri uri) {
        if (uri == null || getContext() == null) return false;

        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri)) {
            return inputStream != null;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isAllowedUploadType(@Nullable String mimeType,
                                        @Nullable String fileName,
                                        boolean allowTextFiles) {
        String type = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);

        if (type.startsWith("image/")) return true;

        if (allowTextFiles && type.startsWith("text/")) return true;

        if (type.equals("application/pdf")) return true;

        if (type.equals("application/msword")) return true;

        if (type.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) return true;

        if (type.equals("application/vnd.ms-powerpoint")) return true;

        if (type.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation")) return true;

        // Fallback for devices that return application/octet-stream or no useful MIME type.
        return name.endsWith(".pdf")
                || name.endsWith(".doc")
                || name.endsWith(".docx")
                || name.endsWith(".ppt")
                || name.endsWith(".pptx")
                || name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".png")
                || name.endsWith(".webp")
                || name.endsWith(".gif")
                || name.endsWith(".bmp")
                || name.endsWith(".heic")
                || name.endsWith(".heif")
                || (allowTextFiles && name.endsWith(".txt"));
    }

    @Nullable
    private String validateSelectedUploadFile(Uri fileUri, boolean allowTextFiles) {
        if (fileUri == null) {
            return "No file was selected.";
        }

        if (getContext() == null || !isAdded()) {
            return "Screen is not ready. Please try again.";
        }

        if (!canReadSelectedFile(fileUri)) {
            return "Cannot read this file. Please choose another file.";
        }

        String fileName = getFileName(fileUri);
        String fileType = getMimeType(fileUri);
        long fileSize = getFileSize(fileUri);

        if (fileName == null || fileName.trim().isEmpty()) {
            return "Could not read the selected file name.";
        }

        if (fileSize > MAX_UPLOAD_BYTES) {
            return "File is too large. Please choose a file smaller than 25 MB.";
        }

        if (!isAllowedUploadType(fileType, fileName, allowTextFiles)) {
            return "Unsupported file type. Please upload a PDF, Word, PowerPoint, image"
                    + (allowTextFiles ? " or text file." : " file.");
        }

        return null;
    }

    private void safelyDeleteLocalFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) return;

        try {
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception ignored) {
        }
    }

    private void uploadSelectedModuleFile(Uri fileUri) {
        if (pendingUploadModuleId == null || pendingUploadTitle == null) {
            safeToast("Please choose a module and title first", Toast.LENGTH_SHORT);
            return;
        }

        String validationError = validateSelectedUploadFile(fileUri, false);
        if (validationError != null) {
            safeToast(validationError, Toast.LENGTH_LONG);
            return;
        }

        ModuleNote note = null;

        try {
            String fileName = getFileName(fileUri);
            String fileType = getMimeType(fileUri);
            long fileSize = getFileSize(fileUri);

            note = viewModel.createUploadingModuleNote(
                    pendingUploadModuleId,
                    pendingUploadTitle,
                    fileName,
                    fileType,
                    fileSize,
                    pendingUploadFolderId
            );

            String safeFileName = sanitizeFileName(fileName);
            String userId = note.getUserId();

            InputStream inputStream = requireContext().getContentResolver().openInputStream(fileUri);
            if (inputStream == null) {
                viewModel.markModuleNoteFailed(note);
                safeToast("Could not read the selected file.", Toast.LENGTH_LONG);
                return;
            }

            File localFile = new File(requireContext().getFilesDir(),
                    "module_notes/" + userId + "/" + pendingUploadModuleId + "/" + note.getNoteId() + "_" + safeFileName);
            localFile.getParentFile().mkdirs();

            FileOutputStream outputStream = new FileOutputStream(localFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.close();
            inputStream.close();

            String localPath = localFile.getAbsolutePath();

            viewModel.markModuleNoteUploaded(note, localPath);
            pendingUploadTitle = null;
            pendingUploadModuleId = null;
            pendingUploadFolderId = null;

            safeToast("File saved locally", Toast.LENGTH_SHORT);

        } catch (Exception e) {
            if (note != null && isAdded()) {
                viewModel.markModuleNoteFailed(note);
            }

            pendingUploadFolderId = null;

            String message = e.getMessage() == null
                    ? "Could not save file. Please try again."
                    : "Could not save file: " + e.getMessage();

            safeToast(message, Toast.LENGTH_LONG);
        }
    }
    private void uploadSelectedPersonalAttachment(Uri fileUri) {
        if (activeAttachmentNoteId == null || activeAttachmentNoteId.trim().isEmpty()) {
            safeToast("Open or create a personal note first", Toast.LENGTH_SHORT);
            return;
        }

        String validationError = validateSelectedUploadFile(fileUri, true);
        if (validationError != null) {
            safeToast(validationError, Toast.LENGTH_LONG);
            return;
        }

        PersonalNoteAttachment attachment = null;

        try {
            String fileName = getFileName(fileUri);
            String fileType = getMimeType(fileUri);
            long fileSize = getFileSize(fileUri);

            attachment = viewModel.createUploadingPersonalAttachment(
                    activeAttachmentNoteId,
                    fileName,
                    fileType,
                    fileSize
            );

            if (activeAttachmentBelongsToUnsavedDraft) {
                temporaryAttachments.add(attachment);
            }

            String safeFileName = sanitizeFileName(fileName);

            String userId = LocalSessionManager.getCurrentUserId(requireContext());

            if (userId == null || userId.trim().isEmpty()) {
                viewModel.markPersonalAttachmentFailed(attachment);
                safeToast("Please log in again before uploading attachments.", Toast.LENGTH_LONG);
                return;
            }

            String storagePath = "personal_note_attachments/"
                    + activeAttachmentNoteId + "/"
                    + attachment.getAttachmentId() + "_" + safeFileName;

            InputStream inputStream = requireContext().getContentResolver().openInputStream(fileUri);
            if (inputStream == null) {
                viewModel.markPersonalAttachmentFailed(attachment);
                safeToast("Could not read the selected file.", Toast.LENGTH_LONG);
                return;
            }

            File localFile = new File(requireContext().getFilesDir(),
                    "personal_attachments/" + userId + "/" + activeAttachmentNoteId + "/" + attachment.getAttachmentId() + "_" + safeFileName);
            localFile.getParentFile().mkdirs();

            FileOutputStream outputStream = new FileOutputStream(localFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.close();
            inputStream.close();

            String localPath = localFile.getAbsolutePath();

            viewModel.markPersonalAttachmentUploaded(
                    attachment,
                    storagePath,
                    localPath
            );

            attachment.setStoragePath(storagePath);
            attachment.setDownloadUrl(localPath);
            attachment.setUploadStatus("DONE");

            safeToast("Attachment saved locally", Toast.LENGTH_SHORT);

        } catch (Exception e) {
            if (attachment != null && isAdded()) {
                viewModel.markPersonalAttachmentFailed(attachment);
            }

            String message = e.getMessage() == null
                    ? "Could not save attachment. Please try again."
                    : "Could not save attachment: " + e.getMessage();

            safeToast(message, Toast.LENGTH_LONG);
        }
    }
    private void observeAttachmentCount(String noteId, TextView attachmentCountLabel) {
        LiveData<List<PersonalNoteAttachment>> liveData = viewModel.getAttachmentsForNote(noteId);
        liveData.observe(getViewLifecycleOwner(), attachments -> {
            int count = attachments == null ? 0 : attachments.size();
            setAttachmentCountLabel(attachmentCountLabel, count);
        });
    }
    private void observeAttachments(String noteId, LinearLayout attachmentList, TextView attachmentCountLabel) {
        LiveData<List<PersonalNoteAttachment>> liveData = viewModel.getAttachmentsForNote(noteId);
        liveData.observe(getViewLifecycleOwner(), attachments -> {
            attachmentList.removeAllViews();
            int count = attachments == null ? 0 : attachments.size();
            setAttachmentCountLabel(attachmentCountLabel, count);
            if (attachments == null || attachments.isEmpty()) {
                return;
            }
            for (PersonalNoteAttachment attachment : attachments) {
                attachmentList.addView(createAttachmentRow(attachment));
            }
        });
    }
    private View createAttachmentRow(PersonalNoteAttachment attachment) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(8), dp(12), dp(8));
        row.setBackground(roundedBg(Color.WHITE, BORDER_BLUE, 1, 14));
        row.setClickable(true);
        row.setOnClickListener(v -> openPersonalAttachmentLikeModuleNote(attachment));
        TextView name = new TextView(requireContext());
        String status = attachment.getUploadStatus() == null
                ? ""
                : " • " + attachment.getUploadStatus().toLowerCase(Locale.ROOT);
        name.setText("📎 " + attachment.getFileName() + status);
        name.setTextSize(14);
        name.setTextColor(DARK_BLUE);
        name.setSingleLine(true);
        MaterialButton open = makeCleanButton("Open", PRIMARY_BLUE, Color.WHITE);
        addButtonHint(open, "Open attachment");
        open.setOnClickListener(v -> openPersonalAttachmentLikeModuleNote(attachment));
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        row.addView(name, nameParams);
        row.addView(open);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, dp(6), 0, 0);
        row.setLayoutParams(rowParams);
        return row;
    }
    private void openPersonalAttachmentLikeModuleNote(PersonalNoteAttachment attachment) {
        if (attachment == null) {
            Toast.makeText(requireContext(), "Attachment not found", Toast.LENGTH_SHORT).show();
            return;
        }
        String url = attachment.getDownloadUrl();
        if (url == null || url.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Attachment is not uploaded yet", Toast.LENGTH_SHORT).show();
            return;
        }
        if ("UPLOADING".equalsIgnoreCase(attachment.getUploadStatus())) {
            Toast.makeText(requireContext(), "Attachment is still uploading", Toast.LENGTH_SHORT).show();
            return;
        }
        String fileName = attachment.getFileName() == null || attachment.getFileName().trim().isEmpty()
                ? "Attachment"
                : attachment.getFileName();
        NoteListItem fileItem = new NoteListItem(
                NoteListItem.TYPE_MODULE_FILE,
                attachment.getAttachmentId(),
                fileName,
                "Personal attachment",
                fileName,
                url,
                null
        );
        openFileInsideApp(fileItem);
    }
    @Nullable
    private String guessAttachmentMimeType(@Nullable String savedFileType, @Nullable String fileName) {
        if (savedFileType != null) {
            String type = savedFileType.trim();
            if (!type.isEmpty() && type.contains("/")) {
                return type;
            }
        }
        String extension = null;
        if (fileName != null) {
            String safeName = fileName.trim();
            int dotIndex = safeName.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < safeName.length() - 1) {
                extension = safeName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
            }
        }
        if (extension == null || extension.isEmpty()) {
            return "*/*";
        }
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        if (mimeType != null && !mimeType.trim().isEmpty()) {
            return mimeType;
        }
        switch (extension) {
            case "pdf": return "application/pdf";
            case "doc": return "application/msword";
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "ppt": return "application/vnd.ms-powerpoint";
            case "pptx": return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "gif": return "image/gif";
            case "webp": return "image/webp";
            default: return "*/*";
        }
    }
    private void cleanupTemporaryAttachments() {
        if (temporaryAttachments.isEmpty()) return;
        for (PersonalNoteAttachment attachment : new ArrayList<>(temporaryAttachments)) {
            String downloadUrl = attachment.getDownloadUrl();
            if (downloadUrl != null && !downloadUrl.trim().isEmpty()) {
                safelyDeleteLocalFile(downloadUrl);
            }
            viewModel.deletePersonalAttachment(attachment);
        }
        temporaryAttachments.clear();
    }
    private void cleanupTemporaryDraftNote() {
        if (temporaryDraftItem != null) {
            viewModel.deleteNote(temporaryDraftItem);
            temporaryDraftItem = null;
            activeAttachmentNoteId = null;
        }
    }
    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) result = cursor.getString(nameIndex);
                }
            } catch (Exception ignored) {}
        }
        if (result == null) {
            result = uri.getLastPathSegment();
            if (result == null) result = "attachment";
        }
        return result;
    }
    private long getFileSize(Uri uri) {
        long size = 0;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (sizeIndex >= 0) size = cursor.getLong(sizeIndex);
                }
            } catch (Exception ignored) {}
        }
        return size;
    }
    private String getMimeType(Uri uri) {
        String mimeType = requireContext().getContentResolver().getType(uri);
        if (mimeType != null) return mimeType;
        String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
        if (extension != null) {
            String guessed = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase(Locale.ROOT));
            if (guessed != null) return guessed;
        }
        return "application/octet-stream";
    }
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) return "file";
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
    private void handleNoteClick(NoteListItem item) {
        if (item.getType() == NoteListItem.TYPE_MODULE_FILE) {
            if (item.getStorageUri() == null || item.getStorageUri().trim().isEmpty()) {
                Toast.makeText(requireContext(), "File is not uploaded yet", Toast.LENGTH_SHORT).show();
                return;
            }
            openFileInsideApp(item);
            return;
        }
        showPersonalNoteViewer(item);
    }
    private void openFileInsideApp(NoteListItem item) {
        String fileUrl = item.getStorageUri();
        String fileName = item.getContentPreview() != null
                ? item.getContentPreview().toLowerCase(Locale.ROOT)
                : "";
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            Toast.makeText(requireContext(), "File is not uploaded yet", Toast.LENGTH_SHORT).show();
            return;
        }
        File localFile = new File(fileUrl);
        if (!localFile.exists()) {
            Toast.makeText(requireContext(), "File no longer exists on this device", Toast.LENGTH_SHORT).show();
            return;
        }
        if (fileName.endsWith(".pdf")) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri contentUri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    localFile);
            intent.setDataAndType(contentUri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivity(Intent.createChooser(intent, "Open PDF"));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(requireContext(), "No PDF viewer found on this device.", Toast.LENGTH_LONG).show();
            }
            return;
        }
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout mainLayout = new LinearLayout(requireContext());
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.WHITE);
        LinearLayout toolbar = new LinearLayout(requireContext());
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setPadding(dp(12), dp(10), dp(12), dp(8));
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setBackgroundColor(Color.WHITE);
        toolbar.setElevation(dp(8));
        TextView backButton = new TextView(requireContext());
        backButton.setText("←");
        backButton.setTextSize(28);
        backButton.setGravity(Gravity.CENTER);
        backButton.setTextColor(0xFF333333);
        backButton.setTypeface(Typeface.DEFAULT_BOLD);
        backButton.setPadding(dp(8), dp(2), dp(18), dp(2));
        TextView titleText = new TextView(requireContext());
        titleText.setText(item.getTitle());
        titleText.setTextSize(16);
        titleText.setTypeface(Typeface.DEFAULT_BOLD);
        titleText.setTextColor(0xFF222222);
        titleText.setSingleLine(true);
        titleText.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.addView(backButton);
        toolbar.addView(titleText, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));
        FrameLayout viewerFrame = new FrameLayout(requireContext());
        WebView webView = new WebView(requireContext());
        webView.setBackgroundColor(Color.WHITE);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setVerticalScrollBarEnabled(true);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        if (fileName.endsWith(".ppt")
                || fileName.endsWith(".pptx")
                || fileName.endsWith(".doc")
                || fileName.endsWith(".docx")) {
            webView.setInitialScale(120);
        } else {
            webView.setInitialScale(100);
        }
        webView.setWebViewClient(new WebViewClient());
        String viewerUrl;
        if (fileName.endsWith(".jpg")
                || fileName.endsWith(".jpeg")
                || fileName.endsWith(".png")
                || fileName.endsWith(".webp")
                || fileName.endsWith(".gif")) {
            viewerUrl = "file://" + fileUrl;
        } else {
            viewerUrl = "file://" + fileUrl;
        }
        webView.loadUrl(viewerUrl);
        LinearLayout zoomBar = new LinearLayout(requireContext());
        zoomBar.setOrientation(LinearLayout.HORIZONTAL);
        zoomBar.setGravity(Gravity.CENTER);
        zoomBar.setPadding(dp(8), dp(8), dp(8), dp(8));
        zoomBar.setBackgroundColor(0xDD333333);
        MaterialButton btnZoomOut = new MaterialButton(requireContext());
        btnZoomOut.setText("-");
        btnZoomOut.setTextColor(Color.WHITE);
        btnZoomOut.setBackgroundTintList(ColorStateList.valueOf(0xFF444444));
        btnZoomOut.setCornerRadius(dp(36));
        btnZoomOut.setMinWidth(0);
        btnZoomOut.setMinimumWidth(0);
        btnZoomOut.setPadding(dp(22), dp(6), dp(22), dp(6));
        MaterialButton btnZoomIn = new MaterialButton(requireContext());
        btnZoomIn.setText("+");
        btnZoomIn.setTextColor(Color.WHITE);
        btnZoomIn.setBackgroundTintList(ColorStateList.valueOf(0xFF444444));
        btnZoomIn.setCornerRadius(dp(36));
        btnZoomIn.setMinWidth(0);
        btnZoomIn.setMinimumWidth(0);
        btnZoomIn.setPadding(dp(22), dp(6), dp(22), dp(6));
        LinearLayout.LayoutParams zoomButtonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        zoomButtonParams.setMargins(dp(5), 0, dp(5), 0);
        zoomBar.addView(btnZoomOut, zoomButtonParams);
        zoomBar.addView(btnZoomIn, zoomButtonParams);
        FrameLayout.LayoutParams zoomBarParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END
        );
        zoomBarParams.setMargins(0, 0, dp(20), dp(20));
        viewerFrame.addView(webView);
        viewerFrame.addView(zoomBar, zoomBarParams);
        mainLayout.addView(toolbar);
        mainLayout.addView(viewerFrame, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1
        ));
        dialog.setContentView(mainLayout);
        backButton.setOnClickListener(v -> dialog.dismiss());
        btnZoomIn.setOnClickListener(v -> webView.zoomIn());
        btnZoomOut.setOnClickListener(v -> webView.zoomOut());
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
            );
            window.setBackgroundDrawableResource(android.R.color.white);
        }
        webView.postDelayed(() -> {
            if (fileName.endsWith(".ppt")
                    || fileName.endsWith(".pptx")
                    || fileName.endsWith(".doc")
                    || fileName.endsWith(".docx")) {
                webView.zoomIn();
            }
        }, 1200);
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}