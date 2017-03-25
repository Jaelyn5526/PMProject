package jaelyn.pm;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.util.Random;

/**
 * Created by zaric on 17-03-21.
 */

public class PMSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable{

    //onTouch的各种状态
    private static final int STATE_DOWN = 0;
    private static final int STATE_MOVE = 1;
    private static final int STATE_UP = 2;
    private static final int STATE_FLY = 3;
    private static final int STATE_NORMAL = -1;

    private int mTotalPM = 400;

    private Context context;
    private int mInterpolatorMode = 3;
    private int mPathMode = 0;

    /**
     * 角度的范围 0 - 2*PI
     */
    /*private double mPointAngleStar = 0;
    private double mPointAngleStop = 2 * Math.PI;*/
    private double mPointAngleDur = Math.PI / 2;
    /**
     * 点在同心圆的取值范围
     * mPointStarR,mPointStopR  半径长度
     * mPointStarDx, mPointStopDx 同心圆的宽
     */
    private float mPointStarDx = 50, mPointStarR = 50;
    private float mPointStopDx = 60, mPointStopR = 300;

    private Random mRandom;

    private Paint mPaint, mClearPaint;
    private Paint mPaintTest1, mPaintTest2, mPaintArc;

    private SurfaceHolder mHolder;
    private boolean isRunning = false;
    private float mCX = 500, mCY = 1000;
    private PMElement[] mPms = new PMElement[mTotalPM];

    private int runTime = 0;
    private int runTimeCount = 1;


    //当前onTouch的状态
    private int touchState = STATE_NORMAL;
    private Path fingerPath = new Path();
    private PointF currentPoint = new PointF();
    private PathMeasure fingerPathMea = new PathMeasure();
    private float fingerPathLenght = 0;
    private float fingerPathAngle = 0;

    public PMSurfaceView(Context context) {
        super(context);
        initView(context);
    }

    public PMSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public PMSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    public void initView(Context context) {
        this.context = context;
        mPaint = new Paint();
        mPaint.setColor(Color.argb(255, 255, 200, 51));
        mPaint.setStrokeWidth(px2dp(10));
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStyle(Paint.Style.FILL);

        mPaintTest1 = new Paint();
        mPaintTest1.setColor(Color.argb(150, 255, 0, 0));
        mPaintTest1.setStrokeWidth(px2dp(10));
        mPaintTest1.setAntiAlias(true);
        mPaintTest1.setDither(true);
        mPaintTest1.setStyle(Paint.Style.STROKE);

        mPaintTest2 = new Paint();
        mPaintTest2.setColor(Color.argb(150, 0, 255, 0));
        mPaintTest2.setStrokeWidth(px2dp(10));
        mPaintTest2.setAntiAlias(true);
        mPaintTest2.setDither(true);
        mPaintTest2.setStyle(Paint.Style.STROKE);

        mPaintArc = new Paint();
        mPaintArc.setColor(Color.argb(135, 122, 200, 255));
        mPaintArc.setStrokeWidth(px2dp(10));
        mPaintArc.setAntiAlias(true);
        mPaintArc.setDither(true);
        mPaintArc.setStyle(Paint.Style.FILL);

        mRandom = new Random(System.currentTimeMillis());
        mClearPaint = new Paint();
        mClearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mHolder = getHolder();
        mHolder.addCallback(this);

        mPointStarDx = px2dp(100);
        mPointStopDx = px2dp(120);

        mPointStarR = px2dp(20);
        mPointStopR = px2dp(300);

    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        isRunning = true;
        Thread thread = new Thread(this);
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        isRunning = false;
    }

