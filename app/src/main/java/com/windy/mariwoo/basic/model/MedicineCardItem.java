package com.windy.mariwoo.basic.model;

import java.util.List;

public class MedicineCardItem {
    private String medicineName;                  // 약 이름
    private List<MedicineScheduleItem> schedules; // 시간대 목록

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public List<MedicineScheduleItem> getSchedules() { return schedules; }
    public void setSchedules(List<MedicineScheduleItem> schedules) { this.schedules = schedules; }
}