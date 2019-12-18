package com.example.myapplication.SelectableTextView;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.text.Layout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.example.myapplication.R;

/**
 * Operate windows : copy, select all
 */
public class OperateWindow extends PopupWindow {

    private Context          mContext;
    private TextView         mTextView;
    private SelectionInfo    mSelectionInfo;
    private  View inflate;
    private TextView tv_select_all;//全选
    private TextView tv_copy;//复制

    private int[] mLocation = new int[2];
    private int mWidth;
    private int mHeight;

    public OperateWindow( Context context,
                          TextView textView,
                          SelectionInfo selectionInfo,
                          View.OnClickListener copyOnClickListener,
                          View.OnClickListener selectOnClickListener) {
        super(context);
        this.mContext = context;
        this.mTextView = textView;
        this.mSelectionInfo = selectionInfo;


        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflate = inflater.inflate(R.layout.popupwindow_operate, null);
        inflate.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        setContentView(inflate);
        mWidth =  inflate.getMeasuredWidth();
        mHeight = inflate.getMeasuredHeight();
        setFocusable(false);
        setClippingEnabled(false);
//        mWindow.setClippingEnabled(false);//是否可以在屏幕外显示
        tv_copy = (TextView) inflate.findViewById(com.jaeger.library.R.id.tv_copy);
        tv_select_all = (TextView) inflate.findViewById(com.jaeger.library.R.id.tv_select_all);
        ColorDrawable dw = new ColorDrawable(00000000);
        setBackgroundDrawable(dw);
        tv_copy.setOnClickListener(copyOnClickListener);
        tv_select_all.setOnClickListener(selectOnClickListener);
    }


    /**
     * 显示弹窗的方法
     */
    public void show() {
        // 获取在当前窗口内的绝对坐标
        mTextView.getLocationInWindow(mLocation);
        // 定位弹窗位置
        Layout layout = mTextView.getLayout();
        // 得到当前字符段的左边X坐标+Y坐标
        int posX = (int) layout.getPrimaryHorizontal(mSelectionInfo.getStart() )+ mLocation[0];
        int posY = layout.getLineTop(layout.getLineForOffset(
                mSelectionInfo.getStart())) + mLocation[1] - mHeight - 16;
        // 设置边界值
        if (posX <= 0) posX = 16;
        if (posY < 0) posY = 16;
        if ((posX + mWidth) > TextLayoutUtil.getScreenWidth(mContext)) {
            posX = TextLayoutUtil.getScreenWidth(mContext) - mWidth - 16;
        }
        // 设置阴影效果
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setElevation(8f);
        }
        showAtLocation(mTextView, Gravity.NO_GRAVITY, posX, posY);
    }

    public void hideWindow() {
        if (isShowing()) {
            dismiss();
        }
    }

}