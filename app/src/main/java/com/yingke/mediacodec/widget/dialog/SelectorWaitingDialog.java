package com.yingke.mediacodec.widget.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;

import com.yingke.mediacodec.R;


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
public class SelectorWaitingDialog extends Dialog {

    public Context context;

    public SelectorWaitingDialog(Context context) {
        super(context, R.style.selector_alert_dialog);
        this.context = context;
        setCancelable(true);
        setCanceledOnTouchOutside(false);
        Window window = getWindow();
        window.setWindowAnimations(R.style.DialogWindowStyle);
    }


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.selector_alert_dialog_layout);
    }
}
