package com.windy.mariwoo.basic.ui.changeName;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.windy.mariwoo.R;

public class ChangeInfoFragment extends Fragment {

    private ChangeInfoViewModel mViewModel;

    public static ChangeInfoFragment newInstance() {
        return new ChangeInfoFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_change_info, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(ChangeInfoViewModel.class);
        // TODO: Use the ViewModel
    }

}