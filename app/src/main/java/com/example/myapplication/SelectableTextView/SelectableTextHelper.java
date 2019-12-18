package com.example.myapplication.SelectableTextView;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.text.Layout;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.collection.SparseArrayCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;
import com.example.myapplication.model.Book;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by Elder on 2017/3/9.
 * 创建可选文本控件的辅助类
 */

public class SelectableTextHelper {

    private static       int DEFAULT_SELECTION_LENGTH = 1;
    private static final int DEFAULT_SHOW_DURATION    = 100;

    private Context                            mContext;
    private TextView                           mTextView;
    /**
     * 保存该书所有标记的集合
     */
    /**
     * 可变文本的接口非常强大
     */
    private Spannable                          mSpannable;
    /**
     * 改变文本背景色
     */
    private BackgroundColorSpan                mBgSpan;
    /**
     * 比HashMap<Integer,Object>更高效
     */
    private SparseArrayCompat<MyClickableSpan> clickSpanMap;

    private int   mTouchX;
    private int   mTouchY;
    private int[] mLocation = new int[2];

    private int              mSelectedColor;
    private int              mCursorHandleColor;
    private int              mCursorHandleSize;
    /**
     * 记录每次选中后的信息实体类
     */
    private SelectionInfo    mSelectionInfo = new SelectionInfo();
    private OnSelectListener mSelectListener;
    /**
     * 弹出Menu窗口
     */
    private OperateWindow    mOperateWindow;
    /**
     * 两个选择游标
     */
    private CursorHandle     mStartHandle;
    private CursorHandle     mEndHandle;
    /**
     * 滚动界面时隐藏选中状态的标志
     */
    private boolean          isHideWhenScroll;
    private boolean          isHide         = true;

    private ViewTreeObserver.OnPreDrawListener       mOnPreDrawListener;
    /**
     * 滑动状态改变时的监听器
     */
    private ViewTreeObserver.OnScrollChangedListener mOnScrollChangedListener;

    private SelectableTextHelper(Builder builder) {
        mTextView = builder.mTextView;
        mContext = mTextView.getContext();
        mSelectedColor = builder.mSelectedColor;
        mCursorHandleColor = builder.mCursorHandleColor;
        mCursorHandleSize = TextLayoutUtil.dp2px(mContext, builder.mCursorHandleSizeInDp);
//        mBookList = builder.mBookList;
        init();
    }

