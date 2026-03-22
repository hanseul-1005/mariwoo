package com.windy.mariwoo.basic.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.windy.mariwoo.R;
import com.windy.mariwoo.basic.model.MedicineModel;

import java.util.ArrayList;
import java.util.List;

public class MedicineInnerAdapter extends RecyclerView.Adapter<MedicineInnerAdapter.InnerViewHolder> {
    private List<MedicineModel> listMedicine;

    public MedicineInnerAdapter(List<MedicineModel> list) {
        this.listMedicine = list;
    }

    static class InnerViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvTime;
        ImageView imgIcon;
        LinearLayout layoutUpdate;

        InnerViewHolder(View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.itemMedicineInner_textView_type);
            tvTime = itemView.findViewById(R.id.itemMedicineInner_textView_time);
            imgIcon = itemView.findViewById(R.id.itemMedicineInner_imageView_icon);
            layoutUpdate = itemView.findViewById(R.id.itemMedicineInner_layout_morning_update);
        }
    }

    @Override
    public InnerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medicine_inner, parent, false);
        return new InnerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(InnerViewHolder holder, int position) {
        MedicineModel item = listMedicine.get(position);
        holder.tvType.setText(item.getType()); // 실제 시간대 명칭 전달 가능
        holder.tvTime.setText(item.getTime());


        Log.d("HS", "innerAdapter listMedicine size : "+listMedicine.size());
        if("아침".equals(item.getType())) {
            holder.imgIcon.setImageResource(R.drawable.morning);
        } else if("점심".equals(item.getType())) {
            holder.imgIcon.setImageResource(R.drawable.launch);
        } else if("저녁".equals(item.getType())) {
            holder.imgIcon.setImageResource(R.drawable.dinner);
        } else {
            holder.imgIcon.setImageResource(R.drawable.bed);
        }
    }

    @Override
    public int getItemCount() {
        return listMedicine.size();
    }
}