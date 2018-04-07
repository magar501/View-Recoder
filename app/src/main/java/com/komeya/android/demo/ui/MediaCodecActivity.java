package com.komeya.android.demo.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.komeya.android.demo.screenrecoder.R;
import com.komeya.android.demo.service.MediaCodecService;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Name: MediaCodecActivity
 * Author: komeya
 * Email: seventeenxu@aliyun.com
 * Date: 2018/3/30
 * Description:mediaCodec recoder screen
 */
public class MediaCodecActivity extends Activity {
	private static final String TAG = "MediaCodecActivity";
	private static final int REQUEST_RECODER_CODE = 0x101;
	private static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 0x102;
	private static final int WHAT_HANDLER = 0x103;

	private MediaCodecService mMediaCodecService;
	private MediaProjectionManager mProjectionManager;
	private DisplayMetrics mDisplayMetrics;
	private MediaProjection mMediaProjection;
	private Button btnRecoder;
	private TextView tvContent;
	private int num;
	private Timer timer = new Timer();
	private Handler handler = new Handler(Looper.myLooper()) {
		@Override
		public void handleMessage(Message msg) {
			Log.e(TAG, "num: " + num);
			if (msg.what == WHAT_HANDLER) {
				if (num <= 0) {
					stopRecoder();
				}
				tvContent.setText(String.valueOf(num));
			}
		}
	};
	private TimerTask timerTask = new TimerTask() {
		@Override
		public void run() {
			num--;
			handler.obtainMessage(WHAT_HANDLER).sendToTarget();
		}
	};

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//全屏
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
						WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_mediacodec_demo);

		initView();
	}

	private void initView() {
		btnRecoder = findViewById(R.id.btn_recoder);
		tvContent = findViewById(R.id.tv_content);

		//倒计时
		num = 10;
		tvContent.setText(String.valueOf(num));

		//获取屏幕分辨率
		mDisplayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);

		btnRecoder.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				btnRecoder.setVisibility(View.GONE);
				prepareRecoderPermission();
			}
		});
	}

	/**
	 * 录屏相关权限配置
	 */
	private void prepareRecoderPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (!checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE);
				return;
			}
		}

		startRecoder();
	}

	/**
	 * 开始录屏
	 */
	private void startRecoder() {
		Log.e(TAG, "startRecoder");
		mProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
		if (mProjectionManager != null) {
			Intent captureIntent = mProjectionManager.createScreenCaptureIntent();
			startActivityForResult(captureIntent, REQUEST_RECODER_CODE);
		}
	}

	/**
	 * 停止录屏
	 */
	private void stopRecoder() {
		Log.e(TAG, "stopRecoder");
		btnRecoder.setVisibility(View.VISIBLE);
		if (timer != null) {
			timer.cancel();
		}
		if (mMediaCodecService != null) {
			mMediaCodecService.quit();
		}
		mMediaCodecService = null;
		mMediaProjection = null;
	}

	@Override
	protected void onDestroy() {
		if (mMediaCodecService != null) {
			mMediaCodecService.quit();
		}
		mMediaCodecService = null;
		super.onDestroy();
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_RECODER_CODE) {
			mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
			if (mMediaProjection == null) {
				Log.i(TAG, "media projection is null");
				return;
			}

			//config
			File destFile = new File(Environment.getExternalStorageDirectory(), "Movies/" + System.currentTimeMillis() + ".mp4");
			int width = mDisplayMetrics.widthPixels;
			int height = mDisplayMetrics.heightPixels;
			mMediaCodecService = new MediaCodecService(width, height, mDisplayMetrics.densityDpi, mMediaProjection, destFile.getAbsolutePath());
			mMediaCodecService.start();
			timer.schedule(timerTask, 1000, 1000);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				startRecoder();
			} else {
				showToast("权限被禁止，无法录屏！");
			}
		}
	}

	private boolean checkPermission(@NonNull String permission) {
		return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
	}

	public void showToast(String toastText) {
		Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_SHORT).show();
	}
}