    private void init() {
        // 由于 TextView 的文本的 BufferType 类型；
        // 是 SPANNABLE 时才可以设置 Span ，实现选中的效果；
        mTextView.setText(mTextView.getText(), TextView.BufferType.SPANNABLE);
        // 初始化保存标记对象的集合
        clickSpanMap = new SparseArrayCompat<>();

        mTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showSelectView(mTouchX, mTouchY);
                return true;
            }
        });

        mTextView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mTouchX = (int) event.getX();
                mTouchY = (int) event.getY();
                return false;
            }
        });

        mTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 当TextView有可点击部分时将屏蔽TextView的单击事件
                if (mTextView.getSelectionStart() == -1 && mTextView.getSelectionEnd() == -1) {
                    resetSelectionInfo();
                    hideSelectView();
                }
            }
        });
        // 设置当前TextView关联状态变化时的监听
        mTextView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {

            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                destroy();
            }
        });

        mOnPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {

            @Override
            public boolean onPreDraw() {
                if (isHideWhenScroll) {
                    isHideWhenScroll = false;
                    postShowSelectView(DEFAULT_SHOW_DURATION);
                }
                return true;
            }
        };
        mTextView.getViewTreeObserver().addOnPreDrawListener(mOnPreDrawListener);

        mOnScrollChangedListener = new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                if (!isHideWhenScroll && !isHide) {
                    isHideWhenScroll = true;
                    if (mOperateWindow != null) {
                        mOperateWindow.hideWindow();
                    }
                    if (mStartHandle != null) {
                        mStartHandle.dismiss();
                    }
                    if (mEndHandle != null) {
                        mEndHandle.dismiss();
                    }
                }
            }
        };
        mTextView.getViewTreeObserver().addOnScrollChangedListener(mOnScrollChangedListener);

        mOperateWindow = new OperateWindow(mContext,  mTextView, mSelectionInfo, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 获取剪贴板管理器
                ClipboardManager clip = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                // 设置剪贴板内容
                clip.setPrimaryClip(ClipData.newPlainText(mSelectionInfo.getSelectionContent(), mSelectionInfo.getSelectionContent()));
                if (mSelectListener != null) {
                    mSelectListener.onTextSelected(mSelectionInfo.getSelectionContent());
                }
                // 取消选中状态
                resetSelectionInfo();
                hideSelectView();
            }
        },
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hideSelectView();
                        selectText(0, mTextView.getText().length());
                        isHide = false;
                        showCursorHandle(mStartHandle);
                        showCursorHandle(mEndHandle);
                        mOperateWindow.show();
                    }
                });
    }

    /**
     * 延迟显示的方法
     *
     * @param duration:设置延迟的时间
     */
    private void postShowSelectView(int duration) {
        mTextView.removeCallbacks(mShowSelectViewRunnable);
        if (duration <= 0) {
            mShowSelectViewRunnable.run();
        } else {
            mTextView.postDelayed(mShowSelectViewRunnable, duration);
        }
    }

    /**
     * TextView显示时候的回调
     */
    private final Runnable mShowSelectViewRunnable = new Runnable() {
        @Override
        public void run() {
            if (isHide) return;
            if (mOperateWindow != null) {
                mOperateWindow.show();
            }
            if (mStartHandle != null) {
                showCursorHandle(mStartHandle);
            }
            if (mEndHandle != null) {
                showCursorHandle(mEndHandle);
            }
        }
    };

    /**
     * 隐藏选中状态的View
     */
    private void hideSelectView() {
        isHide = true;
        if (mStartHandle != null) {
            mStartHandle.dismiss();
        }
        if (mEndHandle != null) {
            mEndHandle.dismiss();
        }
        if (mOperateWindow != null) {
            mOperateWindow.hideWindow();
        }
    }

    /**
     * 重置选择状态
     */
    private void resetSelectionInfo() {
        if (mSpannable != null && mBgSpan != null) {
            // 移除背景色
            mSpannable.removeSpan(mBgSpan);
            mBgSpan = null;
        }
    }


    /**
     * 显示选中文本时的效果
     *
     * @param x:触碰屏幕时的X坐标
     * @param y:触碰屏幕时的Y坐标
     */
    private void showSelectView(int x, int y) {
        // 重置上一次选中的状态
        hideSelectView();
        resetSelectionInfo();
        isHide = false;
        // 新建左右游标
        if (mStartHandle == null) mStartHandle = new CursorHandle(true);
        if (mEndHandle == null) mEndHandle = new CursorHandle(false);
        // 默认选择一个字符
        int startOffset = TextLayoutUtil.getPreciseOffset(mTextView, x, y);
        int endOffset   = startOffset + DEFAULT_SELECTION_LENGTH;

        if (mTextView.getText() instanceof Spannable) {
            mSpannable = (Spannable) mTextView.getText();
        }
        // 边界异常处理
        if (mSpannable == null || startOffset >= mTextView.getText().length()) {
            return;
        }
        selectText(startOffset, endOffset);
        showCursorHandle(mStartHandle);
        showCursorHandle(mEndHandle);
        if (mOperateWindow != null) {
            mOperateWindow.show();
        }
        // 恢复初始值
        DEFAULT_SELECTION_LENGTH = 1;
    }

    /**
     * 选中文本的方法
     *
     * @param startPos:选择文本时的首字索引值
     * @param endPos:选择文本时的尾字索引值
     */
    private void selectText(int startPos, int endPos) {
        if (startPos != -1) {
            mSelectionInfo.setStart(startPos);
        }
        if (endPos != -1) {
            mSelectionInfo.setEnd(endPos);
        }
        if (mSelectionInfo.getStart() > mSelectionInfo.getEnd()) {
            int temp = mSelectionInfo.getStart();
            mSelectionInfo.setStart(mSelectionInfo.getEnd());
            mSelectionInfo.setEnd(temp);
        }

        if (mSpannable != null) {
            if (mBgSpan == null) {
                mBgSpan = new BackgroundColorSpan(mSelectedColor);
            }
            // 截取选中状态的文本
            mSelectionInfo.setSelectionContent(
                    mSpannable.subSequence(
                            mSelectionInfo.getStart(),
                            mSelectionInfo.getEnd()).toString());
            mSpannable.setSpan(mBgSpan,
                    mSelectionInfo.getStart(), mSelectionInfo.getEnd(),
                    Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            // 设置监听选中接口回调时选择到的文字
            if (mSelectListener != null) {
                mSelectListener.onTextSelected(mSelectionInfo.getSelectionContent());
            }
        }
    }

    /**
     * 显示游标的方法
     *
     * @param cursorHandle:需要显示的游标对象
     */
    private void showCursorHandle(CursorHandle cursorHandle) {
        Layout layout = mTextView.getLayout();
        int    offset = cursorHandle.isLeft ? mSelectionInfo.getStart() : mSelectionInfo.getEnd();
        cursorHandle.show((int) layout.getPrimaryHorizontal(offset),
                layout.getLineBottom(layout.getLineForOffset(offset)));
    }

    /**
     * 设置外部调用的监听接口
     *
     * @param selectListener:供外部传入的监听接口对象
     */
    public void setSelectListener(OnSelectListener selectListener) {
        mSelectListener = selectListener;
    }

    /**
     * 当前TextView销毁时释放资源
     */
    private void destroy() {
        // 释放监听器
        mTextView.getViewTreeObserver().removeOnPreDrawListener(mOnPreDrawListener);
        mTextView.getViewTreeObserver().removeOnScrollChangedListener(mOnScrollChangedListener);
        // 取消显示状态
        resetSelectionInfo();
        hideSelectView();
        mStartHandle = null;
        mEndHandle = null;
        mOperateWindow = null;

        if (clickSpanMap != null) {
            clickSpanMap.clear();
            clickSpanMap = null;
        }
    }

    /**
     * 构建者类用于初始化SelectableTextHelper类
     */
    public static class Builder {
        private TextView mTextView;
        private int      mCursorHandleColor    = 0xFF1379D6;
        private int      mSelectedColor        = 0xFFAFE1F4;
        private float    mCursorHandleSizeInDp = 24;
//        private List<Book> mBookList;

        public Builder(TextView textView) {
            mTextView = textView;
        }

        public Builder setCursorHandleColor(@ColorInt int cursorHandleColor) {
            mCursorHandleColor = cursorHandleColor;
            return this;
        }

        public Builder setSelectedColor(@ColorInt int selectedBgColor) {
            mSelectedColor = selectedBgColor;
            return this;
        }

        public Builder setCursorHandleSizeInDp(float cursorHandleSizeInDp) {
            mCursorHandleSizeInDp = cursorHandleSizeInDp;
            return this;
        }


        public Builder setBookList(List<Book> mList) {
//            mBookList = mList;
            return this;
        }

        public SelectableTextHelper build() {
            return new SelectableTextHelper(this);
        }
    }


    /**
     * 选中文本时的两端游标View
     */
    private class CursorHandle extends View {

        private Paint       mPaint;
        private PopupWindow mPopupWindow;

        private int mCircleRadius = mCursorHandleSize / 2;
        private int mWidth        = mCircleRadius * 2;
        private int mHeight       = mCircleRadius * 2;
        private int mPadding      = 25;

        private boolean isLeft;

        private CursorHandle(boolean isLeft) {
            super(mContext);

            this.isLeft = isLeft;

            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaint.setColor(mCursorHandleColor);
            // popWindow用于显示弹出菜单
            mPopupWindow = new PopupWindow(this);
            // 不允许PopWindow超出屏幕范围
            mPopupWindow.setClippingEnabled(false);
            mPopupWindow.setWidth(mWidth + mPadding * 2);
            mPopupWindow.setHeight(mHeight + mPadding / 2);

            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            // 仿照原生游标图形 圆形+方块
            canvas.drawCircle(mPadding + mCircleRadius, mCircleRadius, mCircleRadius, mPaint);
            if (isLeft) {
                canvas.drawRect(mCircleRadius + mPadding, 0, mCircleRadius * 2 + mPadding, mCircleRadius, mPaint);
            } else {
                canvas.drawRect(mPadding, 0, mPadding + mCircleRadius, mCircleRadius, mPaint);
            }
        }

        /**
         * 记录触摸View时的坐标
         */
        private int mAdjustX;
        private int mAdjustY;

        private int mBeforeDragStart;
        private int mBeforeDragEnd;

        @Override
        public boolean onTouchEvent(MotionEvent event) {

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mBeforeDragStart = mSelectionInfo.getStart();
                    mBeforeDragEnd = mSelectionInfo.getEnd();
                    // 返回相对于当前View的相对坐标
                    mAdjustX = (int) event.getX();
                    mAdjustY = (int) event.getY();
                    break;
                // 拖拽触摸结束后显示弹出菜单
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mOperateWindow.show();
                    break;
                case MotionEvent.ACTION_MOVE:
                    mOperateWindow.hideWindow();
                    // 返回相对于屏幕的绝对坐标
                    int rawX = (int) event.getRawX();
                    int rawY = (int) event.getRawY();
                    update(rawX + mAdjustX - mWidth, rawY + mAdjustY - mHeight);
                    break;
            }
            return true;
        }

        /**
         * 随着触摸移动不断更新选中状态
         *
         * @param x:移动手指时的X坐标
         * @param y:移动手指时的Y坐标
         */
        private void update(int x, int y) {
            mTextView.getLocationInWindow(mLocation);
            int oldOffset;

            if (isLeft) {
                oldOffset = mSelectionInfo.getStart();
            } else {
                oldOffset = mSelectionInfo.getEnd();
            }

            y -= mLocation[1];

            int offset = TextLayoutUtil.getHysteresisOffset(mTextView, x, y, oldOffset);

            if (offset != oldOffset) {
                resetSelectionInfo();
                if (isLeft) {
                    // 处理如果出现用户将左边游标拖动到右边游标之后的调转情况
                    if (offset > mBeforeDragEnd) {
                        CursorHandle cursorHandle = getCursorHandle(false);
                        // 改变当前游标的方向
                        changeDirection();
                        // 改变右侧游标的方向
                        cursorHandle.changeDirection();
                        mBeforeDragStart = mBeforeDragEnd;
                        selectText(mBeforeDragEnd, offset);
                        cursorHandle.updateCursorHandle();
                    } else {
                        selectText(offset, -1);
                    }
                    updateCursorHandle();
                } else {
                    // 处理右边超过左边游标的情况
                    if (offset < mBeforeDragStart) {
                        CursorHandle cursorHandle = getCursorHandle(true);
                        cursorHandle.changeDirection();
                        changeDirection();
                        mBeforeDragEnd = mBeforeDragStart;
                        selectText(offset, mBeforeDragStart);
                        cursorHandle.updateCursorHandle();
                    } else {
                        selectText(mBeforeDragStart, offset);
                    }
                    updateCursorHandle();
                }
            }
        }

        /**
         * 更新游标选择区域的方法
         */
        private void updateCursorHandle() {
            mTextView.getLocationInWindow(mLocation);
            Layout layout = mTextView.getLayout();
            if (isLeft) {
                mPopupWindow.update((int) layout.getPrimaryHorizontal(mSelectionInfo.getStart()) - mWidth + getExtraX(),
                        layout.getLineBottom(layout.getLineForOffset(mSelectionInfo.getStart())) + getExtraY(), -1, -1);
            } else {
                mPopupWindow.update((int) layout.getPrimaryHorizontal(mSelectionInfo.getEnd()) + getExtraX(),
                        layout.getLineBottom(layout.getLineForOffset(mSelectionInfo.getEnd())) + getExtraY(), -1, -1);
            }
        }

        /**
         * 显示游标时调用的方法
         *
         * @param x:移动手指时的X坐标
         * @param y:移动手指时的Y坐标
         */
        public void show(int x, int y) {
            mTextView.getLocationInWindow(mLocation);
            int offset = isLeft ? mWidth : 0;
            mPopupWindow.showAtLocation(mTextView, Gravity.NO_GRAVITY, x - offset + getExtraX(), y + getExtraY());
        }

        public int getExtraX() {
            return mLocation[0] - mPadding + mTextView.getPaddingLeft();
        }

        public int getExtraY() {
            return mLocation[1] + mTextView.getPaddingTop();
        }

        /**
         * 改变方向的方法
         */
        private void changeDirection() {
            isLeft = !isLeft;
            invalidate();
        }

        /**
         * 返回游标类型
         *
         * @param isLeft:判断是否为起始游标
         * @return 返回指定的游标对象
         */
        private CursorHandle getCursorHandle(boolean isLeft) {
            if (mStartHandle.isLeft == isLeft) {
                return mStartHandle;
            } else {
                return mEndHandle;
            }
        }

        public void dismiss() {
            mPopupWindow.dismiss();
        }
    }
}
