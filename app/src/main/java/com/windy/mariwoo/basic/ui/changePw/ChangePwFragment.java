package com.windy.mariwoo.basic.ui.changePw;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.windy.mariwoo.R;
import com.windy.mariwoo.basic.adapter.MedicineOuterAdapter;
import com.windy.mariwoo.basic.model.RelationModel;
import com.windy.mariwoo.databinding.FragmentRelationListBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChangePwFragment extends Fragment {

    private FragmentRelationListBinding binding;
    // 목록
    private MedicineOuterAdapter outerAdapter;
    private List<RelationModel> listRelation;

    private SharedPreferences sharedPreferences;
    private String userNo = "";
    private String serverUrl = "";
    private String type = "열람자 ";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ChangePwViewModel relationListViewModel =
                new ViewModelProvider(this).get(ChangePwViewModel.class);

        binding = FragmentRelationListBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        serverUrl = getString(R.string.server_medicine);

        sharedPreferences = getActivity().getSharedPreferences("autoLogin", Activity.MODE_PRIVATE);
        userNo = sharedPreferences.getString("user_no", "-1");


        return root;
    }


    private void getList() {
        Log.i("HS RelationListFragment", "get list");



        // POST 파라미터 추가
        RequestBody formBody = new FormBody.Builder()
                .add("cmd", "login")
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

                                String medicineName = "";

                                for(int i=0; i<jArr.length(); i++) {
                                    String no = jArr.getJSONObject(i).getString("no");
                                    String name = jArr.getJSONObject(i).getString("name");
                                    String tel = jArr.getJSONObject(i).getString("tel");

                                    RelationModel relation = new RelationModel();
                                    relation.setNo(no);
                                    relation.setName(name);
                                    relation.setTel(tel);

                                    listRelation.add(relation);

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
                .add("cmd", "login")
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
                                // adapter reload
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