package kr.windy.mariwoo.basic.adapter;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import kr.windy.mariwoo.R;
import kr.windy.mariwoo.basic.model.MedicineScheduleItem;

import java.util.List;

/**
 * 복용 체크 화면 (MedicineIntakeFragment) 내부 RecyclerView 어댑터
 * MedicineCardAdapter의 내부 RecyclerView에서 사용됨
 *
 * 각 항목은 하나의 복용 스케줄을 나타냄:
 *   - 시간대 아이콘 (아침/점심/저녁/취침 전)
 *   - 시간대 텍스트
 *   - 식전/식후 + 복용 시간 (예: "식전 07:00")
 *   - 복용 상태 표시 ("먹음" 초록 / "안먹음" 빨강)
 *
 * 복용 체크 흐름:
 *   1. "안먹음" 상태의 항목 클릭 → AlertDialog "약을 드셨나요?" 확인
 *   2. "네" 클릭 → onCheckClick 콜백 → MedicineIntakeFragment.checkIntake() 호출
 *   3. 서버 저장 성공 후 item.setTaken(true) 설정 → UI 즉시 갱신
 *
 * 이미 먹음 상태인 항목은 클릭 무시 (중복 체크 방지)
 */
public class MedicineScheduleAdapter extends RecyclerView.Adapter<MedicineScheduleAdapter.ViewHolder> {

    private List<MedicineScheduleItem> schedules; // 시간대별 스케줄 목록
    private OnCheckClickListener listener;         // 복용 체크 클릭 콜백

    /**
     * 복용 체크 클릭 콜백 인터페이스
     * MedicineIntakeFragment에서 서버에 복용 완료 기록 요청 시 사용
     */
    public interface OnCheckClickListener {
        void onCheckClick(MedicineScheduleItem item, int position);
    }

    public MedicineScheduleAdapter(List<MedicineScheduleItem> schedules,
                                   OnCheckClickListener listener) {
        this.schedules = schedules;
        this.listener  = listener;
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

        // 시간대 아이콘 설정 (아침/점심/저녁/취침 전)
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

        // 시간대 텍스트 (아침/점심/저녁/취침 전)
        holder.txtTimeType.setText(item.getIntakeTimeType());

        // 복용 시간 표시: "HH:mm:ss" → "HH:mm" 으로 잘라서 표시
        String time = item.getIntakeTime();
        if (time != null && time.length() >= 5) {
            time = time.substring(0, 5); // "07:00:00" → "07:00"
        }
        holder.txtTime.setText(item.getIntakeType() + " " + time); // 예: "식전 07:00"

        // 복용 상태에 따라 배경색 + 텍스트 설정
        if (item.isTaken()) {
            holder.layoutCheck.setBackgroundResource(R.drawable.layout_medicine_check_radius);
            holder.txtCheck.setText("먹음");
        } else {
            holder.layoutCheck.setBackgroundResource(R.drawable.layout_medicine_check_n_radius);
            holder.txtCheck.setText("안먹음");
        }

        // 미복용 상태일 때만 클릭 이벤트 처리
        // (이미 복용 완료 항목은 클릭 무시 → 중복 체크 방지)
        holder.layoutCheck.setOnClickListener(v -> {
            if (!item.isTaken()) {
                new AlertDialog.Builder(v.getContext())
                        .setTitle("복용 확인")
                        .setMessage("약을 드셨나요?")
                        .setPositiveButton("네", (dialog, which) -> {
                            listener.onCheckClick(item, position); // 서버에 복용 완료 기록 요청
                        })
                        .setNegativeButton("아니요", null)
                        .show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return schedules.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView    imgTimeType;  // 시간대 아이콘
        TextView     txtTimeType;  // 시간대 텍스트 (아침/점심/저녁/취침 전)
        TextView     txtTime;      // 식전/식후 + 복용 시간
        TextView     txtCheck;     // "먹음" / "안먹음"
        LinearLayout layoutCheck;  // 클릭 영역 (복용 체크 버튼 역할)

        public ViewHolder(View itemView) {
            super(itemView);
            imgTimeType = itemView.findViewById(R.id.item_schedule_image_time_type);
            txtTimeType = itemView.findViewById(R.id.item_schedule_text_time_type);
            txtTime     = itemView.findViewById(R.id.item_schedule_text_time);
            txtCheck    = itemView.findViewById(R.id.item_schedule_text_check);
            layoutCheck = itemView.findViewById(R.id.item_schedule_layout_check);
        }
    }
}
