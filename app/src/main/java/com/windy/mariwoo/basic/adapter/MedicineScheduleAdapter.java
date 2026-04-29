package com.windy.mariwoo.basic.adapter;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.windy.mariwoo.R;
import com.windy.mariwoo.basic.model.MedicineScheduleItem;

import java.util.List;

public class MedicineScheduleAdapter extends RecyclerView.Adapter<MedicineScheduleAdapter.ViewHolder> {

    private List<MedicineScheduleItem> schedules;
    private OnCheckClickListener listener;

    public interface OnCheckClickListener {
        void onCheckClick(MedicineScheduleItem item, int position);
    }

    public MedicineScheduleAdapter(List<MedicineScheduleItem> schedules, OnCheckClickListener listener) {
        this.schedules = schedules;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_medicine_schedule, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        MedicineScheduleItem item = schedules.get(position);

        // 아침/점심/저녁/취침 전 아이콘
        switch (item.getIntakeTimeType()) {
            case "아침":
                holder.imgTimeType.setImageResource(R.drawable.morning);
                break;
            case "점심":
                holder.imgTimeType.setImageResource(R.drawable.launch);
                break;
            case "저녁":
                holder.imgTimeType.setImageResource(R.drawable.dinner);
                break;
            case "취침 전":
                holder.imgTimeType.setImageResource(R.drawable.bed);
                break;
        }

        // 시간대 텍스트
        holder.txtTimeType.setText(item.getIntakeTimeType());

        // 식전/식후 + 시간 (예: "식전 07:00")
        String time = item.getIntakeTime();
        if (time != null && time.length() >= 5) {
            time = time.substring(0, 5); // "07:00:00" → "07:00"
        }
        holder.txtTime.setText(item.getIntakeType() + " " + time);

        // 먹음/안먹음 상태
        if (item.isTaken()) {
            holder.layoutCheck.setBackgroundResource(R.drawable.layout_medicine_check_radius);
            holder.txtCheck.setText("먹음");
        } else {
            holder.layoutCheck.setBackgroundResource(R.drawable.layout_medicine_check_n_radius);
            holder.txtCheck.setText("안먹음");
        }

        // 안먹음 클릭 시 AlertDialog
        holder.layoutCheck.setOnClickListener(v -> {
            if (!item.isTaken()) {
                new AlertDialog.Builder(v.getContext())
                        .setTitle("복용 확인")
                        .setMessage("약을 드셨나요?")
                        .setPositiveButton("네", (dialog, which) -> {
                            listener.onCheckClick(item, position);
                        })
                        .setNegativeButton("아니요", null)
                        .show();
            }
        });
    }

    @Override
    public int getItemCount() { return schedules.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgTimeType;
        TextView txtTimeType, txtTime, txtCheck;
        LinearLayout layoutCheck;

        public ViewHolder(View itemView) {
            super(itemView);
            imgTimeType  = itemView.findViewById(R.id.item_schedule_image_time_type);
            txtTimeType  = itemView.findViewById(R.id.item_schedule_text_time_type);
            txtTime      = itemView.findViewById(R.id.item_schedule_text_time);
            txtCheck     = itemView.findViewById(R.id.item_schedule_text_check);
            layoutCheck  = itemView.findViewById(R.id.item_schedule_layout_check);
        }
    }
}
