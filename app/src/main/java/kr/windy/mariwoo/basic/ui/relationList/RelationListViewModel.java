package kr.windy.mariwoo.basic.ui.relationList;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class RelationListViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public RelationListViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is relationList fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}