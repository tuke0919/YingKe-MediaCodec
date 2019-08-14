package com.yingke.mediacodec.transcode;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.os.Build;
import android.util.Log;



import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Set;

/**
 * trans code for video to mp4
 * mediacodec support  3gp and mp4
 */
public class MediaCodecTransCoder {

    private static final String TAG = "MediaCodecTransCoder";
    private static final boolean VERBOSE = true;

    private final static String MIME_TYPE = "video/avc";

    private static final int OUTPUT_FRAME_RATE = 25;               // 25fps
    private static final int OUTPUT_IFRAME_INTERVAL = 10;          // 10 seconds between I-frames
    private static final int TIMEOUT_USEC = 2500;

    private static final int MEDIATYPE_NOT_AUDIO_VIDEO = -233;

    private String mSrcVideoPath;
    private String mOutputVideoPath;

    private MediaMuxer mMediaMuxer;
    private MediaCodec mVideoEncoder;
    private MediaCodec mVideoDecoder;
    private MediaCodec.BufferInfo mBufferInfo;
    private int mTrackIndex;
    private CodecInputSurface mInputSurface;

    private int mNewWidth = -1;
    private int mNewHeight = -1;
    private int mNewBitRate = -1;


    public MediaCodecTransCoder() {

    }

    /**
     * @param srcVideoPath
     * @param outputVideoPath
     * @param newWidth
     * @param newHeight
     * @param newBitrate
     * @param listener
     * @return
     */
    public boolean convertVideo(final String srcVideoPath,
                                String outputVideoPath,
                                int newWidth,
                                int newHeight,
                                int newBitrate,
                                SlimProgressListener listener) {

        this.mSrcVideoPath = srcVideoPath;
        this.mOutputVideoPath = outputVideoPath;

        if (checkParamsError(newWidth, newHeight, newBitrate)) {
            return false;
        }

        // 原始视频的信息
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(mSrcVideoPath);
        String width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        String height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        String rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
      //  String framecount = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT);
        long duration = Long.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) * 1000;

        long startTime = -1;
        long endTime = -1;

        int originalWidth = Integer.valueOf(width);
        int originalHeight = Integer.valueOf(height);

        mNewBitRate = newBitrate;
        mNewWidth   = newWidth;
        mNewHeight  = newHeight;

        boolean error = false;
        long videoStartTime = -1;

        long time = System.currentTimeMillis();

        File cacheFile = new File(outputVideoPath);
        File inputFile = new File(mSrcVideoPath);
        if (!inputFile.canRead()) {
            return false;
        }

        MediaExtractor videoExtractor = null;
        MediaExtractor audioExtractor = null;

