package com.yingke.mediacodec.simple;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.support.annotation.StyleableRes;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 功能：
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/8/5
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class SimpleMediaCodec {

    private static final String TAG = SimpleMediaCodec.class.getSimpleName();
    private static final boolean VERBOSE = true;

    private Context context;

    /** How long to wait for the next buffer to become available. */
    private static final int TIMEOUT_USEC = 10000;
    /** parameters for the video encoder */
    private static final String OUTPUT_VIDEO_MIME_TYPE = "video/avc"; // H.264 Advanced Video Coding
    private static final int OUTPUT_VIDEO_BIT_RATE = 512 * 1024; // 512 kbps maybe better
    private static final int OUTPUT_VIDEO_FRAME_RATE = 25; // 25fps
    private static final int OUTPUT_VIDEO_IFRAME_INTERVAL = 10; // 10 seconds between I-frames
    private static final int OUTPUT_VIDEO_COLOR_FORMAT = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;

    /** parameters for the audio encoder */
    private static final String OUTPUT_AUDIO_MIME_TYPE = "audio/mp4a-latm"; // Advanced Audio  Coding
    private static final int OUTPUT_AUDIO_BIT_RATE = 64 * 1024; // 64 kbps
    private static final int OUTPUT_AUDIO_AAC_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectLC; // better then AACObjectHE?

    /** parameters for the audio encoder config from input stream */
    private int OUTPUT_AUDIO_CHANNEL_COUNT = 1; // Must match the input stream.can not config
    private int OUTPUT_AUDIO_SAMPLE_RATE_HZ = 48000; // Must match the input stream.can not config

    /** Whether to copy the video from the test video. */
    private boolean mCopyVideo = false;
    /** Whether to copy the audio from the test audio. */
    private boolean mCopyAudio = false;
    /** Width of the output frames. */
    private int mWidth = -1;
    /** Height of the output frames. */
    private int mHeight = -1;

    /** The raw resource used as the input file. */
    private String mBaseFileRoot;
    /** The raw resource used as the input file. */
    private String mBaseFile = "http://mov.bn.netease.com/open-movie/nos/mp4/2017/09/07/SCSQ1R2K8_shd.mp4";
    /** The destination file for the encoded output. */
    private String mOutputFile;

    private boolean interrupted = false;

    public SimpleMediaCodec(Context context) {
        this.context = context;
    }

    /**
     * Creates an extractor that reads its frames
     */
    private MediaExtractor createExtractor() throws IOException {
        // net source
        MediaExtractor extractor = new MediaExtractor();
        if (mBaseFile.contains(":")) {
            extractor.setDataSource(mBaseFile);
        } else {
            File mFile = new File(mBaseFile);
            extractor.setDataSource(mFile.toString());
        }
        return extractor;
    }

    /**
     * @param extractor
     * @return
     */
    private int getAndSelectVideoTrackIndex(MediaExtractor extractor) {
        for (int index = 0; index < extractor.getTrackCount(); ++index) {
            if (VERBOSE) {
                Log.d(TAG, "format for track " + index + " is " + getMimeTypeFor(extractor.getTrackFormat(index)));
            }
            if (isVideoFormat(extractor.getTrackFormat(index))) {
                extractor.selectTrack(index);
                return index;
            }
        }
        return -1;
    }

    /**
     * @param extractor
     * @return
     */
    private int getAndSelectAudioTrackIndex(MediaExtractor extractor) {
        for (int index = 0; index < extractor.getTrackCount(); ++index) {
            if (VERBOSE) {
                Log.d(TAG, "format for track " + index + " is " + getMimeTypeFor(extractor.getTrackFormat(index)));
            }
            if (isAudioFormat(extractor.getTrackFormat(index))) {
                extractor.selectTrack(index);
                return index;
            }
        }
        return -1;
    }

    /**
     * @param mediaFormat
     * @return
     */
    private String getMimeTypeFor(MediaFormat mediaFormat) {
        return mediaFormat!= null ? mediaFormat.getString(MediaFormat.KEY_MIME) : null;
    }

    /**
     * @param mediaFormat
     * @return
     */
    private boolean isVideoFormat(MediaFormat mediaFormat) {
        String mimeType = getMimeTypeFor(mediaFormat);
        return mimeType != null && mimeType.startsWith("video/");
    }

    /**
     * @param mediaFormat
     * @return
     */
    private boolean isAudioFormat(MediaFormat mediaFormat) {
        String mimeType = getMimeTypeFor(mediaFormat);
        return mimeType != null && mimeType.startsWith("audio/");
    }

    /**
     * Returns the first codec capable of encoding the specified MIME type, or
     * null if no match was found.
     */
    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {
                continue;
            }

            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    /**
     * Creates a decoder for the given format, which outputs to the given
     * surface.
     *
     * @param inputFormat
     *            the format of the stream to decode
     * @param surface
     *            into which to decode the frames
     */
    private MediaCodec createVideoDecoder(MediaFormat inputFormat, Surface surface) throws IOException {
        MediaCodec decoder = MediaCodec.createDecoderByType(getMimeTypeFor(inputFormat));
        decoder.configure(inputFormat, surface, null, 0);
        decoder.start();
        return decoder;
    }
    /**
     * Creates an encoder for the given format using the specified codec, taking
     * input from a surface.
     *
     * <p>
     * The surface to use as input is stored in the given reference.
     *
     * @param codecInfo
     *            of the codec to use
     * @param format
     *            of the stream to be produced
     * @param surfaceReference
     *            to store the surface to use as input
     */
    private MediaCodec createVideoEncoder(MediaCodecInfo codecInfo,
                                          MediaFormat format,
                                          AtomicReference<Surface> surfaceReference) throws IOException {
        MediaCodec encoder = MediaCodec.createByCodecName(codecInfo.getName());
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        // Must be called before start() is.
        if (surfaceReference != null) {
            surfaceReference.set(encoder.createInputSurface());
        }
        encoder.start();
        return encoder;
    }
    /**
     * Creates a decoder for the given format.
     *
     * @param inputFormat
     *            the format of the stream to decode
     */
    private MediaCodec createAudioDecoder(MediaFormat inputFormat) throws IOException {
        MediaCodec decoder = MediaCodec.createDecoderByType(getMimeTypeFor(inputFormat));
        decoder.configure(inputFormat, null, null, 0);
        decoder.start();
        return decoder;
    }

    /**
     * Creates an encoder for the given format using the specified codec.
     *
     * @param codecInfo
     *            of the codec to use
     * @param format
     *            of the stream to be produced
     */
    private MediaCodec createAudioEncoder(MediaCodecInfo codecInfo, MediaFormat format) throws IOException {
        MediaCodec encoder = MediaCodec.createByCodecName(codecInfo.getName());
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        encoder.start();
        return encoder;
    }


    /**
     * Creates a muxer to write the encoded frames. The muxer is not started as
     * it needs to be started only after all streams have been added.
     */
    private MediaMuxer createMuxer() throws IOException {
        return new MediaMuxer(mOutputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
    }

    /**
     * Does the actual work for extracting, decoding, encoding and muxing.
     */
    private void doExtractDecodeEncodeMux(MediaExtractor videoExtractor,
                                          MediaExtractor audioExtractor,
                                          MediaCodec videoDecoder,
                                          MediaCodec videoEncoder,
                                          MediaCodec audioDecoder,
                                          MediaCodec audioEncoder,
                                          MediaMuxer muxer) {

        // for video buffers
        ByteBuffer[] videoDecoderInputBuffers = null;
        ByteBuffer[] videoDecoderOutputBuffers = null;
        ByteBuffer[] videoEncoderInputBuffers = null;
        ByteBuffer[] videoEncoderOutputBuffers = null;

        MediaCodec.BufferInfo videoDecoderOutputBufferInfo = null;
        MediaCodec.BufferInfo videoEncoderOutputBufferInfo = null;

        if (mCopyVideo) {
            videoDecoderInputBuffers = videoDecoder.getInputBuffers();
            videoDecoderOutputBuffers = videoDecoder.getOutputBuffers();
            videoEncoderInputBuffers = videoEncoder.getInputBuffers();
            videoEncoderOutputBuffers = videoEncoder.getOutputBuffers();
            videoDecoderOutputBufferInfo = new MediaCodec.BufferInfo();
            videoEncoderOutputBufferInfo = new MediaCodec.BufferInfo();
        }

        // for audio buffers
        ByteBuffer[] audioDecoderInputBuffers = null;
        ByteBuffer[] audioDecoderOutputBuffers = null;
        ByteBuffer[] audioEncoderInputBuffers = null;
        ByteBuffer[] audioEncoderOutputBuffers = null;
        MediaCodec.BufferInfo audioDecoderOutputBufferInfo = null;
        MediaCodec.BufferInfo audioEncoderOutputBufferInfo = null;

        if (mCopyAudio) {
            audioDecoderInputBuffers = audioDecoder.getInputBuffers();
            audioDecoderOutputBuffers = audioDecoder.getOutputBuffers();
            audioEncoderInputBuffers = audioEncoder.getInputBuffers();
            audioEncoderOutputBuffers = audioEncoder.getOutputBuffers();
            audioDecoderOutputBufferInfo = new MediaCodec.BufferInfo();
            audioEncoderOutputBufferInfo = new MediaCodec.BufferInfo();
        }

        // We will get these from the decoders when notified of a format change.
        MediaFormat decoderOutputVideoFormat = null;
        MediaFormat decoderOutputAudioFormat = null;
        // We will get these from the encoders when notified of a format change.
        MediaFormat encoderOutputVideoFormat = null;
        MediaFormat encoderOutputAudioFormat = null;
        // We will determine these once we have the output format.
        int outputVideoTrack = -1;
        int outputAudioTrack = -1;
        // Whether things are done on the video side.
        boolean videoExtractorDone = false;
        boolean videoDecoderDone = false;
        boolean videoEncoderDone = false;
        // Whether things are done on the audio side.
        boolean audioExtractorDone = false;
        boolean audioDecoderDone = false;
        boolean audioEncoderDone = false;
        // The video decoder output buffer to process, -1 if none.
        int pendingVideoDecoderOutputBufferIndex = -1;
        // The audio decoder output buffer to process, -1 if none.
        int pendingAudioDecoderOutputBufferIndex = -1;

        boolean muxing = false;

        int videoExtractedFrameCount = 0;
        int videoDecodedFrameCount = 0;
        int videoEncodedFrameCount = 0;

        int audioExtractedFrameCount = 0;
        int audioDecodedFrameCount = 0;
        int audioEncodedFrameCount = 0;
        boolean mVideoConfig = false;
        boolean mainVideoFrame = false;
        long mLastVideoSampleTime = 0;
        long mVideoSampleTime = 0;

        boolean mAudioConfig = false;
        boolean mainAudioFrame = false;
        long mLastAudioSampleTime = 0;
        long mAudioSampleTime = 0;

        while (!interrupted && ((mCopyVideo && !videoEncoderDone) || (mCopyAudio && !audioEncoderDone))) {
            //###########################Video###################################

            // Extract video from file and feed to decoder.
            // Do not extract video if we have determined the output format but
            // we are not yet ready to mux the frames.
            while (mCopyVideo
                    && !videoExtractorDone
                    && (encoderOutputVideoFormat == null || muxing)) {

                int decoderInputBufferIndex = videoDecoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (decoderInputBufferIndex <= MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (VERBOSE) {
                        Log.d(TAG, "no video decoder input buffer: " + decoderInputBufferIndex);
                    }
                    break;
                }
                if (VERBOSE) {
                    Log.d(TAG, "video decoder dequeueInputBuffer: returned input buffer: " + decoderInputBufferIndex);
                }

                ByteBuffer decoderInputBuffer = videoDecoderInputBuffers[decoderInputBufferIndex];
                // read video ts to decoder's intputbuffer
                int size = videoExtractor.readSampleData(decoderInputBuffer, 0);
                if (videoExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC) {
                    Log.d(TAG, " video decoder SAMPLE_FLAG_SYNC ");
                }
                long presentationTime = videoExtractor.getSampleTime();
                if (VERBOSE) {
                    Log.d(TAG, "video extractor: returned buffer of size " + size);
                    Log.d(TAG, "video extractor: returned buffer for time " + presentationTime);
                }
                if (size > 0) {
                    videoDecoder.queueInputBuffer(decoderInputBufferIndex,
                            0,
                            size,
                            presentationTime,
                            videoExtractor.getSampleFlags());
                }
                videoExtractorDone = !videoExtractor.advance();
                if (videoExtractorDone) {
                    if (VERBOSE) {
                        Log.d(TAG, "video extractor: EOS");
                    }
                    videoDecoder.queueInputBuffer(decoderInputBufferIndex,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                }
                videoExtractedFrameCount++;
                // We extracted a frame, let's try something else next.
                break;
            }

            //###########################Audio###################################
            // Extract audio from file and feed to decoder.
            // Do not extract audio if we have determined the output format but
            // we are not yet ready to mux the frames.
            while (mCopyAudio
                    && !audioExtractorDone
                    && (encoderOutputAudioFormat == null || muxing)) {

                int decoderInputBufferIndex = audioDecoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (decoderInputBufferIndex <= MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (VERBOSE) {
                        Log.d(TAG, "no audio decoder input buffer: "+decoderInputBufferIndex);
                    }
                    break;
                }
                if (VERBOSE) {
                    Log.d(TAG, "audio decoder dequeueInputBuffer: returned input buffer: " + decoderInputBufferIndex);
                }
                ByteBuffer decoderInputBuffer = audioDecoderInputBuffers[decoderInputBufferIndex];
                // read audio data
                int size = audioExtractor.readSampleData(decoderInputBuffer, 0);
                long presentationTime = audioExtractor.getSampleTime();
                if (VERBOSE) {
                    Log.d(TAG, "audio extractor: returned buffer of size " + size);
                    Log.d(TAG, "audio extractor: returned buffer for time " + presentationTime);
                }
                if (size > 0) {
                    audioDecoder.queueInputBuffer(decoderInputBufferIndex,
                            0,
                            size, presentationTime,
                            audioExtractor.getSampleFlags());
                }
                // extractor done
                audioExtractorDone = !audioExtractor.advance();
                if (audioExtractorDone) {
                    if (VERBOSE)Log.d(TAG, "audio extractor: EOS");
                    audioDecoder.queueInputBuffer(decoderInputBufferIndex,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                }
                audioExtractedFrameCount++;
                // We extracted a frame, let's try something else next.
                break;
            }

            // Poll output frames from the video decoder and feed the encoder.
            while (mCopyVideo
                    && !videoDecoderDone
                    && pendingVideoDecoderOutputBufferIndex == -1
                    && (encoderOutputVideoFormat == null || muxing)) {


                int decoderOutputBufferIndex = videoDecoder.dequeueOutputBuffer(videoDecoderOutputBufferInfo, TIMEOUT_USEC);
                if (decoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (VERBOSE) {
                        Log.d(TAG, "no video decoder output buffer");
                    }
                    break;
                } else if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // do what for this?
                    decoderOutputVideoFormat = videoDecoder.getOutputFormat();
                    if (VERBOSE) {
                        Log.d(TAG, "video decoder: output format changed: " + decoderOutputVideoFormat);
                    }
                    break;
                } else if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    if (VERBOSE) {
                        Log.d(TAG, "video decoder: output buffers changed");
                    }
                    videoDecoderOutputBuffers = videoDecoder.getOutputBuffers();
                    break;
                }

                if (VERBOSE) {
                    Log.d(TAG, "video decoder: returned output buffer: " + decoderOutputBufferIndex);
                    Log.d(TAG, "video decoder: returned buffer of size " + videoDecoderOutputBufferInfo.size);
                }
                if ((videoDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    if (VERBOSE)Log.d(TAG, "video decoder: codec config buffer");
                    videoDecoder.releaseOutputBuffer(decoderOutputBufferIndex,false);
                    break;
                }
                if (VERBOSE) {
                    Log.d(TAG, "video decoder: returned buffer for time " + videoDecoderOutputBufferInfo.presentationTimeUs);
                }

                pendingVideoDecoderOutputBufferIndex = decoderOutputBufferIndex;
                videoDecodedFrameCount++;
                // We extracted a pending frame, let's try something else next.
                break;
            }

            // Feed the pending decoded video buffer to the video encoder.
            while (mCopyVideo && pendingVideoDecoderOutputBufferIndex != -1) {

                if (VERBOSE) {
                    Log.d(TAG,"video decoder: attempting to process pending buffer: " + pendingVideoDecoderOutputBufferIndex);
                }
                int encoderInputBufferIndex = videoEncoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (encoderInputBufferIndex <= MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (VERBOSE) {
                        Log.d(TAG, "no video encoder input buffer: " +encoderInputBufferIndex);
                    }
                    break;
                }
                if (VERBOSE) {
                    Log.d(TAG, "video encoder: returned input buffer: " + encoderInputBufferIndex);
                }
                ByteBuffer encoderInputBuffer = videoEncoderInputBuffers[encoderInputBufferIndex];

                int size = videoDecoderOutputBufferInfo.size;
                long presentationTime = videoDecoderOutputBufferInfo.presentationTimeUs;
                if (VERBOSE) {
                    Log.d(TAG, "video decoder: processing pending buffer: " + pendingVideoDecoderOutputBufferIndex);
                    Log.d(TAG, "video decoder: pending buffer of size " + size);
                    Log.d(TAG, "video decoder: pending buffer for time " + presentationTime);
                }
                if (size >= 0) {

                    try {
                        // copy decoder output buffer into encoder intput buffer
                        ByteBuffer decoderOutputBuffer = videoDecoderOutputBuffers[pendingVideoDecoderOutputBufferIndex].duplicate();
                        decoderOutputBuffer.position(videoDecoderOutputBufferInfo.offset);
                        decoderOutputBuffer.limit(videoDecoderOutputBufferInfo.offset + size);
                        encoderInputBuffer.position(0);
                        encoderInputBuffer.put(decoderOutputBuffer);
                        //size not enable
                        videoEncoder.queueInputBuffer(encoderInputBufferIndex,
                                0,
                                size,
                                presentationTime,
                                videoDecoderOutputBufferInfo.flags);

                    } catch (Exception e) {
                        // TODO: handle exception
                        e.printStackTrace();
                    }

                }
                videoDecoder.releaseOutputBuffer(pendingVideoDecoderOutputBufferIndex, false);

                pendingVideoDecoderOutputBufferIndex = -1;

                if ((videoDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (VERBOSE)Log.d(TAG, "video decoder: EOS");
                    videoDecoderDone = true;
                }
                // We enqueued a pending frame, let's try something else next.
                break;
            }

            // Poll frames from the video encoder and send them to the muxer.
            while (mCopyVideo
                    && !videoEncoderDone
                    && (encoderOutputVideoFormat == null || muxing)) {

                // can not get avilabel outputBuffers?
                int videoEncoderOutputBufferIndex = videoEncoder.dequeueOutputBuffer(videoEncoderOutputBufferInfo, TIMEOUT_USEC);

                if (videoEncoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (VERBOSE) {
                        Log.d(TAG, "no video encoder output buffer");
                    }
                    break;
                } else if (videoEncoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (VERBOSE) {
                        Log.d(TAG, "video encoder: output format changed");
                    }
                    if (outputVideoTrack >= 0) {
                        // fail("video encoder changed its output format again?");
                        Log.d(TAG,"video encoder changed its output format again?");
                    }
                    encoderOutputVideoFormat = videoEncoder.getOutputFormat();
                    break;
                } else if (videoEncoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    if (VERBOSE) {
                        Log.d(TAG, "video encoder: output buffers changed");
                    }
                    videoEncoderOutputBuffers = videoEncoder.getOutputBuffers();
                    break;
                }

                // assertTrue("should have added track before processing output", muxing);
                if (VERBOSE) {
                    Log.d(TAG, "video encoder: returned output buffer: " + videoEncoderOutputBufferIndex);
                    Log.d(TAG, "video encoder: returned buffer of size " + videoEncoderOutputBufferInfo.size);
                }

                ByteBuffer encoderOutputBuffer = videoEncoderOutputBuffers[videoEncoderOutputBufferIndex];
                if ((videoEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    if (VERBOSE){
                        Log.d(TAG, "video encoder: codec config buffer");
                    }
                    // Simply ignore codec config buffers.
                    mVideoConfig = true;
                    videoEncoder.releaseOutputBuffer(videoEncoderOutputBufferIndex,false);
                    break;
                }
                if (VERBOSE) {
                    Log.d(TAG, "video encoder: returned buffer for time " + videoEncoderOutputBufferInfo.presentationTimeUs);
                }

                if(mVideoConfig){
                    if(!mainVideoFrame){
                        mLastVideoSampleTime = videoEncoderOutputBufferInfo.presentationTimeUs;
                        mainVideoFrame = true;
                    }else{
                        if(mVideoSampleTime == 0){
                            mVideoSampleTime = videoEncoderOutputBufferInfo.presentationTimeUs - mLastVideoSampleTime;
                        }
                    }
                }
                videoEncoderOutputBufferInfo.presentationTimeUs = mLastVideoSampleTime + mVideoSampleTime;
                if (videoEncoderOutputBufferInfo.size != 0) {
                    // muxer
                    muxer.writeSampleData(outputVideoTrack, encoderOutputBuffer, videoEncoderOutputBufferInfo);
                    mLastVideoSampleTime = videoEncoderOutputBufferInfo.presentationTimeUs;
                }

                if ((videoEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (VERBOSE) {
                        Log.d(TAG, "video encoder: EOS");
                    }
                    videoEncoderDone = true;
                }
                videoEncoder.releaseOutputBuffer(videoEncoderOutputBufferIndex, false);
                videoEncodedFrameCount++;
                // We enqueued an encoded frame, let's try something else next.
                break;
            }

            // Poll output frames from the audio decoder.
            // Do not poll if we already have a pending buffer to feed to the
            // encoder.
            while (mCopyAudio
                    && !audioDecoderDone
                    && pendingAudioDecoderOutputBufferIndex == -1
                    && (encoderOutputAudioFormat == null || muxing)) {

                int decoderOutputBufferIndex = audioDecoder.dequeueOutputBuffer(audioDecoderOutputBufferInfo,TIMEOUT_USEC);
                if (decoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (VERBOSE){
                        Log.d(TAG, "no audio decoder output buffer");
                    }
                    break;
                }else if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    decoderOutputAudioFormat = audioDecoder.getOutputFormat();
                    if (VERBOSE) {
                        Log.d(TAG, "audio decoder: output format changed: " + decoderOutputAudioFormat);
                    }
                    break;
                }else if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    if (VERBOSE){
                        Log.d(TAG, "audio decoder: output buffers changed");
                    }
                    audioDecoderOutputBuffers = audioDecoder.getOutputBuffers();
                    break;
                }

                if (VERBOSE) {
                    Log.d(TAG, "audio decoder: returned output buffer: " + decoderOutputBufferIndex);
                    Log.d(TAG, "audio decoder: returned buffer of size " + audioDecoderOutputBufferInfo.size);
                    Log.d(TAG, "audio decoder: returned buffer for time " + audioDecoderOutputBufferInfo.presentationTimeUs);
                }

                if ((audioDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    if (VERBOSE){
                        Log.d(TAG, "audio decoder: codec config buffer");
                    }
                    audioDecoder.releaseOutputBuffer(decoderOutputBufferIndex,false);
                    break;
                }

                if (VERBOSE) {
                    Log.d(TAG, "audio decoder: output buffer is now pending: " + decoderOutputBufferIndex);
                }
                pendingAudioDecoderOutputBufferIndex = decoderOutputBufferIndex;
                audioDecodedFrameCount++;
                // We extracted a pending frame, let's try something else next.
                break;
            }

            // Feed the pending decoded audio buffer to the audio encoder.
            while (mCopyAudio && pendingAudioDecoderOutputBufferIndex != -1) {
                if (VERBOSE) {
                    Log.d(TAG,"audio decoder: attempting to process pending buffer: "+ pendingAudioDecoderOutputBufferIndex);
                }
                int encoderInputBufferIndex = audioEncoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (encoderInputBufferIndex <= MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (VERBOSE){
                        Log.d(TAG, "no audio encoder input buffer: "+encoderInputBufferIndex);
                    }
                    break;
                }
                if (VERBOSE) {
                    Log.d(TAG, "audio encoder: returned input buffer: "+ encoderInputBufferIndex);
                }
                ByteBuffer encoderInputBuffer = audioEncoderInputBuffers[encoderInputBufferIndex];
                int size = audioDecoderOutputBufferInfo.size;
                long presentationTime = audioDecoderOutputBufferInfo.presentationTimeUs;
                if (VERBOSE) {
                    Log.d(TAG, "audio decoder: processing pending buffer: "+ pendingAudioDecoderOutputBufferIndex);
                    Log.d(TAG, "audio decoder: pending buffer of size " + size);
                    Log.d(TAG, "audio decoder: pending buffer for time "+ presentationTime);
                }
                if (size >= 0) {
                    try {
                        // copy
                        ByteBuffer decoderOutputBuffer = audioDecoderOutputBuffers[pendingAudioDecoderOutputBufferIndex].duplicate();
                        decoderOutputBuffer.position(audioDecoderOutputBufferInfo.offset);
                        decoderOutputBuffer.limit(audioDecoderOutputBufferInfo.offset + size);
                        encoderInputBuffer.position(0);
                        encoderInputBuffer.put(decoderOutputBuffer);
                        audioEncoder.queueInputBuffer(encoderInputBufferIndex,
                                0,
                                audioDecoderOutputBufferInfo.offset + size,
                                presentationTime,
                                audioDecoderOutputBufferInfo.flags);

                    } catch (Exception e) {
                        // TODO: handle exception
                        e.printStackTrace();
                    }

                }
                audioDecoder.releaseOutputBuffer(pendingAudioDecoderOutputBufferIndex, false);
                pendingAudioDecoderOutputBufferIndex = -1;
                if ((audioDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (VERBOSE){
                        Log.d(TAG, "audio decoder: EOS");
                    }
                    audioDecoderDone = true;
                }
                // We enqueued a pending frame, let's try something else next.
                break;
            }

            // Poll frames from the audio encoder and send them to the muxer.
            while (mCopyAudio
                    && !audioEncoderDone
                    && (encoderOutputAudioFormat == null || muxing)) {

                int audioEncoderOutputBufferIndex = audioEncoder.dequeueOutputBuffer(audioEncoderOutputBufferInfo,TIMEOUT_USEC);
                if (audioEncoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (VERBOSE) {
                        Log.d(TAG, "no audio encoder output buffer");
                    }
                    break;
                }else if (audioEncoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (VERBOSE){
                        Log.d(TAG, "audio encoder: output format changed");
                    }
                    if (outputAudioTrack >= 0) {
                        // fail("audio encoder changed its output format again?");
                        Log.d(TAG,"audio encoder changed its output format again?");
                    }
                    encoderOutputAudioFormat = audioEncoder.getOutputFormat();
                    break;
                }else if (audioEncoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    if (VERBOSE){
                        Log.d(TAG, "audio encoder: output buffers changed");
                    }
                    audioEncoderOutputBuffers = audioEncoder.getOutputBuffers();
                    break;
                }
                // assertTrue("should have added track before processing output",muxing);
                if (VERBOSE) {
                    Log.d(TAG, "audio encoder: returned output buffer: " + audioEncoderOutputBufferIndex);
                    Log.d(TAG, "audio encoder: returned buffer of size " + audioEncoderOutputBufferInfo.size);
                }
                ByteBuffer encoderOutputBuffer = audioEncoderOutputBuffers[audioEncoderOutputBufferIndex];
                if ((audioEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    if (VERBOSE){
                        Log.d(TAG, "audio encoder: codec config buffer");
                    }
                    // Simply ignore codec config buffers.
                    mAudioConfig = true;
                    audioEncoder.releaseOutputBuffer(audioEncoderOutputBufferIndex,false);
                    break;
                }
                if (VERBOSE) {
                    Log.d(TAG, " audio encoder: returned buffer for time " + audioEncoderOutputBufferInfo.presentationTimeUs);
                }

                if(mAudioConfig){
                    if(!mainAudioFrame){
                        mLastAudioSampleTime = audioEncoderOutputBufferInfo.presentationTimeUs;
                        mainAudioFrame = true;
                    }else{
                        if(mAudioSampleTime == 0){
                            mAudioSampleTime = audioEncoderOutputBufferInfo.presentationTimeUs - mLastAudioSampleTime;
                        }
                    }
                }

                audioEncoderOutputBufferInfo.presentationTimeUs = mLastAudioSampleTime + mAudioSampleTime;
                if (audioEncoderOutputBufferInfo.size != 0) {
                    // write audio data
                    muxer.writeSampleData(outputAudioTrack, encoderOutputBuffer, audioEncoderOutputBufferInfo);
                    mLastAudioSampleTime = audioEncoderOutputBufferInfo.presentationTimeUs;
                }

                if ((audioEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (VERBOSE){
                        Log.d(TAG, "audio encoder: EOS");
                    }
                    audioEncoderDone = true;
                }
                audioEncoder.releaseOutputBuffer(audioEncoderOutputBufferIndex,false);
                audioEncodedFrameCount++;
                // We enqueued an encoded frame, let's try something else next.
                break;
            }

            if (!muxing
                    && (!mCopyAudio || encoderOutputAudioFormat != null)
                    && (!mCopyVideo || encoderOutputVideoFormat != null)) {

            }
                if (mCopyVideo) {
                    Log.d(TAG, "muxer: adding video track.");
                    outputVideoTrack = muxer.addTrack(encoderOutputVideoFormat);
                }
                if (mCopyAudio) {
                    Log.d(TAG, "muxer: adding audio track.");
                    outputAudioTrack = muxer.addTrack(encoderOutputAudioFormat);
                }
                Log.d(TAG, "muxer: starting");
                muxer.start();
                muxing = true;
            }

        Log.d(TAG, "exit looper");

    }


    private void extractDecodeEncodeMux() throws Exception {
        // Exception that may be thrown during release.
        Exception exception = null;
        MediaExtractor videoExtractor = null;
        MediaExtractor audioExtractor = null;
        MediaCodec videoDecoder = null;
        MediaCodec audioDecoder = null;
        MediaCodec videoEncoder = null;
        MediaCodec audioEncoder = null;
        MediaMuxer muxer = null;

        try {
            if (mCopyVideo) {
                videoExtractor = createExtractor();
                int videoInputTrack = getAndSelectVideoTrackIndex(videoExtractor);
                Log.d(TAG, " video track in test video " + videoInputTrack);
                MediaFormat inputVideoFormat = videoExtractor.getTrackFormat(videoInputTrack);

                if (VERBOSE) {
                    Log.d(TAG, "video base input format: " + inputVideoFormat);
                }

                // make sure decode buffer size equals encode buffer size
                inputVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, OUTPUT_VIDEO_COLOR_FORMAT);

                if (mWidth != -1) {
                    inputVideoFormat.setInteger(MediaFormat.KEY_WIDTH, mWidth);
                } else {
                    mWidth = inputVideoFormat.getInteger(MediaFormat.KEY_WIDTH);
                }
                if (mHeight != -1) {
                    inputVideoFormat.setInteger(MediaFormat.KEY_HEIGHT, mHeight);
                } else {
                    mHeight = inputVideoFormat.getInteger(MediaFormat.KEY_HEIGHT);
                }

                if (VERBOSE) {
                    Log.d(TAG, "video match input format: " + inputVideoFormat);
                }

                MediaCodecInfo videoCodecInfo = selectCodec(OUTPUT_VIDEO_MIME_TYPE);
                if (videoCodecInfo == null) {
                    Log.e(TAG, "Unable to find an appropriate codec for " + OUTPUT_VIDEO_MIME_TYPE);
                    return;
                }
                if (VERBOSE) {
                    Log.d(TAG, "video found codec: " + videoCodecInfo.getName());
                }

                MediaFormat outputVideoFormat = MediaFormat.createVideoFormat(OUTPUT_VIDEO_MIME_TYPE, mWidth, mHeight);
                outputVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, OUTPUT_VIDEO_COLOR_FORMAT);
                outputVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, OUTPUT_VIDEO_BIT_RATE);
                outputVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, OUTPUT_VIDEO_FRAME_RATE);
                outputVideoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, OUTPUT_VIDEO_IFRAME_INTERVAL);

                if (VERBOSE) {
                    Log.d(TAG, "video encode format: " + outputVideoFormat);
                }

                videoEncoder = createVideoEncoder(videoCodecInfo, outputVideoFormat, null);
                videoDecoder = createVideoDecoder(inputVideoFormat, null);
            }

            if (mCopyAudio) {
                audioExtractor = createExtractor();
                int audioInputTrack = getAndSelectAudioTrackIndex(audioExtractor);

                if (VERBOSE) {
                    Log.d(TAG, " audio track in test audio " + audioInputTrack);
                }

                MediaFormat inputAudioFormat = audioExtractor.getTrackFormat(audioInputTrack);

                if (VERBOSE) {
                    Log.d(TAG, "audio base input format: " + inputAudioFormat);
                }

                // inputFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,OUTPUT_AUDIO_AAC_PROFILE);
                OUTPUT_AUDIO_SAMPLE_RATE_HZ = inputAudioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                OUTPUT_AUDIO_CHANNEL_COUNT = inputAudioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

                if (VERBOSE) {
                    Log.d(TAG, "audio match input format: " + inputAudioFormat);
                }

                MediaCodecInfo audioCodecInfo = selectCodec(OUTPUT_AUDIO_MIME_TYPE);
                if (audioCodecInfo == null) {
                    Log.e(TAG, "Unable to find an appropriate codec for "
                            + OUTPUT_AUDIO_MIME_TYPE);
                    return;
                }
                if (VERBOSE) {
                    Log.d(TAG, "audio found codec: " + audioCodecInfo.getName());
                }

                MediaFormat outputAudioFormat = MediaFormat.createAudioFormat(OUTPUT_AUDIO_MIME_TYPE, OUTPUT_AUDIO_SAMPLE_RATE_HZ, OUTPUT_AUDIO_CHANNEL_COUNT);
                outputAudioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 100 * 1024);
                outputAudioFormat.setInteger(MediaFormat.KEY_BIT_RATE, OUTPUT_AUDIO_BIT_RATE);
                outputAudioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, OUTPUT_AUDIO_AAC_PROFILE);

                if (VERBOSE) {
                    Log.d(TAG, "audio encode format: " + outputAudioFormat);
                }

                // Create a MediaCodec for the desired codec, then configure it
                // as an encoder with our desired properties.
                audioEncoder = createAudioEncoder(audioCodecInfo, outputAudioFormat);
                // Create a MediaCodec for the decoder, based on the extractor's
                // format.
                audioDecoder = createAudioDecoder(inputAudioFormat);
            }

            setOutputFile();
            // Creates a muxer but do not start or add tracks just yet.
            muxer = createMuxer();

            doExtractDecodeEncodeMux(videoExtractor,
                    audioExtractor,
                    videoDecoder,
                    videoEncoder,
                    audioDecoder,
                    audioEncoder,
                    muxer);

        }finally {
            if (VERBOSE)
                Log.d(TAG, "releasing extractor, decoder, encoder, and muxer");
            try {
                if (videoExtractor != null) {
                    videoExtractor.release();
                }
            } catch (Exception e) {
                Log.e(TAG, "error while releasing videoExtractor", e);
            }
            try {
                if (audioExtractor != null) {
                    audioExtractor.release();
                }
            } catch (Exception e) {
                Log.e(TAG, "error while releasing audioExtractor", e);

            }
            try {
                if (videoDecoder != null) {
                    videoDecoder.stop();
                    videoDecoder.release();
                }
            } catch (Exception e) {
                Log.e(TAG, "error while releasing videoDecoder", e);

            }
            try {


            } catch (Exception e) {
                Log.e(TAG, "error while releasing outputSurface", e);

            }
            try {
                if (videoEncoder != null) {
                    videoEncoder.stop();
                    videoEncoder.release();
                }
            } catch (Exception e) {
                Log.e(TAG, "error while releasing videoEncoder", e);

            }
            try {
                if (audioDecoder != null) {
                    audioDecoder.stop();
                    audioDecoder.release();
                }
            } catch (Exception e) {
                Log.e(TAG, "error while releasing audioDecoder", e);

            }
            try {
                if (audioEncoder != null) {
                    audioEncoder.stop();
                    audioEncoder.release();
                }
            } catch (Exception e) {
                Log.e(TAG, "error while releasing audioEncoder", e);

            }
            try {
                if (muxer != null) {
                    muxer.stop();
                    muxer.release();
                }
            } catch (Exception e) {
                Log.e(TAG, "error while releasing muxer", e);

            }
            try {

            } catch (Exception e) {
                Log.e(TAG, "error while releasing inputSurface", e);

            }
        }
        if (exception != null) {
            throw exception;
        }
    }

    private void setOutputFile() {
        mOutputFile = context.getExternalFilesDir("").getAbsolutePath() + "/" + "simpleMediaCodec_" + System.currentTimeMillis() + ".mp4";
    }






    private void showSupportedColorFormat(MediaCodecInfo.CodecCapabilities caps) {
        for (int c : caps.colorFormats) {
            Log.d(TAG, "supported color format: " + c);
        }
    }





}
