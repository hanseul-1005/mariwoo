package com.windy.mariwoo.basic.adapter;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.windy.mariwoo.R;
import com.windy.mariwoo.basic.model.MedicineModel;

import java.util.List;

public class MedicineInnerAdapter extends RecyclerView.Adapter<MedicineInnerAdapter.ViewHolder> {

    private Context context;
    private List<MedicineModel> list;
    private OnModifyClickListener listener;

    public interface OnModifyClickListener {
        void onModifyClick(MedicineModel item);
    }

    public MedicineInnerAdapter(Context context, List<MedicineModel> list, OnModifyClickListener listener) {
        this.context  = context;
        this.list     = list;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_medicine_inner, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        MedicineModel item = list.get(position);
        // 아이콘 + 시간대 설정
        switch (item.getType()) {
            case "0": case "아침":
                holder.imgIcon.setImageResource(R.drawable.morning);
                holder.txtType.setText("아침");
                break;
            case "1": case "점심":
                holder.imgIcon.setImageResource(R.drawable.launch);
                holder.txtType.setText("점심");
                break;
            case "2": case "저녁":
                holder.imgIcon.setImageResource(R.drawable.dinner);
                holder.txtType.setText("저녁");
                break;
            case "3": case "취침 전":
                holder.imgIcon.setImageResource(R.drawable.bed);
                holder.txtType.setText("취침 전");
                break;

        }

        holder.txtTime.setText(item.getTime());

        if (listener != null) {
            // ✅ 수정 화면 — 수정 버튼 표시, 복용 여부 숨김
            holder.layoutUpdate.setOnClickListener(v -> listener.onModifyClick(item));
            holder.layoutUpdate.setBackgroundResource(R.drawable.layout_medicine_check_radius); // ✅

            holder.tvBtn.setText("수정");

        } else {
            // ✅ 조회 화면 — 수정 버튼 숨김, 복용 여부 표시

            Log.i("HS", "intakeYn=" + item.getIntakeYn()); // ✅ 여기
            if ("1".equals(item.getIntakeYn())) {
                // 복용 완료 → 초록색
                holder.tvBtn.setText("먹음");
                holder.layoutUpdate.setBackgroundResource(R.drawable.layout_medicine_check_radius); // ✅
                Log.i("HS", "복용완료");
            } else {
                // 미복용
                holder.tvBtn.setText("안먹음");
                holder.layoutUpdate.setBackgroundResource(R.drawable.layout_medicine_check_n_radius); // ✅
                Log.i("HS", "미복용");
            }
        }
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView    imgIcon;
        TextView     txtType, txtTime, tvBtn;
        LinearLayout layoutUpdate;

        public ViewHolder(View itemView) {
            super(itemView);
            imgIcon      = itemView.findViewById(R.id.itemMedicineInner_imageView_icon);
            txtType      = itemView.findViewById(R.id.itemMedicineInner_textView_type);
            txtTime      = itemView.findViewById(R.id.itemMedicineInner_textView_time);
            layoutUpdate = itemView.findViewById(R.id.itemMedicineInner_layout_morning_update);
            tvBtn = itemView.findViewById(R.id.itemMedicineInner_textView_btn);
        }
    }
}