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
public abstract class SingleFragmentActivity extends AppCompatActivity {

    protected Toolbar toolbar = null;

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

    /**
     * 初始化toolbar
     */
    protected void initActionbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        if (toolbar == null) {
            throw new IllegalStateException("Toolbar_actionbar toolbar has not be found in layout,be sure you have define toolbar in the layout");
        } else {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setBackgroundResource(getActionBarBg());
            toolbar.setNavigationIcon(R.mipmap.left_back);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBack(v);
                }
            });
            toolbar.setTitleTextAppearance(this, R.style.TitleTextStyle);
        }
    }

    public void onBack(View view) {
        onBackPressed();
    }

    protected int getActionBarBg() {
        return R.drawable.toolbar_bg;
    }

    /**
     * @return  碎片
     */
    protected abstract Fragment createFragment();


}
