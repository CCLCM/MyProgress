package com.zhidao.myapplication.view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.Build;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import com.zhidao.myapplication.R;


/**
 * 水波进度条
 * Created by littlejie on 2017/2/26.
 */

public class WaveProgress extends View {

    private static final String TAG = WaveProgress.class.getSimpleName();

    //浅色波浪方向
    private static final int L2R = 0;
    private static final int R2L = 1;

    /**
     * 截取的路径
     */
    private Path mDstPath;

    /**
     * Path 长度
     */
    private float mPathLength;

    private PathMeasure mPathMeasure;
    /**
     * View 是个正方形，宽高中小的一个值,根据小的值来定位绘制
     */
    private int mRealSize;

    public static final int ANIMATOR_TIME = 1000;
    private int mDefaultSize;
    //圆心
    private Point mCenterPoint;
    //半径
    private float mRadius;
    //圆的外接矩形
    private RectF mRectF;
    private RectF mBGRectF;
    //深色波浪移动距离
    private float mDarkWaveOffset;
    //浅色波浪移动距离
    private float mLightWaveOffset;
    private float mRightMarkOffset;
    //浅色波浪方向
    private boolean isR2L;
    //是否锁定波浪不随进度移动
    private boolean lockWave;

    //是否开启抗锯齿
    private boolean antiAlias;
    //最大值
    private float mMaxValue;
    //当前值
    private float mValue;
    private float mDesValue;
    //当前进度
    private float mPercent;

    //绘制提示
    private TextPaint mHintPaint;
    private CharSequence mHint;
    private int mHintColor;
    private float mHintSize;

    private Paint mPercentPaint;
    private Paint mDesPaint;
    private Paint mRightMarkPaint;
    private float mValueSize;
    private float mDesValueSize;
    private int mValueColor;
    private int mDesValueColor;
    private int mCircenterColor;
    //圆环宽度
    private float mCircleWidth;
    //圆环
    private Paint mCirclePaint;
    private Paint mBGCirclePaint;
    //背景圆环颜色
    private int mBgCircleColor;

    //水波路径
    private Path mWaveLimitPath;
    private Path mWavePath;
    //水波高度
    private float mWaveHeight;
    //水波数量
    private int mWaveNum;
    //深色水波
    private Paint mWavePaint;
    //深色水波颜色
    private int mDarkWaveColor;
    //浅色水波颜色
    private int mLightWaveColor;

    //深色水波贝塞尔曲线上的起始点、控制点
    private Point[] mDarkPoints;
    //浅色水波贝塞尔曲线上的起始点、控制点
    private Point[] mLightPoints;

    //贝塞尔曲线点的总个数
    private int mAllPointCount;
    private int mHalfPointCount;
    private boolean isDownloadComplete = false;
    private ValueAnimator mProgressAnimator;
    private long mDarkWaveAnimTime;
    private ValueAnimator mDarkWaveAnimator;
    private long mLightWaveAnimTime;
    private ValueAnimator mLightWaveAnimator;
    /**
     * 对号
     */
    private ValueAnimator mRightMarkValueAnimator;

    public WaveProgress(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mDefaultSize = MiscUtil.dipToPx(context, 150);
        mRectF = new RectF();
        mBGRectF = new RectF();
        mCenterPoint = new Point();

        initAttrs(context, attrs);
        initPaint();
        initPath();
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.WaveProgress);

        antiAlias = typedArray.getBoolean(R.styleable.WaveProgress_antiAlias, true);
        mDarkWaveAnimTime = typedArray.getInt(R.styleable.WaveProgress_darkWaveAnimTime, 1000);
        mLightWaveAnimTime = typedArray.getInt(R.styleable.WaveProgress_lightWaveAnimTime, 1000);
        mMaxValue = typedArray.getFloat(R.styleable.WaveProgress_maxValue, 100);

        mValue = typedArray.getFloat(R.styleable.WaveProgress_value, 0);
        mValueSize = typedArray.getDimension(R.styleable.WaveProgress_valueSize, 15);
        mValueColor = typedArray.getColor(R.styleable.WaveProgress_valueColor, Color.BLACK);


        mDesValue = typedArray.getFloat(R.styleable.WaveProgress_des_value, 0);
        mDesValueSize = typedArray.getDimension(R.styleable.WaveProgress_des_valueSize, 15);
        mDesValueColor = typedArray.getColor(R.styleable.WaveProgress_des_valueColor, Color.BLACK);


        mCircenterColor = typedArray.getColor(R.styleable.WaveProgress_CircenterColor,Color.parseColor("#1ad8d8d8"));


