package com.example.mpproject.presentation.view;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mpproject.R;
import com.example.mpproject.data.local.AppDatabase;
import com.example.mpproject.data.local.entity.ResearchPaperEntity;
import com.example.mpproject.data.repository.ResearchPaperRepositoryImpl;
import com.example.mpproject.databinding.FragmentResearchBinding;
import com.example.mpproject.domain.repository.ResearchPaperRepository;
import com.example.mpproject.presentation.adapter.ResearchPaperAdapter;
import com.example.mpproject.presentation.viewmodel.ResearchViewModel;
import com.example.mpproject.presentation.viewmodel.ResearchViewModelFactory;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.example.mpproject.data.local.LocalSessionManager;

import java.io.File;
import java.util.List;
import java.util.Locale;

public class ResearchFragment extends Fragment {

    private FragmentResearchBinding binding;
    private ResearchViewModel viewModel;
    private ResearchPaperAdapter adapter;

    private Uri selectedFileUri;
    private String selectedFileName;

    private LiveData<List<ResearchPaperEntity>> currentLiveData;

    private final ActivityResultLauncher<String[]> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    try {
                        requireContext().getContentResolver().takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                    } catch (SecurityException ignored) {
                        // Some file providers do not allow persistable permission.
                        // Upload can still continue immediately.
                    }

                    selectedFileUri = uri;
                    selectedFileName = getFileName(uri);

