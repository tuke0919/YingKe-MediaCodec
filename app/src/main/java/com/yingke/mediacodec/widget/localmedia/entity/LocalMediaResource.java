package com.yingke.mediacodec.widget.localmedia.entity;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.yingke.mediacodec.widget.localmedia.config.MediaConfig;


/**
 * 功能：本地媒体资源
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/5/24
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class LocalMediaResource implements Parcelable {

    /**
     * 媒体类型 {@link MediaConfig.MediaType}
     */
    private MediaConfig.MediaType mediaType;

    /**
     * mimeType {@link MediaConfig.MimeType}
     */
    private String mimeType;

    // 媒体路径， image类型 就是图片，video类型 是缩略图
    private String mediaPath;

    // 宽
    private int width;
    // 高
    private int height;

    // 时长 - 音视频
    private long duration;


    // 列表位置
    public int positionInRecyclerView;

    // 被选中
    private boolean isChecked;
    // 第几个选中
    private int selectedNumber;

    // 被压缩
    private boolean isCompressed;
    // 压缩路径
    private String compressPath;
    // 被裁剪
    private boolean isCroped;
    // 裁剪路径
    private String cropPath;




    public MediaConfig.MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaConfig.MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public String getMediaPath() {
        return mediaPath;
    }

    public void setMediaPath(String mediaPath) {
        this.mediaPath = mediaPath;
    }

    public int getPositionInRecyclerView() {
        return positionInRecyclerView;
    }

    public void setPositionInRecyclerView(int positionInRecyclerView) {
        this.positionInRecyclerView = positionInRecyclerView;
    }

    public int getSelectedNumber() {
        return selectedNumber;
    }

    public void setSelectedNumber(int selectedNumber) {
        this.selectedNumber = selectedNumber;
    }

    public String getMimeType() {
        if (TextUtils.isEmpty(mimeType)) {
            mimeType = "image/jpeg";
        }
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isCompressed() {
        return isCompressed;
    }

    public void setCompressed(boolean compressed) {
        isCompressed = compressed;
    }

    public String getCompressPath() {
        return compressPath;
    }

    public void setCompressPath(String compressPath) {
        this.compressPath = compressPath;
    }

    public boolean isCroped() {
        return isCroped;
    }

    public void setCroped(boolean croped) {
        isCroped = croped;
    }

    public String getCropPath() {
        return cropPath;
    }

    public void setCropPath(String cropPath) {
        this.cropPath = cropPath;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public static Creator<LocalMediaResource> getCREATOR() {
        return CREATOR;
    }

    public LocalMediaResource() {
    }



    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mediaType == null ? -1 : this.mediaType.ordinal());
        dest.writeByte(this.isChecked ? (byte) 1 : (byte) 0);
        dest.writeString(this.mediaPath);
        dest.writeInt(this.positionInRecyclerView);
        dest.writeInt(this.selectedNumber);
        dest.writeString(this.mimeType);
        dest.writeInt(this.width);
        dest.writeInt(this.height);
        dest.writeByte(this.isCompressed ? (byte) 1 : (byte) 0);
        dest.writeString(this.compressPath);
        dest.writeByte(this.isCroped ? (byte) 1 : (byte) 0);
        dest.writeString(this.cropPath);
        dest.writeLong(this.duration);
    }

    protected LocalMediaResource(Parcel in) {
        int tmpMediaType = in.readInt();
        this.mediaType = tmpMediaType == -1 ? null : MediaConfig.MediaType.values()[tmpMediaType];
        this.isChecked = in.readByte() != 0;
        this.mediaPath = in.readString();
        this.positionInRecyclerView = in.readInt();
        this.selectedNumber = in.readInt();
        this.mimeType = in.readString();
        this.width = in.readInt();
        this.height = in.readInt();
        this.isCompressed = in.readByte() != 0;
        this.compressPath = in.readString();
        this.isCroped = in.readByte() != 0;
        this.cropPath = in.readString();
        this.duration = in.readLong();
    }

    public static final Creator<LocalMediaResource> CREATOR = new Creator<LocalMediaResource>() {
        @Override
        public LocalMediaResource createFromParcel(Parcel source) {
            return new LocalMediaResource(source);
        }

        @Override
        public LocalMediaResource[] newArray(int size) {
            return new LocalMediaResource[size];
        }
    };
}
