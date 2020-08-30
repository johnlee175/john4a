package com.johnsoft.library.view;

import java.util.HashMap;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.EditText;

/**
 * @author John Kenrinus Lee 
 * @date 2014-7-18
 */
public class RichEditText extends EditText
{
	HashMap<String, BitmapDrawable> smallEmoMap;
	TouchEventListener touchEventListener;
	boolean drawableRightEnabled;
	Drawable normalDrawableRight;
	Drawable enableDrawableRight;
	int drawableRightTouchErrorLimit;
	
    public RichEditText(Context context) 
    {  
        super(context); 
    }
  
    public RichEditText(Context context, AttributeSet attrs) 
    {  
        super(context, attrs);  
    }  
  
    @Override  
    protected void onDraw(Canvas canvas) 
    {  
        super.onDraw(canvas);  
    }
    
    @Override  
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) 
    {  
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);  
    }
    
    public void setDrawableLeft(int left)
    {
    	setCompoundDrawablesWithIntrinsicBounds(left, 0, 0, 0);
    }
    
    public void setDrawableLeft(Drawable left)
    {
    	setCompoundDrawablesWithIntrinsicBounds(left, null, null, null);
    }
    
    public void setDrawableRight(Drawable normalDraw, Drawable enableDraw, TouchEventListener l) 
    {  
    	touchEventListener = l;
    	normalDrawableRight = normalDraw;
    	enableDrawableRight = enableDraw;
        setCompoundDrawablesWithIntrinsicBounds(null, null, normalDraw, null);  
    }  
    
    public void setDrawableRight(int normalDraw, int enableDraw, TouchEventListener l) 
    {  
    	touchEventListener = l;
    	normalDrawableRight = getResources().getDrawable(normalDraw);
    	enableDrawableRight = getResources().getDrawable(enableDraw);
        setCompoundDrawablesWithIntrinsicBounds(0, 0, normalDraw, 0);  
    }
    
    public boolean isDrawableRightEnabled()
	{
		return drawableRightEnabled;
	}
    
    public void setDrawableRightEnabled(boolean drawableRightEnabled)
	{
		this.drawableRightEnabled = drawableRightEnabled;
		if(drawableRightEnabled)
			setCompoundDrawablesWithIntrinsicBounds(null, null, enableDrawableRight, null);
		else
			setCompoundDrawablesWithIntrinsicBounds(null, null, normalDrawableRight, null);
	}
    
    /**设置响应右边图标点击事件的误差范围*/
    public void setDrawableRightTouchErrorLimit(int drawableRightTouchErrorLimit)
	{
		this.drawableRightTouchErrorLimit = drawableRightTouchErrorLimit;
	}
    
    /** 在文本中混排表情*/
    public void insertIcon(String emoticons, Drawable d)
    {
    	Editable editable = getText();
    	int length = getText().length();
    	
    	SpannableStringBuilder ss = new SpannableStringBuilder();
    	ss.append(editable);
    	ss.append(emoticons);
    	
//    	ImageSpan[] imageSpans = editable.getSpans(0, length, ImageSpan.class);
//    	//new一个SpannableString里面包含EditText已有内容，另外添加一个字符串[/xx/]用于在后面替换一个图片
//        SpannableString ss = new SpannableString(editable.toString() + emoticons);
//        for(ImageSpan is : imageSpans)
//        {//将以往的表情添加进去
//        	 ss.setSpan(is, editable.getSpanStart(is), editable.getSpanEnd(is), editable.getSpanFlags(is));
//        }
        
        //设置图片大小以合适文本框
        d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        Drawable drawable = resizeBitmapToView(emoticons, d);
        //将新添加的Drawable图片实例化为一个ImageSpan型
        ImageSpan span = new ImageSpan((drawable != null ? drawable : d), ImageSpan.ALIGN_BASELINE);
        //将ImageSpan代替之前添加的[/xx/]字符串
        ss.setSpan(span, length, length + emoticons.length(),  
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        setText(ss);
      //设置光标位置到最后
        setSelection(getText().length());
    }
    
    protected Drawable resizeBitmapToView(String emoticons, Drawable d)
	{
//    	int size = (int)((d.getIntrinsicHeight() << 1) / 3.0f);
    	int size = (int)getTextSize() + 1;
    	if(d instanceof BitmapDrawable)
        {
        	if(smallEmoMap == null)
        		smallEmoMap = new HashMap<String, BitmapDrawable>();
        	BitmapDrawable bd = smallEmoMap.get(emoticons);
        	if(bd == null)
        	{
        		bd = new BitmapDrawable(getResources(), ((BitmapDrawable)d).getBitmap());
        		bd.setBounds(0, 0, size, size);
        		smallEmoMap.put(emoticons, bd);
        	}
        	return bd;
        }
    	return null;
	}

	public void deleteLastOne()
    {
    	Editable editable = getText();
    	//先取出所有的span
    	Object[] spans = editable.getSpans(0, editable.length(), Object.class);
    	int selectionStart = -1, selectionEnd = -1;
    	for(Object span : spans)
    	{//这次遍历是为获得选中的区域
    		if(span == Selection.SELECTION_START)
    		{
    			selectionStart = editable.getSpanEnd(span);
    		}
    		else if(span == Selection.SELECTION_END)
    		{
    			selectionEnd = editable.getSpanEnd(span);
    		}
    	}
    	if(selectionStart < selectionEnd)
    	{//用户选中了某个区域
    		editable.delete(selectionStart, selectionEnd);
    	}else{//此时selectionStart == selectionEnd; 用户没有选中任何字符, selectionStart代表光标所在位置
    		int lastImageSpanEnd = -1/* 最后一个ImageSpan的截止位置 */, maxImageSpanIndex = -1/* 最后一个ImageSpan的数组索引*/ ;
    		for(int i = 0; i < spans.length; ++i)
        	{
        		 if(spans[i] instanceof ImageSpan)
        		 {
        			 int end = editable.getSpanEnd(spans[i]);
        			 if(end > lastImageSpanEnd && end <= selectionStart/*这里的最后一个是相对于光标位置*/)
        			 {
        				 lastImageSpanEnd = end;
        				 maxImageSpanIndex = i;
        			 }
        		 }
        	}
    		if(lastImageSpanEnd == selectionStart)
    		{ //最后面的是一个ImageSpan, 从字符串中删除ImageSpan所占的字符比如[/xx/],并且要从Span列表中删除它
    			editable.delete(editable.getSpanStart(spans[maxImageSpanIndex]), lastImageSpanEnd);
    			editable.removeSpan(spans[maxImageSpanIndex]);
    		}else{//最后面的是一个普通文本字符,直接删掉
    			int st = selectionStart - 1;
    			editable.delete(st < 0 ? 0 : st, selectionStart);
    		}
    	}
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
    	if(event.getAction() == MotionEvent.ACTION_UP)
    	{//保证触发在手指弹起状态
    		float x = event.getX();
    		Rect rect = getCompoundDrawables()[2].getBounds();
    		int vxs = getWidth() - getPaddingRight() - rect.width();
    		int vxe = getWidth() - getPaddingRight();
    		if(x > (vxs - drawableRightTouchErrorLimit) && x < (vxe + drawableRightTouchErrorLimit))
    		{//保证按在了右边的图标上
    			touchEventListener.onTouched(new TouchEvent(this, event, true, drawableRightEnabled));
    			return true;
    		} else {
    			touchEventListener.onTouched(new TouchEvent(this, event, false, drawableRightEnabled));
    		}
    	}
    	return super.onTouchEvent(event);
    }
    
    public interface TouchEventListener
    {
    	void onTouched(TouchEvent event);
    }

	public static class TouchEvent
	{
		public TouchEvent(RichEditText view, MotionEvent rawEvent,
						  boolean isDrawbleRightClicked, boolean isDrawbleRightStateEnabled)
		{
			super();
			this.view = view;
			this.rawEvent = rawEvent;
			this.isDrawbleRightClicked = isDrawbleRightClicked;
			this.isDrawbleRightStateEnabled = isDrawbleRightStateEnabled;
		}
		public RichEditText view;
		public MotionEvent rawEvent;
		public boolean isDrawbleRightClicked;
		public boolean isDrawbleRightStateEnabled;
	}
}  