                    showAddPaperDialog();
                }
            });

    public ResearchFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentResearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupViewModel();
        setupRecyclerView();
        setupObservers();
        setupClicks();
        observeAllPapers();
    }

    private void setupViewModel() {
        AppDatabase db = AppDatabase.getDatabase(requireContext());

        ResearchPaperRepository repository =
                new ResearchPaperRepositoryImpl(requireContext(), db.researchPaperDao());

        ResearchViewModelFactory factory = new ResearchViewModelFactory(repository);

        viewModel = new ViewModelProvider(this, factory).get(ResearchViewModel.class);
    }

    private void setupRecyclerView() {
        adapter = new ResearchPaperAdapter(new ResearchPaperAdapter.OnResearchPaperClickListener() {
            @Override
            public void onPaperClick(ResearchPaperEntity paper) {
                showPaperOptionsDialog(paper);
            }

            @Override
            public void onPaperLongClick(ResearchPaperEntity paper) {
                confirmDeletePaper(paper);
            }
        });

        binding.recyclerResearchPapers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerResearchPapers.setAdapter(adapter);
    }

    private void setupObservers() {
        viewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.trim().isEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.fabAddResearchPaper.setEnabled(loading == null || !loading);
        });
    }

    private void setupClicks() {
        binding.fabAddResearchPaper.setOnClickListener(v -> openFilePicker());

        binding.chipGroupResearchFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds == null || checkedIds.isEmpty()) return;

            int checkedId = checkedIds.get(0);

            if (checkedId == R.id.chipAll) {
                observeAllPapers();
            } else if (checkedId == R.id.chipUnread) {
                observeStatus("UNREAD");
            } else if (checkedId == R.id.chipReading) {
                observeStatus("READING");
            } else if (checkedId == R.id.chipRead) {
                observeStatus("READ");
            } else if (checkedId == R.id.chipImportant) {
                observeImportant();
            }
        });

        binding.edtSearchResearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s == null ? "" : s.toString().trim();

                if (query.isEmpty()) {
                    refreshCurrentFilter();
                } else {
                    observeSearch(query);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });
    }

    private String getCurrentUserId() {
        return LocalSessionManager.getCurrentUserId(requireContext());
    }

    private void observeAllPapers() {
        String userId = getCurrentUserId();

        if (userId == null) {
            Toast.makeText(requireContext(), "Please log in first.", Toast.LENGTH_SHORT).show();
            return;
        }

        switchLiveData(viewModel.getAllPapers(userId));
    }

    private void observeStatus(String status) {
        String userId = getCurrentUserId();

        if (userId == null) {
            Toast.makeText(requireContext(), "Please log in first.", Toast.LENGTH_SHORT).show();
            return;
        }

        switchLiveData(viewModel.getPapersByStatus(userId, status));
    }

    private void observeImportant() {
        String userId = getCurrentUserId();

        if (userId == null) {
            Toast.makeText(requireContext(), "Please log in first.", Toast.LENGTH_SHORT).show();
            return;
        }

        switchLiveData(viewModel.getImportantPapers(userId));
    }

    private void observeSearch(String query) {
        String userId = getCurrentUserId();

        if (userId == null) {
            Toast.makeText(requireContext(), "Please log in first.", Toast.LENGTH_SHORT).show();
            return;
        }

        switchLiveData(viewModel.searchPapers(userId, query));
    }

    private void switchLiveData(LiveData<List<ResearchPaperEntity>> newLiveData) {
        if (newLiveData == null || binding == null) return;

        if (currentLiveData != null) {
            currentLiveData.removeObservers(getViewLifecycleOwner());
        }

        currentLiveData = newLiveData;

        currentLiveData.observe(getViewLifecycleOwner(), papers -> {
            if (binding == null) return;

            adapter.submitList(papers);

            boolean empty = papers == null || papers.isEmpty();
            binding.txtEmptyResearch.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.recyclerResearchPapers.setVisibility(empty ? View.GONE : View.VISIBLE);
        });
    }

    private void refreshCurrentFilter() {
        if (binding == null) return;

        String query = binding.edtSearchResearch.getText() == null
                ? ""
                : binding.edtSearchResearch.getText().toString().trim();

        if (!query.isEmpty()) {
            observeSearch(query);
            return;
        }

        int checkedId = binding.chipGroupResearchFilters.getCheckedChipId();

        if (checkedId == R.id.chipUnread) {
            observeStatus("UNREAD");
        } else if (checkedId == R.id.chipReading) {
            observeStatus("READING");
        } else if (checkedId == R.id.chipRead) {
            observeStatus("READ");
        } else if (checkedId == R.id.chipImportant) {
            observeImportant();
        } else {
            observeAllPapers();
        }
    }

    private void openFilePicker() {
        filePickerLauncher.launch(new String[]{
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "image/jpeg",
                "image/png",
                "image/webp"
        });
    }

    private void showAddPaperDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        boolean isDarkMode =
                (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                        == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        int surfaceColor = isDarkMode
                ? Color.rgb(28, 28, 30)
                : Color.WHITE;

        int softSurfaceColor = isDarkMode
                ? Color.rgb(40, 40, 44)
                : Color.rgb(246, 248, 252);

        int onSurfaceColor = isDarkMode
                ? Color.WHITE
                : Color.rgb(20, 20, 24);

        int onSurfaceVariantColor = isDarkMode
                ? Color.rgb(210, 210, 215)
                : Color.rgb(95, 99, 108);

        int primaryColor = isDarkMode
                ? Color.rgb(130, 180, 255)
                : Color.rgb(33, 120, 230);

        int outlineColor = isDarkMode
                ? Color.rgb(90, 90, 96)
                : Color.rgb(215, 221, 232);

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setFillViewport(false);
        scrollView.setClipToPadding(false);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));

        GradientDrawable background = new GradientDrawable();
        background.setColor(surfaceColor);
        background.setCornerRadius(dp(28));
        root.setBackground(background);

        TextView titleView = new TextView(requireContext());
        titleView.setText("Add Research Paper");
        titleView.setTextSize(22);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setTextColor(onSurfaceColor);

        TextView subtitleView = new TextView(requireContext());
        subtitleView.setText("Save the source, organise your notes, and track reading progress.");
        subtitleView.setTextSize(13);
        subtitleView.setTextColor(onSurfaceVariantColor);
        subtitleView.setPadding(0, dp(4), 0, dp(14));

        root.addView(titleView);
        root.addView(subtitleView);

        TextView fileLabelTitle = makeSectionLabel("Selected file", onSurfaceColor);
        root.addView(fileLabelTitle);

        TextView fileLabel = new TextView(requireContext());
        fileLabel.setText(shortenFileName(selectedFileName));
        fileLabel.setTextSize(13);
        fileLabel.setTextColor(primaryColor);
        fileLabel.setSingleLine(false);
        fileLabel.setPadding(dp(12), dp(10), dp(12), dp(10));

        GradientDrawable fileBackground = new GradientDrawable();
        fileBackground.setColor(adjustAlpha(primaryColor, 0.10f));
        fileBackground.setCornerRadius(dp(16));
        fileBackground.setStroke(dp(1), adjustAlpha(primaryColor, 0.35f));
        fileLabel.setBackground(fileBackground);

        LinearLayout.LayoutParams fileParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        fileParams.setMargins(0, 0, 0, dp(16));
        fileLabel.setLayoutParams(fileParams);
        root.addView(fileLabel);

        TextView paperDetailsLabel = makeSectionLabel("Paper details", onSurfaceColor);
        root.addView(paperDetailsLabel);

        TextInputEditText edtTitle = addTextField(root, "Paper title", 1, softSurfaceColor, outlineColor);
        TextInputEditText edtAuthors = addTextField(root, "Authors", 1, softSurfaceColor, outlineColor);
        TextInputEditText edtYear = addTextField(root, "Year", 1, softSurfaceColor, outlineColor);
        TextInputEditText edtCategory = addTextField(root, "Theme / Category", 1, softSurfaceColor, outlineColor);

        TextView notesLabel = makeSectionLabel("Research notes", onSurfaceColor);
        notesLabel.setPadding(0, dp(8), 0, dp(8));
        root.addView(notesLabel);

        TextInputEditText edtSummary = addTextField(root, "Summary", 3, softSurfaceColor, outlineColor);
        TextInputEditText edtKeyFindings = addTextField(root, "Key findings", 2, softSurfaceColor, outlineColor);
        TextInputEditText edtMethodology = addTextField(root, "Methodology", 2, softSurfaceColor, outlineColor);
        TextInputEditText edtRelevance = addTextField(root, "Relevance to my project", 2, softSurfaceColor, outlineColor);

        TextView progressLabel = makeSectionLabel("Reading progress", onSurfaceColor);
        progressLabel.setPadding(0, dp(8), 0, dp(8));
        root.addView(progressLabel);

        Spinner statusSpinner = new Spinner(requireContext());
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"UNREAD", "READING", "READ"}
        );
        statusSpinner.setAdapter(statusAdapter);
        statusSpinner.setPadding(dp(12), 0, dp(12), 0);

        GradientDrawable spinnerBackground = new GradientDrawable();
        spinnerBackground.setColor(softSurfaceColor);
        spinnerBackground.setCornerRadius(dp(16));
        spinnerBackground.setStroke(dp(1), outlineColor);
        statusSpinner.setBackground(spinnerBackground);

        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
        );
        spinnerParams.setMargins(0, 0, 0, dp(10));
        statusSpinner.setLayoutParams(spinnerParams);
        root.addView(statusSpinner);

        CheckBox chkImportant = new CheckBox(requireContext());
        chkImportant.setText("Mark as important");
        chkImportant.setTextColor(onSurfaceColor);
        chkImportant.setTextSize(14);

        LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        checkParams.setMargins(0, 0, 0, dp(16));
        chkImportant.setLayoutParams(checkParams);
        root.addView(chkImportant);

        LinearLayout buttonRow = new LinearLayout(requireContext());
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);

        MaterialButton btnCancel = new MaterialButton(requireContext());
        btnCancel.setText("Cancel");
        btnCancel.setAllCaps(false);
        btnCancel.setCornerRadius(dp(18));
        btnCancel.setStrokeWidth(dp(1));
        btnCancel.setStrokeColor(ColorStateList.valueOf(outlineColor));
        btnCancel.setTextColor(primaryColor);
        btnCancel.setBackgroundTintList(ColorStateList.valueOf(softSurfaceColor));

        MaterialButton btnSave = new MaterialButton(requireContext());
        btnSave.setText("Save");
        btnSave.setAllCaps(false);
        btnSave.setCornerRadius(dp(18));
        btnSave.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
        btnSave.setTextColor(Color.WHITE);

        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
                0,
                dp(50),
                1
        );
        cancelParams.setMargins(0, 0, dp(8), 0);

        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                0,
                dp(50),
                1
        );
        saveParams.setMargins(dp(8), 0, 0, 0);

        buttonRow.addView(btnCancel, cancelParams);
        buttonRow.addView(btnSave, saveParams);
        root.addView(buttonRow);

        scrollView.addView(root);
        dialog.setContentView(scrollView);

        btnCancel.setOnClickListener(v -> {
            selectedFileUri = null;
            selectedFileName = null;
            dialog.dismiss();
        });

        btnSave.setOnClickListener(v -> {
            String userId = getCurrentUserId();

            if (userId == null) {
                Toast.makeText(requireContext(), "Please log in first.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedFileUri == null) {
                Toast.makeText(requireContext(), "No file selected.", Toast.LENGTH_SHORT).show();
                return;
            }

            String title = getText(edtTitle);

            if (title.isEmpty()) {
                edtTitle.setError("Enter paper title");
                return;
            }

            hideKeyboard(edtTitle);

            viewModel.uploadResearchPaper(
                    userId,
                    selectedFileUri,
                    selectedFileName,
                    title,
                    getText(edtAuthors),
                    getText(edtYear),
                    getText(edtCategory),
                    getText(edtSummary),
                    getText(edtKeyFindings),
                    getText(edtMethodology),
                    getText(edtRelevance),
                    statusSpinner.getSelectedItem().toString(),
                    chkImportant.isChecked()
            );

            selectedFileUri = null;
            selectedFileName = null;
            dialog.dismiss();
        });

        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            WindowManager.LayoutParams params = new WindowManager.LayoutParams();
            params.copyFrom(window.getAttributes());
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);
        }
    }

    private TextInputEditText addTextField(LinearLayout root,
                                           String hint,
                                           int minLines,
                                           int backgroundColor,
                                           int outlineColor) {
        TextInputLayout inputLayout = new TextInputLayout(requireContext());
        inputLayout.setHint(hint);
        inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_FILLED);
        inputLayout.setBoxBackgroundColor(backgroundColor);
        inputLayout.setBoxCornerRadii(dp(16), dp(16), dp(16), dp(16));
        inputLayout.setBoxStrokeColor(outlineColor);
        inputLayout.setBoxStrokeWidth(dp(1));
        inputLayout.setBoxStrokeWidthFocused(dp(1));

        TextInputEditText editText = new TextInputEditText(requireContext());
        editText.setSingleLine(minLines == 1);
        editText.setMinLines(minLines);
        editText.setMaxLines(minLines == 1 ? 1 : 5);
        editText.setTextSize(14);

        if (minLines == 1) {
            editText.setMinHeight(dp(58));
            editText.setPadding(dp(12), dp(10), dp(12), 0);
            editText.setGravity(Gravity.CENTER_VERTICAL);
        } else {
            editText.setMinHeight(dp(92));
            editText.setGravity(Gravity.TOP | Gravity.START);
            editText.setPadding(dp(12), dp(14), dp(12), dp(10));
        }

        inputLayout.addView(editText);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(12));
        inputLayout.setLayoutParams(params);

        root.addView(inputLayout);

        return editText;
    }

    private TextView makeSectionLabel(String text, int textColor) {
        TextView label = new TextView(requireContext());
        label.setText(text);
        label.setTextSize(13);
        label.setTypeface(null, Typeface.BOLD);
        label.setTextColor(textColor);
        label.setPadding(0, 0, 0, dp(8));
        return label;
    }

    private String shortenFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "Selected research file";
        }

        String cleaned = fileName.trim();

        if (cleaned.length() <= 42) {
            return cleaned;
        }

        String extension = "";
        int dotIndex = cleaned.lastIndexOf(".");

        if (dotIndex >= 0 && dotIndex < cleaned.length() - 1) {
            extension = cleaned.substring(dotIndex);
        }

        return cleaned.substring(0, Math.min(32, cleaned.length())) + "..." + extension;
    }

    private String getText(TextInputEditText editText) {
        if (editText.getText() == null) {
            return "";
        }

        return editText.getText().toString().trim();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        return Color.argb(alpha, red, green, blue);
    }

    private void showPaperOptionsDialog(ResearchPaperEntity paper) {
        String[] options = {
                "Open file",
                "View details",
                "Mark as unread",
                "Mark as reading",
                "Mark as read",
                paper.isImportant() ? "Remove important" : "Mark important",
                "Delete"
        };

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(paper.getTitle().isEmpty() ? "Research Paper" : paper.getTitle())
                .setItems(options, (dialog, which) -> {
                    ResearchPaperEntity updatedPaper;

                    switch (which) {
                        case 0:
                            openPaperFile(paper);
                            break;

                        case 1:
                            showPaperDetailsDialog(paper);
                            break;

                        case 2:
                            updatedPaper = copyPaper(paper);
                            if (updatedPaper != null) {
                                updatedPaper.setStatus("UNREAD");
                                viewModel.updatePaper(updatedPaper);
                                refreshCurrentFilter();
                            }
                            break;

                        case 3:
                            updatedPaper = copyPaper(paper);
                            if (updatedPaper != null) {
                                updatedPaper.setStatus("READING");
                                viewModel.updatePaper(updatedPaper);
                                refreshCurrentFilter();
                            }
                            break;

                        case 4:
                            updatedPaper = copyPaper(paper);
                            if (updatedPaper != null) {
                                updatedPaper.setStatus("READ");
                                viewModel.updatePaper(updatedPaper);
                                refreshCurrentFilter();
                            }
                            break;

                        case 5:
                            updatedPaper = copyPaper(paper);
                            if (updatedPaper != null) {
                                updatedPaper.setImportant(!paper.isImportant());
                                viewModel.updatePaper(updatedPaper);
                                refreshCurrentFilter();
                            }
                            break;

                        case 6:
                            confirmDeletePaper(paper);
                            break;
                    }
                })
                .show();
    }

    private ResearchPaperEntity copyPaper(ResearchPaperEntity original) {
        if (original == null) return null;

        return new ResearchPaperEntity(
                original.getPaperId(),
                original.getUserId(),
                original.getTitle(),
                original.getAuthors(),
                original.getYear(),
                original.getCategory(),
                original.getSummary(),
                original.getKeyFindings(),
                original.getMethodology(),
                original.getRelevance(),
                original.getFileName(),
                original.getFileUrl(),
                original.getStoragePath(),
                original.getStatus(),
                original.isImportant(),
                original.getUploadedAt(),
                original.getUpdatedAt()
        );
    }

    private void showPaperDetailsDialog(ResearchPaperEntity paper) {
        String details =
                "Title:\n" + paper.getTitle() + "\n\n" +
                        "Authors:\n" + paper.getAuthors() + "\n\n" +
                        "Year:\n" + paper.getYear() + "\n\n" +
                        "Category:\n" + paper.getCategory() + "\n\n" +
                        "Status:\n" + paper.getStatus() + "\n\n" +
                        "Important:\n" + (paper.isImportant() ? "Yes" : "No") + "\n\n" +
                        "Summary:\n" + paper.getSummary() + "\n\n" +
                        "Key Findings:\n" + paper.getKeyFindings() + "\n\n" +
                        "Methodology:\n" + paper.getMethodology() + "\n\n" +
                        "Relevance:\n" + paper.getRelevance();

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Paper Details")
                .setMessage(details)
                .setPositiveButton("Open File", (dialog, which) -> openPaperFile(paper))
                .setNegativeButton("Close", null)
                .show();
    }

    private void confirmDeletePaper(ResearchPaperEntity paper) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete paper?")
                .setMessage("This will delete the research paper and its saved details.")
                .setPositiveButton("Delete", (dialog, which) -> viewModel.deletePaper(paper))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openPaperFile(ResearchPaperEntity paper) {
        if (paper == null) {
            Toast.makeText(requireContext(), "Paper not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileUrl = paper.getFileUrl();
        String fileName = paper.getFileName();

        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            Toast.makeText(requireContext(), "File is not uploaded yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (fileName == null || fileName.trim().isEmpty()) {
            fileName = paper.getTitle() == null || paper.getTitle().trim().isEmpty()
                    ? "Research paper"
                    : paper.getTitle();
        }

        openResearchFileInsideApp(
                paper.getTitle() == null || paper.getTitle().trim().isEmpty()
                        ? fileName
                        : paper.getTitle(),
                fileName,
                fileUrl
        );
    }

    private void openResearchFileInsideApp(String title, String fileName, String fileUrl) {
        String safeFileUrl = fileUrl == null ? "" : fileUrl.trim();
        String safeFileName = fileName == null ? "" : fileName.trim().toLowerCase(Locale.ROOT);
        String safeTitle = title == null || title.trim().isEmpty() ? "Research Paper" : title.trim();

        if (safeFileUrl.isEmpty()) {
            Toast.makeText(requireContext(), "File is not uploaded yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (safeFileName.endsWith(".pdf")
                || safeFileName.endsWith(".doc")
                || safeFileName.endsWith(".docx")
                || safeFileName.endsWith(".ppt")
                || safeFileName.endsWith(".pptx")) {
            String mimeType;
            if (safeFileName.endsWith(".pdf")) {
                mimeType = "application/pdf";
            } else if (safeFileName.endsWith(".doc")) {
                mimeType = "application/msword";
            } else if (safeFileName.endsWith(".docx")) {
                mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            } else if (safeFileName.endsWith(".ppt")) {
                mimeType = "application/vnd.ms-powerpoint";
            } else {
                mimeType = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            }

            File localFile = new File(safeFileUrl);
            if (!localFile.exists()) {
                Toast.makeText(requireContext(), "File no longer exists on this device", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri contentUri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    localFile);
            intent.setDataAndType(contentUri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            try {
                startActivity(Intent.createChooser(intent, "Open file"));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(requireContext(), "No app found to open this file type.", Toast.LENGTH_LONG).show();
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
        backButton.setClickable(true);
        backButton.setFocusable(true);

        TextView titleText = new TextView(requireContext());
        titleText.setText(safeTitle);
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

        if (safeFileName.endsWith(".ppt")
                || safeFileName.endsWith(".pptx")
                || safeFileName.endsWith(".doc")
                || safeFileName.endsWith(".docx")) {
            webView.setInitialScale(120);
        } else {
            webView.setInitialScale(100);
        }

        webView.setWebViewClient(new WebViewClient());

        String viewerUrl;

        if (safeFileName.endsWith(".jpg")
                || safeFileName.endsWith(".jpeg")
                || safeFileName.endsWith(".png")
                || safeFileName.endsWith(".webp")
                || safeFileName.endsWith(".gif")) {
            viewerUrl = safeFileUrl;
        } else {
            viewerUrl = "https://docs.google.com/gview?embedded=true&url=" + Uri.encode(safeFileUrl);
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

        dialog.setOnDismissListener(d -> {
            try {
                webView.stopLoading();
                webView.loadUrl("about:blank");
                webView.clearHistory();
                webView.destroy();
            } catch (Exception ignored) {
                // Keep app safe if WebView cleanup fails.
            }
        });

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
            if (safeFileName.endsWith(".ppt")
                    || safeFileName.endsWith(".pptx")
                    || safeFileName.endsWith(".doc")
                    || safeFileName.endsWith(".docx")) {
                webView.zoomIn();
            }
        }, 1200);
    }

    private String getFileName(Uri uri) {
        String result = null;

        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = requireContext().getContentResolver()
                    .query(uri, null, null, null, null)) {

                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);

                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }

        if (result == null) {
            result = uri.getLastPathSegment();
        }

        if (result == null || result.trim().isEmpty()) {
            result = "research_paper.pdf";
        }

        return result;
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm =
                (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (currentLiveData != null) {
            currentLiveData.removeObservers(getViewLifecycleOwner());
            currentLiveData = null;
        }

        binding = null;
    }
}