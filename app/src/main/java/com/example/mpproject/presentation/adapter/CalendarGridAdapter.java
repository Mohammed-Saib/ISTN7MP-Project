package com.example.mpproject.presentation.adapter;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mpproject.R;
import com.example.mpproject.presentation.view.calendar.CalendarDayItem;

import java.util.List;

// [Adapter] Renders the 42-cell (6-row × 7-column) month grid.
// Each cell is a CalendarDayItem; the GridLayoutManager handles the 7-column split.
public class CalendarGridAdapter extends ListAdapter<CalendarDayItem, CalendarGridAdapter.DayViewHolder> {

    public interface OnDayClickListener {
        void onDayClick(int dayOfMonth);
    }

    private static final DiffUtil.ItemCallback<CalendarDayItem> DIFF =
            new DiffUtil.ItemCallback<CalendarDayItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull CalendarDayItem oldItem, @NonNull CalendarDayItem newItem) {
                    // Padding cells are interchangeable blank slots
                    if (oldItem.isPadding() && newItem.isPadding()) return true;
                    if (oldItem.isPadding() || newItem.isPadding()) return false;
                    // Real days are the same slot when they represent the same day in the same month context
                    return oldItem.getDayOfMonth() == newItem.getDayOfMonth()
                            && oldItem.isCurrentMonth() == newItem.isCurrentMonth();
                }

                @Override
                public boolean areContentsTheSame(@NonNull CalendarDayItem oldItem, @NonNull CalendarDayItem newItem) {
                    if (oldItem.isPadding() && newItem.isPadding()) return true;
                    return oldItem.getDayOfMonth() == newItem.getDayOfMonth()
                            && oldItem.isCurrentMonth() == newItem.isCurrentMonth()
                            && oldItem.isToday() == newItem.isToday()
                            && oldItem.isSelected() == newItem.isSelected()
                            && oldItem.hasTodo() == newItem.hasTodo()
                            && oldItem.getEventDotColors().equals(newItem.getEventDotColors());
                }
            };

    private final OnDayClickListener listener;

    public CalendarGridAdapter(OnDayClickListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class DayViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvDay;
        private final LinearLayout llDots;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDay  = itemView.findViewById(R.id.tv_day_number);
            llDots = itemView.findViewById(R.id.ll_dots);
        }

        void bind(CalendarDayItem item) {
            Context ctx = itemView.getContext();

            if (item.isPadding()) {
                tvDay.setText("");
                tvDay.setBackground(null);
                llDots.removeAllViews();
                itemView.setOnClickListener(null);
                return;
            }

            tvDay.setText(String.valueOf(item.getDayOfMonth()));

            // Background: filled circle for selected, outline circle for today, none otherwise
            if (item.isSelected()) {
                tvDay.setBackground(ContextCompat.getDrawable(ctx, R.drawable.bg_day_selected));
                tvDay.setTextColor(ContextCompat.getColor(ctx, R.color.on_primary));
            } else if (item.isToday()) {
                tvDay.setBackground(ContextCompat.getDrawable(ctx, R.drawable.bg_day_today));
                tvDay.setTextColor(ContextCompat.getColor(ctx, R.color.primary_light));
            } else {
                tvDay.setBackground(null);
                // Full opacity for current month days; 33% opacity for out-of-month padding days
                int baseColor = getThemeColor(ctx, com.google.android.material.R.attr.colorOnSurface);
                tvDay.setTextColor(item.isCurrentMonth() ? baseColor : (baseColor & 0x00FFFFFF) | 0x55000000);
            }

            // Dot row — rebuild from scratch each bind
            llDots.removeAllViews();
            for (int colorRes : item.getEventDotColors()) {
                View dot = new View(ctx);
                int size = dpToPx(ctx, 6);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
                lp.setMargins(2, 0, 2, 0);
                dot.setLayoutParams(lp);
                GradientDrawable d = new GradientDrawable();
                d.setShape(GradientDrawable.OVAL);
                d.setColor(ContextCompat.getColor(ctx, colorRes));
                dot.setBackground(d);
                llDots.addView(dot);
            }

            // Todo dot — neutral grey added after event dots if space allows
            if (item.hasTodo() && item.getEventDotColors().size() < 3) {
                View dot = new View(ctx);
                int size = dpToPx(ctx, 6);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
                lp.setMargins(2, 0, 2, 0);
                dot.setLayoutParams(lp);
                GradientDrawable d = new GradientDrawable();
                d.setShape(GradientDrawable.OVAL);
                d.setColor(ContextCompat.getColor(ctx, R.color.event_todo));
                dot.setBackground(d);
                llDots.addView(dot);
            }

            itemView.setOnClickListener(v -> listener.onDayClick(item.getDayOfMonth()));
        }

        // Resolve a theme attribute to a color int
        private int getThemeColor(Context ctx, int attrRes) {
            int[] attrs = {attrRes};
            android.content.res.TypedArray ta = ctx.obtainStyledAttributes(attrs);
            int color = ta.getColor(0, 0xFF808080);
            ta.recycle();
            return color;
        }

        private int dpToPx(Context ctx, int dp) {
            return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
        }
    }
}
