package com.komeya.android.demo.service;

import android.app.Service;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.os.Binder;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;

/**
 * Name: MediaRecordService
 * Author: komeya
 * Email: seventeenxu@aliyun.com
 * Date: 2018/3/30
 * Description:MediaRecord Recoder
 */
public class MediaRecordService extends Service {
	private static final String TAG = "MediaRecordService";
	private static final int FRAME_RATE = 30;//建议>=30
	private static final int BIT_RATE = 5 * 1024 * 1024;
	private MediaProjection mediaProjection;
	private MediaRecorder mediaRecorder;
	private VirtualDisplay virtualDisplay;

	@Override
	public IBinder onBind(Intent intent) {
		return new ScreenRecordBinder();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		HandlerThread serviceThread = new HandlerThread("service_thread",
						android.os.Process.THREAD_PRIORITY_BACKGROUND);
		serviceThread.start();
		mediaRecorder = new MediaRecorder();
	}

	/**
	 * 开始录制
	 */
	public void startRecord() {
		mediaRecorder.start();
	}

	/**
	 * 停止录制
	 */
	public void stopRecord() {
		mediaRecorder.stop();
		mediaRecorder.release();
		virtualDisplay.release();
		mediaProjection.stop();
	}

	/**
	 * 第一步，配置初始化参数
	 *
	 * @param width
	 * @param height
	 * @param dpi
	 * @param destPath
	 */
	public void config(int width, int height, int dpi, String destPath, MediaProjection project) {
		Log.e(TAG, "config() width: " + width + " height: " + height);
		this.mediaProjection = project;
		//不设置以下项表示不带音频
		mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

		mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
		mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		mediaRecorder.setOutputFile(destPath);
		mediaRecorder.setVideoSize(width, width);
		mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
		//不设置以下项表示不带音频
		mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
		mediaRecorder.setVideoEncodingBitRate(BIT_RATE);
		mediaRecorder.setVideoFrameRate(FRAME_RATE);

		try {
			mediaRecorder.prepare();

			virtualDisplay = mediaProjection.createVirtualDisplay("MediaRecordService", width, height, dpi,
							DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaRecorder.getSurface(), null, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public class ScreenRecordBinder extends Binder {
		public MediaRecordService getRecordService() {
			return MediaRecordService.this;
		}
	}
}
