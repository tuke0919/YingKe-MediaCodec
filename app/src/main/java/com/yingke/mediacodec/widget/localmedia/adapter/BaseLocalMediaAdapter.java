package com.yingke.mediacodec.widget.localmedia.adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.PorterDuff;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.yingke.mediacodec.R;
import com.yingke.mediacodec.utils.ToastUtil;
import com.yingke.mediacodec.widget.localmedia.anim.OptAnimationLoader;
import com.yingke.mediacodec.widget.localmedia.config.LocalMediaConfig;
import com.yingke.mediacodec.widget.localmedia.config.MediaConfig;
import com.yingke.mediacodec.widget.localmedia.config.MediaMimeType;
import com.yingke.mediacodec.widget.localmedia.entity.LocalMediaResource;
import com.yingke.mediacodec.widget.localmedia.manager.DataManager;


import java.io.File;
import java.util.List;



/**
 * 功能：本地媒体列表 的适配器
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/5/28
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class BaseLocalMediaAdapter extends BaseRecycleViewAdapter<LocalMediaResource> {
    private final static int DURATION = 450;
    private Animation animation;
    private boolean isZoomAnim = true;

    public BaseLocalMediaAdapter(Context context) {
        this(context, null);
    }

    public BaseLocalMediaAdapter(Context context, List<LocalMediaResource> mDataList) {
        super(context, mDataList);
        animation = OptAnimationLoader.loadAnimation(context, R.anim.modal_in);
    }

    @Override
    public int getConvertViewResId(int itemViewType) {
        return itemViewType == CameraHolderView.VIEW_TYPE ? getCameraHolderResId() : getMediaHolderResId();
    }

    @Override
    public RecyclerView.ViewHolder getViewHolder(int viewType, View rootView) {
        return viewType == CameraHolderView.VIEW_TYPE ? getCameraHolderView(rootView) : getMediaHolderView(rootView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position == 0) {
            return;
        }
        super.onBindViewHolder(holder, position - 1);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return CameraHolderView.VIEW_TYPE;
        } else {
            return MediaHolderView.VIEW_TYPE;
        }
    }

    @Override
    public LocalMediaResource getItem(int position) {
        return super.getItem(position);
    }

    @Override
    public int getItemCount() {
        return super.getItemCount() + 1;
    }

    /**
     * @return
     */
    protected int getCameraHolderResId() {
        return R.layout.activity_picture_grid_item_camera;
    }

    /**
     * @return
     */
    protected int getMediaHolderResId() {
       return R.layout.activity_picture_grid_item_base;
    }

    /**
     * @param rootView
     * @return
     */
    protected RecyclerView.ViewHolder getCameraHolderView(View rootView){
        return new CameraHolderView(rootView);
    }

    /**
     * @param rootView
     * @return
     */
    protected RecyclerView.ViewHolder getMediaHolderView(View rootView){
        return new MediaHolderView(rootView);
    }


    /**
     * 相机holder
     */
    public class CameraHolderView extends BaseViewHolder<LocalMediaResource> {

        public static final int VIEW_TYPE = 1;
        public ImageView cameraIcon;
        public TextView titleCamera;

        public CameraHolderView(View itemView) {
            super(itemView);
            cameraIcon = itemView.findViewById(R.id.selector_item_camera);
            titleCamera = itemView.findViewById(R.id.selector_item_camera_title);
            String title = LocalMediaConfig.getInstance().getMediaType() == MediaMimeType.ofAudio() ?
                    context.getString(R.string.selector_tape) :
                    context.getString(R.string.selector_take_picture);
            titleCamera.setText(title);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mLocalMediaListener != null) {
                        mLocalMediaListener.onTakePhotoOrRecord();
                    }
                }
            });
        }

        @Override
        public void onRefreshData(int position, LocalMediaResource data) {

        }
    }

    /**
     * 本地媒体 Holder
     */
    public class MediaHolderView extends BaseViewHolder<LocalMediaResource> {
        public static final int VIEW_TYPE = 2;

        public ImageView imageView;
        // check
        public LinearLayout checkLayout;
        public TextView checkTv;

        public LocalMediaResource data;
        public int positionInRecycleView;

        public MediaHolderView(View itemView) {
            super(itemView);
            imageView = (ImageView) findViewById(R.id.selector_item_picture);
            checkLayout = (LinearLayout) findViewById(R.id.selector_item_check_layout);
            checkTv = (TextView) findViewById(R.id.selector_item_check);
        }

        @Override
        public void onRefreshData(final int position, final LocalMediaResource data) {
            if (data == null) {
                return;
            }
            this.positionInRecycleView = position + 1;
            this.data = data;
            data.setPositionInRecyclerView(positionInRecycleView);

            final String path = data.getMediaPath();
            final String mimeType = data.getMimeType();
            final boolean isMultiSelection = (LocalMediaConfig.getInstance().getSelectionMode() == MediaConfig.SelectionMode.MULTI_SELECTION);


            final MediaConfig.MediaType mediaType  = MediaMimeType.judgeMediaType(mimeType);
            if (mediaType == MediaMimeType.ofAudio()) {
                // 音频
                imageView.setImageResource(R.drawable.item_audio_placeholder);

            } else if (mediaType == MediaMimeType.ofVideo()){
                // 视频
                RequestOptions options = new RequestOptions()
                        .centerCrop()
                        .placeholder(R.color.color_f6f6f6)
                        .diskCacheStrategy(DiskCacheStrategy.ALL);
                Glide.with(itemView.getContext())
                        .asBitmap()
                        .load(path)
                        .apply(options)
                        .into(imageView);

            } else {
                // 纯图片
                RequestOptions options = new RequestOptions()
                        .centerCrop()
                        .placeholder(R.color.color_f6f6f6)
                        .diskCacheStrategy(DiskCacheStrategy.ALL);
                Glide.with(itemView.getContext())
                        .asBitmap()
                        .load(path)
                        .apply(options)
                        .into(imageView);

            }
            // 选择
            if(isMultiSelection) {
                // 多选
                checkLayout.setVisibility(View.VISIBLE);
                checkLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // 如原图路径不存在或者路径存在但文件不存在
                        if (!new File(path).exists()) {
                            ToastUtil.showToastShort( MediaMimeType.error(context, mediaType));
                            return;
                        }
                        // 检查选中状态
                        changeCheckboxState(MediaHolderView.this, data);
                    }
                });

                // 已选中的 选中
                selectLocalMedia(this, data.isChecked(), false);

            } else {
                // 单选
                checkLayout.setVisibility(View.GONE);
            }

            // 根布局点击
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    // 如原图路径不存在或者路径存在但文件不存在
                    if (!new File(path).exists()) {
                        ToastUtil.showToastShort( MediaMimeType.error(context, mediaType));
                        return;
                    }
                    // 预览
                    if(mLocalMediaListener != null){
                        mLocalMediaListener.onLocalMediaClick(data, position, mediaType);
                    }
                }
            });
        }
    }

    /**
     * 选中的图片并执行动画
     *
     * @param holder
     * @param isChecked
     * @param isAnim
     */
    public void selectLocalMedia(MediaHolderView holder, boolean isChecked, boolean isAnim) {

        holder.checkTv.setSelected(isChecked);
        if (isChecked) {
            if (isAnim) {
                if (animation != null) {
                    holder.checkTv.startAnimation(animation);
                }
            }
            holder.imageView.setColorFilter(ContextCompat.getColor(context, R.color.image_overlay_true), PorterDuff.Mode.SRC_ATOP);
        } else {
            holder.imageView.setColorFilter(ContextCompat.getColor(context, R.color.image_overlay_false), PorterDuff.Mode.SRC_ATOP);
        }

        // 重新编号
        int size = DataManager.getInstance().selectedSize();
        for (int index = 0, length = size; index < length; index++) {
            LocalMediaResource mediaResource = DataManager.getInstance().getSelectedMedias().get(index);
            mediaResource.setSelectedNumber(index + 1);
        }
    }

    /**
     * 改变图片选中状态
     *
     * @param contentHolder
     * @param localMediaResource
     */
    private void changeCheckboxState(MediaHolderView contentHolder, LocalMediaResource localMediaResource) {

        boolean isChecked = contentHolder.checkTv.isSelected();
        if (isChecked) {
            // 已选中 -> 取消选中
            boolean removeSuccess = false;
            for (LocalMediaResource media : DataManager.getInstance().getSelectedMedias()) {
                if (media.getMediaPath().equals(localMediaResource.getMediaPath())) {
                    // 移除
                    removeSuccess = DataManager.getInstance().removeSelectedMedia(localMediaResource);
                    break;
                }
            }

            if (removeSuccess) {
                localMediaResource.setChecked(false);
                // 是否需要缩放
                if (isZoomAnim) {
                    disZoom(contentHolder.imageView);
                }
                //通知点击项发生了改变
                selectLocalMedia(contentHolder, !isChecked, true);
            }

        } else {
            // 未选中 -> 选中

            boolean success = DataManager.getInstance().addSelectedMedia(context, localMediaResource);
            if (success) {
                int size = DataManager.getInstance().selectedSize();
                localMediaResource.setChecked(true);
                localMediaResource.setSelectedNumber(size);

                // 是否需要缩放
                if (isZoomAnim) {
                    zoom(contentHolder.imageView);
                }
                //通知点击项发生了改变
                selectLocalMedia(contentHolder, !isChecked, true);
            }
        }

        if (mLocalMediaListener != null) {
            mLocalMediaListener.onSelectMediaChanged(DataManager.getInstance().getSelectedMedias());
        }
    }

    /**
     *  放大图片
     * @param iv_img
     */
    private void zoom(ImageView iv_img) {
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(iv_img, "scaleX", 1f, 1.12f),
                ObjectAnimator.ofFloat(iv_img, "scaleY", 1f, 1.12f)
        );
        set.setDuration(DURATION);
        set.start();
    }

    /**
     * 缩小图片
     * @param iv_img
     */
    private void disZoom(ImageView iv_img) {
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(iv_img, "scaleX", 1.12f, 1f),
                ObjectAnimator.ofFloat(iv_img, "scaleY", 1.12f, 1f)
        );
        set.setDuration(DURATION);
        set.start();
    }


    private OnLocalMediaListener mLocalMediaListener;

    /**
     * 设置监听器
     * @param localMediaListener
     */
    public void setLocalMediaListener(OnLocalMediaListener localMediaListener) {
        this.mLocalMediaListener = localMediaListener;
    }


    public interface OnLocalMediaListener {
        /**
         * 拍照，录视频，录音
         */
        void onTakePhotoOrRecord();

        /**
         * 返回所有的 选中media
         *
         * @param selectImages
         */
        void onSelectMediaChanged(List<LocalMediaResource> selectImages);

        /**
         * 媒体点击回调，可做预览等
         *
         * @param media
         * @param position
         */
        void onLocalMediaClick(LocalMediaResource media, int position, MediaConfig.MediaType mediaType);


    }


}
