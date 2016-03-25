package com.johnsoft.library.util.media;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * <uses-permission android:name="android.permission.RECORD_AUDIO"/>
 * write PCM/WAVE
 */
public class AudioRecorder
{
	public static final String TAG = "AudioRecorder";
	
	private AudioRecord mAudioRecord;
	private short[] mBuffer;
	
	private int sampleRateInHz = 44100; //11025, 22050, 44100
	private int channelConfig = AudioFormat.CHANNEL_IN_MONO; //AudioFormat.CHANNEL_IN_STEREO
	private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;//As now the vast majority of audio is 16
	private int audioSource = MediaRecorder.AudioSource.MIC; //MediaRecorder.AudioSource.VOICE_RECOGNITION...
	
	private int minBufferSize;
	private boolean prepared;
	private boolean started;
	
	private OnRecordListener listener;
	
	public AudioRecorder(OnRecordListener listener)
	{
		this.listener = listener;
	}
	
	public void prepare()
	{
		minBufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat); 
		mAudioRecord = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, minBufferSize);
		if(mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED)
		{
			Log.e(TAG, "AudioRecorder prepare failed");
			prepared = false;
			return;
		}
		mBuffer = new short[minBufferSize];
		prepared = true;
	}
	
	public void start()
	{
		if(!prepared)
			return;
		mAudioRecord.startRecording();
		if(mAudioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING)
		{
			Log.e(TAG, "AudioRecorder start failed");
			started = false;
			return;
		}
		started = true;
		while(started)
		{
			int sampleReadCount = mAudioRecord.read(mBuffer, 0, minBufferSize);
			listener.record(mAudioRecord, mBuffer, sampleReadCount);
		}
	}
	
	public void stop()
	{
		if(started)
		{
			started = false;
			mAudioRecord.stop();
		}
	}
	
	public void destroy()
	{
		if(prepared)
		{
			prepared = false;
			mAudioRecord.release();
		}
		mAudioRecord = null;
		mBuffer = null;
	}

	public final int getSampleRateInHz()
	{
		return sampleRateInHz;
	}

	public final void setSampleRateInHz(int sampleRateInHz)
	{
		this.sampleRateInHz = sampleRateInHz;
	}

	public final int getChannelConfig()
	{
		return channelConfig;
	}

	public final void setChannelConfig(int channelConfig)
	{
		this.channelConfig = channelConfig;
	}

	public final int getAudioFormat()
	{
		return audioFormat;
	}

	public final void setAudioFormat(int audioFormat)
	{
		this.audioFormat = audioFormat;
	}

	public final int getAudioSource()
	{
		return audioSource;
	}

	public final void setAudioSource(int audioSource)
	{
		this.audioSource = audioSource;
	}

	public final int getMinBufferSize()
	{
		return minBufferSize;
	}

	public final boolean isPrepared()
	{
		return prepared;
	}

	public final boolean isStarted()
	{
		return started;
	}
	
	public interface OnRecordListener
	{
		public void record(AudioRecord record, short[] buffer, int readCount);
	}
}
