package com.windy.mariwoo.basic.ui.medicineList;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MedicineListViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public MedicineListViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is medicineList fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}