package serial.jni;

import java.util.concurrent.ConcurrentLinkedQueue;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Process;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewConfiguration;


/**
 * 回顾波形
 *
 * @author Administrator 2014-01-22 将波形绘制由drawLine 改为drawPath，实现速度优化 2014-12-11
 * 当导联显示方式为节律导联，需要特殊处理： 1.不支持智能排列 2.不支持横向滑动3.不支持长按事件 2014-12-11 下午
 * 去除节律导联显示方式 2016-09-19排序默认方式改为智能排序
 * 2016-09-29 1.智能排序算法更改
 * 2.排序方式、显示的是第几屏数据，均改为针对应用程序的全局变量，方便传值给ReportInfo
 */
public class ReviewWave extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = ReviewWave.class.getSimpleName();
    // 病例对应的是多少导联：12或18
    private int mLeadCount = 12;

    // 设置节律导联
    private static final int MSG_LONG_CLICK = 0x00000001;

    // 触摸事件标识
    private static final int TOUCH_NONE = 0x00000002;
    private static final int TOUCH_DRAG_WAVE = 0x00000003;
    // 切屏
    private static final int TOUCH_CHANGE_SCREEN = 0x00000004;
    private static final int TOUCH_LONG_PRESS = 0x00000005;
    private static final int TOUCH_ZOOM = 0x00000006;

    private boolean mIsLeadDragMode;// 可以移动导联位置，只有12导联的时候才可以执行
    private int mDragLeadIndexs = -1;//2016-09-30 只保存用户点击区域的索引值，根据余值判断是否为同一排的，同一排的需要同步移动
    private float mDragLeadYDistance = 0;

    private int touchMode = TOUCH_NONE;
    private static int DRAG_DISTANCE = 30;

    private static final int LONGPRESS_TIMEOUT = ViewConfiguration
            .getLongPressTimeout();
    private static final int TAP_TIMEOUT = ViewConfiguration.getTapTimeout();

    // 自定：
    // 每毫米8个像素
    private float perMillmeter = 8;

    // 采样率
    public static final int SAMPLE_RATE = 1000;

    // 计算多少个像素画一个点
    private float twoPointerDistance;
    // 导联名称所占宽度
    private int mLeadNameWidth;
    private float perColumnWidth;


    private int maxHeight;
    private int mViewWidth;
    private int mViewHeight;
    private int mRhythmLeadTextColor;
    private int mNormalLeadTextColor;
    private int mWaveColor;
    private Paint mPaint;
    private float mFontHeight;


    private float paddingTop = 30;
    private float paddingBottom = 0;
    private float paddingLeft = 0;
    private float paddingRight;

    private RectF[] mLeadRectFs;
    private int mDragPoiner = 0;
    private int mStartDataIndex = 0;
    //2016-10-08当前显示的是第几屏，默认0为第一屏
    private int mWhichScreenData = 0;
    // 参数：
    // 走速
    private float mSpeed = 10;
    // 肢导增益
    private float mLimbLeadGain = 10;
    // 胸导增益
    private float mChestLeadGain = 10;
    // 节律导联
    private int[] mRhythmLeads = new int[3];
    // 导联显示方式

    private static final String LEAD_DISPLAY_6X2P = "6";

    // private static final String LEAD_DISPLAY_RHYTHMS = "rhythmLead";// 需要特殊处理
    private String mLeadDisplayStyle = "12";

    // 保存波形数据
    private ConcurrentLinkedQueue<Short> mWaveDatas;
    // 导联名称
    private String[] mLeadNames;

    private SurfaceHolder mHolder;
    private boolean startDrawWaveThread = true;
    private boolean refreshWave = false;
    private Bitmap mScrollBarSrcBitmap;
    private boolean isSmartSort = true;// 是否是智能排序，智能排序时可上下滚动//2016-09-14 默认为智能排序

    private int wmvscale = 440;// 定标系数2015-08-20 11:08心电图机定标系数是440 ，体检的是806
    private Context mContext;

    public ReviewWave(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        DisplayMetrics dm = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay()
                .getMetrics(dm);
        float scaledDensity = dm.scaledDensity;// 缩放密度
        // DRAG_DISTANCE *= scaledDensity;
        mLeadNames = new String[]{"I", "II", "III", "aVR", "aVL", "aVF", "V1", "V2", "V3", "V4", "V5", "V6"};
//        perMillmeter = dm.densityDpi / 25.4f;
        perMillmeter = 0.014f * 200
                * dm.widthPixels / (20.5f * 25);
        jumpPoint = 1;
        oneXwidth = perMillmeter * 25f / 200 * jumpPoint;//8个像素代表1mm，走速为25mm/s,采样率1000，每隔5个点画一个
//        oneXwidth = 1;//8个像素代表1mm，走速为25mm/s,采样率1000，每隔5个点画一个
        mLastX = 0;
        Log.e(TAG, "oneXwidth:" + oneXwidth + ",scaledDensity:" + scaledDensity + ",dm.heightPixels:" + dm.heightPixels + ",dm.widthPixels:" + dm.widthPixels + ",dm.densityDpi:" + dm.densityDpi);
        twoPointerDistance = 100 * perMillmeter / SAMPLE_RATE;// 2015-06-11
        // 50变200，减少绘点个数
        // 2015-16-10
        // 200变100
        // 设置画笔属性
        mPaint = new Paint();
        mPaint.setStrokeWidth((int) scaledDensity);
        // mPaint.setStrokeWidth(1.5f);

        mPaint.setTextAlign(Paint.Align.LEFT);
        float textSize = 15 * scaledDensity;
        mNormalLeadTextColor = Color.parseColor("#00cc00");
        mRhythmLeadTextColor = Color.YELLOW;
        // mWaveTextColor = mContext.getResources().getColor(
        // R.color.diagnostic_graph_area_graphcolor);
        mWaveColor = Color.GREEN;
        mPaint.setTextSize(textSize);
        mPaint.setAntiAlias(true);
        mLeadNameWidth = (int) mPaint.measureText("aVR");
        // 右边空隙2个字符宽度
        paddingRight = mPaint.measureText("a");
        mLeadNameWidth += paddingRight + 5;
        FontMetrics fontMetrics = mPaint.getFontMetrics();
        mFontHeight = (fontMetrics.descent - fontMetrics.ascent) / 3 * 2;
        mHolder = this.getHolder();
        mHolder.addCallback(this);
        mPaint.setColor(Color.GREEN);

    }


    float oneXwidth;//8个像素代表1mm，走速为25mm/s,采样率1000，每隔5个点画一个
    float mLastX;
    float mLastRightX;
    float[] mLastYs = new float[12];
    float[] mLasterYs = new float[12];
    float[] mCurYs = new float[12];
    int gain = 10;//10mm/mv 8个像素代表1mm
    int jumpPoint;
    boolean flag = false;

    /**
     * 绘制波形
     */
    int n = 0;

    private void drawWave() {

        if (mWaveDatas == null || mWaveDatas.isEmpty()) {
            return;
        }
        if (mLeadRectFs == null && mViewHeight > 0) {
            meanSort();
            // 刷新条加宽
            Canvas canvas = mHolder.lockCanvas(new Rect(0, 0, mLeadNameWidth, mViewHeight));
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
//            mPaint.setColor(Color.WHITE);
//            canvas.drawRect(new Rect(0, 0, mLeadNameWidth, mViewHeight),mPaint);
            mPaint.setColor(Color.GREEN);
            for (int i = 0; i < mLeadCount; i++) {
                canvas.drawText(mLeadNames[i], paddingRight, mCenterLineYs[i], mPaint);
            }
            mPaint.setColor(Color.GREEN);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setStrokeWidth(2.0f);

            mHolder.unlockCanvasAndPost(canvas);
        }


        if (mWaveDatas.size() < 5 * mLeadCount) {
            return;
        }

        float startX = mLastX + oneXwidth;
        if (startX > mViewWidth) {
            mLastX = 0;
        }
        Canvas canvas;
        // 刷新条加宽
        if (mLastX == 0) {
            canvas = mHolder.lockCanvas(new Rect(mLeadNameWidth, 0, (int) (mLeadNameWidth + oneXwidth * 5 + 20), mViewHeight));
        } else {
            // 刷新条加宽
            canvas = mHolder.lockCanvas(new Rect((int) mLastX, 0, (int) (mLastX + oneXwidth * 5 + 20), mViewHeight));
        }

        if(canvas == null){
            return;
        }

        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);


        for (int j = 0; j < 10; j++) {
            n++;
            startX = mLastX + oneXwidth;
            if (startX > mViewWidth) {
                break;
            }
            if (mLastX == 0) {
                startX = mLeadNameWidth;
            }

            for (int i = 0; i < mLeadCount; i++) {
                Short val = mWaveDatas.poll();
                if (val == null) {
                    i--;
                    continue;
                }
                mCurYs[i] = mCenterLineYs[i] - val * 0.001f * gain * perMillmeter;
                //两次锁屏连接处多画一个点，避免锁屏时精度损失造成的波形不连续
                if (j == 0) {
                    if (startX == mLeadNameWidth) {
                        canvas.drawPoint(startX, mCurYs[i], mPaint);
                        mLasterYs[i] = mLastYs[i];
                        mLastYs[i] = mCurYs[i];
                        continue;
                    } else {
                        canvas.drawLine(mLastX - oneXwidth, mLasterYs[i], mLastX, mLastYs[i], mPaint);
                    }
                }
                canvas.drawLine(mLastX, mLastYs[i], startX, mCurYs[i], mPaint);
                mLasterYs[i] = mLastYs[i];
                mLastYs[i] = mCurYs[i];
            }
            mLastX = startX;
        }

        mHolder.unlockCanvasAndPost(canvas);
    }

    public void setRendererColor(int arg1, float arg2, int arg3, int arg4) {


    }

    public void onPause() {

    }

    public void onResume() {

    }

    public void initDisplay() {

    }

    public void setMsg(Handler handler) {
    }

    /**
     * 获取指定区域中用来绘制波形的宽度
     *
     * @return
     */
    private float getWaveWidth(int index) {

        // 非6x2 + 1模式，每个区域大小相同

        // 6x2 + 1模式，单独的节律导联波形区域最大
        float maxRectFWidth = mLeadRectFs[index].right
                - mLeadRectFs[index].left - mLeadNameWidth;

        return maxRectFWidth;
    }


    private float mScrollY;// 波形在可以垂直移动时，垂直方向偏移量，取值范围为-（maxHeight -
    // mViewHeight）~0


    /**
     * 获取 当前是多少导联
     *
     * @return
     */
    public int getLeadCount() {
        return mLeadCount;
    }

    /**
     * 设置导联数 12或18
     *
     * @param leadCount
     */
    public void setLeadCount(int leadCount) {
        this.mLeadCount = leadCount;
    }


    /**
     * 设置波形数据
     *
     * @param ecgDataBuf
     * @return
     */
    public void setEcgDataBuf(ConcurrentLinkedQueue<Short> ecgDataBuf) {
        mWaveDatas = ecgDataBuf;
    }


    float[] mCenterLineYs;


    /**
     * 平均排序
     */
    public void meanSort() {

        maxHeight = mViewHeight;
        float perLeadHeight;
        int allRow = mRow + mDisplayRhythmLeadCount;
        int rectFCount = mRow * mColumn + mDisplayRhythmLeadCount;
        mLeadRectFs = new RectF[rectFCount];
        mCenterLineYs = new float[rectFCount];
        perLeadHeight = (mViewHeight - paddingTop)//2016-09-30  与智能排序保持统一
                / allRow;

        float perColumnWidth = mViewWidth / mColumn;

        for (int i = 0; i < mRow; i++) {
            // mLeadsCenterLineY[i] = paddingTop + perLeadHeight * (i + 0.5f);
            mLeadRectFs[i] = new RectF();
            int whichColumn;
            if (mRow == 0) {// 如果perColumnLeadCount = 0，证明只显示节律导联
                whichColumn = mColumn;
            } else {
                whichColumn = i / mRow;

            }

            mLeadRectFs[i].left = perColumnWidth * whichColumn
                    + paddingLeft;
            mLeadRectFs[i].right = (perColumnWidth * whichColumn
                    + perColumnWidth - paddingRight);
            if (mRow == 0) {
                mLeadRectFs[i].top = paddingTop + perLeadHeight * i;
            } else {

                mLeadRectFs[i].top = paddingTop + perLeadHeight
                        * (i % mRow);
            }
            mLeadRectFs[i].bottom = mLeadRectFs[i].top + perLeadHeight;
            mCenterLineYs[i] = (mLeadRectFs[i].top + mLeadRectFs[i].bottom) / 2;
        }

    }

    public void meanSort6x2() {

        maxHeight = mViewHeight;
        float perLeadHeight;
        int allRow = mRow + mDisplayRhythmLeadCount;
        int rectFCount = mRow * mColumn + mDisplayRhythmLeadCount;
        mLeadRectFs = new RectF[rectFCount];
        mCenterLineYs = new float[rectFCount];
        perLeadHeight = (mViewHeight - paddingTop)//2016-09-30  与智能排序保持统一
                / allRow;

        perColumnWidth = mViewWidth / mColumn;

        for (int i = 0; i < mRow * mColumn; i++) {
            // mLeadsCenterLineY[i] = paddingTop + perLeadHeight * (i + 0.5f);
            mLeadRectFs[i] = new RectF();
            int whichColumn;
            if (mRow == 0) {// 如果perColumnLeadCount = 0，证明只显示节律导联
                whichColumn = mColumn;
            } else {
                whichColumn = i / mRow;

            }

            mLeadRectFs[i].left = perColumnWidth * whichColumn
                    + paddingLeft;
            mLeadRectFs[i].right = (perColumnWidth * whichColumn
                    + perColumnWidth - paddingRight);
            if (mRow == 0) {
                mLeadRectFs[i].top = paddingTop + perLeadHeight * i;
            } else {

                mLeadRectFs[i].top = paddingTop + perLeadHeight
                        * (i % mRow);
            }
            mLeadRectFs[i].bottom = mLeadRectFs[i].top + perLeadHeight;
            mCenterLineYs[i] = (mLeadRectFs[i].top + mLeadRectFs[i].bottom) / 2;
        }

    }


