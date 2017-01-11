package bvideo.lhq.com.bvideo.view;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

import bvideo.lhq.com.bvideo.R;

/**
 * @author lihuiqiang
 * @date 2016-11-1 上午11:13:27
 */
public class TimerRecordView extends View {

    private Paint mRedPaint;

    private Paint mWhitePaint;

    private Paint mTextPaint;

    private Paint mOutsideAnglePaint;

    /**
     * 录制的秒数
     */
    private int mSeconds = 0;

    private String text = "0s";

    private Rect mTextRect;
    /**
     * 外边距
     */
    private int mPadding = 4;
    /**
     * 圆心
     */
    private int center;
    /**
     * 圆弧
     */
    private float sweepAngle = 0;
    /**
     * 控件宽度
     */
    private int mWidth;
    /**
     * 圆环间距
     */
    private int mCirclePadding = 4;
    /**
     * 动态圆环间距
     */
    private int mDynamicCirclePadding = 4;
    /**
     * 外环宽度
     */
    private int mStokeWidth = 8;
    /**
     * 动态外环宽度
     */
    private int mDynamicStokeWidth = 20;

    private RectF mRectF = new RectF();
    /**
     * 未点击
     */
    private final int NORMAL = 0;
    /**
     * 倒计时状态
     */
    private final int RUNNING = NORMAL + 1;
    /**
     * 状态
     */
    private int state = NORMAL;

    public TimerRecordView(Context context) {
        this(context, null);

    }

    public TimerRecordView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);

    }

    public TimerRecordView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initAttrValues(context, attrs);
    }
    /**
     *默认不透明
     */
