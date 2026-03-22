package com.windy.mariwoo.basic.ui.medicineIntake;

import static android.app.Activity.RESULT_OK;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.windy.mariwoo.basic.activity.DatePickerActivity;
import com.windy.mariwoo.basic.activity.MedicineAddActivity;
import com.windy.mariwoo.databinding.FragmentMedicineIntakeBinding;

public class MedicineIntakeFragment extends Fragment {

    private FragmentMedicineIntakeBinding binding;

    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {

            if(result.getResultCode() == RESULT_OK) {
                Intent resultIntent = result.getData();
                String state = resultIntent.getStringExtra("state");

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Log.d("HS", "retrun");
                    }
                });
            }
        }
    });

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        MedicineIntakeViewModel medicineIntakeViewModel =
                new ViewModelProvider(this).get(MedicineIntakeViewModel.class);

        binding = FragmentMedicineIntakeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        LinearLayout layoutCalendar = binding.medicineIntakeFragmentLayoutCalendar;
        layoutCalendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), DatePickerActivity.class);
                activityResultLauncher.launch(intent);
            }
        });

        LinearLayout layoutCheck = binding.medicineIntakeFragmentLayoutMorningCheck;
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