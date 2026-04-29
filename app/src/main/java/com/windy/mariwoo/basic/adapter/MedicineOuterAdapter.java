package com.windy.mariwoo.basic.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.windy.mariwoo.R;
import com.windy.mariwoo.basic.model.MedicineModel;

import java.util.ArrayList;
import java.util.List;

public class MedicineOuterAdapter extends RecyclerView.Adapter<MedicineOuterAdapter.ViewHolder> {

    private Context context;
    private List<MedicineModel> list;
    private OnModifyClickListener listener;

    public interface OnModifyClickListener {
        void onModifyClick(MedicineModel medicine);
    }

    public MedicineOuterAdapter(Context context, List<MedicineModel> list, OnModifyClickListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_medicine_outer, parent, false);
        return new ViewHolder(view);
    }
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        MedicineModel medicine = list.get(position);
        holder.txtName.setText(medicine.getName());

        MedicineInnerAdapter innerAdapter = new MedicineInnerAdapter(
                context,
                medicine.getListMedicine(),
                listener != null ? item -> listener.onModifyClick(medicine) : null // ✅ null 체크
        );
        holder.innerRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        holder.innerRecyclerView.setAdapter(innerAdapter);
    }
    @Override
    public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtName;
        RecyclerView innerRecyclerView;

        public ViewHolder(View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.itemMedicineOuter_textView_title);
            innerRecyclerView = itemView.findViewById(R.id.itemMedicineOuter_recyclerview);
        }
    }
}