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

/**
 * 관계 목록 RecyclerView 어댑터 (RelationListFragment에서 사용)
 *
 * 관계 유형별 UI 분기:
 *   - "열람자" 또는 "대상자" : 삭제 버튼만 표시 (layoutDelete)
 *   - 그 외 ("신청자")       : 수락/거절 버튼 표시 (layoutAccept)
 *
 * 버튼 동작:
 *   - 삭제  : onDelete 콜백 → setAccept("D")
 *   - 수락  : onAccept 콜백 → setAccept("Y")
 *   - 거절  : onReject 콜백 → setAccept("D") (삭제와 동일 처리)
 *
 * 콜백은 RelationListFragment에서 구현하며,
 * 서버 요청 후 성공 시 목록을 다시 불러옴 (getList 재호출)
 */
public class RelationOuterAdapter extends RecyclerView.Adapter<RelationOuterAdapter.OuterViewHolder> {

    private Context             context;
    private List<RelationModel> listRelation;

    /**
     * 관계 항목 클릭 콜백 인터페이스
     */
    public interface OnRelationClickListener {
        void onDelete(RelationModel relation, int position); // 삭제 버튼 클릭
        void onAccept(RelationModel relation, int position); // 수락 버튼 클릭
        void onReject(RelationModel relation, int position); // 거절 버튼 클릭
    }

    private OnRelationClickListener listener;

    /**
     * @param context      Context
     * @param listRelation 관계 목록 데이터
     * @param listener     버튼 클릭 콜백
     */
    public RelationOuterAdapter(Context context, List<RelationModel> listRelation,
                                OnRelationClickListener listener) {
        this.context      = context;
        this.listRelation = listRelation;
        this.listener     = listener;
    }

    /** listener 없는 생성자 (조회 전용용, 현재 미사용) */
    public RelationOuterAdapter(Context context, List<RelationModel> listRelation) {
        this.context      = context;
        this.listRelation = listRelation;
    }

    static class OuterViewHolder extends RecyclerView.ViewHolder {
        TextView        tvName;        // 상대방 이름
        TextView        tvTel;         // 상대방 전화번호
        LinearLayout    layoutDelete;  // 삭제 버튼 영역 (열람자/대상자용)
        LinearLayout    layoutAccept;  // 수락/거절 버튼 영역 (신청자용)
        AppCompatButton btnDelete;     // 삭제 버튼
        AppCompatButton btnAccept;     // 수락 버튼
        AppCompatButton btnReject;     // 거절 버튼

        OuterViewHolder(View itemView) {
            super(itemView);
            tvName      = (TextView) itemView.findViewById(R.id.itemRelationList_textView_name);
            tvTel       = (TextView) itemView.findViewById(R.id.itemRelationList_textView_tel);
            layoutDelete = (LinearLayout) itemView.findViewById(R.id.itemRelationList_layout_delete);
            layoutAccept = (LinearLayout) itemView.findViewById(R.id.itemRelationList_layout_accept);
            btnDelete   = (AppCompatButton) itemView.findViewById(R.id.itemRelationList_button_delete);
            btnAccept   = (AppCompatButton) itemView.findViewById(R.id.itemRelationList_button_accept);
            btnReject   = (AppCompatButton) itemView.findViewById(R.id.itemRelationList_button_reject);
        }
    }

    @NonNull
    @Override
    public OuterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_relation_list, parent, false);
        return new OuterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OuterViewHolder holder, int position) {
        RelationModel relation = listRelation.get(position);

        holder.tvName.setText(relation.getName());
        holder.tvTel.setText(relation.getTel());

        // 관계 유형에 따라 버튼 영역 전환
        if ("열람자".equals(relation.getType()) || "대상자".equals(relation.getType())) {
            // 이미 수락된 관계 → 삭제 버튼만 표시
            holder.layoutDelete.setVisibility(View.VISIBLE);
            holder.layoutAccept.setVisibility(View.GONE);
        } else {
            // 신청 대기 상태 → 수락/거절 버튼 표시
            holder.layoutDelete.setVisibility(View.GONE);
            holder.layoutAccept.setVisibility(View.VISIBLE);
        }

        // 버튼 클릭 리스너 연결
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
