package com.zhidao.myapplication.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.zhidao.myapplication.R;

public class UpdateProgressBar extends View {

    private int mProgressStartColor;
    private int mProgressMidColor;
    private int mProgressEndColor;
    private int mBgStartColor;
    private int mBgMidColor;
    private float mProgressWidth;
    private int mStartAngle;
    private static final int HALF_CIRCLE = 180;
    private boolean mStrokeCap;
    private boolean isFirstEnd = false;
    private boolean isShowAnimator = false;
    private int mDefaultAllAnimDuration;

    private int mMeasureHeight;
    private int mMeasureWidth;

    private Paint mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private Paint mBGCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private Paint mProgressPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private Paint mProgressOutPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private Paint mProgressPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private RectF pRectF;
    private RectF pCenterRectF;

    //球
    private Bitmap mLititleBitmap;  // 圆点图片
    private Matrix mMatrix;             // 矩阵,用于对图片进行一些操作
    private float[] pos;                // 当前点的实际位置
    private float[] tan;                // 当前点的tangent值,用于计算图片所需旋转的角度


    private int mCurProgress = 0;
    private ValueAnimator valueAnimator;

    public UpdateProgressBar(Context context) {
        this(context, null);
    }

    public UpdateProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public UpdateProgressBar(Context context,  AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //setLayerType(LAYER_TYPE_SOFTWARE, null);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.UpdateProgressBar);

        // percentCover 模式特有属性
        mProgressStartColor = ta.getColor(R.styleable.UpdateProgressBar_cover_start_color, Color.YELLOW);
        mProgressMidColor = ta.getColor(R.styleable.UpdateProgressBar_cover_mid_color, mProgressStartColor);
        mProgressEndColor = ta.getColor(R.styleable.UpdateProgressBar_cover_end_color, mProgressStartColor);
        mBgStartColor = ta.getColor(R.styleable.UpdateProgressBar_cover_bg_start_color, Color.LTGRAY);
        mBgMidColor = ta.getColor(R.styleable.UpdateProgressBar_cover_bg_mid_color, mBgStartColor);

        mStrokeCap = ta.getBoolean(R.styleable.UpdateProgressBar_cover_stroke_cap, false);

        // percentSplice 模式特有属性 暂无

        // 公共属性
        mProgressWidth = ta.getDimension(R.styleable.UpdateProgressBar_progress_width, 8f);
        mStartAngle = 90;
        mDefaultAllAnimDuration = 2000;
        ta.recycle();


        mBgPaint.setStyle(Paint.Style.STROKE);
        if (mStrokeCap) {
            mBgPaint.setStrokeCap(Paint.Cap.ROUND);
        }
        mBgPaint.setStrokeWidth(mProgressWidth);

        mProgressPaint.setStyle(Paint.Style.STROKE);
        mProgressPaint.setStrokeCap(Paint.Cap.ROUND);
        mProgressPaint.setAntiAlias(true);
        mProgressPaint.setStrokeWidth(mProgressWidth);


        mProgressOutPaint.setStyle(Paint.Style.STROKE);
        //mProgressOutPaint.setStrokeCap(Paint.Cap.ROUND);
        mProgressOutPaint.setAntiAlias(true);
        mProgressOutPaint.setStrokeWidth(mProgressWidth);
        mProgressOutPaint.setMaskFilter(new BlurMaskFilter(10, BlurMaskFilter.Blur.NORMAL));

        mProgressPointPaint.setStyle(Paint.Style.STROKE);
        mProgressPointPaint.setStrokeCap(Paint.Cap.ROUND);
        mProgressPointPaint.setAntiAlias(true);
        mProgressPointPaint.setStrokeWidth(mProgressWidth);
        mProgressPointPaint.setMaskFilter(new BlurMaskFilter(5, BlurMaskFilter.Blur.NORMAL));

        mBGCirclePaint = new Paint();
        mBGCirclePaint.setAntiAlias(true);
        mBGCirclePaint.setStyle(Paint.Style.FILL);
        mBGCirclePaint.setStrokeCap(Paint.Cap.ROUND);

       /* mProgressPointPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        mProgressPointPaint.setStyle(Paint.Style.FILL);
        mProgressPointPaint.setAntiAlias(true);*/

        initPointBitmap();


    }

    private void initPointBitmap() {
        mMatrix = new Matrix();
        pos = new float[2];
        tan = new float[2];
      /*  mLititleBitmap = ((BitmapDrawable) getResources()
                .getDrawable(R.mipmap.ring_round))
                .getBitmap();*/
        mLititleBitmap = compressSampling();
    }