//	private int mAlpha = 255;
    /**
     * 字体大小
     */
    private int mTextSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, getResources().getDisplayMetrics());
    /**
     * 字体颜色
     */
    private int mTextColor = Color.parseColor("#FFFFFF");
    /**
     * 外圆环背景
     */
    private int outsideCircleColor = Color.parseColor("#ffffff");
    /**
     * 内圆的颜色
     */
    private int insideCircleColor = Color.parseColor("#ff0000");
    /**
     * 动态外圆环颜色
     */
    private int dynamicArcColor = Color.parseColor("#ffeb45");

    private Handler handler = new Handler();

    private void initAttrValues(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs,
                R.styleable.TimerRecordView);
        int n = typedArray.getIndexCount();
        for (int i = 0; i < n; i++) {
            int attr = typedArray.getIndex(i);
            switch (attr) {

                case R.styleable.TimerRecordView_circle_and_outsidecircle_alpha:
//					mAlpha = typedArray.getInt(attr, 255);
                    break;

                case R.styleable.TimerRecordView_text_size:
                    mTextSize = (int) typedArray.getDimension(attr, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics()));
                    break;

                case R.styleable.TimerRecordView_text_color:
                    mTextColor = typedArray.getColor(attr, getResources().getColor(R.color.white));
                    break;

                case R.styleable.TimerRecordView_outside_circle_color:
                    outsideCircleColor = typedArray.getColor(attr, getResources().getColor(R.color.white));
                    break;

                case R.styleable.TimerRecordView_dynamic_arc_color:
                    dynamicArcColor = typedArray.getColor(attr, getResources().getColor(R.color.yellow_light_ffaf23));
                    break;

                case R.styleable.TimerRecordView_inside_circle_color:
                    insideCircleColor = typedArray.getColor(attr, getResources().getColor(R.color.red));
                    break;
            }
        }
        typedArray.recycle();
        init();

    }

    private void init() {
        mWhitePaint = new Paint();
        mWhitePaint.setColor(outsideCircleColor);
        mWhitePaint.setAntiAlias(true);
        mWhitePaint.setDither(true);

        mRedPaint = new Paint();
        mRedPaint.setStyle(Style.FILL);
        mRedPaint.setColor(insideCircleColor);
        mRedPaint.setAntiAlias(true);
        mRedPaint.setDither(true);

        mOutsideAnglePaint = new Paint();
        mOutsideAnglePaint.setStyle(Style.STROKE);
        mOutsideAnglePaint.setStrokeWidth(mStokeWidth);
        mOutsideAnglePaint.setColor(dynamicArcColor);
        mOutsideAnglePaint.setAntiAlias(true);
        mOutsideAnglePaint.setDither(true);

        mTextPaint = new Paint();
        mTextPaint.setColor(mTextColor);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setDither(true);
        mTextRect = new Rect();

        mTextPaint.getTextBounds(text, 0, text.length(), mTextRect);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (state == NORMAL) {
            mWhitePaint.setStyle(Style.FILL);
            mWhitePaint.setAlpha(142);
            canvas.drawCircle(center, center, center - mPadding - mStokeWidth / 2 - mCirclePadding, mWhitePaint);
            mWhitePaint.setStrokeWidth(mStokeWidth);
            mWhitePaint.setStyle(Style.STROKE);
            mWhitePaint.setAlpha(255);
            mWhitePaint.setStrokeWidth(mStokeWidth / 2);
            canvas.drawCircle(center, center, center - mPadding, mWhitePaint);
        } else {
            mRedPaint.setAlpha(200);
            canvas.drawCircle(center, center, center - mPadding - mDynamicStokeWidth / 2 - mDynamicCirclePadding / 2, mRedPaint);
            // -2是感觉文本长度有偏差
            mTextPaint.getTextBounds(text, 0, text.length(), mTextRect);
            canvas.drawText(text, center - mTextRect.width() / 2, center
                    + mTextRect.height() / 2, mTextPaint);
            //画背景圆环
            mWhitePaint.setAlpha(135);
            mWhitePaint.setStrokeWidth(mDynamicStokeWidth / 2);
            canvas.drawCircle(center, center, center - mPadding - mDynamicCirclePadding, mWhitePaint);
            // 动态画外部圆环
            canvas.drawArc(mRectF, 270f, sweepAngle, false, mOutsideAnglePaint);
        }

        super.onDraw(canvas);
    }

    private boolean first = true;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (first) {
            first = false;
            if (getMeasuredHeight() > getMeasuredWidth()) {
                mWidth = getMeasuredWidth();
            } else {
                mWidth = getMeasuredHeight();
            }

            mRectF.set(mPadding + mDynamicCirclePadding, mPadding + mDynamicCirclePadding, mWidth - mPadding - mDynamicCirclePadding, mWidth - mPadding - mDynamicCirclePadding);
            center = mWidth / 2;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                Log.i("lhq", "ACTION_UP=");
                if (state == RUNNING) {
                    state = NORMAL;
                    if (valueAnimator.isRunning()) {
                        valueAnimator.cancel();
                        valueAnimator = null;
                    }
                } else {
                    state = RUNNING;
                    if (onTimerEndListener != null) {
                        start();
                    }else{
                        throw new RuntimeException("the onTimerEndListener must be set!!!");
                    }
                }
                break;
        }
        return true;
    }

    private ValueAnimator valueAnimator;

    @SuppressLint("NewApi")
    public void start() {
        valueAnimator = ValueAnimator.ofFloat(0f, 1f);

        valueAnimator.setDuration(30000);
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator
                .addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        // Log.i("lhq",
                        // "time=="+valueAnimator.getCurrentPlayTime());

                        text = 30 - valueAnimator.getCurrentPlayTime() / 1000
                                + "s";
                        mSeconds = (int) (valueAnimator.getCurrentPlayTime() / 1000);
                        sweepAngle = 360 * (Float) valueAnimator
                                .getAnimatedValue();
                        invalidate();
                    }
                });

        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (onTimerEndListener != null) {
                    // 录制会延时一秒钟，所以延迟1秒结束录制
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            onTimerEndListener.onEndRecord(mSeconds);
                        }
                    }, 1000);
                }
            }
        });

        if (!valueAnimator.isRunning()) {
            valueAnimator.start();
            onTimerEndListener.onStartRecord();
        }
    }

    private OnRecordEndListener onTimerEndListener;

    public void setOnTimerEndListener(OnRecordEndListener onTimerEndListener) {
        this.onTimerEndListener = onTimerEndListener;
    }

    public interface OnRecordEndListener {
        public void onStartRecord();

        public void onEndRecord(int seconds);
    }
}
