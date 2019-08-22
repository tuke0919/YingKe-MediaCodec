package com.yingke.mediacodec.widget.localmedia.mvp;


import com.yingke.mediacodec.widget.localmedia.entity.LocalMediaFolder;

import java.util.List;

/**
 * 功能：
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/5/24
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public interface ILocalMediaView  {


    /**
     * @param localMediaFolders
     */
    void onLocalMediaLoaded(List<LocalMediaFolder> localMediaFolders);

    /**
     * @param message
     */
    void onLocalMediaErr(String message);
}
