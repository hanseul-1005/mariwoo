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

public class MedicineOuterAdapter extends RecyclerView.Adapter<MedicineOuterAdapter.OuterViewHolder> {

    private Context context;
    private List<MedicineModel> listMedicineName;

    public MedicineOuterAdapter(Context context, List<MedicineModel> listMedicineName) {
        this.context = context;
        this.listMedicineName = listMedicineName;
    }
    static class OuterViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        RecyclerView innerRecyclerView;

        //ViewHolder
        OuterViewHolder(View itemView) {
            super(itemView);
            tvTitle = (TextView) itemView.findViewById(R.id.itemMedicineOuter_textView_title);
            innerRecyclerView = itemView.findViewById(R.id.itemMedicineOuter_recyclerview);
        }
    }

    @NonNull
    @Override
    public OuterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_medicine_outer, parent, false);
        return new OuterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OuterViewHolder holder, int position) {
        MedicineModel medicine = listMedicineName.get(position);
        holder.tvTitle.setText(medicine.getName());

        Log.d("HS", "outerAdapter medicine.getListMedicine() size : "+medicine.getListMedicine().size());
        MedicineInnerAdapter innerAdapter = new MedicineInnerAdapter(medicine.getListMedicine());
        LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        holder.innerRecyclerView.setLayoutManager(layoutManager);
        holder.innerRecyclerView.setAdapter(innerAdapter);
        holder.innerRecyclerView.setNestedScrollingEnabled(false); // 스크롤 충돌 방지

    }

    @Override
    public int getItemCount() {
        return listMedicineName.size();
    }
}