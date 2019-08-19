package com.yingke.mediacodec.widget.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;


/**
 * 功能：dialog 管理器
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/5/28
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class DialogManager implements DialogHandler.IDialog{

    private DialogHandler dialogHandler;
    private Context context;
    private Dialog dialog;

    public DialogManager() {
        dialogHandler = new DialogHandler(this);
    }

    public static DialogManager newInstance() {
        return new DialogManager();
    }

    /**
     * 显示dialog
     * @param context
     */
    public void showDialog(Context context) {
        this.context = context;
        if (dialogHandler != null) {
            dialogHandler.sendEmptyMessage(DialogHandler.DIALOG_SHOW);
        }
    }

    /**
     * 隐藏dialog
     */
    public void dismissDialog() {
        if (dialogHandler != null) {
            dialogHandler.sendEmptyMessage(DialogHandler.DIALOG_DISMISS);
        }
    }

    /**
     * 设置自定义dialog
     * @param dialog
     */
    public void setCustomDialog(Dialog dialog) {
        this.dialog = dialog;
    }

    @Override
    public void showWaitingDialog() {
        if (context != null && !((Activity)context).isFinishing()) {
            dismissWaitingDialog();
            if (dialog == null) {
                dialog = new SelectorWaitingDialog(context);
            }
            dialog.show();
        }
    }

    @Override
    public void dismissWaitingDialog() {
        try {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
