package com.yingke.mediacodec.widget.localmedia.mvp;

import android.app.Activity;

import com.yingke.mediacodec.widget.localmedia.entity.LocalMediaFolder;

import java.util.List;

/**
 * 功能：加载本地媒体文件 图片， 视频，音频
 * </p>
 * <p>Copyright corp.netease.com 2019 All right reserved </p>
 *
 * @author tuke 时间 2019/8/22
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class LocalMediaPresenter {

    private ILocalMediaView mLocalMediaView;
    private LocalMediaModel mLocalMediaModel;

    public LocalMediaPresenter(Activity activity, ILocalMediaView localMediaView) {
        mLocalMediaView = localMediaView;
        mLocalMediaModel = new LocalMediaModel(activity, new Callback() {
            @Override
            public void onLocalMediaLoaded(List<LocalMediaFolder> localMediaFolders) {
                  if (mLocalMediaView != null) {
                      mLocalMediaView.onLocalMediaLoaded(localMediaFolders);
                  }
            }

            @Override
            public void onLocalMediaErr(String message) {
                if (mLocalMediaView != null) {
                    mLocalMediaView.onLocalMediaErr(message);
                }
            }
        });
    }

    /**
     * 销毁
     */
    public void onDestroy() {
        if (mLocalMediaView != null) {
            mLocalMediaView = null;
        }
        if (mLocalMediaModel != null) {
            mLocalMediaModel.onDestroy();
        }

    }

    public interface Callback{

        /**
         * @param localMediaFolders
         */
        void onLocalMediaLoaded(List<LocalMediaFolder> localMediaFolders);

        /**
         * @param message
         */
        void onLocalMediaErr(String message);
    }
}