        try {
            videoExtractor = createExtractor();
            audioExtractor = createExtractor();
            mMediaMuxer = new MediaMuxer(mOutputVideoPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            // 混合器 音频轨道
            int muxerAudioTrackIndex = 0;
            int audioIndex = getAndSelectTrackIndex(audioExtractor, false);
            if (audioIndex >= 0) {
                audioExtractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                MediaFormat trackFormat = audioExtractor.getTrackFormat(audioIndex);
                muxerAudioTrackIndex = mMediaMuxer.addTrack(trackFormat);
            }


            // mediacodec + surface + opengl
            if (newWidth == originalWidth && newHeight == originalHeight) {
                // 宽高相同，不解码，编码


            } else {

            }


            if (newWidth != originalWidth || newHeight != originalHeight) {

                int videoIndex = selectTrack(videoExtractor, false);

                if (videoIndex >= 0) {

                    long videoTime = -1;
                    boolean outputDone = false;
                    boolean inputDone = false;
                    boolean decoderDone = false;
                    int swapUV = 0;
                    int videoTrackIndex = MEDIATYPE_NOT_AUDIO_VIDEO;


                    videoExtractor.selectTrack(videoIndex);
                    if (startTime > 0) {
                        videoExtractor.seekTo(startTime, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                    } else {
                        videoExtractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                    }
                    MediaFormat inputFormat = videoExtractor.getTrackFormat(videoIndex);


                    /**
                     ** init mediacodec  / encoder and decoder
                     **/
                    prepareEncoder(inputFormat);


                    ByteBuffer[] decoderInputBuffers = null;
                    ByteBuffer[] encoderOutputBuffers = null;


                    decoderInputBuffers = mVideoDecoder.getInputBuffers();
                    encoderOutputBuffers = mVideoEncoder.getOutputBuffers();


                    while (!outputDone) {
                        if (!inputDone) {
                            boolean eof = false;
                            int index = videoExtractor.getSampleTrackIndex();
                            if (index == videoIndex) {
                                int inputBufIndex = mVideoDecoder.dequeueInputBuffer(TIMEOUT_USEC);
                                if (inputBufIndex >= 0) {
                                    ByteBuffer inputBuf;
                                    if (Build.VERSION.SDK_INT < 21) {
                                        inputBuf = decoderInputBuffers[inputBufIndex];
                                    } else {
                                        inputBuf = mVideoDecoder.getInputBuffer(inputBufIndex);
                                    }
                                    int chunkSize = videoExtractor.readSampleData(inputBuf, 0);
                                    if (chunkSize < 0) {
                                        mVideoDecoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                        inputDone = true;
                                    } else {
                                        mVideoDecoder.queueInputBuffer(inputBufIndex, 0, chunkSize, videoExtractor.getSampleTime(), 0);
                                        videoExtractor.advance();
                                    }
                                }
                            } else if (index == -1) {
                                eof = true;
                            }
                            if (eof) {
                                int inputBufIndex = mVideoDecoder.dequeueInputBuffer(TIMEOUT_USEC);
                                if (inputBufIndex >= 0) {
                                    mVideoDecoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                    inputDone = true;
                                }
                            }
                        }

                        boolean decoderOutputAvailable = !decoderDone;
                        boolean encoderOutputAvailable = true;

                        while (decoderOutputAvailable || encoderOutputAvailable) {

                            int encoderStatus = mVideoEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
                            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                                encoderOutputAvailable = false;
                            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                                if (Build.VERSION.SDK_INT < 21) {
                                    encoderOutputBuffers = mVideoEncoder.getOutputBuffers();
                                }
                            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                MediaFormat newFormat = mVideoEncoder.getOutputFormat();
                                if (videoTrackIndex == MEDIATYPE_NOT_AUDIO_VIDEO) {
                                    videoTrackIndex = mMediaMuxer.addTrack(newFormat);
                                    mTrackIndex = videoTrackIndex;
                                    mMediaMuxer.start();
                                }
                            } else if (encoderStatus < 0) {
                                throw new RuntimeException("unexpected result from mVideoEncoder.dequeueOutputBuffer: " + encoderStatus);
                            } else {
                                ByteBuffer encodedData;
                                if (Build.VERSION.SDK_INT < 21) {
                                    encodedData = encoderOutputBuffers[encoderStatus];
                                } else {
                                    encodedData = mVideoEncoder.getOutputBuffer(encoderStatus);
                                }
                                if (encodedData == null) {
                                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                                }
                                if (mBufferInfo.size > 1) {
                                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                        mMediaMuxer.writeSampleData(videoTrackIndex, encodedData, mBufferInfo);
                                    } else if (videoTrackIndex == MEDIATYPE_NOT_AUDIO_VIDEO) {
                                        byte[] csd = new byte[mBufferInfo.size];
                                        encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                                        encodedData.position(mBufferInfo.offset);
                                        encodedData.get(csd);
                                        ByteBuffer sps = null;
                                        ByteBuffer pps = null;
                                        for (int a = mBufferInfo.size - 1; a >= 0; a--) {
                                            if (a > 3) {
                                                if (csd[a] == 1 && csd[a - 1] == 0 && csd[a - 2] == 0 && csd[a - 3] == 0) {
                                                    sps = ByteBuffer.allocate(a - 3);
                                                    pps = ByteBuffer.allocate(mBufferInfo.size - (a - 3));
                                                    sps.put(csd, 0, a - 3).position(0);
                                                    pps.put(csd, a - 3, mBufferInfo.size - (a - 3)).position(0);
                                                    break;
                                                }
                                            } else {
                                                break;
                                            }
                                        }

                                        MediaFormat newFormat = MediaFormat.createVideoFormat(MIME_TYPE, newWidth, newHeight);
                                        if (sps != null && pps != null) {
                                            newFormat.setByteBuffer("csd-0", sps);
                                            newFormat.setByteBuffer("csd-1", pps);
                                        }
                                        videoTrackIndex = mMediaMuxer.addTrack(newFormat);
                                        mMediaMuxer.start();
                                    }
                                }
                                outputDone = (mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                                mVideoEncoder.releaseOutputBuffer(encoderStatus, false);
                            }
                            if (encoderStatus != MediaCodec.INFO_TRY_AGAIN_LATER) {
                                continue;
                            }

                            if (!decoderDone) {
                                int decoderStatus = mVideoDecoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
                                if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                                    decoderOutputAvailable = false;
                                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {

                                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                    MediaFormat newFormat = mVideoDecoder.getOutputFormat();
                                    Log.e(TAG, "newFormat = " + newFormat);
                                } else if (decoderStatus < 0) {
                                    throw new RuntimeException("unexpected result from mVideoDecoder.dequeueOutputBuffer: " + decoderStatus);
                                } else {
                                    boolean doRender = false;

                                    doRender = mBufferInfo.size != 0;

                                    if (endTime > 0 && mBufferInfo.presentationTimeUs >= endTime) {
                                        inputDone = true;
                                        decoderDone = true;
                                        doRender = false;
                                        mBufferInfo.flags |= MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                                    }
                                    if (startTime > 0 && videoTime == -1) {
                                        if (mBufferInfo.presentationTimeUs < startTime) {
                                            doRender = false;
                                            Log.e(TAG, "drop frame startTime = " + startTime + " present time = " + mBufferInfo.presentationTimeUs);
                                        } else {
                                            videoTime = mBufferInfo.presentationTimeUs;
                                        }
                                    }
                                    mVideoDecoder.releaseOutputBuffer(decoderStatus, doRender);
                                    if (doRender) {
                                        boolean errorWait = false;
                                        try {
                                            mInputSurface.awaitNewImage();
                                        } catch (Exception e) {
                                            errorWait = true;
                                            Log.e(TAG, e.getMessage());
                                        }
                                        if (!errorWait) {

                                            mInputSurface.drawImage();
                                            mInputSurface.setPresentationTime(mBufferInfo.presentationTimeUs * 1000);

                                            if (listener != null) {
                                                listener.onProgress((float) mBufferInfo.presentationTimeUs / (float) duration * 100);
                                            }

                                            mInputSurface.swapBuffers();

                                        }
                                    }
                                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                        decoderOutputAvailable = false;
                                        Log.e(TAG, "decoder stream end");

                                        mVideoEncoder.signalEndOfInputStream();

                                    }
                                }
                            }
                        }
                    }
                    if (videoTime != -1) {
                        videoStartTime = videoTime;
                    }


                }


                videoExtractor.unselectTrack(videoIndex);

            } else {
                Log.e(TAG,"startvideorecord");
                long videoTime = simpleReadAndWriteTrack(videoExtractor, mMediaMuxer, mBufferInfo, startTime, endTime, cacheFile, false);
                if (videoTime != -1) {
                    videoStartTime = videoTime;
                }
            }

//            if (!error) {
//                Log.e(TAG,"startaudiorecord");
//                simpleReadAndWriteTrack(extractor, mMediaMuxer, mBufferInfo, videoStartTime, endTime, cacheFile, true);
//            }
            
            writeAudioTrack(audioExtractor, mMediaMuxer, mBufferInfo, videoStartTime, endTime, cacheFile, muxerAudioTrackIndex);

        } catch (Exception e) {
            error = true;
            Log.e(TAG, e.getMessage());
        } finally {
            if (videoExtractor != null) {
                videoExtractor.release();
                videoExtractor = null;
            }


            if (audioExtractor != null) {
                audioExtractor.release();
                audioExtractor = null;
            }
            Log.e(TAG, "time = " + (System.currentTimeMillis() - time));
        }


