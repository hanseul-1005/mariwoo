package kr.windy.mariwoo.basic.model;

import java.util.List;

/**
 * 약 정보 범용 데이터 모델
 *
 * 사용 화면:
 *   - MedicineListFragment   : 내 약 목록 (외부 카드 = 약 이름, 내부 = 스케줄)
 *   - RelationIntakeFragment : 가족 복용 내역 조회
 *
 * 중첩 구조:
 *   - 외부 MedicineModel: name (약 이름), listMedicine (스케줄 목록)
 *   - 내부 MedicineModel: no (schedule_no), type (시간대), time (시각), intakeYn (복용 여부)
 *
 * 필드:
 *   no         - schedule_no 또는 medicine_no (문자열)
 *   name       - 약 이름
 *   time       - 복용 시간 (HH:mm)
 *   type       - 시간대 인덱스("0"~"3") 또는 텍스트("아침"~"취침 전")
 *   intakeYn   - 복용 여부 ("1" = 복용, "0" = 미복용)
 *   medicineNo - 약 번호 (MedicineModifyActivity로 이동 시 사용)
 *   listMedicine - 해당 약의 시간대별 스케줄 목록 (중첩 구조)
 */
public class MedicineModel {

    private String no;           // schedule_no 또는 medicine_no
    private String name;         // 약 이름
    private String time;         // 복용 시간 (HH:mm)
    private String type;         // 시간대 ("0"=아침 ~ "3"=취침 전, 또는 텍스트)
    private String intakeYn;     // 복용 여부 ("1"=복용, "0"=미복용)
    private String medicineNo;   // 약 번호 (수정 화면 이동 시 사용)

    private List<MedicineModel> listMedicine; // 중첩 스케줄 목록 (외부 카드에서 사용)

    public String getNo() { return no; }
    public void setNo(String no) { this.no = no; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getIntakeYn() { return intakeYn; }
    public void setIntakeYn(String intakeYn) { this.intakeYn = intakeYn; }

    public String getMedicineNo() { return medicineNo; }
    public void setMedicineNo(String medicineNo) { this.medicineNo = medicineNo; }

    public List<MedicineModel> getListMedicine() { return listMedicine; }
    public void setListMedicine(List<MedicineModel> listMedicine) { this.listMedicine = listMedicine; }
}
