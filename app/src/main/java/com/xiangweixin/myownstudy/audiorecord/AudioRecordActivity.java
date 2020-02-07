package com.xiangweixin.myownstudy.audiorecord;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;

public class AudioRecordActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    /**
     * 采样率，现在能够保证在所有设备上使用的采样率是44100Hz, 但是其他的采样率（22050, 16000, 11025）在一些设备上也可以使用。
     */
    public static final int SAMPLE_RATE_INHZ = 44100;

    /**
     * 声道数。CHANNEL_IN_MONO and CHANNEL_IN_STEREO. 其中CHANNEL_IN_MONO是可以保证在所有设备能够使用的。
     */
    public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    /**
     * 返回的音频数据的格式。 ENCODING_PCM_8BIT, ENCODING_PCM_16BIT, and ENCODING_PCM_FLOAT.
     */
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord audioRecord;
    private int recordBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_INHZ, CHANNEL_CONFIG, AUDIO_FORMAT);

    private volatile boolean isRecording = false;
    private byte[] latestData = new byte[recordBufSize];

    private SurfaceView mSurfaceView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSurfaceView = new SurfaceView(this);
        mSurfaceView.getHolder().addCallback(this);
        setContentView(mSurfaceView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    }

    private void startRecord() {
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE_INHZ, CHANNEL_CONFIG, AUDIO_FORMAT, recordBufSize);

        final File file = new File("sdcard/test.pcm");
        if (file.exists()) {
            file.delete();
        }

        audioRecord.startRecording();
        isRecording = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    FileOutputStream fos = new FileOutputStream(file);
                    while (isRecording) {
                        int read = audioRecord.read(latestData, 0, recordBufSize);
                        if (read != AudioRecord.ERROR_INVALID_OPERATION) {
                            fos.write(latestData);
                            Log.i("XWXDebug", "recording....");
                        }
                    }

                    Log.i("XWXDebug", "recording finish.");
                    fos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void stopRecord() {
        isRecording = false;
        if (audioRecord != null) {
            audioRecord.stop();
//            audioRecord.release();
//            audioRecord = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startRecord();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopRecord();
    }
}