    @Override
    public void run() {
//        creatPM();
        while (isRunning) {
            runTime ++;
            if (touchState != STATE_NORMAL && runTime % 8 == 0){
                if (touchState == STATE_UP){
                    touchState = STATE_NORMAL;
                }
                creatPM();
            }
            if (runTime % 16 == 0){
                runTime = 0;
                draw();
                move();
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mCX = w / 2;
        mCY = h / 2;
        mPointStarR = px2dp(10);
        mPointStopR = w / 5 * 2;
    }

    private void draw() {
        Canvas canvas = mHolder.lockCanvas();
        canvas.drawPaint(mClearPaint);
        //绘制参考线
        drawAssist(canvas);
        for (int i = 0; i < mTotalPM; i++) {
            if (mPms[i] != null && mPms[i].progress <= 1){
                mPaint.setAlpha((int) ((1 - mPms[i].progress) * 175 + 80));
//                Shader shader = new RadialGradient(mPms[i].x, mPms[i].y, mPaint.getStrokeWidth()/2, Color.RED, Color.TRANSPARENT, Shader.TileMode.MIRROR);
//                mPaint.setShader(shader);
                canvas.drawPoint(mPms[i].x, mPms[i].y, mPaint);
                drawDerive(canvas, mPms[i]);
            }
        }
        mHolder.unlockCanvasAndPost(canvas);
    }

    private void drawDerive(Canvas canvas, PMElement pm){
        double dx;
        double pmDx;
        double x;
        double y;
        for (int i = 0; i < pm.dervie.length; i+=2) {
            pmDx = pm.progress * pm.pathLenght + pm.dervie[i];
            dx = Math.abs(pmDx / Math.cos(pm.dervie[i+1]));
            x = dx * Math.sin(pm.angle + pm.dervie[i+1]) + pm.centerPoint.x;
            y = dx * Math.cos(pm.angle + pm.dervie[i+1]) + pm.centerPoint.y;
            mPaint.setAlpha((int) ((1 - dx / pm.pathLenght)  * 175 + 80));
//            Shader shader = new RadialGradient((float) x, (float) y, mPaint.getStrokeWidth()/2, Color.RED, Color.TRANSPARENT, Shader.TileMode.MIRROR);
//            mPaint.setShader(shader);
            canvas.drawPoint((float) x, (float) y, mPaint);
        }

    }

    private void move() {
        for (int i = 0; i < mTotalPM; i++) {
            if (mPms[i] != null && mPms[i].progress <= 1){
                calculatePmXY(mPms[i]);
            }
        }
    }

    /**
     * @param canvas 参考线、参考点
     */
    private void drawAssist(Canvas canvas) {
        /*canvas.drawCircle(mCX, mCY, px2dp(20), mPaintTest1);
        for (int i = 0; i < mTotalPM; i++) {
            if (mPms[i] != null){
                canvas.drawCircle(mPms[i].point[0], mPms[i].point[1], px2dp(20), mPaintTest2);
                canvas.drawCircle(mPms[i].point[2], mPms[i].point[3], px2dp(20), mPaintTest2);
                canvas.drawPath(mPms[i].path, mPaintTest1);
            }
        }
        canvas.drawCircle(mCX, mCY, mPointStarR, mPaintTest2);
        canvas.drawCircle(mCX, mCY, mPointStarR + mPointStarDx, mPaintTest2);
        canvas.drawCircle(mCX, mCY, mPointStopR, mPaintTest1);
        canvas.drawCircle(mCX, mCY, mPointStopR + mPointStopDx, mPaintTest1);*/

        /*float starAngle = (float) ((180 / Math.PI) * (Math.PI * 2 - mPointAngleStop + Math.PI / 2));
        float durationAngle = (float) ((180 / Math.PI) * (mPointAngleStop - mPointAngleStar));
        canvas.drawArc(new RectF(mCX - mPointStopR/2, mCY - mPointStopR/2, mCX + mPointStopR/2, mCY + mPointStopR/2),
                starAngle, durationAngle,
                true, mPaintArc);*/
        canvas.drawPath(fingerPath, mPaintTest1);
    }

    public int px2dp(float px) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (px / scale + 0.5f);
    }

    public void calculatePmXY(PMElement pmElement) {
        pmElement.progress = (System.currentTimeMillis() - pmElement.starTime) / pmElement.duration;
        float progressIn = getInterpolator(1, pmElement.progress);
        //大于的时候 动画结束
        if (pmElement.progress > 1) {
            return;
        }
        float dx = pmElement.pathLenght * progressIn;
        float[] pos = new float[2];
        float[] tan = new float[2];
        pmElement.pathMeasure.getPosTan(dx, pos, tan);
        pmElement.x = pos[0];
        pmElement.y = pos[1];
    }

    /**
     * 加速器
     * @param factor
     * @param input
     * @return
     */
    private float getInterpolator(float factor, float input) {
        switch (mInterpolatorMode) {
        case 0: //匀速
            return input;
        case 1: //慢->快
            if (factor == 1.0f) {
                return input * input;
            } else {
                return (float) Math.pow(input, factor * 2);
            }
        case 2: //快->慢
            if (factor == 1.0f) {
                return (1.0f - ((1.0f - input) * (1.0f - input)));
            } else {
                return (float) (1.0f - Math.pow((1.0f - input), 2 * factor));
            }
        case 3: //加速减速
            return (float) (Math.cos((input + 1) * Math.PI) / 2.0f) + 0.5f;
        }
        return input;
    }

    private void creatPM(){
        for (int i = 0; i < mTotalPM; i++) {
            if (mPms[i] == null){
                mPms[i] = new PMElement();
                creatPMPoint(mPms[i]);
                break;
            }
            if (mPms[i].progress >= 1){
                creatPMPoint(mPms[i]);
                break;
            }
        }
    }

    private void creatPMPoint(PMElement pmElement){
        pmElement.angleStar = fingerPathAngle + Math.PI * 3 / 4;
        pmElement.centerPoint.set(currentPoint);
        pmElement.starTime = System.currentTimeMillis();
        //计算起始点的半径、夹角
        double rdmStarR = mRandom.nextDouble();
        double starDx = mPointStarDx * rdmStarR;
        double pointStarR = mPointStarR + starDx;
        pmElement.angle = mPointAngleDur * mRandom.nextDouble() + pmElement.angleStar;
        double sinAn = Math.sin(pmElement.angle);
        double cosAn = Math.cos(pmElement.angle);
        pmElement.x = (float) (sinAn * pointStarR) + pmElement.centerPoint.x;
        pmElement.y = (float) (cosAn * pointStarR) + pmElement.centerPoint.y;


        //计算结束点的半径、夹角
        double rdmStopR = mRandom.nextDouble();
        double stopDx = mPointStopDx * rdmStopR;
        double pointStopR = mPointStopR + stopDx;

        pmElement.point = new float[24];
        pmElement.point[0] = pmElement.x;
        pmElement.point[1] = pmElement.y;
        pmElement.point[2] = (float) (sinAn * pointStopR) + pmElement.centerPoint.x;
        pmElement.point[3] = (float) (cosAn * pointStopR) + pmElement.centerPoint.y;

        pmElement.path = new Path();
        pmElement.path.moveTo(pmElement.point[0], pmElement.point[1]);
        pmElement.path.lineTo(pmElement.point[2], pmElement.point[3]);
        PathMeasure pathMeasure = new PathMeasure(pmElement.path, false);
        pmElement.pathLenght = pathMeasure.getLength();
        pmElement.pathMeasure = pathMeasure;
        pmElement.duration = (float) (mRandom.nextDouble() * 1000 + 5000);
        pmElement.progress = 0;

        pmElement.dervie = new double[20];
        for (int i = 0; i < pmElement.dervie.length; i += 2) {
            pmElement.dervie[i] = mRandom.nextInt(100) - 50;
            pmElement.dervie[i+1] = mPointAngleDur * mRandom.nextDouble()
                    + pmElement.angleStar - pmElement.angle;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            Log.d("touch--> down", touchState+"");
            if (touchState == STATE_NORMAL) {
                touchState = STATE_DOWN;
                touchDown(event);
                Log.d("touch-->", "down");
                return true;
            }
            break;
        case MotionEvent.ACTION_MOVE:
            Log.d("touch--> move", touchState+"");
            if (touchState == STATE_MOVE | touchState == STATE_DOWN) {
                touchState = STATE_MOVE;
                touchMove(event);
                Log.d("touch-->", "move");
                return true;
            }
            break;
        case MotionEvent.ACTION_CANCEL:
        case MotionEvent.ACTION_UP:
            Log.d("touch--> up", touchState+"");
            if (touchState == STATE_MOVE | touchState == STATE_DOWN) {
                touchState = STATE_UP;
                Log.d("touch-->", "up");
                touchUp(event);
            }
            break;
        }
        return super.onTouchEvent(event);
    }

    /**
     * 手指点下后的处理
     */
    public void touchDown(MotionEvent event) {
        fingerPath.reset();
        fingerPathLenght = 0;
        fingerPathAngle = 0;
        fingerPath.moveTo(event.getX(), event.getY());
        currentPoint.set(event.getX(), event.getY());
    }

    /**
     * 手指移动处理
     *
     * @param event
     */
    public void touchMove(MotionEvent event) {
        currentPoint.set(event.getX(), event.getY());
        fingerPath.lineTo(event.getX(), event.getY());
        fingerPathMea.setPath(fingerPath, false);
        float lenght = fingerPathMea.getLength();
        if (lenght - fingerPathLenght > px2dp(3)){
            fingerPathLenght = lenght;
            float[] pos = new float[2];
            float[] tan = new float[2];
            fingerPathMea.getPosTan(fingerPathLenght, pos, tan);
            fingerPathAngle = (float) Math.atan(tan[0] / tan[1]);
        }
    }

    /**
     * 手指抬起处理
     *
     * @param event
     */
    public void touchUp(MotionEvent event) {
        currentPoint.set(event.getX(), event.getY());
        fingerPath.lineTo(event.getX(), event.getY());
        fingerPathMea.setPath(fingerPath, false);
        fingerPathLenght = fingerPathMea.getLength();
        float[] pos = new float[2];
        float[] tan = new float[2];
        fingerPathMea.getPosTan(fingerPathLenght, pos, tan);
        fingerPathAngle = (float) Math.atan(tan[0] / tan[1]);

    }

    /**
     * 设置速度插值器
     * @param interpolatorMode
     */
    public void setInterpolator(int interpolatorMode){
        this.mInterpolatorMode = interpolatorMode;
    }

    public void setPathMode(int pathMode){
        this.mPathMode = pathMode;
    }


}
