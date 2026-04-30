// CalendarDayAdapter.java
package com.windy.mariwoo.basic.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.windy.mariwoo.R;

import java.util.List;
import java.util.Map;

public class CalendarDayAdapter extends RecyclerView.Adapter<CalendarDayAdapter.ViewHolder> {

    private List<Integer> dayList;
    private Map<String, String> statusMap; // "2025-02-13" → "all" or "partial"
    private int year, month;

    public CalendarDayAdapter(List<Integer> dayList, Map<String, String> statusMap) {
        this.dayList   = dayList;
        this.statusMap = statusMap;
    }

    public void updateData(List<Integer> dayList, Map<String, String> statusMap, int year, int month) {
        this.dayList   = dayList;
        this.statusMap = statusMap;
        this.year      = year;
        this.month     = month;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        int day = dayList.get(position);

        if (day == 0) {
            // 빈칸
            holder.txtDay.setText("");
            holder.itemView.setBackgroundColor(0x00000000); // 투명
            return;
        }

        holder.txtDay.setText(String.valueOf(day));

        // 날짜 키 생성 "2025-02-03"
        String dateKey = String.format("%04d-%02d-%02d", year, month, day);
        String status  = statusMap.get(dateKey);

        if ("all".equals(status)) {
            // 전부 복용 → 초록색
            holder.itemView.setBackgroundResource(R.drawable.bg_calendar_green);
        } else if ("partial".equals(status)) {
            // 일부 미복용 → 빨간색
            holder.itemView.setBackgroundResource(R.drawable.bg_calendar_red);
        } else {
            // 등록된 약 없음 → 색 없음
            holder.itemView.setBackgroundColor(0x00000000);
        }

        // 일요일 빨간 글씨, 토요일 파란 글씨
        int col = position % 7;
        if (col == 0) {
            holder.txtDay.setTextColor(0xFFE53935); // 빨강
        } else if (col == 6) {
            holder.txtDay.setTextColor(0xFF1565C0); // 파랑
        } else {
            holder.txtDay.setTextColor(0xFF222222); // 기본
        }
    }

    @Override
    public int getItemCount() { return dayList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtDay;

        public ViewHolder(View itemView) {
            super(itemView);
            txtDay = itemView.findViewById(R.id.itemCalendarDay_text_day);
        }
    }
}