        mHint = typedArray.getString(R.styleable.WaveProgress_hint);
        mHintColor = typedArray.getColor(R.styleable.WaveProgress_hintColor, Color.BLACK);
        mHintSize = typedArray.getDimension(R.styleable.WaveProgress_hintSize, 15);

        mCircleWidth = typedArray.getDimension(R.styleable.WaveProgress_circleWidth, 15);
        mBgCircleColor = typedArray.getColor(R.styleable.WaveProgress_bgCircleColor, Color.WHITE);

        mWaveHeight = typedArray.getDimension(R.styleable.WaveProgress_waveHeight, 40);
        mWaveNum = typedArray.getInt(R.styleable.WaveProgress_waveNum, 1);
        mDarkWaveColor = typedArray.getColor(R.styleable.WaveProgress_darkWaveColor,
                getResources().getColor(android.R.color.holo_blue_dark));
        mLightWaveColor = typedArray.getColor(R.styleable.WaveProgress_lightWaveColor,
                getResources().getColor(android.R.color.holo_green_light));

        isR2L = typedArray.getInt(R.styleable.WaveProgress_lightWaveDirect, R2L) == R2L;
        lockWave = typedArray.getBoolean(R.styleable.WaveProgress_lockWave, false);

        typedArray.recycle();
    }

    private void initPaint() {
        mHintPaint = new TextPaint();
        // 设置抗锯齿,会消耗较大资源，绘制图形速度会变慢。
        mHintPaint.setAntiAlias(antiAlias);
        // 设置绘制文字大小
        mHintPaint.setTextSize(mHintSize);
        // 设置画笔颜色
        mHintPaint.setColor(mHintColor);
        // 从中间向两边绘制，不需要再次计算文字
        mHintPaint.setTextAlign(Paint.Align.CENTER);

        mCirclePaint = new Paint();
        mCirclePaint.setAntiAlias(antiAlias);
        mCirclePaint.setStrokeWidth(mCircleWidth);
        mCirclePaint.setStyle(Paint.Style.STROKE);
        mCirclePaint.setStrokeCap(Paint.Cap.ROUND);

        mBGCirclePaint = new Paint();
        mBGCirclePaint.setAntiAlias(antiAlias);
        mBGCirclePaint.setStyle(Paint.Style.FILL);
        mBGCirclePaint.setStrokeCap(Paint.Cap.ROUND);

        mWavePaint = new Paint();
        mWavePaint.setAntiAlias(antiAlias);
        mWavePaint.setStyle(Paint.Style.FILL);

        mPercentPaint = new Paint();
        mPercentPaint.setTextAlign(Paint.Align.CENTER);
        mPercentPaint.setAntiAlias(antiAlias);
        mPercentPaint.setColor(mValueColor);
        mPercentPaint.setTextSize(mValueSize);

        mDesPaint = new Paint();
        mDesPaint.setTextAlign(Paint.Align.CENTER);
        mDesPaint.setColor(mDesValueColor);
        mDesPaint.setTextSize(mDesValueSize);

        mRightMarkPaint = new Paint();
        mRightMarkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRightMarkPaint.setStyle(Paint.Style.STROKE);
        mRightMarkPaint.setStrokeJoin(Paint.Join.ROUND);
        mRightMarkPaint.setStrokeWidth(mDesValueSize / 2);
        mRightMarkPaint.setAntiAlias(true);
        mRightMarkPaint.setColor(Color.WHITE);
    }

    private void initPath() {
        mWaveLimitPath = new Path();
        mWavePath = new Path();

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(MiscUtil.measure(widthMeasureSpec, mDefaultSize),
                MiscUtil.measure(heightMeasureSpec, mDefaultSize));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.d(TAG, "onSizeChanged: w = " + w + "; h = " + h + "; oldw = " + oldw + "; oldh = " + oldh);
        int minSize = Math.min(getMeasuredWidth() - getPaddingLeft() - getPaddingRight() - 2 * (int) mCircleWidth,
                getMeasuredHeight() - getPaddingTop() - getPaddingBottom() - 2 * (int) mCircleWidth);
        mRadius = minSize / 2;
        mCenterPoint.x = getMeasuredWidth() / 2;
        mCenterPoint.y = getMeasuredHeight() / 2;
        //绘制圆弧的边界
        mRectF.left = mCenterPoint.x - mRadius - mCircleWidth / 2 - 10;
        mRectF.top = mCenterPoint.y - mRadius - mCircleWidth / 2 - 10;
        mRectF.right = mCenterPoint.x + mRadius + mCircleWidth / 2 + 10;
        mRectF.bottom = mCenterPoint.y + mRadius + mCircleWidth / 2 + 10;

        mBGRectF.left = mCenterPoint.x - mRadius - mCircleWidth / 2;
        mBGRectF.top = mCenterPoint.y - mRadius - mCircleWidth / 2;
        mBGRectF.right = mCenterPoint.x + mRadius + mCircleWidth / 2;
        mBGRectF.bottom = mCenterPoint.y + mRadius + mCircleWidth / 2;
        Log.d(TAG, "onSizeChanged: 控件大小 = " + "(" + getMeasuredWidth() + ", " + getMeasuredHeight() + ")"
                + ";圆心坐标 = " + mCenterPoint.toString()
                + ";圆半径 = " + mRadius
                + ";圆的外接矩形 = " + mRectF.toString());
        initWavePoints();
        //开始动画
        setValue(mValue);
        setValue(mDesValue);
        startWaveAnimator();
        initMarkAnimator();
    }

    private void initWavePoints() {
        //当前波浪宽度
        float waveWidth = (mRadius * 2) / mWaveNum;
        mAllPointCount = 8 * mWaveNum + 1;
        mHalfPointCount = mAllPointCount / 2;
        mDarkPoints = getPoint(false, waveWidth);
        mLightPoints = getPoint(isR2L, waveWidth);
    }

    /**
     * 从左往右或者从右往左获取贝塞尔点
     *
     * @return
     */
    private Point[] getPoint(boolean isR2L, float waveWidth) {
        Point[] points = new Point[mAllPointCount];
        //第1个点特殊处理，即数组的中点
        points[mHalfPointCount] = new Point((int) (mCenterPoint.x + (isR2L ? mRadius : -mRadius)), mCenterPoint.y);
        //屏幕内的贝塞尔曲线点
        for (int i = mHalfPointCount + 1; i < mAllPointCount; i += 4) {
            float width = points[mHalfPointCount].x + waveWidth * (i / 4 - mWaveNum);
            points[i] = new Point((int) (waveWidth / 4 + width), (int) (mCenterPoint.y - mWaveHeight));
            points[i + 1] = new Point((int) (waveWidth / 2 + width), mCenterPoint.y);
            points[i + 2] = new Point((int) (waveWidth * 3 / 4 + width), (int) (mCenterPoint.y + mWaveHeight));
            points[i + 3] = new Point((int) (waveWidth + width), mCenterPoint.y);
        }
        //屏幕外的贝塞尔曲线点
        for (int i = 0; i < mHalfPointCount; i++) {
            int reverse = mAllPointCount - i - 1;
            points[i] = new Point((isR2L ? 2 : 1) * points[mHalfPointCount].x - points[reverse].x,
                    points[mHalfPointCount].y * 2 - points[reverse].y);
        }
        //对从右向左的贝塞尔点数组反序，方便后续处理
        return isR2L ? MiscUtil.reverse(points) : points;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isDownloadComplete) {
            drawDownAllCenter(canvas);
            drawRightMark(canvas);
        } else {
            drawLightWave(canvas);
            drawDarkWave(canvas);
        }

        drawBG(canvas);
        drawProgress(canvas);
        drawCircle(canvas);
    }

    public void showRightMarkAnimator() {
        stopWaveAnimator();
        mRightMarkValueAnimator.start();
    }

    private void drawRightMark(Canvas canvas) {
        if (mDstPath == null) {
            return;
        }
        // 刷新当前截取 Path
        mDstPath.reset();

        // 避免硬件加速的Bug
        mDstPath.lineTo(0, 0);

        // 截取片段
        float stop = mPathLength * mRightMarkOffset;
        mPathMeasure.getSegment(0, stop, mDstPath, true);
        // 绘制截取的片段
        canvas.drawPath(mDstPath, mRightMarkPaint);
    }

    /**
     * 关联对号 Path
     */
    private void initRightMarkPath() {
        mPathMeasure = new PathMeasure();
        mRealSize = getWidth();
        Path path = new Path();
        // 对号起点
        float startX = (float) (0.35 * mRealSize);
        float startY = (float) (0.4 * mRealSize);
        path.moveTo(startX, startY);
        // 对号拐角点
        float cornerX = (float) (0.46 * mRealSize);
        float cornerY = (float) (0.5 * mRealSize);
        path.lineTo(cornerX, cornerY);
        // 对号终点
        float endX = (float) (0.70 * mRealSize);
        float endY = (float) (0.25 * mRealSize);
        path.lineTo(endX, endY);

        // 重新关联Path
        mPathMeasure.setPath(path, false);

        // 此时为对号 Path 的长度
        mPathLength = mPathMeasure.getLength();
        mDstPath = new Path();
    }

    private void drawBG(Canvas canvas) {
        canvas.save();
        canvas.rotate(270, mCenterPoint.x, mCenterPoint.y);
        mBGCirclePaint.setColor(mCircenterColor);
        canvas.drawArc(mBGRectF, 0, 360, true, mBGCirclePaint);
        canvas.restore();
    }

    private void drawDownAllCenter(Canvas canvas) {
        canvas.save();
        mBGCirclePaint.setColor(mDarkWaveColor);
        canvas.drawArc(mBGRectF, 0, 360, true, mBGCirclePaint);
        canvas.restore();
    }


    /**
     * 绘制圆环
     *
     * @param canvas
     */
    private void drawCircle(Canvas canvas) {
        canvas.save();
        //画背景圆环
        mCirclePaint.setColor(mBgCircleColor);
        canvas.drawArc(mRectF, 0, 360, false, mCirclePaint);
        canvas.restore();
    }

    /**
     * 绘制深色波浪(贝塞尔曲线)
     *
     * @param canvas
     */
    private void drawDarkWave(Canvas canvas) {
        mWavePaint.setColor(mDarkWaveColor);
        drawWave(canvas, mWavePaint, mDarkPoints, mDarkWaveOffset);
    }

    /**
     * 绘制浅色波浪(贝塞尔曲线)
     *
     * @param canvas
     */
    private void drawLightWave(Canvas canvas) {
        mWavePaint.setColor(mLightWaveColor);
        //从右向左的水波位移应该被减去
        drawWave(canvas, mWavePaint, mLightPoints, isR2L ? -mLightWaveOffset : mLightWaveOffset);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void drawWave(Canvas canvas, Paint paint, Point[] points, float waveOffset) {
        mWaveLimitPath.reset();
        mWavePath.reset();
        float height = lockWave ? 0 : mRadius - 2 * mRadius * mPercent;
        //moveTo和lineTo绘制出水波区域矩形
        mWavePath.moveTo(points[0].x + waveOffset, points[0].y + height);

        for (int i = 1; i < mAllPointCount; i += 2) {
            mWavePath.quadTo(points[i].x + waveOffset, points[i].y + height,
                    points[i + 1].x + waveOffset, points[i + 1].y + height);
        }
        //mWavePath.lineTo(points[mAllPointCount - 1].x, points[mAllPointCount - 1].y + height);
        //不管如何移动，波浪与圆路径的交集底部永远固定，否则会造成上移的时候底部为空的情况
        mWavePath.lineTo(points[mAllPointCount - 1].x, mCenterPoint.y + mRadius);
        mWavePath.lineTo(points[0].x, mCenterPoint.y + mRadius);
        mWavePath.close();
        mWaveLimitPath.addCircle(mCenterPoint.x, mCenterPoint.y, mRadius, Path.Direction.CW);
        //取该圆与波浪路径的交集，形成波浪在圆内的效果
        mWaveLimitPath.op(mWavePath, Path.Op.INTERSECT);
        canvas.drawPath(mWaveLimitPath, paint);
    }

    //前一次绘制时的进度
    private float mPrePercent;
    //当前进度值
    private String mPercentValue;

    private void drawProgress(Canvas canvas) {
        float y = mCenterPoint.y - (mPercentPaint.descent() + mPercentPaint.ascent()) / 2;

        if (mPrePercent == 0.0f || Math.abs(mPercent - mPrePercent) >= 0.01f) {
            mPercentValue = String.format("%.00f%%", mPercent * 100);
            mPrePercent = mPercent;
        }

        if (!isDownloadComplete && mPrePercent > 0.01f) {
            canvas.drawText(mPercentValue, mCenterPoint.x, y, mPercentPaint);
            canvas.drawText("正在下载", mCenterPoint.x, y + mValueSize, mDesPaint);
        } else {
            //canvas.drawText("建立下载", mCenterPoint.x, y, mPercentPaint);
            if (isDownloadComplete) {
                canvas.drawText("下载完成", mCenterPoint.x, y + mValueSize, mDesPaint);
            } else {
                canvas.drawText("正在建立下载", mCenterPoint.x, y + mValueSize, mDesPaint);
            }
        }

        if (mHint != null) {
            float hy = mCenterPoint.y * 2 / 3 - (mHintPaint.descent() + mHintPaint.ascent()) / 2;
            canvas.drawText(mHint.toString(), mCenterPoint.x, hy, mHintPaint);
        }
    }

    public float getMaxValue() {
        return mMaxValue;
    }

    public void setMaxValue(float maxValue) {
        mMaxValue = maxValue;
    }

    public float getValue() {
        return mValue;
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
        Log.d(TAG, "setValue, value = " + value + ";start = " + start + "; end = " + end);
        startAnimator(start, end, mDarkWaveAnimTime);
    }

    private void startAnimator(final float start, float end, long animTime) {
        Log.d(TAG, "startAnimator,value = " + mValue
                + ";start = " + start + ";end = " + end + ";time = " + animTime);
        //当start=0且end=0时，不需要启动动画
        if (start == 0 && end == 0) {
            return;
        }
        mProgressAnimator = ValueAnimator.ofFloat(start, end);
        mProgressAnimator.setDuration(animTime);
        mProgressAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mPercent = (float) animation.getAnimatedValue();
                if (mPercent == 0.0f) {
                    stopWaveAnimator();
                } else {
                    startWaveAnimator();
                }
                mValue = mPercent * mMaxValue;
                invalidate();
            }
        });
        mProgressAnimator.start();
    }

    private void startWaveAnimator() {
        startLightWaveAnimator();
        startDarkWaveAnimator();
        initMarkAnimator();
    }

    private void initMarkAnimator() {
        if (mRightMarkValueAnimator != null && mRightMarkValueAnimator.isRunning()) {
            return;
        }
        mRightMarkValueAnimator = ValueAnimator.ofFloat(0, 1);
        // 动画过程
        mRightMarkValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mRightMarkOffset = (float) animation.getAnimatedValue();
                invalidate();
            }
        });

        // 动画时间
        mRightMarkValueAnimator.setDuration(ANIMATOR_TIME);

        // 插值器
        mRightMarkValueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        post(new Runnable() {
            @Override
            public void run() {
                initRightMarkPath();
            }
        });
    }

    private void stopWaveAnimator() {
        if (mDarkWaveAnimator != null && mDarkWaveAnimator.isRunning()) {
            mDarkWaveAnimator.cancel();
            mDarkWaveAnimator.removeAllUpdateListeners();
            mDarkWaveAnimator = null;
        }
        if (mLightWaveAnimator != null && mLightWaveAnimator.isRunning()) {
            mLightWaveAnimator.cancel();
            mLightWaveAnimator.removeAllUpdateListeners();
            mLightWaveAnimator = null;
        }
    }

    private void startLightWaveAnimator() {
        if (mLightWaveAnimator != null && mLightWaveAnimator.isRunning()) {
            return;
        }
        mLightWaveAnimator = ValueAnimator.ofFloat(0, 2 * mRadius);
        mLightWaveAnimator.setDuration(mLightWaveAnimTime);
        mLightWaveAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mLightWaveAnimator.setInterpolator(new LinearInterpolator());
        mLightWaveAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mLightWaveOffset = (float) animation.getAnimatedValue();
                postInvalidate();
            }
        });
        mLightWaveAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mLightWaveOffset = 0;
            }

            @Override
            public void onAnimationEnd(Animator animation) {

            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mLightWaveAnimator.start();
    }

    private void startDarkWaveAnimator() {
        if (mDarkWaveAnimator != null && mDarkWaveAnimator.isRunning()) {
            return;
        }
        mDarkWaveAnimator = ValueAnimator.ofFloat(0, 2 * mRadius);
        mDarkWaveAnimator.setDuration(mDarkWaveAnimTime);
        mDarkWaveAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mDarkWaveAnimator.setInterpolator(new LinearInterpolator());
        mDarkWaveAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mDarkWaveOffset = (float) animation.getAnimatedValue();
                postInvalidate();
            }
        });
        mDarkWaveAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mDarkWaveOffset = 0;
            }

            @Override
            public void onAnimationEnd(Animator animation) {

            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mDarkWaveAnimator.start();
    }

    public void setDownloadComplete(boolean b) {
        isDownloadComplete = b;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopWaveAnimator();
        if (mProgressAnimator != null && mProgressAnimator.isRunning()) {
            mProgressAnimator.cancel();
            mProgressAnimator.removeAllUpdateListeners();
            mProgressAnimator = null;
        }

        // 取消对号动画
        boolean isRightMarkNeedCancel =
                (mRightMarkValueAnimator != null && mRightMarkValueAnimator.isRunning());
        if (isRightMarkNeedCancel) {
            isDownloadComplete = false;
            mRightMarkValueAnimator.cancel();
        }
    }
}
