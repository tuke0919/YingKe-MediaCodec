package com.yingke.mediacodec.widget.localmedia.mvp;

import android.Manifest;
import android.app.Activity;

import com.yingke.mediacodec.R;
import com.yingke.mediacodec.utils.ToastUtil;
import com.yingke.mediacodec.utils.permissions.RxPermissions;
import com.yingke.mediacodec.widget.dialog.DialogManager;
import com.yingke.mediacodec.widget.localmedia.LocalMediaLoader;
import com.yingke.mediacodec.widget.localmedia.entity.LocalMediaFolder;
import com.yingke.mediacodec.widget.localmedia.entity.LocalMediaResource;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

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
public class LocalMediaModel {

    private Activity context;

    // 当前相册媒体
    @Deprecated
    private List<LocalMediaResource> currentFolderMedias = new ArrayList<>();
    // 所有相册文件夹
    private List<LocalMediaFolder> imageFoldersList = new ArrayList<>();
    // 本地文件加载器
    private LocalMediaLoader localMediaLoader;

    private RxPermissions rxPermissions;
    private DialogManager dialogManager;

    private boolean dialog;

    // 回调
    private LocalMediaPresenter.Callback mCallback;

    public LocalMediaModel(Activity context, LocalMediaPresenter.Callback callback) {
        this.context = context;
        mCallback = callback;

        localMediaLoader = new LocalMediaLoader(context);
        rxPermissions = new RxPermissions(context);
        dialogManager = DialogManager.newInstance();
        dialog = true;
    }

    /**
     * 初始化数据
     */
    public void requestMedias(){
        rxPermissions.request(Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Boolean aBoolean) {
                        if (aBoolean) {
                            if (dialog) {
                                dialogManager.showDialog(context);
                            }
                            readLocalMedia();
                        } else {
                            ToastUtil.showToastShort(context.getString(R.string.selector_jurisdiction));
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });

    }

    /**
     * @param dialog
     */
    public void requestMedias(boolean dialog){
        this.dialog = dialog;
        requestMedias();

    }

    /**
     * 读本地媒体文件
     * @throws Exception
     */
    private void readLocalMedia() {
        try {
            localMediaLoader.loadMedia(new LocalMediaLoader.LocalMediaLoadCallback() {
                @Override
                public void loadCompleted(List<LocalMediaFolder> imageFoldersList) {
                    if (imageFoldersList.size() > 0) {
                        LocalMediaModel.this.imageFoldersList.clear();
                        LocalMediaModel.this.imageFoldersList.addAll(imageFoldersList);
                        LocalMediaFolder firstFolder = imageFoldersList.get(0);
                        firstFolder.setChecked(true);

                        List<LocalMediaResource> firstFolderImages = firstFolder.getFolderMedias();
                        if (firstFolderImages != null && firstFolderImages.size() > 0) {
                            currentFolderMedias.clear();
                            currentFolderMedias.addAll(firstFolderImages);
                        }
                        // 主线程
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (mCallback != null) {
                                    mCallback.onLocalMediaLoaded(LocalMediaModel.this.imageFoldersList);
                                }
                            }
                        });

                    } else {
                        // 主线程
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (mCallback != null) {
                                    mCallback.onLocalMediaErr("no medias");
                                }
                            }
                        });

                    }

                    if (dialog) {
                        dialogManager.dismissDialog();
                    }

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onDestroy() {
        if (mCallback != null) {
            mCallback = null;
        }
    }






}
