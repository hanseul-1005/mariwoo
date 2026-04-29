package com.windy.mariwoo.basic.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.windy.mariwoo.R;
import com.windy.mariwoo.basic.model.MedicineCardItem;

import java.util.List;

public class MedicineCardAdapter extends RecyclerView.Adapter<MedicineCardAdapter.ViewHolder> {

    private List<MedicineCardItem> cards;
    private MedicineScheduleAdapter.OnCheckClickListener listener;

    public MedicineCardAdapter(List<MedicineCardItem> cards, MedicineScheduleAdapter.OnCheckClickListener listener) {
        this.cards = cards;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_medicine_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        MedicineCardItem card = cards.get(position);

        // 약 이름
        holder.txtName.setText(card.getMedicineName());

        // 시간대 RecyclerView
        MedicineScheduleAdapter scheduleAdapter =
                new MedicineScheduleAdapter(card.getSchedules(), listener);
        holder.recyclerView.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
        holder.recyclerView.setAdapter(scheduleAdapter);
    }

    @Override
    public int getItemCount() { return cards.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtName;
        RecyclerView recyclerView;

        public ViewHolder(View itemView) {
            super(itemView);
            txtName     = itemView.findViewById(R.id.item_card_text_name);
            recyclerView = itemView.findViewById(R.id.item_card_recyclerview_schedule);
        }
    }
}