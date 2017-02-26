package com.littlejie.circleprogress;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * 带有刻度的圆形进度条
 * Created by littlejie on 2017/2/26.
 */

public class DialProgress extends View {

    private static final String TAG = DialProgress.class.getSimpleName();
    private Context mContext;

    //圆心坐标
    private float mCenterX, mCenterY;
    private float mRadius;

    private boolean antiAlias;
    //绘制数值
    private Paint mValuePaint;
    private int mValueColor;
    private float mMaxValue;
    private float mValue;
    private float mValueSize;
    private String mPrecisionFormat;

    //绘制单位
    private Paint mUnitPaint;
    private float mUnitSize;
    private int mUnitColor;
    private CharSequence mUnit;
    //前景圆弧
    private Paint mArcPaint;
    private int mArcColor1, mArcColor2, mArcColor3;
    private float mArcWidth;
    private int mDialIntervalDegree;
    private float mStartAngle, mSweepAngle;
    private RectF mRectF;
    //渐变
    private int[] mGradientColors = {Color.GREEN, Color.YELLOW, Color.RED};
    //当前进度，[0.0f,1.0f]
    private float mPercent;
    //动画时间
    private long mAnimTime;
    //属性动画
    private ValueAnimator mAnimator;

    //背景圆弧
    private Paint mBgArcPaint;
    private int mBgArcColor;

    //刻度线颜色
    private Paint mDialPaint;
    private float mDialWidth;
    private int mDialColor;

    private int mDefaultSize;
    private int[] mLocationOnScreen = new int[2];

    public DialProgress(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mContext = context;
        mDefaultSize = MiscUtil.dipToPx(context, 150);
        mRectF = new RectF();
        getLocationOnScreen(mLocationOnScreen);
        initConfig(context, attrs);
        initPaint();
        setValue(mValue);
    }

    private void initConfig(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.DialProgress);

        antiAlias = typedArray.getBoolean(R.styleable.DialProgress_antiAlias, true);
        mMaxValue = typedArray.getFloat(R.styleable.DialProgress_maxValue, Constant.DEFAULT_MAX_VALUE);
        mValue = typedArray.getFloat(R.styleable.DialProgress_value, Constant.DEFAULT_VALUE);
        mValueSize = typedArray.getDimension(R.styleable.DialProgress_valueSize, Constant.DEFAULT_VALUE_SIZE);
        mValueColor = typedArray.getColor(R.styleable.DialProgress_valueColor, Color.BLACK);
        mDialIntervalDegree = typedArray.getInt(R.styleable.DialProgress_dialIntervalDegree, 10);
        int precision = typedArray.getInt(R.styleable.DialProgress_precision, 0);
        mPrecisionFormat = MiscUtil.getPrecisionFormat(precision);

        mUnit = typedArray.getString(R.styleable.DialProgress_unit);
        mUnitColor = typedArray.getColor(R.styleable.DialProgress_unitColor, Color.BLACK);
        mUnitSize = typedArray.getDimension(R.styleable.DialProgress_unitSize, Constant.DEFAULT_UNIT_SIZE);
        mUnitColor = typedArray.getColor(R.styleable.DialProgress_unitColor, Color.BLACK);

        mArcWidth = typedArray.getDimension(R.styleable.DialProgress_arcWidth, Constant.DEFAULT_ARC_WIDTH);
        mArcColor1 = typedArray.getColor(R.styleable.DialProgress_arcColor1, Color.GREEN);
        mArcColor2 = typedArray.getColor(R.styleable.DialProgress_arcColor2, Color.YELLOW);
        mArcColor3 = typedArray.getColor(R.styleable.DialProgress_arcColor3, Color.RED);
        mGradientColors = new int[]{mArcColor1, mArcColor2, mArcColor3};

        mStartAngle = typedArray.getFloat(R.styleable.DialProgress_startAngle, Constant.DEFAULT_START_ANGLE);
        mSweepAngle = typedArray.getFloat(R.styleable.DialProgress_sweepAngle, Constant.DEFAULT_SWEEP_ANGLE);

        mAnimTime = typedArray.getInt(R.styleable.DialProgress_animTime, Constant.DEFAULT_ANIM_TIME);

        mBgArcColor = typedArray.getColor(R.styleable.DialProgress_bgArcColor, Color.GRAY);
        mDialWidth = typedArray.getDimension(R.styleable.DialProgress_dialWidth, 2);
        mDialColor = typedArray.getColor(R.styleable.DialProgress_dialColor, Color.WHITE);

