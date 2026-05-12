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

/**
 * 복용 체크 화면 (MedicineIntakeFragment) 외부 RecyclerView 어댑터
 *
 * 구조:
 *   - 외부: 약 이름 카드 (MedicineCardItem)
 *   - 내부: 해당 약의 시간대별 스케줄 목록 (MedicineScheduleAdapter)
 *
 * 사용 예:
 *   cardAdapter = new MedicineCardAdapter(cardList, (item, position) -> checkIntake(item, position));
 *
 * @see MedicineScheduleAdapter     내부 스케줄 어댑터
 * @see com.windy.mariwoo.basic.model.MedicineCardItem 약 카드 데이터 모델
 */
public class MedicineCardAdapter extends RecyclerView.Adapter<MedicineCardAdapter.ViewHolder> {

    private List<MedicineCardItem> cards;    // 약 카드 목록
    private MedicineScheduleAdapter.OnCheckClickListener listener; // 복용 체크 클릭 콜백

    public MedicineCardAdapter(List<MedicineCardItem> cards,
                               MedicineScheduleAdapter.OnCheckClickListener listener) {
        this.cards    = cards;
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

        // 약 이름 표시
        holder.txtName.setText(card.getMedicineName());

        // 내부 RecyclerView: 시간대별 스케줄 목록 (아침/점심/저녁 등)
        MedicineScheduleAdapter scheduleAdapter =
                new MedicineScheduleAdapter(card.getSchedules(), listener);
        holder.recyclerView.setLayoutManager(
                new LinearLayoutManager(holder.itemView.getContext()));
        holder.recyclerView.setAdapter(scheduleAdapter);
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView     txtName;     // 약 이름
        RecyclerView recyclerView; // 스케줄 목록 (내부 RecyclerView)

        public ViewHolder(View itemView) {
            super(itemView);
            txtName      = itemView.findViewById(R.id.item_card_text_name);
            recyclerView = itemView.findViewById(R.id.item_card_recyclerview_schedule);
        }
    }
}
