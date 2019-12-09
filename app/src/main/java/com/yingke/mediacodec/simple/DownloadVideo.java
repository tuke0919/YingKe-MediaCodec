package com.yingke.mediacodec.simple;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Set;

/**
 * 功能：MediaExtractor + MediaMuxer  分离抽取 ，合成视频
 * </p>
 * <p>Copyright corp.xxx.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/8/6
 * @email
 * <p>
 * 最后修改人：无
 * <p>
 */
public class DownloadVideo {
    private static String TAG = DownloadVideo.class.getSimpleName();

    private String mVideoNetworkUrl;
    private String mVideoOutputPath ;

    private boolean mOutputFileSuccess;

    private Thread mMuxerThread;

    public DownloadVideo() {

    }

    public DownloadVideo(String mVideoNetworkUrl, String mVideoOutputPath) {
        this.mVideoNetworkUrl = mVideoNetworkUrl;
        this.mVideoOutputPath = mVideoOutputPath;

        File outputFile = new File(mVideoOutputPath);
        if (outputFile.exists()) {
            outputFile.delete();
        }
        try {
            mOutputFileSuccess = outputFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.e(TAG, "doDownload : mVideoNetworkUrl =  " + mVideoNetworkUrl + " mVideoOutputPath = " + mVideoOutputPath );

        if (mCallback != null) {
            mCallback.onTextCallback("mVideoNetworkUrl =  " + mVideoNetworkUrl);
            mCallback.onTextCallback("mVideoOutputPath =  " + mVideoOutputPath);
        }

    }

    /**
     * 开始
     */
    public void start() {
        if (!mOutputFileSuccess) {
            Log.e(TAG, "start : create output file error " );

            if (mCallback != null) {
                mCallback.onTextCallback("start : create output file error ");
            }

            return;
        }

        mMuxerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    doDownload();

                } catch (InterruptedIOException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        mMuxerThread.start();
    }


    /**
     * @throws IOException
     */
    private void doDownload() throws IOException {
        int maxVideoIntputSize = 0;
        int maxAudioIntputSize = 0;
        int videoFrameRate = 0;

        // 复用器 video 轨道
        int videoMuxerTrackIndex = -1;
        // 复用器 audio 轨道
        int audioMuxerTrackIndex = -1;

        // 创建复用器
        MediaMuxer mediaMuxer = new MediaMuxer(mVideoOutputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        // 创建视频抽取器
        MediaExtractor videoExtractor = createExtractor();
        // 获取并选择视频轨道
        int videoExtractorTrackIndex = getAndSelectTrackIndex(videoExtractor, true);

        // 创建音频抽取器
        MediaExtractor audioExtractor = createExtractor();
        // 获取并选择音频轨道
        int audioExtractorTrackIndex = getAndSelectTrackIndex(audioExtractor, false);

        if (videoExtractorTrackIndex != -1) {
            // 获取视频轨道 媒体格式
            MediaFormat videoTrackFormat = videoExtractor.getTrackFormat(videoExtractorTrackIndex);

            Log.e(TAG, "doDownload : videoTrackFormat =  " );
            if (mCallback != null) {
                mCallback.onTextCallback("doDownload : videoTrackFormat = ");
            }

            outputMediaFormat(videoTrackFormat);
            // 复用器添加视频媒体格式，并返回复用器视频轨道
            videoMuxerTrackIndex = mediaMuxer.addTrack(videoTrackFormat);

            // 抽取器 视频轨道媒体格式数据

            // 最大视频大小
            maxVideoIntputSize = videoTrackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
            // 视频帧率
            videoFrameRate = videoTrackFormat.getInteger(MediaFormat.KEY_FRAME_RATE);

            Log.e(TAG, "doDownload :"
                    + " maxVideoIntputSize =  "  + maxVideoIntputSize
                    + " videoFrameRate = " + videoFrameRate
                    + " videoMuxerTrackIndex = " + videoMuxerTrackIndex);

            if (mCallback != null) {
                mCallback.onTextCallback("doDownload: ");
                mCallback.onTextCallback("maxVideoIntputSize =  "  + maxVideoIntputSize);
                mCallback.onTextCallback("videoFrameRate = " + videoFrameRate);
                mCallback.onTextCallback("videoMuxerTrackIndex = " + videoMuxerTrackIndex);
            }
        }

        if (audioExtractorTrackIndex != -1) {
            // 获取音频轨道 媒体格式
            MediaFormat audioTrackFormat = audioExtractor.getTrackFormat(audioExtractorTrackIndex);

            Log.e(TAG, "doDownload : audioTrackFormat =  " );
            if (mCallback != null) {
                mCallback.onTextCallback("doDownload : videoTrackFormat = ");
            }

            outputMediaFormat(audioTrackFormat);
            // 复用器添加音频媒体格式，并返回复用器音频轨道
            audioMuxerTrackIndex = mediaMuxer.addTrack(audioTrackFormat);

            // 最大音频输入大小
            maxAudioIntputSize = audioTrackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);

            Log.e(TAG, "doDownload :"
                    + " audioMuxerTrackIndex =  "  + audioMuxerTrackIndex
                    + " maxAudioIntputSize = " + maxAudioIntputSize);

            if (mCallback != null) {
                mCallback.onTextCallback("doDownload: ");
                mCallback.onTextCallback("audioMuxerTrackIndex =  "  + audioMuxerTrackIndex);
                mCallback.onTextCallback("maxAudioIntputSize = " + maxAudioIntputSize);
            }
        }

        Log.e(TAG, "doDownload : mediaMuxer.start()" );
        if (mCallback != null) {
            mCallback.onTextCallback("doDownload : mediaMuxer.start() ");
        }


        // 开始合成
        mediaMuxer.start();

        // 循环直接写 视频到 muxer复用器
        if (videoMuxerTrackIndex != -1) {
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            bufferInfo.presentationTimeUs = 0;

            ByteBuffer buffer = ByteBuffer.allocate(maxVideoIntputSize);
            while (!mMuxerThread.isInterrupted()) {
                // 抽取器 读数据到buffer
                int sampleSize = videoExtractor.readSampleData(buffer, 0);
                if (sampleSize < 0) {
                    break;
                }
                bufferInfo.offset = 0;
                bufferInfo.size = sampleSize;
                bufferInfo.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;
                bufferInfo.presentationTimeUs += 1000 * 1000 / videoFrameRate;

                Log.e(TAG, "doDownload : videoMuxer = "
                        + " offset = " + bufferInfo.offset
                        + " size = " + bufferInfo.size
                        + " flags = " + bufferInfo.flags
                        + " presentationTimeUs = " + bufferInfo.presentationTimeUs);

                if (mCallback != null) {
                    mCallback.onTextCallback("doDownload: videoMuxer =  ");
                    mCallback.onTextCallback("offset = " + bufferInfo.offset);
                    mCallback.onTextCallback("size = " + bufferInfo.size);
                    mCallback.onTextCallback("flags = " + bufferInfo.flags);
                    mCallback.onTextCallback("presentationTimeUs = " + bufferInfo.presentationTimeUs);
                }
                // 把buffer数据 直接写到 复用器，不经任何编码，解码
                mediaMuxer.writeSampleData(videoMuxerTrackIndex, buffer, bufferInfo);

                // 判断抽取器 是否读完
                boolean videoExtractorDone = videoExtractor.advance();
                if (!videoExtractorDone) {
                    break;
                }
            }
        }
        Log.e(TAG, "doDownload : videoMuxer end" );
        if (mCallback != null) {
            mCallback.onTextCallback("doDownload : videoMuxer end ");
        }

        // 循环写音频 到muxer复用器
        if (audioMuxerTrackIndex != -1) {
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            bufferInfo.presentationTimeUs = 0;

            ByteBuffer buffer = ByteBuffer.allocate(maxAudioIntputSize);
            while (!mMuxerThread.isInterrupted()) {
                // 抽取器 读数据到buffer
                int sampleSize = audioExtractor.readSampleData(buffer, 0);
                if (sampleSize < 0) {
                    break;
                }
                bufferInfo.offset = 0;
                bufferInfo.size = sampleSize;
                bufferInfo.flags = audioExtractor.getSampleFlags();
                bufferInfo.presentationTimeUs = audioExtractor.getSampleTime();

                Log.e(TAG, "doDownload : audioMuxer = "
                        + " offset = " + bufferInfo.offset
                        + " size = " + bufferInfo.size
                        + " flags = " + bufferInfo.flags
                        + " presentationTimeUs = " + bufferInfo.presentationTimeUs);

                if (mCallback != null) {
                    mCallback.onTextCallback("doDownload: audioMuxer =  ");
                    mCallback.onTextCallback("offset = " + bufferInfo.offset);
                    mCallback.onTextCallback("size = " + bufferInfo.size);
                    mCallback.onTextCallback("flags = " + bufferInfo.flags);
                    mCallback.onTextCallback("presentationTimeUs = " + bufferInfo.presentationTimeUs);
                }

                // 把buffer数据 直接写到 复用器，不经任何编码，解码
                mediaMuxer.writeSampleData(audioMuxerTrackIndex, buffer, bufferInfo);
                // 判断抽取器 是否读完
                boolean audioExtractorDone = audioExtractor.advance();
                if (!audioExtractorDone) {
                    break;
                }
            }
        }
        Log.e(TAG, "doDownload : audioMuxer end" );
        if (mCallback != null) {
            mCallback.onTextCallback("doDownload : audioMuxer end ");
        }
        // 释放抽取器
        videoExtractor.release();
        audioExtractor.release();
        // 释放复用器
        mediaMuxer.stop();
        mediaMuxer.release();

    }

    /**
     * 创建收抽取器
     * @return
     * @throws IOException
     */
    public MediaExtractor createExtractor() throws IOException {
        MediaExtractor mediaExtractor = new MediaExtractor();
        mediaExtractor.setDataSource(mVideoNetworkUrl);
        return mediaExtractor;
    }

    /**
     * 获取并且选择 轨道
     * @param mediaExtractor
     * @param isVideo
     * @return
     */
    public int getAndSelectTrackIndex(MediaExtractor mediaExtractor, boolean isVideo) {

        for (int index = 0 ; index < mediaExtractor.getTrackCount(); index ++) {
            MediaFormat mediaFormat  = mediaExtractor.getTrackFormat(index);

            Log.e(TAG, "getAndSelectTrackIndex " + " index = " + index);
            outputMediaFormat(mediaFormat);

            if (isVideo) {
                if (isVideoTrack(mediaFormat)) {
                    mediaExtractor.selectTrack(index);
                    return index;
                }
            } else {
                if (isAudioTrack(mediaFormat)) {
                    mediaExtractor.selectTrack(index);
                    return index;
                }
            }
        }
        return -1;
    }

    /**
     * @param mediaFormat
     */
    private void outputMediaFormat(MediaFormat mediaFormat) {
        Class clazz = MediaFormat.class;
        try {
            Method getMap = clazz.getDeclaredMethod("getMap");
            //获取私有权限
            getMap.setAccessible(true);
            HashMap<String, Object> map = (HashMap<String, Object>) getMap.invoke(mediaFormat);

            if (map != null) {
                Set<String> keySet = map.keySet();
                for (String key : keySet) {
                     Object value = map.get(key);
                     String strValue = "";
                     if (value instanceof Integer) {
                         strValue = ((Integer) value).intValue() + "";
                     } else if (value instanceof Float) {
                         strValue = ((Float) value).floatValue() + "";
                     } else if (value instanceof Long) {
                         strValue = ((Long) value).longValue() + "";
                     } else if (value instanceof String){
                         strValue = (String) value;
                     }

                    Log.e(TAG, "outputMediaFormat " + " key = " + key + " value = " + value);
                    if (mCallback != null) {
                        mCallback.onTextCallback("outputMediaFormat " + " key = " + key + " value = " + value);
                    }
                }
            }

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * 是否 video轨道
     * @param mediaFormat
     * @return
     */
    public boolean isVideoTrack(MediaFormat mediaFormat) {
        return mediaFormat != null && mediaFormat.getString(MediaFormat.KEY_MIME).startsWith("video/");
    }

    /**
     * 是否 audio 轨道
     * @param mediaFormat
     * @return
     */
    public boolean isAudioTrack(MediaFormat mediaFormat) {
        return mediaFormat != null && mediaFormat.getString(MediaFormat.KEY_MIME).startsWith("audio/");
    }

    private OnProgressCallback mCallback;

    public void setCallback(OnProgressCallback mCallback) {
        this.mCallback = mCallback;
    }

    public interface OnProgressCallback{
        /**
         * @param text
         */
        void onTextCallback(String text);
    }

    public void onDestroy() {
        if (mMuxerThread != null) {
            mMuxerThread.interrupt();
        }
    }


}
