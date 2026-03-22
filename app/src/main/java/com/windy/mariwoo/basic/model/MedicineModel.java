package com.windy.mariwoo.basic.model;

import java.util.List;

public class MedicineModel {
    private String no;
    private String name;
    private String time;
    private String type;
    private String intakeYn;

    private List<MedicineModel> listMedicine;


    public String getNo() {
        return no;
    }

    public void setNo(String no) {
        this.no = no;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIntakeYn() {
        return intakeYn;
    }

    public void setIntakeYn(String intakeYn) {
        this.intakeYn = intakeYn;
    }

    public List<MedicineModel> getListMedicine() {
        return listMedicine;
    }

    public void setListMedicine(List<MedicineModel> listMedicine) {
        this.listMedicine = listMedicine;
    }
}
