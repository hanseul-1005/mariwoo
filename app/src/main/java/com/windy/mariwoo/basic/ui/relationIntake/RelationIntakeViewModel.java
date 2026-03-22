package com.windy.mariwoo.basic.ui.relationIntake;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class RelationIntakeViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public RelationIntakeViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is relationIntake fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}