package com.example.myapplication.adapter;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.model.PersonInfo;
import com.example.myapplication.R;
import com.example.myapplication.hold.CopyTextHolder;

import java.util.List;

/**
 * @author ChenYu
 * Author's github https://github.com/PrettyAnt
 * <p>
 * Created on 15:22  2019-12-16
 * PackageName : com.example.myapplication.adapter
 * describle :
 */
public class CopyTextadapter extends RecyclerView.Adapter<CopyTextHolder> {

    private Activity         activity;
    private List<PersonInfo> personInfos;

    public CopyTextadapter(Activity activity, List<PersonInfo> personInfos) {
        this.activity = activity;
        this.personInfos = personInfos;
    }

    @NonNull
    @Override
    public CopyTextHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View inflate = LayoutInflater
                .from(activity)
                .inflate(R.layout.item_copytext, parent, false);
        CopyTextHolder copyTextHolder = new CopyTextHolder(activity,inflate);
        Log.i("wangbei", "onCreateViewHolder: ---------->>>>>>> 状态更新");
        return copyTextHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull CopyTextHolder holder, int position) {
        holder.tvName.setText(personInfos.get(position).getName());
        holder.tvAge.setText(personInfos.get(position).getAge());
        holder.tvDesc.setText(personInfos.get(position).getDescrible());
    }

    @Override
    public int getItemCount() {
        return personInfos.size();
    }
}
