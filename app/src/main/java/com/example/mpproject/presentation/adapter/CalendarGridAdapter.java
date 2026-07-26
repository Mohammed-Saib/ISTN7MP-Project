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

public class CalendarGridAdapter extends ListAdapter<CalendarDayItem, CalendarGridAdapter.DayViewHolder> {

    public interface OnDayClickListener {
        void onDayClick(int dayOfMonth);
    }

    private static final DiffUtil.ItemCallback<CalendarDayItem> DIFF =
            new DiffUtil.ItemCallback<CalendarDayItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull CalendarDayItem oldItem, @NonNull CalendarDayItem newItem) {
                    if (oldItem.isPadding() && newItem.isPadding()) return true;
                    if (oldItem.isPadding() || newItem.isPadding()) return false;
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
        private int cachedThemeColor = 0;
        // Pre-created dot views to avoid allocation per bind
        private static final int MAX_DOTS = 3;
        private final View[] dotViews = new View[MAX_DOTS];
        private final GradientDrawable[] dotDrawables = new GradientDrawable[MAX_DOTS];

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDay  = itemView.findViewById(R.id.tv_day_number);
            llDots = itemView.findViewById(R.id.ll_dots);
            Context ctx = itemView.getContext();
            for (int i = 0; i < MAX_DOTS; i++) {
                View dot = new View(ctx);
                int size = dpToPx(ctx, 6);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
                lp.setMargins(2, 0, 2, 0);
                dot.setLayoutParams(lp);
                GradientDrawable d = new GradientDrawable();
                d.setShape(GradientDrawable.OVAL);
                dot.setBackground(d);
                dotViews[i] = dot;
                dotDrawables[i] = d;
            }
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

            if (item.isSelected()) {
                tvDay.setBackground(ContextCompat.getDrawable(ctx, R.drawable.bg_day_selected));
                tvDay.setTextColor(ContextCompat.getColor(ctx, R.color.on_primary));
            } else if (item.isToday()) {
                tvDay.setBackground(ContextCompat.getDrawable(ctx, R.drawable.bg_day_today));
                tvDay.setTextColor(ContextCompat.getColor(ctx, R.color.primary_light));
            } else {
                tvDay.setBackground(null);
                if (cachedThemeColor == 0) {
                    int[] attrs = {com.google.android.material.R.attr.colorOnSurface};
                    android.content.res.TypedArray ta = ctx.obtainStyledAttributes(attrs);
                    cachedThemeColor = ta.getColor(0, 0xFF808080);
                    ta.recycle();
                }
                int baseColor = cachedThemeColor;
                tvDay.setTextColor(item.isCurrentMonth() ? baseColor : (baseColor & 0x00FFFFFF) | 0x55000000);
            }

            llDots.removeAllViews();
            List<Integer> colors = item.getEventDotColors();
            int dotCount = Math.min(colors.size(), MAX_DOTS);
            if (item.hasTodo() && dotCount < MAX_DOTS) {
                dotCount++;
            }

            for (int i = 0; i < dotCount && i < MAX_DOTS; i++) {
                int colorRes;
                if (i < colors.size()) {
                    colorRes = colors.get(i);
                } else {
                    colorRes = R.color.event_todo;
                }
                dotDrawables[i].setColor(ContextCompat.getColor(ctx, colorRes));
                if (dotViews[i].getParent() == null) {
                    llDots.addView(dotViews[i]);
                }
            }

            for (int i = dotCount; i < MAX_DOTS; i++) {
                if (dotViews[i].getParent() != null) {
                    llDots.removeView(dotViews[i]);
                }
            }

            itemView.setOnClickListener(v -> listener.onDayClick(item.getDayOfMonth()));
        }

        private int dpToPx(Context ctx, int dp) {
            return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
        }
    }
}
