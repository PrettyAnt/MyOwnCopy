package com.example.myapplication.hold;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.SelectableTextView.SelectableTextHelper;
import com.example.myapplication.model.Book;
//import com.jaeger.library.SelectableTextHelper;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author ChenYu
 * Author's github https://github.com/PrettyAnt
 * <p>
 * Created on 15:23  2019-12-16
 * PackageName : com.example.myapplication.hold
 * describle :
 */
public class CopyTextHolder extends RecyclerView.ViewHolder {
    private  Activity activity;
    @BindView(R.id.tv_name)
    public TextView tvName;
    @BindView(R.id.tv_age)
    public TextView tvAge;
    @BindView(R.id.tv_desc)
    public TextView tvDesc;

    public CopyTextHolder(Activity activity, View inflate) {
        super(inflate);
        this.activity = activity;
        ButterKnife.bind(this, inflate);
        initView();
    }

    private void initView() {
//        SelectableTextHelper.Builder builder = new SelectableTextHelper.Builder(tvDesc);
//        builder
//                .setSelectAll(true)
//                .build();
        ArrayList<Book> mBookList = new ArrayList<>();
        new SelectableTextHelper.Builder(tvDesc)
                .setSelectedColor(ContextCompat.getColor(activity, R.color.selected_blue))
                .setCursorHandleSizeInDp(20)
                .setBookList(mBookList)
                .setCursorHandleColor(ContextCompat.getColor(activity, R.color.cursor_handle_color))
                .build();
    }
}
