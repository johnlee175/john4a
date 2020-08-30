package com.johnsoft.library.util.animation;

import java.util.ArrayList;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;

public class AnimationSequence
{
	private ArrayList<Animation> animations = new ArrayList<Animation>();
	private View view;
	
	public AnimationSequence addAnimation(Animation animation)
	{
		this.animations.add(animation);
		return this;
	}
	
	public AnimationSequence addAnimations(Animation...animations)
	{
		for(Animation animation : animations)
			this.animations.add(animation);
		return this;
	}
	
	public AnimationSequence build(View view)
	{
		this.view = view;
		int size = animations.size();
		for(int i = 0; i < size - 1; ++i)
		{
			Animation animation = animations.get(i);
			animation.setAnimationListener(new SequenceAnimationListener(view, animations.get(i + 1)));
		}
		return this;
	}
	
	public void play()
	{
		view.startAnimation(animations.get(0));
	}
	
	public void playOneShot(View view)
	{
		 build(view);
		 int size = animations.size();
		 if(size > 0)
			 animations.get(size - 1).setAnimationListener(new ClearAnimationListener(this));
		 play();
	}
	
	public void clear()
	{
		view = null;
		animations.clear();
	}
	
	private static class SequenceAnimationListener implements AnimationListener
	{
		public void onAnimationStart(Animation animation) {}
		public void onAnimationRepeat(Animation animation){}
		@Override
		public void onAnimationEnd(Animation animation)
		{
			view.startAnimation(this.animation);
			this.view = null;
			this.animation = null;
		}
		public SequenceAnimationListener(View view, Animation animation)
		{
			this.view = view;
			this.animation = animation;
		}
		private Animation animation;
		private View view;
	}
	
	private static class ClearAnimationListener implements AnimationListener
	{
		public void onAnimationStart(Animation animation) {}
		public void onAnimationRepeat(Animation animation) {}
		@Override
		public void onAnimationEnd(Animation animation)
		{
			as.clear();
			as = null;
		}
		public ClearAnimationListener(AnimationSequence as)
		{
			 this.as = as;
		}
		private AnimationSequence as;
	}
}
