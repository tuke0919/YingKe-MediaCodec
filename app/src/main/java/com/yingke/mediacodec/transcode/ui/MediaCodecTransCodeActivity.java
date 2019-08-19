package com.yingke.mediacodec.transcode.ui;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;


import com.yingke.mediacodec.utils.FileUtils;
import com.yingke.mediacodec.R;
import com.yingke.mediacodec.transcode.MediaCodecTransCodeManager;
import com.yingke.mediacodec.transcode.listener.ProgressListener;

import java.io.File;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/***
 * MediaCodec 转码
 * */
public class MediaCodecTransCodeActivity extends AppCompatActivity {
    private static final int REQUEST_FOR_VIDEO_FILE = 1000;


    private TextView mVideoInputInfo;
    private TextView mVideoOutputInfo;
    private TextView mIndicator;
    private TextView mProgress;

    private String inputPath;
    private String outputPath;

    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_transcode);

        List<String> permissionReqlist = new ArrayList<String>();

        if (!PermissionUtil.isGranted(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            permissionReqlist.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        }

        String[] reqarray = new String[permissionReqlist.size()];

        for (int i = 0; i < permissionReqlist.size(); i++) {
            reqarray[i] = permissionReqlist.get(i);
        }
        if (reqarray.length > 0)
            ActivityCompat.requestPermissions(this, reqarray, 100);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        initView();
    }

    private void initView() {
        Button btn_select = (Button) findViewById(R.id.btn_select);
        btn_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("video/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, REQUEST_FOR_VIDEO_FILE);
            }
        });


        mVideoInputInfo = findViewById(R.id.tv_input);
        mVideoOutputInfo = findViewById(R.id.tv_output);
        mIndicator = findViewById(R.id.tv_indicator);
        mProgress = findViewById(R.id.tv_progress);
        mProgressBar = findViewById(R.id.pb_compress);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_FOR_VIDEO_FILE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                try {
                    inputPath = Util.getFilePath(this, data.getData());
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(inputPath);
                    String width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                    String height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                    String bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
                    String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    File f = new File(inputPath);
                    long fileSize = f.length();

                    String before = "inputPath:" +inputPath+ "\n"
                            + "width:" + width + "\n"
                            + "height:" + height + "\n"
                            + "bitrate:" + bitrate + "\n"
                            + "fileSize:" + Formatter.formatFileSize(MediaCodecTransCodeActivity.this,fileSize) +"\n" +"duration(ms):"+duration;
                    mVideoInputInfo.setText(before);

                    final String destPath = FileUtils.getOutputFile("MediaCodecTranCode", "trancode", ".mp4").getAbsolutePath();
                    MediaCodecTransCodeManager.convertVideo(inputPath, destPath, 720, 720, 200 * 360 * 30,
                            new ProgressListener() {
                        @Override
                        public void onStart() {
                            mProgressBar.setVisibility(View.VISIBLE);
                            mIndicator.setText("");
                        }

                        @Override
                        public void onFinish(boolean result) {
                            if (result) {
                                mProgress.setText("100%");
                                mProgressBar.setVisibility(View.INVISIBLE);
                                mIndicator.setText("Convert Success!");

                                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                                retriever.setDataSource(destPath);
                                String width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                                String height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                                String bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);

                                File f = new File(destPath);
                                long fileSize = f.length();

                                String after = "outputPath:" +destPath+ "\n"
                                        + "width:" + width + "\n"
                                        + "height:" + height + "\n"
                                        + "bitrate:" + bitrate + "\n"
                                        + "fileSize:" + Formatter.formatFileSize(MediaCodecTransCodeActivity.this,fileSize);
                                mVideoOutputInfo.setText(after);
                            } else {

                                mProgress.setText("0%");
                                mIndicator.setText("Convert Failed!");
                                mProgressBar.setVisibility(View.INVISIBLE);
                                Util.writeFile(MediaCodecTransCodeActivity.this, "Failed Compress!!!" + new SimpleDateFormat("HH:mm:ss").format(new Date()));
                            }
                        }


                        @Override
                        public void onProgress(float percent) {
                            mProgress.setText(String.valueOf(percent) + "%");
                        }
                    });


                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }

            }
        }
    }


    @SuppressWarnings("deprecation")
    public static Locale getSystemLocaleLegacy(Configuration config) {
        return config.locale;
    }

    @TargetApi(Build.VERSION_CODES.N)
    public static Locale getSystemLocale(Configuration config) {
        return config.getLocales().get(0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MediaCodecTransCodeManager.cancelTransCodeTask();
    }
}