//    Timer drawWaveTimer = null;

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        mViewHeight = getHeight();
        mViewWidth = getWidth();
        Log.d(TAG, "surfaceCreated");
//		startDrawWaveThread = true;
//		refreshWave = true;
        mWaveColor = Color.rgb(0, 255, 0);
        mNormalLeadTextColor = Color.rgb(0, 255, 0);
        mRhythmLeadTextColor = Color.rgb(0, 255, 0);
//        WaveStyleUtil.setBackgroundSize(mContext, mViewWidth, mViewHeight);
//        setBackgroundDrawable(WaveStyleUtil.backgroundDrawable);
    }

    private int mRow = 12;
    private int mColumn = 1;
    private int mDisplayRhythmLeadCount = 0;


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        // TODO Auto-generated method stub
        Log.d(TAG, "surfaceChanged");
        // 控件大小改变时调用
        mViewHeight = height;
        mViewWidth = width;

    }

    public void startRenderer() {
        if (refreshWave) {
            return;
        }
        refreshWave = true;
        mLastX = 0;
        mLastYs = new float[12];
        mLasterYs = new float[12];
        mCurYs = new float[12];

        mLeadRectFs = null;
        mCenterLineYs = null;
        if (waveRenderer == null) {
            waveRenderer = new HealthWave();
        }
        waveRenderer.start();
    }

    public void stopRenderer() {
        if (!refreshWave) {
            return;
        }
        refreshWave = false;

        if (waveRenderer != null && waveRenderer.isAlive()) {
            waveRenderer.interrupt();
            waveRenderer = null;
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        Log.d(TAG, "surfaceDestroyed");
        stopRenderer();

    }

    private class HealthWave extends Thread {

        @Override
        public void run() {
//            Process.setThreadPriority(Process.THREAD_PRIORITY_DISPLAY);
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY);
            while (refreshWave) {
                if(!refreshWave){
                    break;
                }
                drawWave();
                try {
                    sleep(10);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    private HealthWave waveRenderer;
}
