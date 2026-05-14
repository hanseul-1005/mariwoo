package kr.windy.mariwoo.basic.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import kr.windy.mariwoo.R;
import kr.windy.mariwoo.basic.model.MedicineModel;

import java.util.List;

/**
 * 약 목록 / 가족 복용 내역 내부 RecyclerView 어댑터 (시간대별 스케줄)
 *
 * MedicineOuterAdapter의 내부 RecyclerView에서 사용됨
 *
 * listener 유무에 따른 동작 분기:
 *   - listener != null : 수정 화면 → "수정" 버튼 표시, 배경색 active
 *   - listener == null : 조회 화면 → 복용 여부("먹음"/"안먹음") 표시, 배경색 분기
 *
 * 시간대 아이콘:
 *   - "0" 또는 "아침"   : morning 이미지
 *   - "1" 또는 "점심"   : launch 이미지
 *   - "2" 또는 "저녁"   : dinner 이미지
 *   - "3" 또는 "취침 전" : bed 이미지
 */
public class MedicineInnerAdapter extends RecyclerView.Adapter<MedicineInnerAdapter.ViewHolder> {

    private Context             context;
    private List<MedicineModel> list;
    private OnModifyClickListener listener; // null이면 조회 전용 모드

    /**
     * 수정 버튼 클릭 콜백 인터페이스
     */
    public interface OnModifyClickListener {
        void onModifyClick(MedicineModel item);
    }

    public MedicineInnerAdapter(Context context, List<MedicineModel> list,
                                OnModifyClickListener listener) {
        this.context  = context;
        this.list     = list;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_medicine_inner, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        MedicineModel item = list.get(position);

        // 시간대 아이콘 및 텍스트 설정
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
            // 수정 화면: 클릭 시 수정 콜백 호출, "수정" 텍스트 표시
            holder.layoutUpdate.setOnClickListener(v -> listener.onModifyClick(item));
            holder.layoutUpdate.setBackgroundResource(R.drawable.layout_medicine_check_radius);
            holder.tvBtn.setText("수정");

        } else {
            // 조회 화면: 복용 여부에 따라 배경색과 텍스트 분기
            Log.i("HS", "intakeYn=" + item.getIntakeYn());

            if ("1".equals(item.getIntakeYn())) {
                // 복용 완료 → 초록색 배경 + "먹음"
                holder.tvBtn.setText("먹음");
                holder.layoutUpdate.setBackgroundResource(R.drawable.layout_medicine_check_radius);
                Log.i("HS", "복용완료");
            } else {
                // 미복용 → 빨간색 배경 + "안먹음"
                holder.tvBtn.setText("안먹음");
                holder.layoutUpdate.setBackgroundResource(R.drawable.layout_medicine_check_n_radius);
                Log.i("HS", "미복용");
            }
        }
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView    imgIcon;      // 시간대 아이콘 (아침/점심/저녁/취침 전)
        TextView     txtType;      // 시간대 텍스트
        TextView     txtTime;      // 복용 시간 (HH:mm)
        TextView     tvBtn;        // "수정" 또는 "먹음"/"안먹음"
        LinearLayout layoutUpdate; // 클릭 영역 (수정 또는 복용 상태 표시)

        public ViewHolder(View itemView) {
            super(itemView);
            imgIcon      = itemView.findViewById(R.id.itemMedicineInner_imageView_icon);
            txtType      = itemView.findViewById(R.id.itemMedicineInner_textView_type);
            txtTime      = itemView.findViewById(R.id.itemMedicineInner_textView_time);
            layoutUpdate = itemView.findViewById(R.id.itemMedicineInner_layout_morning_update);
            tvBtn        = itemView.findViewById(R.id.itemMedicineInner_textView_btn);
        }
    }
}
