package com.windy.mariwoo.basic.model;

public class MedicineScheduleItem {
    private long scheduleNo;      // medicine_schedule.no
    private String intakeTimeType; // 아침/점심/저녁/취침 전
    private String intakeType;     // 식전/식후
    private String intakeTime;     // 07:00
    private boolean isTaken;       // 먹음/안먹음

    public long getScheduleNo() { return scheduleNo; }
    public void setScheduleNo(long scheduleNo) { this.scheduleNo = scheduleNo; }

    public String getIntakeTimeType() { return intakeTimeType; }
    public void setIntakeTimeType(String intakeTimeType) { this.intakeTimeType = intakeTimeType; }

    public String getIntakeType() { return intakeType; }
    public void setIntakeType(String intakeType) { this.intakeType = intakeType; }

    public String getIntakeTime() { return intakeTime; }
    public void setIntakeTime(String intakeTime) { this.intakeTime = intakeTime; }

    public boolean isTaken() { return isTaken; }
    public void setTaken(boolean taken) { isTaken = taken; }
}