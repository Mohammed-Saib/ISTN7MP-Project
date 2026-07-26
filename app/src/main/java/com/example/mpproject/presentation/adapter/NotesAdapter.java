package com.example.mpproject.presentation.adapter;

import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mpproject.databinding.ItemNoteBinding;
import com.example.mpproject.presentation.model.NoteListItem;

import java.util.Objects;

public class NotesAdapter extends ListAdapter<NoteListItem, NotesAdapter.NoteViewHolder> {

    public interface OnNoteActionListener {
        void onOpen(NoteListItem item);
        void onRename(NoteListItem item);
        void onMoveToFolder(NoteListItem item);
        void onDelete(NoteListItem item);
    }

    private static final DiffUtil.ItemCallback<NoteListItem> DIFF =
            new DiffUtil.ItemCallback<NoteListItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull NoteListItem oldItem, @NonNull NoteListItem newItem) {
                    return oldItem.getType() == newItem.getType()
                            && Objects.equals(oldItem.getId(), newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull NoteListItem oldItem, @NonNull NoteListItem newItem) {
                    return Objects.equals(oldItem.getTitle(), newItem.getTitle())
                            && Objects.equals(oldItem.getSubtitle(), newItem.getSubtitle())
                            && Objects.equals(oldItem.getContentPreview(), newItem.getContentPreview());
                }
            };

    private final OnNoteActionListener listener;

    public NotesAdapter(OnNoteActionListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNoteBinding binding = ItemNoteBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new NoteViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class NoteViewHolder extends RecyclerView.ViewHolder {

        private final ItemNoteBinding binding;

        NoteViewHolder(ItemNoteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(NoteListItem item) {
            if (item.getType() == NoteListItem.TYPE_MODULE_FILE) {
                binding.txtNoteType.setText("MODULE FILE");
            } else {
                binding.txtNoteType.setText("PERSONAL NOTE");
            }

            binding.txtNoteTitle.setText(item.getTitle());
            binding.txtNoteSubtitle.setText(item.getSubtitle());

            String preview = item.getContentPreview();
            if (item.getType() == NoteListItem.TYPE_PERSONAL_NOTE
                    && preview != null && preview.contains("<")) {
                binding.txtNotePreview.setText(
                        Html.fromHtml(preview, Html.FROM_HTML_MODE_LEGACY)
                );
            } else {
                binding.txtNotePreview.setText(preview);
            }

            binding.getRoot().setOnClickListener(v -> listener.onOpen(item));

            binding.btnNoteMore.setOnClickListener(v -> {
                PopupMenu menu = new PopupMenu(v.getContext(), binding.btnNoteMore);

                menu.getMenu().add("Open");
                menu.getMenu().add("Rename");
                menu.getMenu().add("Move to folder");
                menu.getMenu().add("Delete");

                menu.setOnMenuItemClickListener(menuItem -> {
                    String action = menuItem.getTitle().toString();

                    if (action.equals("Open")) {
                        listener.onOpen(item);
                    } else if (action.equals("Rename")) {
                        listener.onRename(item);
                    } else if (action.equals("Move to folder")) {
                        listener.onMoveToFolder(item);
                    } else if (action.equals("Delete")) {
                        listener.onDelete(item);
                    }

                    return true;
                });

                menu.show();
            });
        }
    }
}
