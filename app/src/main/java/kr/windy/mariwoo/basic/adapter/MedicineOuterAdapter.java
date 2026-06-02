package kr.windy.mariwoo.basic.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import kr.windy.mariwoo.R;
import kr.windy.mariwoo.basic.model.MedicineModel;

import java.util.List;

/**
 * 내 약 목록 / 가족 복용 내역 조회 화면에서 사용하는 외부 RecyclerView 어댑터
 *
 * 구조 (중첩 RecyclerView):
 *   - 외부 (이 어댑터): 약 이름 단위 카드 (MedicineModel.name)
 *   - 내부 (MedicineInnerAdapter): 해당 약의 시간대별 스케줄 (MedicineModel.listMedicine)
 *
 * listener 유무에 따른 동작 분기:
 *   - listener != null : 수정 화면 (내 약 목록) → "수정" 버튼 표시
 *   - listener == null : 조회 화면 (가족 복용 내역) → 복용 여부 표시
 *
 * 사용 화면:
 *   - MedicineListFragment   : 내 약 목록 (수정 버튼 있음)
 *   - RelationIntakeFragment : 가족 복용 내역 조회 (수정 버튼 없음)
 */
public class MedicineOuterAdapter extends RecyclerView.Adapter<MedicineOuterAdapter.ViewHolder> {

    private Context             context;
    private List<MedicineModel> list;
    private OnModifyClickListener listener; // null이면 조회 전용 모드

    /**
     * 수정 버튼 클릭 콜백 인터페이스
     * MedicineListFragment에서 MedicineModifyActivity로 이동하는 데 사용
     */
    public interface OnModifyClickListener {
        void onModifyClick(MedicineModel medicine);
    }

    /**
     * @param context  Context
     * @param list     약 목록
     * @param listener 수정 버튼 클릭 콜백 (null이면 조회 전용)
     */
    public MedicineOuterAdapter(Context context, List<MedicineModel> list,
                                OnModifyClickListener listener) {
        this.context  = context;
        this.list     = list;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_medicine_outer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        MedicineModel medicine = list.get(position);

        // 약 이름 표시
        holder.txtName.setText(medicine.getName());

        // 내부 RecyclerView: 시간대별 스케줄 (아침/점심/저녁 등)
        // listener가 null이면 MedicineInnerAdapter도 조회 모드로 동작
        MedicineInnerAdapter innerAdapter = new MedicineInnerAdapter(
                context,
                medicine.getListMedicine(),
                listener != null ? item -> listener.onModifyClick(medicine) : null
        );
        holder.innerRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        holder.innerRecyclerView.setAdapter(innerAdapter);
    }

    @Override
    public int getItemCount() { return list == null ? 0 : list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView     txtName;          // 약 이름
        RecyclerView innerRecyclerView; // 스케줄 목록 (내부 RecyclerView)

        public ViewHolder(View itemView) {
            super(itemView);
            txtName          = itemView.findViewById(R.id.itemMedicineOuter_textView_title);
            innerRecyclerView = itemView.findViewById(R.id.itemMedicineOuter_recyclerview);
        }
    }
}

