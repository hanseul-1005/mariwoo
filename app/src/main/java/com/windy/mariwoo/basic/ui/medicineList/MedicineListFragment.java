package com.windy.mariwoo.basic.ui.medicineList;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.windy.mariwoo.R;
import com.windy.mariwoo.basic.activity.MedicineAddActivity;
import com.windy.mariwoo.basic.activity.MedicineModifyActivity;
import com.windy.mariwoo.basic.adapter.MedicineOuterAdapter;
import com.windy.mariwoo.basic.model.MedicineModel;
import com.windy.mariwoo.databinding.FragmentMedicineListBinding;

import java.util.ArrayList;
import java.util.List;

public class MedicineListFragment extends Fragment {

    private FragmentMedicineListBinding binding;

    // 목록
    private MedicineOuterAdapter outerAdapter;
    private List<MedicineModel> listMedicineName;



    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentMedicineListBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        AppCompatButton btnAdd = binding.medicineListFragmentButtonAdd;

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), MedicineAddActivity.class);
                activityResultLauncher.launch(intent);
            }
        });

        /*LinearLayout layoutModify = binding.medicineListFragmentLayoutSearch;
        layoutModify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), MedicineModifyActivity.class);
                activityResultLauncher.launch(intent);
            }
        });*/



        // 목록
        View view = inflater.inflate(R.layout.fragment_medicine_list, container, false);
        RecyclerView rvOuterRecyclerView = binding.medicineListFragmentRecyclerviewOuter;

        // 예시 데이터 세팅
        listMedicineName = new ArrayList<>();

        MedicineModel medicine = new MedicineModel();
        medicine.setName("고혈압약");

        List<MedicineModel> listMedicine = new ArrayList<>();

        MedicineModel listModel1 = new MedicineModel();
        listModel1.setType("아침");
        listModel1.setTime("식전 07:00");
        listModel1.setNo("1");
        listMedicine.add(listModel1);

        MedicineModel listModel2 = new MedicineModel();
        listModel2.setType("점심");
        listModel2.setTime("식전 13:00");
        listModel2.setNo("1");
        listMedicine.add(listModel2);

        medicine.setListMedicine(listMedicine);

        listMedicineName.add(medicine);

        medicine = new MedicineModel();
        medicine.setName("당뇨");

        listMedicine = new ArrayList<>();

        MedicineModel listModel3 = new MedicineModel();
        listModel3.setType("아침");
        listModel3.setTime("식전 08:00");
        listModel3.setNo("2");
        listMedicine.add(listModel3);

        medicine.setListMedicine(listMedicine);
        listMedicineName.add(medicine);

        Log.d("HS", "listMedicineName size : "+listMedicineName.size());

        outerAdapter = new MedicineOuterAdapter(getContext(), listMedicineName);
        rvOuterRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        rvOuterRecyclerView.setAdapter(outerAdapter);


        return root;
    }




    // 약 알람 선택 후 해당 값 가져오기
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


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}