        typedArray.recycle();
    }

    private void initPaint() {
        mValuePaint = new Paint();
        mValuePaint.setAntiAlias(antiAlias);
        mValuePaint.setTextSize(mValueSize);
        mValuePaint.setColor(mValueColor);
        mValuePaint.setTypeface(Typeface.DEFAULT_BOLD);
        mValuePaint.setTextAlign(Paint.Align.CENTER);

        mUnitPaint = new Paint();
        mUnitPaint.setAntiAlias(antiAlias);
        mUnitPaint.setTextSize(mUnitSize);
        mUnitPaint.setColor(mUnitColor);
        mUnitPaint.setTextAlign(Paint.Align.CENTER);

        mArcPaint = new Paint();
        mArcPaint.setAntiAlias(antiAlias);
        mArcPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setStrokeWidth(mArcWidth);
        mArcPaint.setStrokeCap(Paint.Cap.BUTT);

        mBgArcPaint = new Paint();
        mBgArcPaint.setAntiAlias(antiAlias);
        mBgArcPaint.setStyle(Paint.Style.STROKE);
        mBgArcPaint.setStrokeWidth(mArcWidth);
        mBgArcPaint.setStrokeCap(Paint.Cap.BUTT);
        mBgArcPaint.setColor(mBgArcColor);

        mDialPaint = new Paint();
        mDialPaint.setAntiAlias(antiAlias);
        mDialPaint.setColor(mDialColor);
        mDialPaint.setStrokeWidth(mDialWidth);
    }

    /**
     * 更新圆弧画笔
     */
    private void updateArcPaint() {
        // 设置渐变
        // 渐变的颜色是360度，如果只显示270，那么则会缺失部分颜色
        SweepGradient sweepGradient = new SweepGradient(mCenterX, mCenterY, mGradientColors, null);
        mArcPaint.setShader(sweepGradient);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int measuredWidth = MiscUtil.measure(widthMeasureSpec, mDefaultSize);
        int measuredHeight = MiscUtil.measure(heightMeasureSpec, mDefaultSize);
        setMeasuredDimension(measuredWidth, measuredHeight);
        int minSize = Math.min(getMeasuredWidth() - getPaddingLeft() - getPaddingRight() - 2 * (int) mArcWidth,
                getMeasuredHeight() - getPaddingTop() - getPaddingBottom() - 2 * (int) mArcWidth);
        mRadius = minSize / 2;
        mCenterX = getMeasuredWidth() / 2;
        mCenterY = getMeasuredHeight() / 2;
        //绘制圆弧的边界
        mRectF.left = mCenterX - mRadius - mArcWidth / 2;
        mRectF.top = mCenterY - mRadius - mArcWidth / 2;
        mRectF.right = mCenterX + mRadius + mArcWidth / 2;
        mRectF.bottom = mCenterY + mRadius + mArcWidth / 2;
        updateArcPaint();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawArc(canvas);
        drawDial(canvas);
        drawText(canvas);
    }

    private void drawArc(Canvas canvas) {
        // 绘制背景圆弧
        // 从进度圆弧结束的地方开始重新绘制，优化性能
        float currentAngle = mSweepAngle * mPercent;
        canvas.save();
        canvas.rotate(mStartAngle, mCenterX, mCenterY);
        canvas.drawArc(mRectF, currentAngle, mSweepAngle - currentAngle, false, mBgArcPaint);
        // 第一个参数 oval 为 RectF 类型，即圆弧显示区域
        // startAngle 和 sweepAngle  均为 float 类型，分别表示圆弧起始角度和圆弧度数
        // 3点钟方向为0度，顺时针递增
        // 如果 startAngle < 0 或者 > 360,则相当于 startAngle % 360
        // useCenter:如果为True时，在绘制圆弧时将圆心包括在内，通常用来绘制扇形
        canvas.drawArc(mRectF, 0, currentAngle, false, mArcPaint);
        canvas.restore();
    }

    private void drawDial(Canvas canvas) {
        int total = (int) (mSweepAngle / mDialIntervalDegree);
        canvas.save();
        canvas.rotate(mStartAngle, mCenterX, mCenterY);
        for (int i = 0; i <= total; i++) {
            canvas.drawLine(mCenterX + mRadius, mCenterY, mCenterX + mRadius + mArcWidth, mCenterY, mDialPaint);
            canvas.rotate(mDialIntervalDegree, mCenterX, mCenterY);
        }
        canvas.restore();
    }

    private void drawText(Canvas canvas) {
        float y = mCenterY - (mValuePaint.descent() + mValuePaint.ascent()) / 2;
        canvas.drawText(String.format(mPrecisionFormat, mValue), mCenterX, y, mValuePaint);

        if (mUnit != null) {
            float uy = mCenterY * 4 / 3 - (mUnitPaint.descent() + mUnitPaint.ascent()) / 2;
            canvas.drawText(mUnit.toString(), mCenterX, uy, mUnitPaint);
        }
    }

    public float getMaxValue() {
        return mMaxValue;
    }

    public void setMaxValue(float maxValue) {
        mMaxValue = maxValue;
    }

    /**
     * 设置当前值
     *
     * @param value
     */
    public void setValue(float value) {
        if (value > mMaxValue) {
            value = mMaxValue;
        }
        float start = mPercent;
        float end = value / mMaxValue;
        startAnimator(start, end, mAnimTime);
    }

    private void startAnimator(float start, float end, long animTime) {
        mAnimator = ValueAnimator.ofFloat(start, end);
        mAnimator.setDuration(animTime);
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mPercent = (float) animation.getAnimatedValue();
                mValue = mPercent * mMaxValue;
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "onAnimationUpdate: percent = " + mPercent
                            + ";currentAngle = " + (mSweepAngle * mPercent)
                            + ";value = " + mValue);
                }
                invalidate();
            }
        });
        mAnimator.start();
    }

    public void setGradientColors(int[] gradientColors) {
        mGradientColors = gradientColors;
        updateArcPaint();
    }

    public void reset() {
        startAnimator(mPercent, 0.0f, 1000L);
    }
}