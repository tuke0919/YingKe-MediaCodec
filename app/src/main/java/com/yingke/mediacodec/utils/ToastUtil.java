package com.yingke.mediacodec.utils;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.yingke.mediacodec.MediaCodecApplication;

import java.lang.reflect.Field;

/**
 * Toast管理
 */
public class ToastUtil {

    public static Toast toast;
    public static Context mContext = MediaCodecApplication.getInstance();

    public static void showToastShort(String toastText) {
        if (mContext == null) {
            return;
        }
        // android p toast 特殊处理
        int version = android.os.Build.VERSION.SDK_INT;
        if (version >= android.os.Build.VERSION_CODES.P) {
            toast = Toast.makeText(mContext, "", Toast.LENGTH_SHORT);
        }
        // 其他
        if (toast == null) {
            toast = Toast.makeText(mContext, "", Toast.LENGTH_SHORT);
        } else if (toast.getDuration() == Toast.LENGTH_LONG) {
            toast.setDuration(Toast.LENGTH_SHORT);
        }
        if (TextUtils.isEmpty(toastText))
            return;
        toast.setText(toastText);
        toast.show();
    }

    public static void showToastLong(String toastText) {
        if (mContext == null) {
            return;
        }
        // android p toast 特殊处理
        int version = android.os.Build.VERSION.SDK_INT;
        if (version >= android.os.Build.VERSION_CODES.P) {
            toast = Toast.makeText(mContext, "", Toast.LENGTH_SHORT);
        }
        // 其他
        if (toast == null) {
            toast = Toast.makeText(mContext, "", Toast.LENGTH_LONG);
        } else if (toast.getDuration() == Toast.LENGTH_SHORT) {
            toast.setDuration(Toast.LENGTH_LONG);
        }
        if (TextUtils.isEmpty(toastText))
            return;
        toast.setText(toastText);
        toast.show();
    }

    public static void showToastShort(int res) {
        showToastShort(mContext.getString(res));
    }

    public static void showToastLong(int res) {
        showToastLong(mContext.getString(res));
    }

    /**
     * 显示自定义布局Toast
     *
     * @param layout 自定义布局
     */
    private static Toast customToast;

    public static void showCustomToast(int layout, int gravity, int xOffset, int yOffset, OnInitToast onInit) {
        if (customToast == null) {
            customToast = new Toast(mContext);
        }
        customToast.setDuration(Toast.LENGTH_SHORT);
        customToast.setGravity(gravity, xOffset, yOffset);
        View view = LayoutInflater.from(mContext).inflate(layout, null);
        if (onInit != null) {
            onInit.onInit(view);
        }
        customToast.setView(view);
        customToast.show();
    }

    /**
     * 初始化自定义Toast
     *
     * @author lichenyang
     */
    public interface OnInitToast {
        void onInit(View view);
    }




}