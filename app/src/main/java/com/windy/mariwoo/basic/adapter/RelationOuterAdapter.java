package com.windy.mariwoo.basic.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.windy.mariwoo.R;
import com.windy.mariwoo.basic.model.MedicineModel;
import com.windy.mariwoo.basic.model.RelationModel;

import java.util.List;

public class RelationOuterAdapter extends RecyclerView.Adapter<RelationOuterAdapter.OuterViewHolder> {

    private Context context;
    private List<RelationModel> listRelation;

    // ✅ 인터페이스 추가
    public interface OnRelationClickListener {
        void onDelete(RelationModel relation, int position);
        void onAccept(RelationModel relation, int position);
        void onReject(RelationModel relation, int position);
    }

    private OnRelationClickListener listener;

    // ✅ 생성자에 listener 추가
    public RelationOuterAdapter(Context context, List<RelationModel> listRelation, OnRelationClickListener listener) {
        this.context = context;
        this.listRelation = listRelation;
        this.listener = listener;
    }

    public RelationOuterAdapter(Context context, List<RelationModel> listRelation) {
        this.context = context;
        this.listRelation = listRelation;
    }
    static class OuterViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvTel;
        LinearLayout layoutDelete;
        LinearLayout layoutAccept;
        AppCompatButton btnDelete;
        AppCompatButton btnAccept;
        AppCompatButton btnReject;

        //ViewHolder
        OuterViewHolder(View itemView) {
            super(itemView);
            tvName = (TextView) itemView.findViewById(R.id.itemRelationList_textView_name);
            tvTel = (TextView) itemView.findViewById(R.id.itemRelationList_textView_tel);
            layoutDelete = (LinearLayout) itemView.findViewById(R.id.itemRelationList_layout_delete);
            layoutAccept = (LinearLayout) itemView.findViewById(R.id.itemRelationList_layout_accept);
            btnDelete = (AppCompatButton) itemView.findViewById(R.id.itemRelationList_button_delete);
            btnAccept = (AppCompatButton) itemView.findViewById(R.id.itemRelationList_button_accept);
            btnReject = (AppCompatButton) itemView.findViewById(R.id.itemRelationList_button_reject);
        }
    }

    @NonNull
    @Override
    public OuterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_relation_list, parent, false);
        return new OuterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OuterViewHolder holder, int position) {
        RelationModel relation = listRelation.get(position);
        holder.tvName.setText(relation.getName());
        holder.tvTel.setText(relation.getTel());

        if("열람자".equals(relation.getType()) || "대상자".equals(relation.getType())) {
            holder.layoutDelete.setVisibility(View.VISIBLE);
            holder.layoutAccept.setVisibility(View.GONE);
        } else {
            holder.layoutDelete.setVisibility(View.GONE);
            holder.layoutAccept.setVisibility(View.VISIBLE);
        }


        // ✅ 버튼 클릭 리스너 추가
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(relation, position);
        });

        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) listener.onAccept(relation, position);
        });

        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) listener.onReject(relation, position);
        });

    }

    @Override
    public int getItemCount() {
        return listRelation.size();
    }
}