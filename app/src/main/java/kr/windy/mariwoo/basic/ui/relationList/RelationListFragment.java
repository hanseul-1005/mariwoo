package kr.windy.mariwoo.basic.ui.relationList;

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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.windy.mariwoo.R;
import kr.windy.mariwoo.basic.activity.RelationAddActivity;
import kr.windy.mariwoo.basic.adapter.RelationOuterAdapter;
import kr.windy.mariwoo.basic.model.RelationModel;
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

/**
 * 관계 목록 Fragment
 * - 스피너로 관계 유형 선택 (열람자 / 대상자 / 신청자)
 * - 각 관계 항목에서 수락(Y) / 거절(D) / 삭제(D) 처리
 * - 추가 버튼 → RelationAddActivity로 이동
 *
 * 관계 유형:
 *   열람자 - 내가 상대방의 복용 현황을 볼 수 있음
 *   대상자 - 상대방이 나의 복용 현황을 볼 수 있음
 *   신청자 - 아직 수락되지 않은 대기 상태
 */
public class RelationListFragment extends Fragment {

    private FragmentRelationListBinding binding;
    private Spinner         spinnerType;
    private AppCompatButton btnAdd;

    // 관계 목록 데이터 및 어댑터
    private RelationOuterAdapter outerAdapter;
    private List<RelationModel>  listRelation;

    private SharedPreferences sharedPreferences;
    private String userNo    = "";
    private String serverUrl = "";
    private String type      = "열람자"; // 현재 선택된 관계 유형 (기본값)

    // 싱글톤 OkHttpClient
    private final OkHttpClient client = new OkHttpClient();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        RelationListViewModel relationListViewModel =
                new ViewModelProvider(this).get(RelationListViewModel.class);

        binding = FragmentRelationListBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        serverUrl = getString(R.string.server_user);

        // Fragment 생명주기 안전 처리
        Activity activity = getActivity();
        if (activity == null) return root;
        sharedPreferences = activity.getSharedPreferences("autoLogin", Activity.MODE_PRIVATE);
        userNo = sharedPreferences.getString("user_no", "-1");

        // 관계 유형 스피너 설정
        spinnerType = root.findViewById(R.id.relationListFragment_spinner_type);
        String[] arrType = {"열람자", "대상자", "신청자"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                activity,
                android.R.layout.simple_spinner_item,
                arrType
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(adapter);

        // 관계 유형 선택 시 목록 재조회
        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                type = arrType[position]; // 선택된 유형 저장
                Toast.makeText(getContext(), type + " 선택됨", Toast.LENGTH_SHORT).show();
                getList(); // 해당 유형 목록 조회
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        listRelation = new ArrayList<>();

        RecyclerView rvOuterRecyclerView = binding.relationListFragmentRecyclerviewOuter;

        // 관계 목록 어댑터 (삭제/수락/거절 콜백)
        outerAdapter = new RelationOuterAdapter(getContext(), listRelation,
                new RelationOuterAdapter.OnRelationClickListener() {
                    @Override
                    public void onDelete(RelationModel relation, int position) {
                        // 삭제: accept='D'로 관계 상태 변경
                        Log.d("HS", "삭제 클릭 : " + relation.getName());
                        setAccept(relation.getNo(), "D");
                    }

                    @Override
                    public void onAccept(RelationModel relation, int position) {
                        // 수락: accept='Y'로 관계 상태 변경
                        Log.d("HS", "수락 클릭 : " + relation.getName());
                        setAccept(relation.getNo(), "Y");
                    }

                    @Override
                    public void onReject(RelationModel relation, int position) {
                        // 거절: accept='D'로 관계 상태 변경 (삭제와 동일 처리)
                        Log.d("HS", "거절 클릭 : " + relation.getName());
                        setAccept(relation.getNo(), "D");
                    }
                });

        rvOuterRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        rvOuterRecyclerView.setAdapter(outerAdapter);

        // 관계 추가 버튼
        btnAdd = root.findViewById(R.id.relationListFragment_button_add);
        btnAdd.setOnClickListener(v -> {
            Activity act = getActivity();
            if (act == null) return;
            startActivity(new Intent(act, RelationAddActivity.class));
        });

        // 초기 목록 조회
        getList();

        return root;
    }

    /**
     * 관계 목록 조회
     * 현재 선택된 type(열람자/대상자/신청자)에 해당하는 관계 목록 요청
     */
    private void getList() {
        Log.i("HS RelationListFragment", "get list");

        RequestBody formBody = new FormBody.Builder()
                .add("cmd",  "relation_list")
                .add("no",   userNo)
                .add("type", type) // 열람자 / 대상자 / 신청자
                .build();

        Request request = new Request.Builder()
                .url(serverUrl)
                .post(formBody)
                .build();

        Log.i("HS RelationListFragment", "request : " + request.toString());

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                // body 읽기는 백그라운드에서 처리
                if (response.body() == null) return;
                final String responseData = response.body().string();

                // Fragment detach 체크
                Activity act = getActivity();
                if (act == null || act.isFinishing()) return;

                act.runOnUiThread(() -> {
                    try {
                        Log.i("HS", "RelationListFragment 응답 성공");

                        JSONObject json   = new JSONObject(responseData);
                        String     result = json.getString("result");

                        if ("true".equals(result)) {
                            JSONArray jArr = json.getJSONArray("listRelation");
                            listRelation.clear();

                            for (int i = 0; i < jArr.length(); i++) {
                                JSONObject obj = jArr.getJSONObject(i);

                                RelationModel relation = new RelationModel();
                                relation.setNo(obj.getString("no"));
                                relation.setName(obj.getString("name"));
                                relation.setTel(obj.getString("tel"));
                                relation.setType(obj.getString("type"));
                                listRelation.add(relation);
                            }

                            if (outerAdapter != null) {
                                outerAdapter.notifyDataSetChanged();
                            }

                        } else {
                            Toast.makeText(getContext(),
                                    "일치하는 정보가 없습니다.\n입력하신 정보를 확인해주세요.",
                                    Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    /**
     * 관계 수락/거절/삭제 처리
     * @param no     관계 번호
     * @param accept 처리 값 ("Y"=수락, "D"=거절/삭제)
     */
    private void setAccept(String no, String accept) {
        Log.i("HS RelationListFragment", "set Accept: " + accept);

        RequestBody formBody = new FormBody.Builder()
                .add("cmd",    "change_accept")
                .add("no",     no)
                .add("accept", accept) // "Y" 또는 "D"
                .build();

        Request request = new Request.Builder()
                .url(serverUrl)
                .post(formBody)
                .build();

        Log.i("HS RelationListFragment", "request : " + request.toString());

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                if (response.body() == null) return;
                final String responseData = response.body().string();

                Activity act = getActivity();
                if (act == null || act.isFinishing()) return;

                act.runOnUiThread(() -> {
                    try {
                        Log.i("HS", "setAccept 응답 성공");

                        JSONObject json   = new JSONObject(responseData);
                        String     result = json.getString("result");

                        if ("true".equals(result)) {
                            getList(); // 처리 성공 후 목록 새로고침
                        } else {
                            Toast.makeText(getContext(),
                                    "처리에 실패했습니다. 다시 시도해주세요.",
                                    Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // ViewBinding 참조 해제 (메모리 누수 방지)
    }
}
