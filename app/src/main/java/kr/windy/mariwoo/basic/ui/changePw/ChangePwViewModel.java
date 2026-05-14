package kr.windy.mariwoo.basic.ui.changePw;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ChangePwViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public ChangePwViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is changePw fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}