        Log.e("ViratPath", mSrcVideoPath + "");
        Log.e("ViratPath", cacheFile.getPath() + "");
        Log.e("ViratPath", inputFile.getPath() + "");



        releaseCoder();

        if(error)
            return  false;
        else
            return true;
    }


    private boolean checkParamsError(int newWidth, int newHeight, int newBitrate) {
        if (newWidth <= 0 || newHeight <= 0 || newBitrate <= 0)
            return true;
        else
            return false;
    }


    private long simpleReadAndWriteTrack(MediaExtractor extractor, MediaMuxer mediaMuxer, MediaCodec.BufferInfo info, long start, long end, File file, boolean isAudio) throws Exception {
        int trackIndex = selectTrack(extractor, isAudio);
        if (trackIndex >= 0) {
            extractor.selectTrack(trackIndex);
            MediaFormat trackFormat = extractor.getTrackFormat(trackIndex);
            int muxerTrackIndex = mediaMuxer.addTrack(trackFormat);

            if(!isAudio)
             mediaMuxer.start();

            int maxBufferSize = trackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
            boolean inputDone = false;
            if (start > 0) {
                extractor.seekTo(start, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            } else {
                extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            }
            ByteBuffer buffer = ByteBuffer.allocateDirect(maxBufferSize);
            long startTime = -1;

            while (!inputDone) {

                boolean eof = false;
                int index = extractor.getSampleTrackIndex();
                if (index == trackIndex) {
                    info.size = extractor.readSampleData(buffer, 0);

                    if (info.size < 0) {
                        info.size = 0;
                        eof = true;
                    } else {
                        info.presentationTimeUs = extractor.getSampleTime();
                        if (start > 0 && startTime == -1) {
                            startTime = info.presentationTimeUs;
                        }
                        if (end < 0 || info.presentationTimeUs < end) {
                            info.offset = 0;
                            info.flags = extractor.getSampleFlags();
                            mediaMuxer.writeSampleData(muxerTrackIndex, buffer, info);
                            extractor.advance();
                        } else {
                            eof = true;
                        }
                    }
                } else if (index == -1) {
                    eof = true;
                }
                if (eof) {
                    inputDone = true;
                }
            }

            extractor.unselectTrack(trackIndex);
            return startTime;
        }
        return -1;
    }


    private long writeAudioTrack(MediaExtractor extractor, MediaMuxer mediaMuxer, MediaCodec.BufferInfo info, long start, long end, File file, int muxerTrackIndex ) throws Exception {
        int trackIndex = selectTrack(extractor, true);
        if (trackIndex >= 0) {
            extractor.selectTrack(trackIndex);
            MediaFormat trackFormat = extractor.getTrackFormat(trackIndex);
          

            int maxBufferSize = trackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
            boolean inputDone = false;
            if (start > 0) {
                extractor.seekTo(start, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            } else {
                extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            }
            ByteBuffer buffer = ByteBuffer.allocateDirect(maxBufferSize);
            long startTime = -1;

            while (!inputDone) {

                boolean eof = false;
                int index = extractor.getSampleTrackIndex();
                if (index == trackIndex) {
                    info.size = extractor.readSampleData(buffer, 0);

                    if (info.size < 0) {
                        info.size = 0;
                        eof = true;
                    } else {
                        info.presentationTimeUs = extractor.getSampleTime();
                        if (start > 0 && startTime == -1) {
                            startTime = info.presentationTimeUs;
                        }
                        if (end < 0 || info.presentationTimeUs < end) {
                            info.offset = 0;
                            info.flags = extractor.getSampleFlags();
                            mediaMuxer.writeSampleData(muxerTrackIndex, buffer, info);
                            extractor.advance();
                        } else {
                            eof = true;
                        }
                    }
                } else if (index == -1) {
                    eof = true;
                }
                if (eof) {
                    inputDone = true;
                }
            }

            extractor.unselectTrack(trackIndex);
            return startTime;
        }
        return -1;
    }
    

    private int selectTrack(MediaExtractor extractor, boolean audio) {
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (audio) {
                if (mime.startsWith("audio/")) {
                    return i;
                }
            } else {
                if (mime.startsWith("video/")) {
                    return i;
                }
            }
        }
        return MEDIATYPE_NOT_AUDIO_VIDEO;
    }


    /**
     * 创建收抽取器
     * @return
     * @throws IOException
     */
    public MediaExtractor createExtractor() throws IOException {
        MediaExtractor mediaExtractor = new MediaExtractor();
        mediaExtractor.setDataSource(mSrcVideoPath);
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
     * Configures encoder and muxer state, and prepares the input Surface.
     */
    private void prepareEncoder(MediaFormat inputFormat) {
        mBufferInfo = new MediaCodec.BufferInfo();

        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mNewWidth, mNewHeight);

        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, mNewBitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, OUTPUT_FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, OUTPUT_IFRAME_INTERVAL);
        if (VERBOSE) Log.d(TAG, "format: " + format);

        // Create a MediaCodec encoder, and configure it with our format.  Get a Surface
        // we can use for input and wrap it with a class that handles the EGL work.
        //
        // If you want to have two EGL contexts -- one for display, one for recording --
        // you will likely want to defer instantiation of CodecInputSurface until after the
        // "display" EGL context is created, then modify the eglCreateContext call to
        // take eglGetCurrentContext() as the share_context argument.
        try {
            mVideoEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mVideoEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mInputSurface = new CodecInputSurface(mVideoEncoder.createInputSurface());
        mInputSurface.makeCurrent();
        mVideoEncoder.start();

        try {
            mVideoDecoder = MediaCodec.createDecoderByType(inputFormat.getString(MediaFormat.KEY_MIME));
        } catch (IOException e) {
            e.printStackTrace();
        }
        mInputSurface.createRender();
        mVideoDecoder.configure(inputFormat, mInputSurface.getSurface(), null, 0);
        mVideoDecoder.start();

        // Output filename.  Ideally this would use Context.getFilesDir() rather than a
        // hard-coded output directory.

        // Create a MediaMuxer.  We can't add the video track and start() the muxer here,
        // because our MediaFormat doesn't have the Magic Goodies.  These can only be
        // obtained from the encoder after it has started processing data.
        //
        // We're not actually interested in multiplexing audio.  We just want to convert
        // the raw H.264 elementary stream we get from MediaCodec into a .mp4 file.


        mTrackIndex = -1;
    }

    /**
     * Releases encoder resources.  May be called after partial / failed initialization.
     */
    private void releaseCoder() {
        if (VERBOSE) Log.d(TAG, "releasing encoder objects");
        if (mVideoEncoder != null) {
            mVideoEncoder.stop();
            mVideoEncoder.release();
            mVideoEncoder = null;
        }
        if (mVideoDecoder != null) {
            mVideoDecoder.stop();
            mVideoDecoder.release();
            mVideoDecoder = null;
        }
        if (mInputSurface != null) {
            mInputSurface.release();
            mInputSurface = null;
        }
        if (mMediaMuxer != null) {
            mMediaMuxer.stop();
            mMediaMuxer.release();
            mMediaMuxer = null;
        }
    }


}
