package com.windy.mariwoo.basic.ui.relationIntake;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.windy.mariwoo.databinding.FragmentRelationIntakeBinding;

public class RelationIntakeFragment extends Fragment {

    private FragmentRelationIntakeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        RelationIntakeViewModel relationIntakeViewModel =
                new ViewModelProvider(this).get(RelationIntakeViewModel.class);

        binding = FragmentRelationIntakeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        LinearLayout layoutCheck = binding.relationIntakeFragmentLayoutMorningCheck;
        layoutCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder msgBuilder = new AlertDialog.Builder(getContext())
                        .setTitle("약 알람")
                        .setMessage("약 드셨어요?")
                        .setPositiveButton("예", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        })
                        .setNeutralButton("아니오", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        });

                AlertDialog msgDlg = msgBuilder.create();
                msgDlg.show();
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}