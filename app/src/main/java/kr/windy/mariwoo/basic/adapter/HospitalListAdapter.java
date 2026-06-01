package kr.windy.mariwoo.basic.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;

import kr.windy.mariwoo.R;
import kr.windy.mariwoo.basic.model.HospitalScheduleModel;

import java.util.List;

public class HospitalListAdapter extends RecyclerView.Adapter<HospitalListAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onModify(HospitalScheduleModel item);
        void onDelete(HospitalScheduleModel item);
    }

    private final Context             context;
    private final List<HospitalScheduleModel> list;
    private final OnItemClickListener listener;

    public HospitalListAdapter(Context context, List<HospitalScheduleModel> list,
                               OnItemClickListener listener) {
        this.context  = context;
        this.list     = list;
        this.listener = listener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView        tvName, tvTime, tvMemo;
        AppCompatButton btnModify, btnDelete;

        ViewHolder(View v) {
            super(v);
            tvName    = v.findViewById(R.id.itemHospitalList_textView_name);
            tvTime    = v.findViewById(R.id.itemHospitalList_textView_time);
            tvMemo    = v.findViewById(R.id.itemHospitalList_textView_memo);
            btnModify = v.findViewById(R.id.itemHospitalList_button_modify);
            btnDelete = v.findViewById(R.id.itemHospitalList_button_delete);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_hospital_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HospitalScheduleModel item = list.get(position);
        holder.tvName.setText(item.getName());
        holder.tvTime.setText(item.getTime());
        holder.tvMemo.setText(item.getMemo());

        holder.btnModify.setOnClickListener(v -> {
            if (listener != null) listener.onModify(item);
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(item);
        });
    }

    @Override
    public int getItemCount() { return list.size(); }
}
