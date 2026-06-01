package kr.windy.mariwoo.basic.model;

public class HospitalScheduleModel {
    private long   no;
    private String name; // 병원명 / 일정명
    private String time; // 예약 일시
    private String memo; // 메모

    public long getNo() { return no; }
    public void setNo(long no) { this.no = no; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
}
