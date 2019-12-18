package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapter.CopyTextadapter;
import com.example.myapplication.model.PersonInfo;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.tv_test) TextView        tvTest;
    @BindView(R.id.rv_list) RecyclerView    rvList;
    private                 CopyTextadapter copyTextadapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initEvent();
    }

    private void initEvent() {
        copyTextadapter = new CopyTextadapter(this, personInfos);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        linearLayoutManager.setStackFromEnd(false);
        rvList.setLayoutManager(linearLayoutManager);
        rvList.setAdapter(copyTextadapter);
        rvList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
//                Log.i("wangbei", "onScrolled----->>dx:"+dx+"  dy:"+dy);
            }
        });

    }

    @OnClick(R.id.tv_test)
    public void onViewClicked() {
        initData();
    }

    private List<PersonInfo> personInfos = new ArrayList<>();

    private void initData() {
        personInfos.clear();
        for (int i = 0; i < 200; i++) {
            PersonInfo personInfo = new PersonInfo();
            personInfo.setName(i % 2 == 0 ? "wangbei普通字符：就是普通字符串的剪切，复制，粘贴。\n" +
                    "（2）URL：在复制的时候可以复制一个URL,这个URL可以是请求ContentProvider的URL，在粘贴的时候调用ContntProvider获取数据，并使用。\n" +
                    "（3）Intent：如，在记事本应用中长按某一个记录本条目，这时会创建删除这个记事本的Intent，并添加到剪贴板，当用户将这个记事本条目拖到垃圾桶松开时，应用会从剪贴板中获取Intent并执行，这个记事本条目就被删除了。" : "chenyu");
            personInfo.setAge("第---->>>>>>>>>> " + (i)+" 条");
            personInfo.setDescrible("ButterKnife是由国外一个大牛开源出来的一个项目，是为了用过" + i + "注解的形式来在android中绑定view以及事件信息。 目前在github上面的开源地址为https");
            personInfos.add(personInfo);
        }
        copyTextadapter.notifyDataSetChanged();
    }
}
