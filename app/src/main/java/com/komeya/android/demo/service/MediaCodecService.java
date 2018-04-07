package com.komeya.android.demo.service;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.projection.MediaProjection;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Name: MediaRecorderActivity
 * Author: komeya
 * Email: seventeenxu@aliyun.com
 * Date: 2018/3/30
 * Description:MediaCodec Recoder
 */
public class MediaCodecService extends Thread {
	private static final String TAG = "MediaCodecService";

	private static final String MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
	private static final int FRAME_RATE = 30;
	private static final int BIT_RATE = 5 * 1024 * 1024;
	private static final int IFRAME_INTERVAL = 10;
	private static final int TIMEOUT_US = 10000;

	private int mWidth;
	private int mHeight;
	private int mDpi;
	private String mDestPath;

	private MediaProjection mMediaProjection;
	private MediaCodec mEncoder;
	private Surface mSurface;
	private MediaMuxer mMuxer;
	private boolean mMuxerStarted = false;
	private int mVideoTrackIndex = -1;
	private AtomicBoolean mQuit = new AtomicBoolean(false);
	private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
	private VirtualDisplay mVirtualDisplay;

	public MediaCodecService(int width, int height, int dpi, MediaProjection mp, String destPath) {
		this.mWidth = width;
		this.mHeight = height;
		this.mDpi = dpi;
		this.mMediaProjection = mp;
		this.mDestPath = destPath;
	}

	@Override
	public void run() {
		try {
			initEncoder();
			mMuxer = new MediaMuxer(mDestPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

			mVirtualDisplay = mMediaProjection.createVirtualDisplay(TAG + "-mVirtualDisplay", mWidth, mHeight, mDpi,
							DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, mSurface, null, null);

			while (!mQuit.get()) {
				int index = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_US);
				if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {// 后续输出格式变化
					resetOutputFormat();
				} else if (index >= 0) {// 有效输出
					if (!mMuxerStarted) {
						throw new IllegalStateException("MediaMuxer dose not call addTrack(format)！");
					}
					encodeToVideoTrack(index);
					mEncoder.releaseOutputBuffer(index, false);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			release();
		}

	}

	/**
	 * 硬解码获取实时帧数据并写入mp4文件
	 *
	 * @param index
	 */
	private void encodeToVideoTrack(int index) {
		// 获取到的实时帧视频数据
		ByteBuffer encodedData = mEncoder.getOutputBuffer(index);

		if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
			Log.i(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
			mBufferInfo.size = 0;
		}
		if (mBufferInfo.size == 0) {
			Log.i(TAG, "info.size == 0, drop it.");
			encodedData = null;
		} else {
			Log.i(TAG, "got buffer, info: size=" + mBufferInfo.size + ", presentationTimeUs="
							+ mBufferInfo.presentationTimeUs + ", offset=" + mBufferInfo.offset);
		}
		if (encodedData != null) {
			mMuxer.writeSampleData(mVideoTrackIndex, encodedData, mBufferInfo);
		}
	}

	private void resetOutputFormat() {
		// should happen before receiving buffers, and should only happen once
		if (mMuxerStarted) {
			throw new IllegalStateException("output format already changed!");
		}
		MediaFormat newFormat = mEncoder.getOutputFormat();
		mVideoTrackIndex = mMuxer.addTrack(newFormat);
		mMuxer.start();
		mMuxerStarted = true;
	}

	/**
	 * init MediaCodec
	 *
	 * @throws IOException
	 */
	private void initEncoder() throws IOException {
		MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
		format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
		format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
		format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
		//		int[] colorFormat =  mEncoder.getCodecInfo().getCapabilitiesForType(MIME_TYPE).colorFormats;
		format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

		mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
		mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		mSurface = mEncoder.createInputSurface();
		mEncoder.start();
	}

	/**
	 * 资源释放
	 */
	private void release() {
		if (mEncoder != null) {
			mEncoder.stop();
			mEncoder.release();
			mEncoder = null;
		}
		if (mVirtualDisplay != null) {
			mVirtualDisplay.release();
		}
		if (mMediaProjection != null) {
			mMediaProjection.stop();
		}
		if (mMuxer != null) {
			mMuxer.stop();
			mMuxer.release();
			mMuxer = null;
		}
	}

	/**
	 * stop recoder task 线程开关
	 */
	public final void quit() {
		mQuit.set(true);
	}
}
