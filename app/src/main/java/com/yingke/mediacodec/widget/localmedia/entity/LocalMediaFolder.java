package com.yingke.mediacodec.widget.localmedia.entity;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * 功能：相册文件夹
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/5/24
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class LocalMediaFolder implements Parcelable {
    // 文件夹名称
    private String folderName;
    // 文件夹路径
    private String folderPath;
    // 第一张图片路径
    private String firstImagePath;
    // 图片数目
    private int imageNum;
    // 选中数目
    private int checkedNum;
    // 是否被选中
    private boolean isChecked;
    // 文件夹中图片
    private List<LocalMediaResource> folderImages = new ArrayList<LocalMediaResource>();

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public String getFirstImagePath() {
        return firstImagePath;
    }

    public void setFirstImagePath(String firstImagePath) {
        this.firstImagePath = firstImagePath;
    }

    public int getImageNum() {
        return imageNum;
    }

    public void setImageNum(int imageNum) {
        this.imageNum = imageNum;
    }

    public int getCheckedNum() {
        return checkedNum;
    }

    public void setCheckedNum(int checkedNum) {
        this.checkedNum = checkedNum;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public List<LocalMediaResource> getFolderImages() {
        return folderImages;
    }

    public void setFolderImages(List<LocalMediaResource> folderImages) {
        this.folderImages = folderImages;
    }

    public static Creator<LocalMediaFolder> getCREATOR() {
        return CREATOR;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.folderName);
        dest.writeString(this.folderPath);
        dest.writeString(this.firstImagePath);
        dest.writeInt(this.imageNum);
        dest.writeInt(this.checkedNum);
        dest.writeByte(this.isChecked ? (byte) 1 : (byte) 0);
        dest.writeTypedList(this.folderImages);
    }

    public LocalMediaFolder() {
    }

    protected LocalMediaFolder(Parcel in) {
        this.folderName = in.readString();
        this.folderPath = in.readString();
        this.firstImagePath = in.readString();
        this.imageNum = in.readInt();
        this.checkedNum = in.readInt();
        this.isChecked = in.readByte() != 0;
        this.folderImages = in.createTypedArrayList(LocalMediaResource.CREATOR);
    }

    public static final Creator<LocalMediaFolder> CREATOR = new Creator<LocalMediaFolder>() {
        @Override
        public LocalMediaFolder createFromParcel(Parcel source) {
            return new LocalMediaFolder(source);
        }

        @Override
        public LocalMediaFolder[] newArray(int size) {
            return new LocalMediaFolder[size];
        }
    };
}
