package com.johnsoft.library.util.media;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

/**
 * read PCM/WAVE
 */
public class AudioTracker
{
	public static final String TAG = "AudioTracker";
	
	private AudioTrack mAudioTrack;
	private short[] mBuffer;
	
	private int sampleRateInHz = 8000; //11025, 22050, 44100
	private int channelConfig = AudioFormat.CHANNEL_OUT_MONO; //AudioFormat.CHANNEL_OUT_STEREO
	private int audioFormat = AudioFormat.ENCODING_PCM_16BIT; //As now the vast majority of audio is 16
	private int streamType = AudioManager.STREAM_MUSIC; //AudioManager.STREAM_VOICE_CALL
	private int mode = AudioTrack.MODE_STREAM;
	
	private int minBufferSize;
	private boolean prepared;
	private boolean started;
	
	private OnTrackListener listener;
	
	public AudioTracker(OnTrackListener listener)
	{
		this.listener = listener;
	}
	
	public void prepare()
	{
		minBufferSize = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
		mAudioTrack = new AudioTrack(streamType, sampleRateInHz, channelConfig, audioFormat, minBufferSize, mode);
		if(mAudioTrack.getState() != AudioTrack.STATE_INITIALIZED)
		{
			Log.e(TAG, "AudioTracker prepare failed");
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
		mAudioTrack.play();
		if(mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING)
		{
			Log.e(TAG, "AudioTracker start failed");
			started = false;
			return;
		}
		started = true;
		while(started)
		{
			int readCount = listener.track(mAudioTrack, mBuffer);
			mAudioTrack.write(mBuffer, 0, readCount);
		}
	}
	
	public void stop()
	{
		if(started)
		{
			started = false;
			mAudioTrack.stop();
		}
	}
	
	public void destroy()
	{
		if(prepared)
		{
			prepared = false;
			mAudioTrack.release();
		}
		mAudioTrack = null;
		mBuffer = null;
	}
	
	public int getSampleRateInHz()
	{
		return sampleRateInHz;
	}

	public void setSampleRateInHz(int sampleRateInHz)
	{
		this.sampleRateInHz = sampleRateInHz;
	}

	public int getChannelConfig()
	{
		return channelConfig;
	}

	public void setChannelConfig(int channelConfig)
	{
		this.channelConfig = channelConfig;
	}

	public int getAudioFormat()
	{
		return audioFormat;
	}

	public void setAudioFormat(int audioFormat)
	{
		this.audioFormat = audioFormat;
	}
	
	public int getStreamType()
	{
		return streamType;
	}
	
	public void setStreamType(int streamType)
	{
		this.streamType = streamType;
	}

	public int getMinBufferSize()
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

	public interface OnTrackListener
	{
		public int track(AudioTrack track, short[] buffer);
	}
}
