package com.windy.mariwoo.basic.ui.relationList;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.windy.mariwoo.basic.ui.medicineList.MedicineListViewModel;
import com.windy.mariwoo.databinding.FragmentMedicineListBinding;
import com.windy.mariwoo.databinding.FragmentRelationListBinding;

public class RelationListFragment extends Fragment {

    private FragmentRelationListBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        RelationListViewModel relationListViewModel =
                new ViewModelProvider(this).get(RelationListViewModel.class);

        binding = FragmentRelationListBinding.inflate(inflater, container, false);
        View root = binding.getRoot();


        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}