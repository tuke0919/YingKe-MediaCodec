package com.yingke.mediacodec.compose;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.util.Log;

import com.yingke.mediacodec.player.PlayerLog;
import com.yingke.mediacodec.utils.CodecUtil;
import com.yingke.mediacodec.utils.ToastUtil;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;

import bolts.Task;

/**
 * 功能：
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/8/19
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class AudioCodec {

    public static final String TAG = "AudioCodec";

    public final static int TIMEOUT_USEC = 10;


    /**
     * 从视频文件中分离出音频，并保存到本地
     *
     * @param videoPath
     * @param audioSavePath
     * @param listener
     * */
    public static void separateAudio(String videoPath,
                                     final String audioSavePath,
                                     final OnDecoderListener listener){

        PlayerLog.e(TAG,"---separateAudio-----" );

        try {
            final MediaExtractor audioExtractor = CodecUtil.createExtractor(videoPath);
            int extractorTrack = CodecUtil.getAndSelectTrackIndex(audioExtractor, false);
            boolean hasAudio = false;
            if (extractorTrack > -1) {
                hasAudio = true;
            }

            final MediaMuxer mediaMuxer = CodecUtil.createMuxer(audioSavePath);
            final MediaFormat trackFormat = audioExtractor.getTrackFormat(extractorTrack);
            final int muxerAudioTrack = mediaMuxer.addTrack(trackFormat);
            mediaMuxer.start();

            if (!hasAudio) {
                ToastUtil.showToastShort("没有音频");
                return;
            }
            // 子线程
            Task.call(new Callable<Object>() {

                @Override
                public Object call() throws Exception {

                    try{
                        ByteBuffer byteBuffer = ByteBuffer.allocate(trackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE));
                        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

                        audioExtractor.readSampleData(byteBuffer, 0);
                        if (audioExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC) {
                            audioExtractor.advance();
                        }
                        while (true) {
                            int readSampleSize = audioExtractor.readSampleData(byteBuffer, 0);
                            PlayerLog.e(TAG,"---读取音频数据，当前读取到的大小-----：：：" + readSampleSize);
                            if (readSampleSize < 0) {
                                break;
                            }

                            bufferInfo.size = readSampleSize;
                            bufferInfo.flags = audioExtractor.getSampleFlags();
                            bufferInfo.offset = 0;
                            bufferInfo.presentationTimeUs = audioExtractor.getSampleTime();
                            Log.e(TAG,"----写入音频数据---当前的时间戳：：：" + audioExtractor.getSampleTime());

                            mediaMuxer.writeSampleData(muxerAudioTrack, byteBuffer, bufferInfo);
                            audioExtractor.advance();
                        }
                        // 循环写入结束


                        mediaMuxer.release();
                        audioExtractor.release();

                        // 主线程
                        Task.call(new Callable<Object>() {
                            @Override
                            public Object call() throws Exception {
                                if (listener != null){
                                    listener.onDecodeSuc();
                                }
                                return null;
                            }
                        }, Task.UI_THREAD_EXECUTOR);


                    } catch (Exception e){
                        e.printStackTrace();
                        // 主线程
                        Task.call(new Callable<Object>() {
                            @Override
                            public Object call() throws Exception {
                                if (listener != null){
                                    listener.onDecodeErr();
                                }
                                return null;
                            }
                        }, Task.UI_THREAD_EXECUTOR);
                    }

                    return null;
                }
            }, Task.BACKGROUND_EXECUTOR);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将音频文件解码成原始的PCM数据
     *
     * @param audioPath
     * @param pcmSavePath
     * @param listener
     */
    public static void decodeAudioToPCM(String audioPath, String pcmSavePath, final OnDecoderListener listener) {
        PlayerLog.e(TAG,"---decodeAudioToPCM-----" );

        try {

            FileOutputStream pcmOutputStream = new FileOutputStream(pcmSavePath);

            final MediaExtractor audioExtractor = CodecUtil.createExtractor(audioPath);
            int extractorTrack = CodecUtil.getAndSelectTrackIndex(audioExtractor, false);
            boolean hasAudio = false;
            if (extractorTrack > -1) {
                hasAudio = true;
            }

            final MediaMuxer mediaMuxer = CodecUtil.createMuxer(pcmSavePath);
            final MediaFormat trackFormat = audioExtractor.getTrackFormat(extractorTrack);

            if (!hasAudio) {
                ToastUtil.showToastShort("没有音频");
                return;
            }

            // 初始化音频的解码器
            String mime = trackFormat.getString(MediaFormat.KEY_MIME);
            MediaCodec audioDecoder = MediaCodec.createDecoderByType(mime);
            audioDecoder.configure(trackFormat, null, null, 0);
            audioDecoder.start();

            ByteBuffer[] inputBuffers = audioDecoder.getInputBuffers();
            ByteBuffer[] outputBuffers = audioDecoder.getOutputBuffers();


            MediaCodec.BufferInfo decodeBufferInfo = new MediaCodec.BufferInfo();
            MediaCodec.BufferInfo inputInfo = new MediaCodec.BufferInfo();

            boolean decodeEnd = false;
            boolean inputDone = false;

            while (!decodeEnd) {

                if (!inputDone) {
                    // 循环读取帧
                    for (int i = 0; i < inputBuffers.length; i++) {
                        int inputIndex = audioDecoder.dequeueInputBuffer(TIMEOUT_USEC);
                        if (inputIndex >= 0) {
                            ByteBuffer inputBuffer = inputBuffers[inputIndex];
                            inputBuffer.clear();
                            // MediaExtractor读取数据到inputBuffer中
                            int sampleSize = audioExtractor.readSampleData(inputBuffer, 0);

                            if (sampleSize < 0) {
                                // 写EOS帧，输入结束
                                audioDecoder.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                inputDone = true;
                            } else {

                                inputInfo.offset = 0;
                                inputInfo.size = sampleSize;
                                inputInfo.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;
                                inputInfo.presentationTimeUs = audioExtractor.getSampleTime();
                                PlayerLog.e(TAG, "往解码器写入数据---当前帧的时间戳----" + inputInfo.presentationTimeUs);

                                audioDecoder.queueInputBuffer(inputIndex, inputInfo.offset, sampleSize, inputInfo.presentationTimeUs, 0);//通知MediaDecode解码刚刚传入的数据
                                // 下一帧取样处
                                audioExtractor.advance();
                            }
                        }
                    }
                }
                // 解码器输出 结束标志
                boolean decodeOutputDone = false;
                byte[] chunkPCM;
                while (!decodeOutputDone) {
                    int outputIndex = audioDecoder.dequeueOutputBuffer(decodeBufferInfo, TIMEOUT_USEC);
                    switch (outputIndex) {
                        case MediaCodec.INFO_TRY_AGAIN_LATER:
                            decodeOutputDone = true;
                            break;
                        case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                            outputBuffers = audioDecoder.getOutputBuffers();
                            break;
                        case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                            // 只调用一次
                            MediaFormat newFormat = audioDecoder.getOutputFormat();
                            int muxerAudioTrack = mediaMuxer.addTrack(newFormat);
                            if (muxerAudioTrack > -1) {
                                mediaMuxer.start();
                            }
                            break;
                        default:
                            if (outputIndex > 0) {
                                ByteBuffer outputBuffer;
                                if (Build.VERSION.SDK_INT >= 21) {
                                    outputBuffer = audioDecoder.getOutputBuffer(outputIndex);
                                } else {
                                    outputBuffer = outputBuffers[outputIndex];
                                }

                                chunkPCM = new byte[decodeBufferInfo.size];
                                outputBuffer.get(chunkPCM);
                                outputBuffer.clear();
                                // 数据写入文件中
                                pcmOutputStream.write(chunkPCM);
                                pcmOutputStream.flush();
                                PlayerLog.e(TAG, "---释放输出流缓冲区----:::" + outputIndex);
                                audioDecoder.releaseOutputBuffer(outputIndex, false);

                                if ((decodeBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {

                                    audioDecoder.release();
                                    audioDecoder.stop();

                                    decodeEnd = true;
                                    decodeOutputDone = true;
                                }
                            }
                            break;
                    }
                }
            }

            if (pcmOutputStream != null) {
                pcmOutputStream.close();
            }

            // 主线程
            Task.call(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    if (listener != null) {
                        listener.onDecodeSuc();
                    }
                    return null;
                }
            },Task.UI_THREAD_EXECUTOR);



        } catch (Exception e) {
            e.printStackTrace();

            // 主线程
            Task.call(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    if (listener != null) {
                        listener.onDecodeErr();
                    }
                    return null;
                }
            },Task.UI_THREAD_EXECUTOR);

        }
    }





}
