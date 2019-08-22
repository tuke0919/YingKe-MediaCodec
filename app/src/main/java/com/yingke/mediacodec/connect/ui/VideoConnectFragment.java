package com.yingke.mediacodec.connect.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.yingke.mediacodec.R;
import com.yingke.mediacodec.utils.ScreenUtils;
import com.yingke.mediacodec.utils.ToastUtil;
import com.yingke.mediacodec.widget.localmedia.adapter.BaseLocalMediaAdapter;
import com.yingke.mediacodec.widget.localmedia.config.LocalMediaConfig;
import com.yingke.mediacodec.widget.localmedia.config.MediaConfig;
import com.yingke.mediacodec.widget.localmedia.entity.LocalMediaFolder;
import com.yingke.mediacodec.widget.localmedia.entity.LocalMediaResource;
import com.yingke.mediacodec.widget.localmedia.manager.DataManager;
import com.yingke.mediacodec.widget.localmedia.mvp.ILocalMediaView;
import com.yingke.mediacodec.widget.localmedia.mvp.LocalMediaPresenter;
import com.yingke.mediacodec.widget.localmedia.views.GridSpacingItemDecoration;

import java.util.List;

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
public class VideoConnectFragment extends Fragment implements ILocalMediaView , BaseLocalMediaAdapter.OnLocalMediaListener {

    private View mRootView;
    private RecyclerView mRecyclerView;
    private BaseLocalMediaAdapter mMediaAdapter;

    // 媒体文件处理
    private LocalMediaPresenter mMediaPresenter;

    public static VideoConnectFragment newInstance() {
        return new VideoConnectFragment();
    }

    @Override
    public View onCreateView( LayoutInflater inflater,  ViewGroup container,  Bundle savedInstanceState) {
        if (mRootView == null) {
            mRootView = inflater.inflate(R.layout.activity_media_codec_video_connect, null);
            initViews();
            initDatas();
        }

        ViewGroup parent = (ViewGroup) mRootView.getParent();
        if (parent != null) {
            parent.removeView(mRootView);
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    public void initViews() {
        mRecyclerView = mRootView.findViewById(R.id.recyclerview);
    }

    public void initDatas() {
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(new GridSpacingItemDecoration(4, ScreenUtils.dip2px(getContext(), 2), false));
        mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 4));

        mMediaAdapter = new BaseLocalMediaAdapter(getContext());
        mMediaAdapter.setLocalMediaListener(this);
        mRecyclerView.setAdapter(mMediaAdapter);

        LocalMediaConfig config = LocalMediaConfig.getInstance().setMediaType(MediaConfig.MediaType.MEDIA_TYPE_VIDEO)
                .setSelectionMode(MediaConfig.SelectionMode.MULTI_SELECTION);
        getMediaPresenter().requestMedias();
    }

    @Override
    public void onViewCreated( View view,  Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    /**
     * @return 媒体处理
     */
    private LocalMediaPresenter getMediaPresenter() {
        if (mMediaPresenter == null) {
            mMediaPresenter = new LocalMediaPresenter(getActivity(), this);
        }
        return mMediaPresenter;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onLocalMediaLoaded(List<LocalMediaFolder> localMediaFolders) {
        List<LocalMediaResource> currentMedias = localMediaFolders.get(0).getFolderMedias();
        DataManager.getInstance().setCurrentFolderMedias(currentMedias);
        mMediaAdapter.setDataList(DataManager.getInstance().getCurrentFolderMedias());
        mMediaAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLocalMediaErr(String message) {
        ToastUtil.showToastShort(message);
    }

    @Override
    public void onTakePhotoOrRecord() {

    }

    @Override
    public void onSelectMediaChanged(List<LocalMediaResource> selectImages) {

    }

    @Override
    public void onLocalMediaClick(LocalMediaResource media, int position, MediaConfig.MediaType mediaType) {

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getMediaPresenter() != null) {
            getMediaPresenter().onDestroy();
        }
        // 清空数据
        DataManager.getInstance().clear();
    }
}
