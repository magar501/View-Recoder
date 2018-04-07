package com.komeya.android.demo.ui;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.komeya.android.demo.screenrecoder.R;
import com.komeya.android.demo.service.MediaRecordService;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Name: MediaRecorderActivity
 * Author: komeya
 * Email: seventeenxu@aliyun.com
 * Date: 2018/3/30
 * Description:mediaRecorder recoder view
 */
public class MediaRecorderActivity extends Activity implements SurfaceHolder.Callback {
	private static final String TAG = "MediaRecorderActivity";
	private static final int REQUEST_RECODER_CODE = 0x101;
	private static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 0x102;

	private DisplayMetrics mDisplayMetrics;
	private SurfaceView surfaceView;
	private SurfaceHolder surfaceHolder;
	private Button btnRecoder;
	private MediaRecordService recordService;
	private MediaProjectionManager mProjectionManager;
	private MediaProjection mMediaProjection;

	private int[] yAxis;//Y轴坐标集合
	private int centerY;//中心线的Y坐标
	private int oldX;
	private int oldY;//上一个XY点
	private int currentX;//当前绘制到的X轴上的点

	private boolean isRecodering;

	private Timer timer = new Timer();
	private TimerTask timerTask = new TimerTask() {
		@Override
		public void run() {
			if (currentX == 0) {
				oldX = 0;
			}
			Canvas canvas = surfaceHolder.lockCanvas(new Rect(oldX, surfaceView.getTop(), oldX + currentX, surfaceView.getBottom()));
			if (canvas != null) {
				Log.i("Canvas:", "left:" + oldX + " top:" + surfaceView.getTop() + " right:" + (oldX + currentX) + " bottom:" + (surfaceView.getBottom()));

				Paint mPaint = new Paint();
				mPaint.setColor(Color.GREEN);// 画笔为绿色
				mPaint.setStrokeWidth(2);// 设置画笔粗细

				int y;
				for (int i = oldX + 1; i < currentX; i++) {// 绘画正弦波
					y = yAxis[i - 1];
					canvas.drawLine(oldX, oldY, i, y, mPaint);
					oldX = i;
					oldY = y;
				}
				surfaceHolder.unlockCanvasAndPost(canvas);// 解锁画布，提交画好的图像
			}

			currentX++;//往前进
			if (currentX == yAxis.length - 1) {//如果到了终点，则清屏重来
				canvas = surfaceHolder.lockCanvas(null);
				if (canvas != null) {
					canvas.drawColor(Color.BLACK);// 清除画布
					surfaceHolder.unlockCanvasAndPost(canvas);
					currentX = 0;
					oldY = centerY;
				}
			}
		}
	};

	//为了获取binder对象
	private ServiceConnection connection = new ServiceConnection() {


		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.e(TAG, "onServiceConnected()");
			MediaRecordService.ScreenRecordBinder binder = (MediaRecordService.ScreenRecordBinder) service;
			recordService = binder.getRecordService();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.e(TAG, "onServiceDisconnected()");
		}
	};

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//全屏
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
						WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_media_recoder_demo);

		initView();
		initService();
	}

	/**
	 * 启动service，通过binder获取service对象
	 */
	private void initService() {
		Intent intent = new Intent(this, MediaRecordService.class);
		bindService(intent, connection, BIND_AUTO_CREATE);
	}

	private void initView() {
		//获取屏幕分辨率
		mDisplayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);

		surfaceView = findViewById(R.id.surface_view);
		btnRecoder = findViewById(R.id.btn_recoder);
		btnRecoder.setEnabled(false);

		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(this);

		btnRecoder.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isRecodering) {
					btnRecoder.setText(R.string.start);
					stopRecoder();
				} else {
					btnRecoder.setText(R.string.stop);
					prepareRecoderPermission();
				}
				isRecodering = !isRecodering;
			}
		});
	}

	/**
	 * 录屏相关权限配置
	 */
	private void prepareRecoderPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (!checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) || !checkPermission(Manifest.permission.RECORD_AUDIO)) {
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}
								, REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE);
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
		if (timer != null) {
			timer.cancel();
		}
		if (recordService != null) {
			recordService.stopRecord();
		}
		mMediaProjection = null;
	}

	@Override
	protected void onDestroy() {
		unbindService(connection);
		if (surfaceHolder != null) {
			surfaceHolder.removeCallback(this);
		}
		super.onDestroy();
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_RECODER_CODE) {
			oldY = centerY;
			//动态绘制正弦曲线
			timer.schedule(timerTask, 0, 5);

			mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
			if (mMediaProjection == null) {
				Log.i(TAG, "media projection is null");
				return;
			}

			//config
			File destFile = new File(Environment.getExternalStorageDirectory(), "Movies/" + System.currentTimeMillis() + ".mp4");
			recordService.config(mDisplayMetrics.widthPixels,
							mDisplayMetrics.heightPixels,
							mDisplayMetrics.densityDpi,
							destFile.getAbsolutePath(),
							mMediaProjection);
			recordService.startRecord();
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

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// 初始化y轴数据
		centerY = mDisplayMetrics.heightPixels / 4;
		yAxis = new int[mDisplayMetrics.widthPixels];//点数等于屏幕宽度像素点

		for (int i = 1; i < yAxis.length; i++) {// 计算正弦波
			yAxis[i - 1] = centerY - (int) (80 * Math.sin(i * 2 * Math.PI / 180));
		}

		btnRecoder.setEnabled(true);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (timer != null) {
			timer.cancel();
			timer.purge();
		}
	}

	/**
	 * 应用权限检查
	 *
	 * @param permission
	 * @return
	 */
	private boolean checkPermission(@NonNull String permission) {
		return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
	}

	/**
	 * toast
	 *
	 * @param toastText
	 */
	public void showToast(String toastText) {
		Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_SHORT).show();
	}
}