package kr.windy.mariwoo.basic.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;

import kr.windy.mariwoo.R;
import kr.windy.mariwoo.basic.model.HospitalScheduleModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HospitalListAdapter extends RecyclerView.Adapter<HospitalListAdapter.ViewHolder> {

    private static final String[] DAY_NAMES = {"일", "월", "화", "수", "목", "금", "토"};

    public interface OnItemClickListener {
        void onModify(HospitalScheduleModel item);
        void onDelete(HospitalScheduleModel item);
    }

    private final Context                     context;
    private final List<HospitalScheduleModel> list;
    private final OnItemClickListener         listener;

    public HospitalListAdapter(Context context, List<HospitalScheduleModel> list,
                               OnItemClickListener listener) {
        this.context  = context;
        this.list     = list;
        this.listener = listener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView        tvName, tvDate, tvTime, tvMemo;
        LinearLayout    layoutMemo;
        AppCompatButton btnModify, btnDelete;

        ViewHolder(View v) {
            super(v);
            tvName     = v.findViewById(R.id.itemHospitalList_textView_name);
            tvDate     = v.findViewById(R.id.itemHospitalList_textView_date);
            tvTime     = v.findViewById(R.id.itemHospitalList_textView_time);
            tvMemo     = v.findViewById(R.id.itemHospitalList_textView_memo);
            layoutMemo = v.findViewById(R.id.itemHospitalList_layout_memo);
            btnModify  = v.findViewById(R.id.itemHospitalList_button_modify);
            btnDelete  = v.findViewById(R.id.itemHospitalList_button_delete);
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

        // 비고 내용 없으면 힌트 텍스트 표시
        String memo = item.getMemo();
        holder.layoutMemo.setVisibility(View.VISIBLE);
        if (memo != null && !memo.trim().isEmpty()) {
            holder.tvMemo.setText(memo);
            holder.tvMemo.setTextColor(0xFF666666);
        } else {
            holder.tvMemo.setText("입력된 메모가 없습니다.");
            holder.tvMemo.setTextColor(0xFFCCCCCC);
        }

        // "yyyy-MM-dd HH:mm:ss" → 날짜/시간 분리
        String fullTime = item.getTime();
        if (fullTime != null && fullTime.contains(" ")) {
            String[] parts = fullTime.split(" ");
            holder.tvDate.setText(formatDate(parts[0]));  // "MM.dd"
            String hhmm = parts[1].length() >= 5 ? parts[1].substring(0, 5) : parts[1];
            holder.tvTime.setText(hhmm);
        } else {
            holder.tvDate.setText(fullTime);
            holder.tvTime.setText("");
        }

        holder.btnModify.setOnClickListener(v -> {
            if (listener != null) listener.onModify(item);
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(item);
        });
    }

    /**
     * "yyyy-MM-dd" → "MM.dd (요일)"
     */
    private String formatDate(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(dateStr));
            int month     = cal.get(Calendar.MONTH) + 1;
            int day       = cal.get(Calendar.DAY_OF_MONTH);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            int idx = Math.max(0, Math.min(dayOfWeek - 1, DAY_NAMES.length - 1));
            return String.format(Locale.getDefault(), "%02d.%02d (%s)", month, day, DAY_NAMES[idx]);
        } catch (Exception e) {
            return dateStr;
        }
    }

    @Override
    public int getItemCount() { return list == null ? 0 : list.size(); }
}

