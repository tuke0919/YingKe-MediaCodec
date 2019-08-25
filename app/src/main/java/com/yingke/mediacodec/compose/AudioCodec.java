package com.yingke.mediacodec.compose;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.util.Log;

import com.yingke.mediacodec.player.PlayerLog;
import com.yingke.mediacodec.utils.CodecUtil;
import com.yingke.mediacodec.utils.FileUtils;
import com.yingke.mediacodec.utils.ToastUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
     * video -> aac
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
     * 音频文件 解码成pcm mp3->pcm
     *
     * @param audioPath
     * @param pcmSavePath
     * @param listener
     */
    public static void decodeAudioToPCM(String audioPath, final String pcmSavePath, final OnDecoderListener listener) {
        PlayerLog.e(TAG,"---decodeAudioToPCM-----" );
        PlayerLog.e(TAG,"audioPath = " + audioPath + " pcmSavePath = " + pcmSavePath);

        try {

            final MediaExtractor audioExtractor = CodecUtil.createExtractor(audioPath);
            int extractorTrack = CodecUtil.getAndSelectTrackIndex(audioExtractor, false);
            boolean hasAudio = false;
            if (extractorTrack > -1) {
                hasAudio = true;
            }

            final MediaMuxer mediaMuxer = CodecUtil.createMuxer(pcmSavePath);
            final MediaFormat trackFormat = audioExtractor.getTrackFormat(extractorTrack);

            if (!hasAudio) {
                // 主线程 错误
                Task.call(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        if (listener != null) {
                            listener.onDecodeErr();
                        }
                        return null;
                    }
                },Task.UI_THREAD_EXECUTOR);
                return;
            }

            // 有音频 子线程
            Task.callInBackground(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    // pcm输出流
                    FileOutputStream pcmOutputStream = new FileOutputStream(pcmSavePath);

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
                        PlayerLog.e(TAG, "---读取一帧---- inputDone ： " + inputDone);
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
                                        PlayerLog.e(TAG, "---读取结束，写入解码EOS---- inputDone ： " + inputDone);
                                    } else {

                                        inputInfo.offset = 0;
                                        inputInfo.size = sampleSize;
                                        inputInfo.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;
                                        inputInfo.presentationTimeUs = audioExtractor.getSampleTime();
                                        PlayerLog.e(TAG, "往解码器写入数据---当前帧的时间戳----" + inputInfo.presentationTimeUs);

                                        audioDecoder.queueInputBuffer(inputIndex, inputInfo.offset, sampleSize, inputInfo.presentationTimeUs, 0);//通知MediaDecode解码刚刚传入的数据
                                        // 下一帧取样处
                                        PlayerLog.e(TAG, "--指向下一帧---- ");

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
                            PlayerLog.e(TAG, "---读取解码器输出---- outputIndex ： " + outputIndex);

                            switch (outputIndex) {
                                case MediaCodec.INFO_TRY_AGAIN_LATER:
                                    decodeOutputDone = true;
                                    break;
                                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                                    outputBuffers = audioDecoder.getOutputBuffers();
                                    break;
                                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
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
                                            PlayerLog.e(TAG, "---解码器 输出结束----" );

                                            audioDecoder.stop();
                                            audioDecoder.release();
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

                    return null;
                }
            });

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

    /**
     * 编码 pcm 到音频 pcm -> aac
     * @param pcmPath
     * @param audioPath
     * @param listener
     */
    public static void encodePcmToAudio(final String pcmPath, final String audioPath, final OnEncoderListener listener) {

        if (!new File(pcmPath).exists()) {
            Task.call(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    if (listener != null) {
                        listener.onEncodeErr();
                    }
                    return null;
                }
            },Task.UI_THREAD_EXECUTOR);
            return;
        }


        // 子线程
        Task.callInBackground(new Callable<Object>() {
            @Override
            public Object call() throws Exception {

                try {
                    FileInputStream fis = new FileInputStream(pcmPath);
                    byte[] buffer = new byte[8 * 1024];
                    byte[] allAudioBytes;

                    int inputIndex;
                    ByteBuffer inputBuffer;
                    int outputIndex;
                    ByteBuffer outputBuffer;
                    byte[] chunkAudio;
                    int outBitSize;
                    int outPacketSize;
                    // 初始化编码器
                    // mime type 采样率 声道数
                    MediaFormat encodeFormat = MediaFormat.createAudioFormat("audio/mp4a-latm", 44100, 2);
                    encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);//比特率
                    encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
                    encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 500 * 1024);

                    MediaCodec mediaEncode = MediaCodec.createEncoderByType("audio/mp4a-latm");
                    mediaEncode.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                    mediaEncode.start();

                    ByteBuffer[] encodeInputBuffers = mediaEncode.getInputBuffers();
                    ByteBuffer[] encodeOutputBuffers = mediaEncode.getOutputBuffers();
                    MediaCodec.BufferInfo encodeBufferInfo = new MediaCodec.BufferInfo();

                    // 初始化文件写入流
                    FileOutputStream fos = new FileOutputStream(new File(audioPath));
                    BufferedOutputStream bos = new BufferedOutputStream(fos, 500 * 1024);
                    boolean isReadEnd = false;
                    while (!isReadEnd) {
                        // 先把所有输入Buffers填充数据
                        for (int i = 0; i < encodeInputBuffers.length - 1; i++) {
                            if (fis.read(buffer) != -1) {
                                allAudioBytes = Arrays.copyOf(buffer, buffer.length);
                            } else {
                                PlayerLog.e(TAG, "---文件读取完成---");
                                isReadEnd = true;
                                break;
                            }
                            Log.e(TAG, "---io---读取文件-写入编码器--" + allAudioBytes.length);
                            inputIndex = mediaEncode.dequeueInputBuffer(-1);
                            inputBuffer = encodeInputBuffers[inputIndex];
                            // 同解码器
                            inputBuffer.clear();
                            inputBuffer.limit(allAudioBytes.length);
                            // PCM数据填充给inputBuffer
                            inputBuffer.put(allAudioBytes);
                            mediaEncode.queueInputBuffer(inputIndex, 0, allAudioBytes.length, 0, 0);//通知编码器 编码
                        }
                        outputIndex = mediaEncode.dequeueOutputBuffer(encodeBufferInfo, 10000);//同解码器
                        while (outputIndex >= 0) {
                            // 从编码器中取出数据
                            outBitSize = encodeBufferInfo.size;
                            // 7为ADTS头部的大小
                            outPacketSize = outBitSize + 7;
                            // 拿到输出Buffer
                            outputBuffer = encodeOutputBuffers[outputIndex];
                            outputBuffer.position(encodeBufferInfo.offset);
                            outputBuffer.limit(encodeBufferInfo.offset + outBitSize);
                            chunkAudio = new byte[outPacketSize];
                            // 添加ADTS 代码后面会贴上
                            AudioCodec.addADTStoPacket(chunkAudio, outPacketSize);
                            // 将编码得到的AAC数据 取出到byte[]中 偏移量offset=7 你懂得
                            outputBuffer.get(chunkAudio, 7, outBitSize);
                            outputBuffer.position(encodeBufferInfo.offset);
                            Log.e(TAG, "--编码成功-写入文件----" + chunkAudio.length);
                            //BufferOutputStream 将文件保存到内存卡中 *.aac
                            bos.write(chunkAudio, 0, chunkAudio.length);
                            bos.flush();

                            mediaEncode.releaseOutputBuffer(outputIndex, false);
                            outputIndex = mediaEncode.dequeueOutputBuffer(encodeBufferInfo, 10000);
                        }
                    }
                    mediaEncode.stop();
                    mediaEncode.release();
                    fos.close();
                    // 主线程
                    Task.call(new Callable<Object>() {
                        @Override
                        public Object call() throws Exception {
                            if (listener != null) {
                                listener.onEncodeSuc();
                            }
                            return null;
                        }
                    }, Task.UI_THREAD_EXECUTOR);

                } catch (Exception e) {
                    e.printStackTrace();
                    // 主线程
                    Task.call(new Callable<Object>() {
                        @Override
                        public Object call() throws Exception {
                            if (listener != null) {
                                listener.onEncodeErr();
                            }
                            return null;
                        }
                    }, Task.UI_THREAD_EXECUTOR);
                }

                return null;
            }
        });
    }


    /**
     * 写入ADTS头部数据
     * */
    public static void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2; // AAC LC
        int freqIdx = 4; // 44.1KHz
        int chanCfg = 2; // CPE

        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }


    /**
     * 混合两个音频 文件 mp3 mp3-> aac
     * @param audioPathOne
     * @param audioPathTwo
     * @param outPath
     * @param listener
     */
    public static void createAudioMix(final String audioPathOne,
                                      final String audioPathTwo,
                                      final String outPath,
                                      final OnEncoderListener listener){

        final boolean[] decodeResultSuc = new boolean[]{false, false};
        final boolean[] decodeResultErr = new boolean[]{false, false};

        // 音频文件名
        final String audioFileNameOne = audioPathOne.substring(audioPathOne.lastIndexOf("/"), audioPathOne.lastIndexOf("."));
        final String pcmPathOne = FileUtils.getAudioPcmPath(audioFileNameOne);

        PlayerLog.e(TAG, "audioFileNameOne = " + audioFileNameOne);
        PlayerLog.e(TAG, "pcmPathOne = " + pcmPathOne);

        decodeAudioToPCM(audioPathOne, pcmPathOne, new OnDecoderListener() {
            @Override
            public void onDecodeSuc() {
                PlayerLog.e(TAG, "---decodeAudioToPCM--- onDecodeSuc: audioFileNameOne = " + audioFileNameOne);

                decodeResultSuc[0] = true;
            }

            @Override
            public void onDecodeErr() {
                PlayerLog.e(TAG, "---decodeAudioToPCM--- onDecodeErr: audioFileNameOne = " + audioFileNameOne);

                decodeResultErr[0] = true;
            }
        });

        // 音频文件名
        final String audioFileNameTwo = audioPathTwo.substring(audioPathTwo.lastIndexOf("/"), audioPathTwo.lastIndexOf("."));
        final String pcmPathTwo = FileUtils.getAudioPcmPath(audioFileNameTwo);

        PlayerLog.e(TAG, "audioFileNameTwo = " + audioFileNameTwo);
        PlayerLog.e(TAG, "pcmPathTwo = " + pcmPathTwo);

        decodeAudioToPCM(audioPathTwo, pcmPathTwo, new OnDecoderListener() {
            @Override
            public void onDecodeSuc() {
                PlayerLog.e(TAG, "---decodeAudioToPCM--- onDecodeSuc: audioFileNameTwo = " + audioFileNameTwo);

                decodeResultSuc[1] = true;
            }

            @Override
            public void onDecodeErr() {
                PlayerLog.e(TAG, "---decodeAudioToPCM--- onDecodeErr: audioFileNameTwo = " + audioFileNameTwo);

                decodeResultErr[1] = true;
            }
        });

        // 合成两个pcm文件，子线程
        Task.callInBackground(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                boolean isMixAudioEnd = false;
                while (!isMixAudioEnd){
                    PlayerLog.e(TAG, "---isMixAudioEnd： " + isMixAudioEnd);

                    PlayerLog.e(TAG, "---合成两个pcm文件，子线程---- decodeResultSuc[0] = " + decodeResultSuc[0]);
                    PlayerLog.e(TAG, "---合成两个pcm文件，子线程---- decodeResultSuc[1] = " + decodeResultSuc[1]);

                    // 两个pcm都成功
                    if (decodeResultSuc[0] && decodeResultSuc[1]) {

                        // 结束
                        isMixAudioEnd = true;

                        // 检查文件存在
                        final File pcmOne = new File(pcmPathOne);
                        final File pcmTwo = new File(pcmPathTwo);
                        if (!pcmOne.exists() || !pcmTwo.exists()) {
                            // 主线程
                            Task.call(new Callable<Object>() {
                                @Override
                                public Object call() throws Exception {
                                    PlayerLog.e(TAG, "--- 有pcm文件不存在---- pcmOne = " + pcmOne.exists() + " pcmTwo = " + pcmTwo.exists() );

                                    if (listener != null) {
                                        listener.onEncodeErr();
                                    }
                                    return null;
                                }
                            }, Task.UI_THREAD_EXECUTOR);

                            break;
                        }
                        // 两个pcm文件
                        List<File> pcmFiles = new ArrayList<>();
                        pcmFiles.add(pcmOne);
                        pcmFiles.add(pcmTwo);
                        // 混合两个pcm文件，到一个acc
                        audioPcmMix(pcmFiles, outPath, 1, 1, new OnEncoderListener() {
                            @Override
                            public void onEncodeSuc() {
                                // 主线程
                                if (listener != null) {
                                    listener.onEncodeSuc();
                                }
                            }

                            @Override
                            public void onEncodeErr() {
                                // 主线程
                                if (listener != null) {
                                    listener.onEncodeErr();
                                }
                            }
                        });
                    }

                    // 如果有一个pcm失败, 就都失败
                    if (decodeResultErr[0] || decodeResultErr[1]) {
                        // 结束
                        isMixAudioEnd = true;

                        Task.call(new Callable<Object>() {
                            @Override
                            public Object call() throws Exception {
                                if (listener != null) {
                                    listener.onEncodeErr();
                                }
                                return null;
                            }
                        }, Task.UI_THREAD_EXECUTOR);
                    }
                    // 线程睡眠
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
                return null;
            }
        });

    }

    /**
     * 两个pcm文件 混合成一个aac,两个音频同时播放，不是顺序播放
     * @param rawAudioFiles      原始pcm文件
     * @param mixedAudioOutFile  aac文件
     * @param firstVol
     * @param secondVol
     * @param listener
     * @throws IOException
     */
    public static void audioPcmMix(List<File> rawAudioFiles,
                                   final String mixedAudioOutFile,
                                   int firstVol,
                                   int secondVol,
                                   final OnEncoderListener listener) throws IOException {

        PlayerLog.e(TAG, "---audioPcmMix----");

        File mixedAudioFile = new File(mixedAudioOutFile);
        if (mixedAudioFile.exists()) {
            mixedAudioFile.delete();
        }

        final int size = rawAudioFiles.size();
        // 两个pcm文件流对象
        FileInputStream[] pcmFileStreams = new FileInputStream[size];
        // 两组pcm字节
        byte[][] allPcmBytes = new byte[size][];
        // 两个组pcm流结束
        boolean[] pcmStreamsDone = new boolean[size];

        // 初始化两个pcm输入流
        for (int index = 0; index < size; index ++ ) {
            File pcmFile = rawAudioFiles.get(index);
            pcmFileStreams[index] = new FileInputStream(pcmFile);
        }

        byte[] buffer = new byte[8 * 1024];

        // 是否已经开始编码
        final boolean[] isStartEncode = {false};

        while (true) {

            // 两个pcm文件 同时读buffer
            PlayerLog.e(TAG, "---两个pcm文件 同时读buffer----");

            for (int streamIndex = 0; streamIndex < size; streamIndex ++ ) {
                FileInputStream inputStream = pcmFileStreams[streamIndex];
                if (!pcmStreamsDone[streamIndex] && (inputStream.read(buffer)) != -1) {
                    allPcmBytes[streamIndex] = Arrays.copyOf(buffer, buffer.length);
                } else {
                    pcmStreamsDone[streamIndex] = true;
                    allPcmBytes[streamIndex] = new byte[8 * 1024];
                }
            }

            // 混合两个pcm buffer
            PlayerLog.e(TAG, "---混合两个pcm buffer----");
            byte[] mixBytes = nativeAudioMix(allPcmBytes, firstVol, secondVol);
            // 存放混合后的buffer
            putMixedPcmBuffer(mixBytes);

            // mixBytes 就是混合后的数据
            Log.e(TAG, "-----混音后的数据---" + mixBytes.length + "---isStartEncode--" + isStartEncode[0]);

            if (!isStartEncode[0]){

                PlayerLog.e(TAG, "---开始编码,混合后的 pcm buffer----");
                // 开始编码, 子线程
                Task.callInBackground(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {

                        isStartEncode[0] = true;
                        try {
                            Log.e(TAG,"start encode thread.....");

                            // 从pcm buffer拿到数据 编码成aac
                            getPcmBufferToAudio( mixedAudioOutFile);

                            // 主线程
                            Task.call(new Callable<Object>() {
                                @Override
                                public Object call() throws Exception {
                                    if (listener != null){
                                        listener.onEncodeSuc();
                                    }

                                    return null;
                                }
                            }, Task.UI_THREAD_EXECUTOR);

                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.e(TAG," encode error-----------error------");

                            // 主线程
                            Task.call(new Callable<Object>() {
                                @Override
                                public Object call() throws Exception {
                                    if (listener != null){
                                        listener.onEncodeErr();
                                    }

                                    return null;
                                }
                            }, Task.UI_THREAD_EXECUTOR);

                        }
                        return null;
                    }
                });
            }

            boolean done = true;
            // 有一个流没结束，就没结束
            for (boolean streamEnd : pcmStreamsDone) {
                if (!streamEnd) {
                    done = false;
                }
            }

            // 混音结束
            if (done) {
                isPcmMixDone = true;
                break;
            }
        }

    }

    /**
     * jni进行音频的混音处理，提升速度
     * */
    public static byte[] nativeAudioMix(byte[][] allAudioBytes, float firstVol, float secondVol){
        if (allAudioBytes == null || allAudioBytes.length == 0)
            return null;

        byte[] realMixAudio = allAudioBytes[0];

        //如果只有一个音频的话，就返回这个音频数据
        if(allAudioBytes.length == 1)
            return realMixAudio;

        // 音频混合 buffer
        return AudioMixJni.audioMix(allAudioBytes[0], allAudioBytes[1], realMixAudio, firstVol, secondVol);
    }

    /**
     * 存放混音的pcm数据
     */
    private static ArrayList<byte[]> pcmMixedDatas;

    /**
     * @param pcmChunk 存放混音后的buffer
     */
    private static void putMixedPcmBuffer(byte[] pcmChunk) {
        // 记得加锁
        synchronized (AudioCodec.class) {
            if (pcmMixedDatas == null){
                pcmMixedDatas = new ArrayList<>();
            }
            pcmMixedDatas.add(pcmChunk);
        }
    }

    /**
     * @return 获取混音后的Buffer
     */
    private static byte[] getMixedPcmBuffer() {
        // 记得加锁
        synchronized (AudioCodec.class) {
            if (pcmMixedDatas.isEmpty()) {
                return null;
            }
            // 每次取出index 0 的数据
            byte[] pcmChunk = pcmMixedDatas.get(0);
            // 取出后将此数据remove掉 既能保证PCM数据块的取出顺序 又能及时释放内存
            pcmMixedDatas.remove(pcmChunk);
            return pcmChunk;
        }
    }

    /**
     * 原始pcm数据，转aac音频
     * */
    static boolean isPcmMixDone = false;

    /**
     * 从pcm buffer拿到数据 编码成aac
     * @param outputFile
     * @throws IOException
     */
    public static void getPcmBufferToAudio(String outputFile) throws IOException {
        int inputIndex;
        ByteBuffer inputBuffer;
        int outputIndex;
        ByteBuffer outputBuffer;
        byte[] chunkAudio;
        int outBitSize;
        int outPacketSize;
        byte[] chunkPCM;
        // 初始化编码器
        MediaFormat encodeFormat = MediaFormat.createAudioFormat("audio/mp4a-latm",44100,2);//mime type 采样率 声道数
        encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);//比特率
        encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 500 * 1024);

        MediaCodec mediaEncode = MediaCodec.createEncoderByType("audio/mp4a-latm");
        mediaEncode.configure(encodeFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaEncode.start();

        ByteBuffer[] encodeInputBuffers = mediaEncode.getInputBuffers();
        ByteBuffer[] encodeOutputBuffers = mediaEncode.getOutputBuffers();
        MediaCodec.BufferInfo encodeBufferInfo = new MediaCodec.BufferInfo();

        // 初始化文件写入流
        FileOutputStream fos = new FileOutputStream(new File(outputFile));
        BufferedOutputStream bos = new BufferedOutputStream(fos,500*1024);
        Log.e(TAG,"--encodeBufferInfo---"+encodeBufferInfo.size);

        while (!pcmMixedDatas.isEmpty() || !isPcmMixDone){

            for (int i = 0; i < encodeInputBuffers.length - 1; i++) {
                // 获取混音后的Buffer
                chunkPCM = getMixedPcmBuffer();
                if (chunkPCM == null) {
                    break;
                }
                Log.e(TAG,"--AAC编码器--取数据---"+chunkPCM.length);

                inputIndex = mediaEncode.dequeueInputBuffer(-1);
                inputBuffer = encodeInputBuffers[inputIndex];
                inputBuffer.clear();
                inputBuffer.limit(chunkPCM.length);
                // PCM数据填充给inputBuffer
                inputBuffer.put(chunkPCM);
                // //通知编码器 编码
                mediaEncode.queueInputBuffer(inputIndex, 0, chunkPCM.length, 0, 0);
            }

            outputIndex = mediaEncode.dequeueOutputBuffer(encodeBufferInfo, 10000);
            while (outputIndex >= 0) {

                outBitSize = encodeBufferInfo.size;
                // 7为ADTS头部的大小
                outPacketSize = outBitSize + 7;
                // 拿到输出Buffer
                if (Build.VERSION.SDK_INT >= 21){
                    outputBuffer = mediaEncode.getOutputBuffer(outputIndex);
                }else {
                    outputBuffer = encodeOutputBuffers[outputIndex];
                }

                outputBuffer.position(encodeBufferInfo.offset);
                outputBuffer.limit(encodeBufferInfo.offset + outBitSize);
                chunkAudio = new byte[outPacketSize];
                // 添加ADTS 代码后面会贴上
                addADTStoPacket(chunkAudio,outPacketSize);
                // 将编码得到的AAC数据 取出到byte[]中 偏移量offset=7 你懂得
                outputBuffer.get(chunkAudio, 7, outBitSize);
                outputBuffer.position(encodeBufferInfo.offset);
                try {
                    Log.e(TAG,"---保存文件----"+chunkAudio.length);
                    // BufferOutputStream 将文件保存到内存卡中 *.aac
                    bos.write(chunkAudio,0,chunkAudio.length);
                    bos.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mediaEncode.releaseOutputBuffer(outputIndex,false);
                outputIndex = mediaEncode.dequeueOutputBuffer(encodeBufferInfo, 10000);
            }
        }
        mediaEncode.stop();
        mediaEncode.release();
        fos.close();
    }



















}
