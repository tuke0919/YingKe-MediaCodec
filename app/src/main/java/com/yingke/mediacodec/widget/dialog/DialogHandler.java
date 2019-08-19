package com.yingke.mediacodec.widget.dialog;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * 功能：
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/5/28
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class DialogHandler extends Handler {
    public static final int DIALOG_SHOW = 0;
    public static final int DIALOG_DISMISS = 1;

    private IDialog dialog;

    public DialogHandler(IDialog dialog) {
        super(Looper.getMainLooper());
        this.dialog = dialog;
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what) {
            case DIALOG_SHOW:
                showPleaseDialog();
                break;
            case DIALOG_DISMISS:
                dismissDialog();
                break;
        }
    }

    /**
     * loading dialog
     */
    protected void showPleaseDialog() {
       if (dialog != null) {
           dialog.showWaitingDialog();
       }
    }

    /**
     * dismiss dialog
     */
    protected void dismissDialog() {
        if (dialog != null) {
            dialog.dismissWaitingDialog();
        }
    }

    public interface IDialog{
        /**
         * 显示Dialog
         */
        public void showWaitingDialog();

        /**
         * 消失Dialog
         */
        public void dismissWaitingDialog();
    }


}
