// CalendarDayAdapter.java
package kr.windy.mariwoo.basic.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import kr.windy.mariwoo.R;

import java.util.List;
import java.util.Map;

/**
 * 복용 달력 RecyclerView 어댑터 (7칸 그리드)
 *
 * 각 셀:
 *   - day == 0   : 빈 칸 (해당 월의 1일 이전 / 말일 이후 패딩)
 *   - day > 0    : 실제 날짜 숫자 표시
 *
 * 복용 상태별 배경색:
 *   - "all"     : 초록색 배경 (해당 날짜 모든 약 복용 완료)
 *   - "partial" : 빨간색 배경 (일부 미복용)
 *   - null      : 투명 (스케줄 없음 또는 미래 날짜)
 *
 * 요일별 텍스트 색상:
 *   - 일요일(col==0) : 빨강
 *   - 토요일(col==6) : 파랑
 *   - 그 외          : 기본 (어두운 회색)
 *
 * updateData()를 통해 데이터 갱신 가능 (월 이동 시 호출)
 */
public class CalendarDayAdapter extends RecyclerView.Adapter<CalendarDayAdapter.ViewHolder> {

    private List<Integer>        dayList;   // 날짜 목록 (0=빈칸, 양수=날짜)
    private Map<String, String>  statusMap; // 날짜 → 복용 상태 ("all" or "partial")
    private int year, month;                // 현재 표시 중인 연/월

    public CalendarDayAdapter(List<Integer> dayList, Map<String, String> statusMap) {
        this.dayList   = dayList;
        this.statusMap = statusMap;
    }

    /**
     * 달력 데이터 갱신
     * - 월 이동 버튼 클릭 후 서버 응답 수신 시 호출
     *
     * @param dayList   새로운 날짜 목록
     * @param statusMap 새로운 복용 상태 맵
     * @param year      갱신할 연도
     * @param month     갱신할 월 (1~12)
     */
    public void updateData(List<Integer> dayList, Map<String, String> statusMap, int year, int month) {
        this.dayList   = dayList;
        this.statusMap = statusMap;
        this.year      = year;
        this.month     = month;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        int day = dayList.get(position);

        // 빈 칸 처리 (해당 월 범위 밖)
        if (day == 0) {
            holder.txtDay.setText("");
            holder.itemView.setBackgroundColor(0x00000000); // 투명
            return;
        }

        holder.txtDay.setText(String.valueOf(day));

        // 날짜 키 생성 (서버 응답 키와 동일한 형식: "2025-05-13")
        String dateKey = String.format("%04d-%02d-%02d", year, month, day);
        String status  = statusMap.get(dateKey);

        // 복용 상태에 따른 배경색 적용
        if ("all".equals(status)) {
            holder.itemView.setBackgroundResource(R.drawable.bg_calendar_green); // 전부 복용
        } else if ("partial".equals(status)) {
            holder.itemView.setBackgroundResource(R.drawable.bg_calendar_red);   // 일부 미복용
        } else {
            holder.itemView.setBackgroundColor(0x00000000); // 스케줄 없음 → 투명
        }

        // 요일별 텍스트 색상 (그리드 7칸 기준, position % 7)
        int col = position % 7;
        if (col == 0) {
            holder.txtDay.setTextColor(0xFFE53935); // 일요일: 빨강
        } else if (col == 6) {
            holder.txtDay.setTextColor(0xFF1565C0); // 토요일: 파랑
        } else {
            holder.txtDay.setTextColor(0xFF222222); // 평일: 기본
        }
    }

    @Override
    public int getItemCount() {
        return dayList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtDay; // 날짜 숫자 TextView

        public ViewHolder(View itemView) {
            super(itemView);
            txtDay = itemView.findViewById(R.id.itemCalendarDay_text_day);
        }
    }
}
