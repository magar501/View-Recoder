package com.komeya.android.demo.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.komeya.android.demo.screenrecoder.R;

/**
 * Name: MainActivity
 * Author: komeya
 * Email: seventeenxu@aliyun.com
 * Date: 2018/3/30
 * Description: demo home
 */
public class MainActivity extends Activity {
	private static final String TAG = "MainActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	/**
	 * MediaCodec 录屏
	 *
	 * @param view
	 */
	public void codecRecoder(View view) {
		startActivity(new Intent(MainActivity.this, MediaCodecActivity.class));
	}

	/**
	 * MediaRecoder 录屏
	 *
	 * @param view
	 */
	public void mediaRecorder(View view) {
		startActivity(new Intent(MainActivity.this, MediaRecorderActivity.class));
	}
}