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
import android.text.style.BackgroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
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
import android.text.style.AlignmentSpan;

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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NotesFragment extends Fragment {

    private FragmentNotesBinding binding;
    private NotesViewModel viewModel;
    private NotesAdapter adapter;

    private List<Module> currentModules = new ArrayList<>();

    private String pendingUploadTitle;
    private String pendingUploadModuleId;

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

    private static final int HIGHLIGHT_COLOR = 0xFFFFFF66;
    private static final int SCREEN_BG = 0xFFF8FBFF;
    private static final int DARK_BLUE = 0xFF08123D;
    private static final int PRIMARY_BLUE = 0xFF2563EB;
    private static final int BORDER_BLUE = 0xFFDDE7FF;
    private static final int MUTED_TEXT = 0xFF6B7280;
    private static final int TOP_SKY_BLUE = 0xFFE3F2FF;
    private static final int EDIT_BUTTON = 0xFF4F6F8F;
    private static final int VIEW_BUTTON = 0xFF6B5B95;

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

        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (userId == null) {
            Toast.makeText(requireContext(), "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }

        AppDatabase db = AppDatabase.getDatabase(requireContext());

        /*
         * IMPORTANT:
         * Your NotesViewModel constructor now needs PersonalNoteAttachmentRepository.
         * This assumes NotesViewModelFactory was updated to accept the same extra repository.
         */
        NotesViewModelFactory factory = new NotesViewModelFactory(
                userId,
                new ModuleRepositoryImpl(db.moduleDao()),
                new ModuleNoteRepositoryImpl(db.moduleNoteDao()),
                new PersonalNoteRepositoryImpl(db.personalNoteDao()),
                new PersonalNoteAttachmentRepositoryImpl(db.personalNoteAttachmentDao())
        );

        viewModel = new ViewModelProvider(this, factory).get(NotesViewModel.class);

        setupRecyclerView();
        setupButtons();
        observeData();
    }

    private void persistReadPermission(Uri uri) {
        try {
            requireContext().getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (SecurityException e) {
            Toast.makeText(requireContext(),
                    "Could not keep access to selected file: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
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
        button.setOnLongClickListener(v -> {
            Toast.makeText(requireContext(), hint, Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    private void resetFormattingState() {
        activeBold = false;
        activeItalic = false;
        activeUnderline = false;
        activeHighlight = false;
        activeTextSizeScale = 1.0f;
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
                view.setTextColor(DARK_BLUE);
                view.setSingleLine(true);
                view.setPadding(dp(12), 0, dp(8), 0);
                return view;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextSize(14);
                view.setTextColor(DARK_BLUE);
                view.setPadding(dp(14), dp(10), dp(14), dp(10));
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
        contentInput.setMinLines(16);
        contentInput.setMinHeight(dp(360));
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
        noteCard.setMinimumHeight(dp(430));
        noteCard.setBackground(roundedBg(Color.WHITE, BORDER_BLUE, 1, 16));

        TextView charCount = charCountTarget != null ? charCountTarget : new TextView(requireContext());
        charCount.setText("Characters: " + contentInput.getText().length());
        charCount.setTextSize(13);
        charCount.setTextColor(MUTED_TEXT);
        charCount.setGravity(Gravity.END);

        contentInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                charCount.setText("Characters: " + s.length());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        noteCard.addView(contentInput, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
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

    private void setupRecyclerView() {
        adapter = new NotesAdapter(new NotesAdapter.OnNoteActionListener() {
            @Override public void onOpen(NoteListItem item) { handleNoteClick(item); }
            @Override public void onRename(NoteListItem item) { showRenameNoteDialog(item); }
            @Override public void onDelete(NoteListItem item) { showDeleteNoteDialog(item); }
        });

        binding.rvNotes.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvNotes.setAdapter(adapter);
    }

    private void setupButtons() {
        binding.btnSettings.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_notesFragment_to_settingsFragment));

        binding.fabAddNote.setOnClickListener(this::showAddMenu);

        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setSearchQuery(s == null ? "" : s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

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
        });

        binding.spinnerModuleFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position <= 0) {
                    viewModel.setSelectedModuleId(null);
                    return;
                }
                int moduleIndex = position - 1;
                if (moduleIndex >= 0 && moduleIndex < currentModules.size()) {
                    viewModel.setSelectedModuleId(currentModules.get(moduleIndex).getModuleId());
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {
                viewModel.setSelectedModuleId(null);
            }
        });
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

        ArrayAdapter<String> moduleAdapter = createModuleSpinnerAdapter(moduleNames);
        binding.spinnerModuleFilter.setAdapter(moduleAdapter);
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

        menu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.equals("Upload module file")) showUploadDialog();
            else showPersonalNoteDialog();
            return true;
        });

        menu.show();
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

        Spinner moduleSpinner = new Spinner(requireContext());
        List<String> moduleNames = new ArrayList<>();
        for (Module module : currentModules) {
            String label = module.getModuleCode() != null && !module.getModuleCode().isEmpty()
                    ? module.getModuleCode() + " - " + module.getName()
                    : module.getName();
            moduleNames.add(label);
        }
        moduleSpinner.setAdapter(createModuleSpinnerAdapter(moduleNames));

        layout.addView(titleInput);
        layout.addView(moduleSpinner);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Upload module note")
                .setMessage("Choose the module and title first. Then select a PDF, PowerPoint, Word document or image.")
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

                    moduleFilePickerLauncher.launch(new String[]{
                            "application/pdf",
                            "application/vnd.ms-powerpoint",
                            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                            "application/msword",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "image/*"
                    });
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

        LinearLayout topBar = createTopBar(btnBack, topTitle, btnSave, null, moduleSpinner);

        LinearLayout editorLayout = new LinearLayout(requireContext());
        editorLayout.setOrientation(LinearLayout.VERTICAL);
        editorLayout.setPadding(dp(22), 0, dp(22), dp(10));
        editorLayout.setBackgroundColor(SCREEN_BG);

        EditText titleInput = makeTitleInput("Title goes here");
        EditText contentInput = makeContentInput();
        TextView attachmentStatus = makeAttachmentStatusText();
        final boolean[] observingDraftAttachments = {false};

        LinearLayout noteCard = createNoteCard(contentInput, null);
        editorLayout.addView(titleInput);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, dp(14), 0, 0);
        editorLayout.addView(noteCard, cardParams);
        editorLayout.addView(attachmentStatus);

        HorizontalScrollView formatScroll = createFormattingToolbar(contentInput, () -> {
            ensureDraftNoteExists(titleInput, contentInput, moduleSpinner, true);
            if (!observingDraftAttachments[0] && activeAttachmentNoteId != null) {
                observeAttachmentCount(activeAttachmentNoteId, attachmentStatus);
                observingDraftAttachments[0] = true;
            }
            launchPersonalAttachmentPicker();
        });

        attachmentStatus.setOnClickListener(v -> {
            if (activeAttachmentNoteId == null || activeAttachmentNoteId.trim().isEmpty()) {
                Toast.makeText(requireContext(), "Attach a file first", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean saved = savePersonalNoteFromEditor(titleInput, contentInput, moduleSpinner);
            if (saved) {
                String title = titleInput.getText().toString().trim();
                String htmlContent = Html.toHtml(contentInput.getText(), Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);
                String moduleName = getSelectedPersonalModuleId(moduleSpinner) == null ? "Personal" : "Module";
                NoteListItem viewItem = new NoteListItem(
                        NoteListItem.TYPE_PERSONAL_NOTE,
                        activeAttachmentNoteId,
                        title,
                        moduleName,
                        htmlContent,
                        null
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
        screen.addView(formatScroll);

        dialog.setContentView(screen);
        addLiveFormattingWatcher(contentInput);

        btnBack.setOnClickListener(v -> {
            if (noteSaved[0]) {
                dialog.dismiss();
                return;
            }
            confirmSaveBeforeLeaving(dialog, titleInput, contentInput, moduleSpinner, noteSaved);
        });

        dialog.setOnKeyListener((dialogInterface, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                if (noteSaved[0]) dialog.dismiss();
                else confirmSaveBeforeLeaving(dialog, titleInput, contentInput, moduleSpinner, noteSaved);
                return true;
            }
            return false;
        });

        btnSave.setOnClickListener(v -> {
            boolean saved = savePersonalNoteFromEditor(titleInput, contentInput, moduleSpinner);
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

        final String[] savedTitle = { item.getTitle() == null ? "Untitled note" : item.getTitle() };
        final String[] savedHtml = { item.getContentPreview() == null ? "" : item.getContentPreview() };

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

        contentHolder.addView(titleView);
        contentHolder.addView(titleInput);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, dp(14), 0, 0);
        contentHolder.addView(viewCard, cardParams);
        contentHolder.addView(editCard, cardParams);
        contentHolder.addView(attachmentHeader);
        contentHolder.addView(attachmentList);

        HorizontalScrollView formatScroll = createFormattingToolbar(contentInput, () -> {
            activeAttachmentNoteId = item.getId();
            activeAttachmentBelongsToUnsavedDraft = false;
            launchPersonalAttachmentPicker();
        });
        formatScroll.setVisibility(View.GONE);

        ScrollView scroll = new ScrollView(requireContext());
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(SCREEN_BG);
        scroll.addView(contentHolder);

        screen.addView(topBar);
        screen.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
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

                String newHtml = Html.toHtml(contentInput.getText(), Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);
                viewModel.updatePersonalNote(item, newTitle, newHtml);

                savedTitle[0] = newTitle;
                savedHtml[0] = newHtml;
                topTitle.setText(newTitle);
                titleView.setText(newTitle);
                bodyView.setText(Html.fromHtml(newHtml, Html.FROM_HTML_MODE_LEGACY));
                hasUnsavedChanges[0] = false;
                isEditMode[0] = false;
                attachmentList.setVisibility(View.VISIBLE);
                switchToViewMode(btnMode, btnSave, titleView, viewCard, titleInput, editCard, formatScroll);
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
                        switchToViewMode(btnMode, btnSave, titleView, viewCard, titleInput, editCard, formatScroll);
                    });
                } else {
                    isEditMode[0] = false;
                    attachmentList.setVisibility(View.VISIBLE);
                    switchToViewMode(btnMode, btnSave, titleView, viewCard, titleInput, editCard, formatScroll);
                }
            }
        });

        btnSave.setOnClickListener(v -> {
            String newTitle = titleInput.getText().toString().trim();
            if (newTitle.isEmpty()) {
                Toast.makeText(requireContext(), "Title cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            String newHtml = Html.toHtml(contentInput.getText(), Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);
            viewModel.updatePersonalNote(item, newTitle, newHtml);

            savedTitle[0] = newTitle;
            savedHtml[0] = newHtml;
            topTitle.setText(newTitle);
            titleView.setText(newTitle);
            bodyView.setText(Html.fromHtml(newHtml, Html.FROM_HTML_MODE_LEGACY));
            hasUnsavedChanges[0] = false;
            isEditMode[0] = false;
            attachmentList.setVisibility(View.VISIBLE);

            switchToViewMode(btnMode, btnSave, titleView, viewCard, titleInput, editCard, formatScroll);
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
                                  HorizontalScrollView formatScroll) {
        btnMode.setText("Edit");
        btnMode.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
        btnMode.setTextColor(DARK_BLUE);
        btnSave.setVisibility(View.GONE);
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
                                          boolean[] noteSaved) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Save note?")
                .setMessage("Do you want to save this note before leaving?")
                .setPositiveButton("Save", (d, which) -> {
                    boolean saved = savePersonalNoteFromEditor(titleInput, contentInput, moduleSpinner);
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

    private boolean savePersonalNoteFromEditor(EditText titleInput, EditText contentInput, Spinner moduleSpinner) {
        String title = titleInput.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Title is required", Toast.LENGTH_SHORT).show();
            return false;
        }

        String htmlContent = Html.toHtml(contentInput.getText(), Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);

        if (temporaryDraftItem != null) {
            viewModel.updatePersonalNote(temporaryDraftItem, title, htmlContent);
            return true;
        }

        String moduleId = getSelectedPersonalModuleId(moduleSpinner);
        PersonalNote note = viewModel.addPersonalNote(title, htmlContent, moduleId);
        temporaryDraftItem = makeTemporaryItem(note, htmlContent);
        activeAttachmentNoteId = note.getNoteId();
        return true;
    }

    private void ensureDraftNoteExists(EditText titleInput,
                                       EditText contentInput,
                                       Spinner moduleSpinner,
                                       boolean showMessage) {
        if (activeAttachmentNoteId != null && !activeAttachmentNoteId.trim().isEmpty()) return;

        String title = titleInput.getText().toString().trim();
        if (title.isEmpty()) title = "Untitled note";

        String htmlContent = Html.toHtml(contentInput.getText(), Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);
        String moduleId = getSelectedPersonalModuleId(moduleSpinner);

        PersonalNote note = viewModel.addPersonalNote(title, htmlContent, moduleId);
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
                null
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
        MaterialButton bullet = makeFormatButton("☷", "Add bullet point");
        MaterialButton alignLeft = makeFormatButton("≡", "Align left");
        MaterialButton more = makeFormatButton("⋯", "More formatting options");
        MaterialButton attach = makeFormatButton("📎", "Attach file");

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
            activeTextSizeScale -= 0.10f;

            if (activeTextSizeScale < 0.70f) {
                activeTextSizeScale = 0.70f;
            }

            if (hasTextSelection(contentInput)) {
                applyTextSizeToSelection(contentInput, activeTextSizeScale);
            }

            Toast.makeText(requireContext(), "Text size decreased", Toast.LENGTH_SHORT).show();

            setFormatButtonActive(sizeDown, activeTextSizeScale < 1.0f);
            setFormatButtonActive(sizeUp, activeTextSizeScale > 1.0f);
        });

        sizeUp.setOnClickListener(v -> {
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

        alignLeft.setOnClickListener(v ->
                applyParagraphAlignment(contentInput, android.text.Layout.Alignment.ALIGN_NORMAL));

        more.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), v);

            popup.getMenu().add("Align center");
            popup.getMenu().add("Align right");
            popup.getMenu().add("Normal text size");

            popup.setOnMenuItemClickListener(item -> {
                String selected = item.getTitle().toString();

                if (selected.equals("Align center")) {
                    applyParagraphAlignment(contentInput, android.text.Layout.Alignment.ALIGN_CENTER);
                } else if (selected.equals("Align right")) {
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

        attach.setOnClickListener(v -> {
            if (onAttachClicked == null) {
                Toast.makeText(requireContext(), "Attachments are only for personal notes", Toast.LENGTH_SHORT).show();
                return;
            }

            onAttachClicked.run();
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

        /*
         * Pushes the attachment button toward the right.
         * On smaller screens, the toolbar can still scroll horizontally.
         */
        View spacer = new View(requireContext());
        row.addView(spacer, new LinearLayout.LayoutParams(dp(18), 1));

        row.addView(attach);

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

    private void applyParagraphAlignment(EditText editText, android.text.Layout.Alignment alignment) {
        Editable editable = editText.getText();

        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();

        if (start < 0 || end < 0) return;

        if (start == end) {
            start = 0;
            end = editable.length();
        }

        int safeStart = Math.min(start, end);
        int safeEnd = Math.max(start, end);

        AlignmentSpan[] oldAlignmentSpans = editable.getSpans(safeStart, safeEnd, AlignmentSpan.class);

        for (AlignmentSpan span : oldAlignmentSpans) {
            editable.removeSpan(span);
        }

        editable.setSpan(
                new AlignmentSpan.Standard(alignment),
                safeStart,
                safeEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
    }

    private void addLiveFormattingWatcher(EditText contentInput) {
        contentInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                lastTypingStart = start;
                lastTypingCount = after;
            }

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                lastTypingStart = start;
                lastTypingCount = count;
            }

            @Override public void afterTextChanged(Editable s) {
                if (applyingTypingFormat) return;
                if (lastTypingStart < 0 || lastTypingCount <= 0) return;

                int start = lastTypingStart;
                int end = Math.min(s.length(), lastTypingStart + lastTypingCount);
                if (start >= end) return;

                applyingTypingFormat = true;
                if (activeBold) s.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (activeItalic) s.setSpan(new StyleSpan(Typeface.ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (activeUnderline) s.setSpan(new UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (activeHighlight) s.setSpan(new BackgroundColorSpan(HIGHLIGHT_COLOR), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (activeTextSizeScale != 1.0f) s.setSpan(new RelativeSizeSpan(activeTextSizeScale), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                applyingTypingFormat = false;
            }
        });
    }

    private void launchPersonalAttachmentPicker() {
        if (activeAttachmentNoteId == null || activeAttachmentNoteId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Save the note first before attaching files.", Toast.LENGTH_SHORT).show();
            return;
        }

        personalAttachmentPickerLauncher.launch(new String[]{
                "application/pdf",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "image/*",
                "text/*"
        });
    }

    private void uploadSelectedModuleFile(Uri fileUri) {
        if (pendingUploadModuleId == null || pendingUploadTitle == null) {
            Toast.makeText(requireContext(), "Please choose a module and title first", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = getFileName(fileUri);
        String fileType = getMimeType(fileUri);
        long fileSize = getFileSize(fileUri);

        ModuleNote note = viewModel.createUploadingModuleNote(
                pendingUploadModuleId,
                pendingUploadTitle,
                fileName,
                fileType,
                fileSize
        );

        String safeFileName = sanitizeFileName(fileName);
        StorageReference ref = FirebaseStorage.getInstance()
                .getReference()
                .child("users")
                .child(note.getUserId())
                .child("module_notes")
                .child(pendingUploadModuleId)
                .child(note.getNoteId() + "_" + safeFileName);

        ref.putFile(fileUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> {
                    viewModel.markModuleNoteUploaded(note, uri.toString());
                    Toast.makeText(requireContext(), "File uploaded", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    viewModel.markModuleNoteFailed(note);
                    Toast.makeText(requireContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void uploadSelectedPersonalAttachment(Uri fileUri) {
        if (activeAttachmentNoteId == null || activeAttachmentNoteId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Open or create a personal note first", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = getFileName(fileUri);
        String fileType = getMimeType(fileUri);
        long fileSize = getFileSize(fileUri);

        PersonalNoteAttachment attachment = viewModel.createUploadingPersonalAttachment(
                activeAttachmentNoteId,
                fileName,
                fileType,
                fileSize
        );

        if (activeAttachmentBelongsToUnsavedDraft) {
            temporaryAttachments.add(attachment);
        }

        String safeFileName = sanitizeFileName(fileName);
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "unknown_user";

        String storagePath = "users/" + userId + "/personal_note_attachments/"
                + activeAttachmentNoteId + "/" + attachment.getAttachmentId() + "_" + safeFileName;

        StorageReference ref = FirebaseStorage.getInstance().getReference().child(storagePath);

        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType(fileType == null || fileType.trim().isEmpty() ? "application/octet-stream" : fileType)
                .build();

        ref.putFile(fileUri, metadata)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> {
                    viewModel.markPersonalAttachmentUploaded(attachment, storagePath, uri.toString());
                    attachment.setStoragePath(storagePath);
                    attachment.setDownloadUrl(uri.toString());
                    attachment.setUploadStatus("DONE");
                    Toast.makeText(requireContext(), "Attachment uploaded", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    viewModel.markPersonalAttachmentFailed(attachment);
                    Toast.makeText(requireContext(), "Attachment upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
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
        String status = attachment.getUploadStatus() == null ? "" : " • " + attachment.getUploadStatus().toLowerCase(Locale.ROOT);
        name.setText("📎 " + attachment.getFileName() + status);
        name.setTextSize(14);
        name.setTextColor(DARK_BLUE);
        name.setSingleLine(true);

        MaterialButton open = makeCleanButton("Open", PRIMARY_BLUE, Color.WHITE);
        addButtonHint(open, "Open attachment");

        open.setOnClickListener(v -> openPersonalAttachmentLikeModuleNote(attachment));

        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
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

        /*
         * Personal attachments are opened using the same in-app viewer logic as module notes.
         * contentPreview is deliberately set to the file name because openFileInsideApp()
         * uses contentPreview to detect .pdf, .pptx, .docx, and image extensions.
         */
        NoteListItem fileItem = new NoteListItem(
                NoteListItem.TYPE_MODULE_FILE,
                attachment.getAttachmentId(),
                fileName,
                "Personal attachment",
                fileName,
                url
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
            String storagePath = attachment.getStoragePath();

            if (storagePath != null && !storagePath.trim().isEmpty()) {
                FirebaseStorage.getInstance()
                        .getReference()
                        .child(storagePath)
                        .delete()
                        .addOnFailureListener(e -> {
                            // Keep the app usable even if Storage cleanup fails.
                        });
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

        if (fileName.endsWith(".pdf")) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(fileUrl), "application/pdf");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

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
            viewerUrl = fileUrl;
        } else {
            viewerUrl = "https://docs.google.com/gview?embedded=true&url="
                    + Uri.encode(fileUrl);
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
