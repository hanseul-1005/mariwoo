package kr.windy.mariwoo.basic.ui.medicineIntake;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MedicineIntakeViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public MedicineIntakeViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is medicineIntake fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}