    private Bitmap compressSampling() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        options.outWidth = (int) mProgressWidth;
        options.outHeight = (int) mProgressWidth;
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ring_round, options);
        return bm;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mMeasureWidth = getMeasuredWidth();
        mMeasureHeight = getMeasuredHeight();
        if (pRectF == null) {
            float halfProgressWidth = mProgressWidth / 2;
            pRectF = new RectF(halfProgressWidth + getPaddingLeft(),
                    halfProgressWidth + getPaddingTop(),
                    mMeasureWidth - halfProgressWidth - getPaddingRight(),
                    mMeasureHeight - halfProgressWidth - getPaddingBottom());
        }

        if (pCenterRectF == null) {
            float halfProgressWidth = mProgressWidth / 2;
            pCenterRectF = new RectF(halfProgressWidth + getPaddingLeft() + 15,
                    halfProgressWidth + getPaddingTop() + 15,
                    mMeasureWidth - halfProgressWidth - getPaddingRight() - 15,
                    mMeasureHeight - halfProgressWidth - getPaddingBottom() - 15);
        }
    }

    public boolean isRing() {
        if (valueAnimator != null) {
            return valueAnimator.isRunning();
        } else {
            return false;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
/*
        if (!isFirstEnd) {
            drawProgress(canvas);
        } else {
            drawRoate(canvas);
        }
*/

        drawBgProgress(canvas);
        Log.d("chencl_  " ,"chencl_123 " +isShowAnimator );
        if (isShowAnimator) {
            showLoadedCenterUI(canvas);
            showLoadedCriUI(canvas);
        } else {
            drawProgress(canvas);
        }
    }

    private void drawRoate(Canvas canvas) {
        Log.d("chencl_","chencl_  " + mCurProgress);
        canvas.save();
        canvas.rotate(mCurProgress - 90, mMeasureWidth / 2, mMeasureHeight / 2);

        for (int i = 0; i <= 360; i++) {

            float fraction = 0;
            if (i < HALF_CIRCLE) {
                fraction = i / (float) HALF_CIRCLE;
                mProgressPaint.setColor(getGradient(fraction, mProgressEndColor, mProgressMidColor));
            } else {
                fraction = (i - HALF_CIRCLE) / (float) HALF_CIRCLE;
                mProgressPaint.setColor(getGradient(fraction, mProgressMidColor, mProgressEndColor));
            }

            canvas.drawArc(pRectF,
                    startAngle + i,
                    1,
                    false,
                    mProgressPaint);
        }


        Path orbit = new Path();
        canvas.setDrawFilter(
                new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        //按照圆心旋转
        Matrix matrix = new Matrix();
        matrix.setRotate(mStartAngle, mMeasureWidth / 2, mMeasureHeight / 2);
        orbit.addArc(pRectF, startAngle + 4, 359);
        // 创建 PathMeasure
        PathMeasure measure = new PathMeasure(orbit, false);
        measure.getPosTan(measure.getLength() * 1, pos, tan);
        mMatrix.reset();
        mMatrix.postScale(2, 2);
        mMatrix.postTranslate(pos[0] - mLititleBitmap.getWidth(), pos[1] - mLititleBitmap.getHeight());   // 将图片绘制中心调整到与当前点重合
        canvas.drawBitmap(mLititleBitmap, mMatrix, mProgressPointPaint);//绘制球
        //mProgressPointPaint.setColor(getGradient(startAngle / (float) halfProgressSweep, mProgressMidColor, mProgressEndColor));
        mProgressPointPaint.setColor(Color.parseColor("#72ECFF"));
        canvas.drawCircle(pos[0], pos[1], 3, mProgressPointPaint);

        canvas.restore();

    }


    private void showLoadedCenterUI(Canvas canvas) {
        canvas.save();
        mBGCirclePaint.setColor(mBgStartColor);
        canvas.drawArc(pCenterRectF, 0, 360, true, mBGCirclePaint);
        canvas.restore();

    }

    private void showLoadedCriUI(Canvas canvas) {
        canvas.save();
        canvas.rotate(mCurProgress - 90, mMeasureWidth / 2, mMeasureHeight / 2);

        for (int i = 0; i <= 360; i++) {

            float fraction = 0;
            if (i < HALF_CIRCLE) {
                fraction = i / (float) HALF_CIRCLE;
                mProgressPaint.setColor(getGradient(fraction, mProgressEndColor, mProgressMidColor));
            } else {
                fraction = (i - HALF_CIRCLE) / (float) HALF_CIRCLE;
                mProgressPaint.setColor(getGradient(fraction, mProgressMidColor, mProgressEndColor));
            }

            canvas.drawArc(pRectF,
                    startAngle + i,
                    1,
                    false,
                    mProgressPaint);
        }
        canvas.restore();
    }

    private void showDefaultAnimator() {
        //if (valueAnimator == null) {
            valueAnimator = ObjectAnimator.ofFloat(0, 360);
       // }
        valueAnimator.setDuration((long) (mDefaultAllAnimDuration));
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mCurProgress = (int) (float) animation.getAnimatedValue();
                postInvalidate();
            }
        });

        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                isFirstEnd = true;
                Log.d("chencl_  66666666", " true  ");
                showDefaultAnimator();
            }
        });
        valueAnimator.start();
    }

    // 画进度之外的背景
    private void drawBgProgress(Canvas canvas) {
        canvas.save();
        mBgPaint.setColor(mBgStartColor);
        canvas.drawArc(pRectF, 0, 360, false, mBgPaint);
        canvas.restore();
    }

    private int startAngle = 1;

    private void drawProgress(Canvas canvas) {

//        if (!isFirstEnd) {
        canvas.save();
        canvas.rotate(-90, mMeasureWidth / 2, mMeasureHeight / 2);
           /* mProgressOutPaint.setColor(Color.WHITE);

            canvas.drawArc(pRectF,
                    startAngle,
                    startAngle + mCurProgress,
                    false,
                    mProgressOutPaint);
            */

        for (int i = 0; i <= mCurProgress; i++) {

            float fraction = 0;
            if (i < HALF_CIRCLE) {
                fraction = i / (float) HALF_CIRCLE;
                mProgressPaint.setColor(getGradient(fraction, mProgressEndColor, mProgressMidColor));
            } else {
                fraction = (i - HALF_CIRCLE) / (float) HALF_CIRCLE;
                mProgressPaint.setColor(getGradient(fraction, mProgressMidColor, mProgressEndColor));
            }

            canvas.drawArc(pRectF,
                    startAngle + i,
                    1,
                    false,
                    mProgressPaint);

        }


        Path orbit = new Path();
        canvas.setDrawFilter(
                new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        //按照圆心旋转
        Matrix matrix = new Matrix();
        matrix.setRotate(mStartAngle, mMeasureWidth / 2, mMeasureHeight / 2);
        orbit.addArc(pRectF, mCurProgress, 359);
        // 创建 PathMeasure
        PathMeasure measure = new PathMeasure(orbit, false);
        measure.getPosTan(measure.getLength() * 1, pos, tan);
        mMatrix.reset();
        mMatrix.postScale(2, 2);
        mMatrix.postTranslate(pos[0] - mLititleBitmap.getWidth(), pos[1] - mLititleBitmap.getHeight());   // 将图片绘制中心调整到与当前点重合
        canvas.drawBitmap(mLititleBitmap, mMatrix, mProgressPointPaint);//绘制球
        //mProgressPointPaint.setColor(getGradient(startAngle / (float) halfProgressSweep, mProgressMidColor, mProgressEndColor));
        mProgressPointPaint.setColor(Color.parseColor("#72ECFF"));
        canvas.drawCircle(pos[0], pos[1], 5, mProgressPointPaint);
        canvas.restore();

//        }
    }


    public void stopRingAnimator() {
        isShowAnimator = true;
        if (valueAnimator != null && valueAnimator.isRunning()) {
            valueAnimator.cancel();
            valueAnimator.removeAllUpdateListeners();
            valueAnimator = null;
        }
        invalidate();
    }

    public void startRingAnimator() {
        isShowAnimator = false;
        showDefaultAnimator();
    }


    private int getGradient(float fraction, int startColor, int endColor) {
        if (fraction > 1) fraction = 1;
        int alphaStart = Color.alpha(startColor);
        int redStart = Color.red(startColor);
        int blueStart = Color.blue(startColor);
        int greenStart = Color.green(startColor);
        int alphaEnd = Color.alpha(endColor);
        int redEnd = Color.red(endColor);
        int blueEnd = Color.blue(endColor);
        int greenEnd = Color.green(endColor);
        int alphaDifference = alphaEnd - alphaStart;
        int redDifference = redEnd - redStart;
        int blueDifference = blueEnd - blueStart;
        int greenDifference = greenEnd - greenStart;
        int alphaCurrent = (int) (alphaStart + fraction * alphaDifference);
        int redCurrent = (int) (redStart + fraction * redDifference);
        int blueCurrent = (int) (blueStart + fraction * blueDifference);
        int greenCurrent = (int) (greenStart + fraction * greenDifference);
        return Color.argb(alphaCurrent, redCurrent, greenCurrent, blueCurrent);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isShowAnimator = false;
        if (valueAnimator != null && valueAnimator.isRunning()) {
            valueAnimator.cancel();
            valueAnimator.removeAllUpdateListeners();
            valueAnimator = null;
        }

    }

}