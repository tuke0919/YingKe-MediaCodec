package com.yingke.mediacodec;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

/**
 * 功能：
 * </p>
 * <p>Copyright corp.netease.com 2019 All right reserved </p>
 *
 * @author tuke 时间 2019/8/22
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public abstract class SingleFragmentActivity extends BaseActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_fragment);
        initActionbar();

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.id_fragment_container);
        if(fragment == null ) {
            fragment = createFragment();
            fm.beginTransaction().add(R.id.id_fragment_container,fragment).commit();
        }
    }

    @Override
    public boolean hasToolbar() {
        return true;
    }

    @Override
    protected boolean isTransStatusBar() {
        return true;
    }

    /**
     * @return  碎片
     */
    protected abstract Fragment createFragment();


}
