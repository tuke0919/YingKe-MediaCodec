package com.yingke.mediacodec;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.yingke.mediacodec.compose.ui.MediaCodecAudioComposeActivity;
import com.yingke.mediacodec.connect.ui.MediaCodecVideoConnectActivity;
import com.yingke.mediacodec.simple.MediaMuxerActivity;
import com.yingke.mediacodec.player.MediaCodecPlayerActivity;
import com.yingke.mediacodec.recorder.MediaCodecRecorderActivity;
import com.yingke.mediacodec.transcode.ui.MediaCodecTransCodeActivity;

import java.util.ArrayList;

public class MainActivity extends BaseActivity implements View.OnClickListener {

    private RecyclerView mFunctionList;
    private ArrayList<MenuBean> data;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFunctionList = (RecyclerView)findViewById(R.id.main_recyclerview);
        mFunctionList.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false));
        data = new ArrayList<>();
        add("MediaCodec下载网络视频", MediaMuxerActivity.class);
        add("MediaCodec播放器", MediaCodecPlayerActivity.class);
        add("MediaCodec录视频", MediaCodecRecorderActivity.class);
        add("MediaCodec转码", MediaCodecTransCodeActivity.class);
        add("MediaCodec音频分离和混合", MediaCodecAudioComposeActivity.class);
        add("MediaCodec多视频拼接", MediaCodecVideoConnectActivity.class);

        mFunctionList.setAdapter(new MenuAdapter());
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
     * @param name
     * @param clazz
     */
    private void add(String name, Class<?> clazz){
        MenuBean bean=new MenuBean();
        bean.name=name;
        bean.clazz=clazz;
        data.add(bean);
    }

    /**
     * 数据结构
     */
    private class MenuBean{
        public String name;
        public Class<?> clazz;

    }

    private class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.MenuHolder>{


        @Override
        public MenuHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new MenuHolder(getLayoutInflater().inflate(R.layout.main_button,parent,false));
        }

        @Override
        public void onBindViewHolder(MenuHolder holder, int position) {
            holder.setPosition(position);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class MenuHolder extends RecyclerView.ViewHolder{

            private Button mBtn;

            MenuHolder(View itemView) {
                super(itemView);
                mBtn= (Button)itemView.findViewById(R.id.main_function_btn);
                mBtn.setOnClickListener(MainActivity.this);
            }

            public void setPosition(int position){
                MenuBean bean=data.get(position);
                mBtn.setText(bean.name);
                mBtn.setTag(position);
            }
        }

    }

    @Override
    public void onClick(View view){
        int position= (int)view.getTag();
        MenuBean bean=data.get(position);
        startActivity(new Intent(this,bean.clazz));
    }

}
