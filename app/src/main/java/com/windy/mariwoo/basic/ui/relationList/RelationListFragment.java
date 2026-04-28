package com.windy.mariwoo.basic.ui.relationList;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.windy.mariwoo.R;
import com.windy.mariwoo.basic.LoginActivity;
import com.windy.mariwoo.basic.MainActivity;
import com.windy.mariwoo.basic.activity.RelationAddActivity;
import com.windy.mariwoo.basic.adapter.MedicineOuterAdapter;
import com.windy.mariwoo.basic.adapter.RelationOuterAdapter;
import com.windy.mariwoo.basic.model.MedicineModel;
import com.windy.mariwoo.basic.model.RelationModel;
import com.windy.mariwoo.basic.ui.medicineList.MedicineListViewModel;
import com.windy.mariwoo.databinding.FragmentMedicineListBinding;
import com.windy.mariwoo.databinding.FragmentRelationListBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RelationListFragment extends Fragment {

    private FragmentRelationListBinding binding;
    private Spinner spinnerType;
    private AppCompatButton btnAdd;
    // 목록
    private RelationOuterAdapter outerAdapter;
    private List<RelationModel> listRelation;

    private SharedPreferences sharedPreferences;
    private String userNo = "";
    private String serverUrl = "";
    private String type = "열람자";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        RelationListViewModel relationListViewModel =
                new ViewModelProvider(this).get(RelationListViewModel.class);

        binding = FragmentRelationListBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        serverUrl = getString(R.string.server_user);

        sharedPreferences = getActivity().getSharedPreferences("autoLogin", Activity.MODE_PRIVATE);
        userNo = sharedPreferences.getString("user_no", "-1");


        // 1. 스피너 찾기
        spinnerType = root.findViewById(R.id.relationListFragment_spinner_type);

        // 2. 스피너에 넣을 요일 배열 만들기
        String[] arrType = {"열람자", "대상자", "신청자"};

        // 3. 어댑터 생성 및 데이터 연결
        // android.R.layout.simple_spinner_item은 안드로이드 기본 레이아웃을 사용
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getActivity(),
                android.R.layout.simple_spinner_item,
                arrType
        );

        // 4. 드롭다운 클릭 시 보일 아이템 레이아웃 설정
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // 5. 스피너에 어댑터 적용
        spinnerType.setAdapter(adapter);

        // 6. 스피너 아이템 선택 리스너 달기 (선택 옵션)
        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // position을 통해 선택된 요일의 인덱스 확인 가능 (0: 월요일, 1: 화요일 ...)
                type = arrType[position];
                Toast.makeText(getActivity(), type + " 선택됨", Toast.LENGTH_SHORT).show();

                getList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 아무것도 선택되지 않았을 때 동작
            }
        });
        // 예시 데이터 세팅
        listRelation = new ArrayList<>();

        getList();

        RecyclerView rvOuterRecyclerView = binding.relationListFragmentRecyclerviewOuter;

        outerAdapter = new RelationOuterAdapter(getContext(), listRelation, new RelationOuterAdapter.OnRelationClickListener() {
            @Override
            public void onDelete(RelationModel relation, int position) {
                // 삭제 버튼 클릭
                Log.d("HS", "삭제 클릭 : " + relation.getName());
                // TODO: 삭제 REST 호출
                setAccept(relation.getNo(), "D");
            }

            @Override
            public void onAccept(RelationModel relation, int position) {
                // 수락 버튼 클릭
                Log.d("HS", "수락 클릭 : " + relation.getName());
                setAccept(relation.getNo(), "Y");
            }

            @Override
            public void onReject(RelationModel relation, int position) {
                // 거절 버튼 클릭
                Log.d("HS", "거절 클릭 : " + relation.getName());
                setAccept(relation.getNo(), "D");
            }
        });

        rvOuterRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        rvOuterRecyclerView.setAdapter(outerAdapter);

        btnAdd = root.findViewById(R.id.relationListFragment_button_add);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), RelationAddActivity.class);
                startActivity(intent);
            }
        });

        return root;
    }


    private void getList() {
        Log.i("HS RelationListFragment", "get list");


        // POST 파라미터 추가
        RequestBody formBody = new FormBody.Builder()
                .add("cmd", "relation_list")
                .add("no", userNo)
                .add("type", type)
                .build();

        // 요청 만들기
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(serverUrl)
                .post(formBody)
                .build();
        Log.i("HS RelationListFragment", "request : "+request.toString());
        // 응답 콜백
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {

                // 서브 스레드 Ui 변경 할 경우 에러
                // 메인스레드 Ui 설정
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            Log.i("HS", "RelationListFragment 응답 성공");
                            final String responseData = response.body().string();

                            JSONObject json = new JSONObject(responseData);

                            String result = json.getString("result");

                            if("true".equals(result)) {

                                JSONArray jArr = json.getJSONArray("listRelation");
                                listRelation.clear();

                                String medicineName = "";

                                for(int i=0; i<jArr.length(); i++) {
                                    String no = jArr.getJSONObject(i).getString("no");
                                    String name = jArr.getJSONObject(i).getString("name");
                                    String tel = jArr.getJSONObject(i).getString("tel");
                                    String type = jArr.getJSONObject(i).getString("type");

                                    RelationModel relation = new RelationModel();
                                    relation.setNo(no);
                                    relation.setName(name);
                                    relation.setTel(tel);
                                    relation.setType(type);

                                    listRelation.add(relation);

                                }

                                // 4. 파싱이 모두 끝난 후 어댑터 새로고침
                                if(outerAdapter != null) {
                                    outerAdapter.notifyDataSetChanged();
                                }


                            } else {
                                Toast.makeText(getContext(), "일치하는 정보가 없습니다.\n입력하신 정보를 확인해주세요." + responseData, Toast.LENGTH_SHORT).show();
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

            }
        });
    }

    private void setAccept(String no, String accept) {
        Log.i("HS RelationListFragment", "set Accept");



        // POST 파라미터 추가
        RequestBody formBody = new FormBody  .Builder()
                .add("cmd", "change_accept")
                .add("no", no)
                .add("accept", accept)
                .build();

        // 요청 만들기
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(serverUrl)
                .post(formBody)
                .build();
        Log.i("HS RelationListFragment", "request : "+request.toString());
        // 응답 콜백
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {

                // 서브 스레드 Ui 변경 할 경우 에러
                // 메인스레드 Ui 설정
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            Log.i("HS", "응답 성공");
                            final String responseData = response.body().string();

                            JSONObject json = new JSONObject(responseData);

                            String result = json.getString("result");

                            if("true".equals(result)) {
                                getList();
                            } else {
                                Toast.makeText(getContext(), "일치하는 정보가 없습니다.\n입력하신 정보를 확인해주세요." + responseData, Toast.LENGTH_SHORT).show();
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}