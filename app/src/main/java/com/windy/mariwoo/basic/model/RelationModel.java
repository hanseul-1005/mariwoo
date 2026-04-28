package com.windy.mariwoo.basic.model;

import java.util.List;

public class RelationModel {
    private String no;
    private String name;
    private String tel;
    private String accept;
    private String type;

    private List<RelationModel> listRelation;

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

    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }

    public String getAccept() {
        return accept;
    }

    public void setAccept(String accept) {
        this.accept = accept;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<RelationModel> getListRelation() {
        return listRelation;
    }

    public void setListRelation(List<RelationModel> listRelation) {
        this.listRelation = listRelation;
    }
}
