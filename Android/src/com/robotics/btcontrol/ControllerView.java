package com.robotics.btcontrol;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ControllerView extends View{

	public static final int LEFT_TREAD_ID = 1;
	public static final int RIGHT_TREAD_ID = 2;
	public static final int FULL_STOP = 3;
	
	public final int pointerWidth = 24,
				   	 jsOffsetV = 16,
				   	 jsOffsetH = 0,
				   	 InnerRimWidth = 32,
				   	 OuterRimWidth = 3,
				   	 maxSpeed = 225,
				   	 minSpeed = 25,
				   	 rotationSpeed = 175,
				   	 sensitivity = 5;
	
	public int jsDiameter, 
			   jsRadius, 
			   cursorRadius,			   
			   TARadius, leftSpeed, rightSpeed,
			   steering = 0, 
			   lastRtSpeed = 0, 
			   speedStep = 0,
			   lastLfSpeed = 0;
	
	private Paint OuterRimPaint,
				  InnerRimPaint,
				  TAPaint,
				  CursorPaint;
	
	public Rect jsRect;
	
	public float cursorX, cursorY;
	
	OnSpeedChangedListener spListener;
	
	public interface OnSpeedChangedListener {
		public void OnSpeedChanged(int lfSpd, int rtSpd, int param);
	}
		
	public void setOnSpeedChangedListener(OnSpeedChangedListener listener) {
		spListener = listener;
	}
	
	public ControllerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize();
	}

	private void initialize(){
		OuterRimPaint = new Paint();
		OuterRimPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		OuterRimPaint.setARGB(255, 220, 220, 220);
		
		InnerRimPaint = new Paint();
		InnerRimPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		InnerRimPaint.setARGB(255, 64, 64, 64);
		
		TAPaint = new Paint();
		TAPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		TAPaint.setARGB(128, 128, 128, 128);
		
		CursorPaint = new Paint();
		CursorPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		CursorPaint.setARGB(255, 255, 255, 255);
		
		speedStep = (maxSpeed - minSpeed)/sensitivity;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		if(jsRect != null){
			int cx = jsRect.centerX();
			int cy = jsRect.centerY();
			canvas.drawCircle(cx, cy, jsRadius, OuterRimPaint);
			canvas.drawCircle(cx, cy, jsRadius - OuterRimWidth, InnerRimPaint);
			canvas.drawCircle(cx, cy, jsRadius - OuterRimWidth - InnerRimWidth, TAPaint);
			canvas.drawCircle(cursorX, cursorY, cursorRadius, CursorPaint);
		}
		invalidate();
		super.onDraw(canvas);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// TODO Auto-generated method stub
		int mW = MeasureSpec.getSize(widthMeasureSpec);
		int mH = MeasureSpec.getSize(heightMeasureSpec);
		jsDiameter = mW == 0 ? mH : mH == 0 ? mW : mW < mH ? mW : mH;
		jsDiameter = (int) (jsDiameter * 0.90);
		jsRadius = jsDiameter / 2 ;
		cursorRadius = jsRadius/3;
		int left = (mW - jsDiameter)/2 + jsOffsetH;
		int top = (mH - jsDiameter)/2 + jsOffsetV;
		jsRect = new Rect(left, top, left + jsDiameter, top + jsDiameter);
		cursorX = jsRect.centerX();
		cursorY = jsRect.centerY();
		TARadius = jsRadius - InnerRimWidth - OuterRimWidth - cursorRadius;
		setMeasuredDimension(mW,mH);
		//super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
	
	public void calcSpeed(float x, float y) {
		
		//convert from x-y to polar
		double r = calcDist(x, y);
		double theta = Math.atan2(y-jsRect.centerY(), x-jsRect.centerX());
		
		double fraction = Math.round((sensitivity*theta/(0.5*Math.PI)));
		
		//assign the new x-y coordinate of the cursor
		if (r <= TARadius) {
			cursorX = x;
			cursorY = y;
		} else {
			cursorX = (int) (TARadius*Math.cos(theta)) + jsRect.centerX();
			cursorY = (int) (TARadius*Math.sin(theta)) + jsRect.centerY();
		}
		
		/*
		steering = sensitivity - (int) fraction;
		steering *= speedStep;
		*/
		
		if (theta <= 0) {
			steering = sensitivity + (int) fraction;
			steering *= speedStep;
			if (steering >= 0) {
				rightSpeed = maxSpeed;
				leftSpeed = maxSpeed - Math.abs(steering);
				if (Math.abs(leftSpeed) == minSpeed) {
					leftSpeed = rotationSpeed;
					rightSpeed = -rotationSpeed;					
				}
			} else {
				rightSpeed = maxSpeed - Math.abs(steering);
				leftSpeed = maxSpeed;
				if (Math.abs(rightSpeed) == minSpeed) {
					leftSpeed = -rotationSpeed;
					rightSpeed = rotationSpeed;
				}
			}
		} else {
			steering = sensitivity - (int) fraction;
			steering *= speedStep;
			if (steering >= 0) {
				rightSpeed = -maxSpeed;
				leftSpeed = -maxSpeed + Math.abs(steering);
				if (Math.abs(leftSpeed) == minSpeed) {
					leftSpeed = rotationSpeed;
					rightSpeed = -rotationSpeed;
				}
			} else {
				rightSpeed = -maxSpeed + Math.abs(steering);
				leftSpeed = -maxSpeed;
				if (Math.abs(rightSpeed) == minSpeed) {
					leftSpeed = -rotationSpeed;
					rightSpeed = rotationSpeed;
				}

			}
		}
		
		if( leftSpeed != lastLfSpeed || rightSpeed != lastRtSpeed) {
			lastLfSpeed = leftSpeed;
			lastRtSpeed = rightSpeed;
			if (spListener != null) spListener.OnSpeedChanged(leftSpeed, rightSpeed, 0);
		}
		
	}
	
	public double calcDist(double x, double y) {
		return Math.sqrt(Math.pow(x-jsRect.centerX(), 2) + Math.pow(y-jsRect.centerY(), 2));
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent me) {
		// TODO Auto-generated method stub
		float x = me.getX();
		float y = me.getY();
		if (me.getAction() == MotionEvent.ACTION_DOWN) {
			if(calcDist(x,y)  <= cursorRadius) {
				return true;
			} else {
				return false;
			}
		} else if (me.getAction() == MotionEvent.ACTION_UP) {
			calcSpeed(jsRect.centerX(),jsRect.centerY());
			steering = 0;
			lastLfSpeed = lastRtSpeed = 0;
			if (spListener != null) spListener.OnSpeedChanged(0, 0, FULL_STOP);
			return true;
		} else if (me.getAction() == MotionEvent.ACTION_MOVE) {
			calcSpeed(x,y);
			return true;
		}
		return false;
	}
	
}
