package serial.jni;

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

import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * Review Waveform
 * <p>
 * Author: Administrator
 * Date: 2014-01-22
 * Description: Changed waveform drawing from drawLine to drawPath for improved speed.
 * Date: 2014-12-11
 * Description: Special handling required when the lead display mode is rhythm lead:
 * Intelligent arrangement is not supported.
 * Horizontal scrolling is not supported.
 * Long press events are not supported.
 * Date: 2014-12-11 (afternoon)
 * Description: Removed rhythm lead display mode.
 * Date: 2016-09-19
 * Description: Changed default sorting method to intelligent sorting.
 * Date: 2016-09-29
 * Description:
 * Modified intelligent sorting algorithm.
 * Sorting method and displayed screen data are now global variables for the application, making it easier to pass values to ReportInfo.
 */
public class ReviewWave extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = ReviewWave.class.getSimpleName();
    // Number of leads for the case: 12 or 18
    private int mLeadCount = 12;

    // Set rhythm lead
    private static final int MSG_LONG_CLICK = 0x00000001;

    // Touch event identifiers
    private static final int TOUCH_NONE = 0x00000002;
    private static final int TOUCH_DRAG_WAVE = 0x00000003;
    // Screen switch
    private static final int TOUCH_CHANGE_SCREEN = 0x00000004;
    private static final int TOUCH_LONG_PRESS = 0x00000005;
    private static final int TOUCH_ZOOM = 0x00000006;

    private boolean mIsLeadDragMode; // Can move lead positions, only applicable for 12 leads
    private int mDragLeadIndexs = -1; // 2016-09-30 Only save the index value of the user-clicked area, determine
    // if they are in the same row based on the remainder, and move them synchronously if they are in the same row.
    private float mDragLeadYDistance = 0;

    private int touchMode = TOUCH_NONE;
    private static int DRAG_DISTANCE = 30;

    private static final int LONGPRESS_TIMEOUT = ViewConfiguration
            .getLongPressTimeout();
    private static final int TAP_TIMEOUT = ViewConfiguration.getTapTimeout();

    // Customization:
// 8 pixels per millimeter
    private float perMillimeter = 8;

    // Sampling rate
    public static final int SAMPLE_RATE = 1000;

    // Calculate how many pixels to draw a point
    private float twoPointerDistance;
    // Width occupied by lead names
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
    //2016-10-08 The currently displayed screen, default 0 for the first screen
    private int mWhichScreenData = 0;
    // Parameters:
// Paper speed
    private float mSpeed = 10;
    // Limb lead gain
    private float mLimbLeadGain = 10;
    // Chest lead gain
    private float mChestLeadGain = 10;
    // Rhythm leads
    private int[] mRhythmLeads = new int[3];
// Lead display style

    private static final String LEAD_DISPLAY_6X2P = "6";

    // private static final String LEAD_DISPLAY_RHYTHMS = "rhythmLead"; // Requires special handling
    private String mLeadDisplayStyle = "12";

    // Save waveform data
    private ConcurrentLinkedQueue<Short> mWaveDatas;
    // Lead names
    private String[] mLeadNames;

    private SurfaceHolder mHolder;
    private boolean startDrawWaveThread = true;
    private boolean refreshWave = false;
    private Bitmap mScrollBarSrcBitmap;
    private boolean isSmartSort = true; // Whether it is smart sorting, can scroll up and down when smart sorting //2016-09-14 Default is smart sorting

    private int wmvscale = 440; // Calibration coefficient 2015-08-20 11:08 The calibration coefficient of the ECG machine is 440, and the physical examination is 806.
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
//        perMillimeter = dm.densityDpi / 25.4f;
        perMillimeter = 0.014f * 200 * dm.widthPixels / (20.5f * 25);
        jumpPoint = 1;
        oneXwidth = perMillimeter * 25f / 200 * jumpPoint; // 8 pixels represent 1mm, speed is 25mm/s, sampling rate is 1000, draw every 5 points
// oneXwidth = 1; // 8 pixels represent 1mm, speed is 25mm/s, sampling rate is 1000, draw every 5 points
        mLastX = 0;
        Log.e(TAG, "oneXwidth:" + oneXwidth + ",scaledDensity:" + scaledDensity + ",dm.heightPixels:" + dm.heightPixels + ",dm.widthPixels:" + dm.widthPixels + ",dm.densityDpi:" + dm.densityDpi);
        twoPointerDistance = 100 * perMillimeter / SAMPLE_RATE; // 2015-06-11
// 50 changed to 200, reduce the number of plotted points
// 2015-16-10
// 200 changed to 100
// Set paint properties
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
// Right gap is 2 character widths.
        paddingRight = mPaint.measureText("a");
        mLeadNameWidth += paddingRight + 5;
        FontMetrics fontMetrics = mPaint.getFontMetrics();
        mFontHeight = (fontMetrics.descent - fontMetrics.ascent) / 3 * 2;
        mHolder = this.getHolder();
        mHolder.addCallback(this);
        mPaint.setColor(Color.GREEN);

    }


    float oneXwidth;//8 pixels represent 1 mm,
    // the speed is 25 mm/s, the sampling rate is 1000, and every 5 points, draw one
    float mLastX;
    float mLastRightX;
    float[] mLastYs = new float[12];
    float[] mLasterYs = new float[12];
    float[] mCurYs = new float[12];
    int gain = 10;//10mm/mv 8个像素代表1mm
    int jumpPoint;
    boolean flag = false;

    /**
     * draw waveform
     */
    int n = 0;

    private void drawWave() {

        if (mWaveDatas == null || mWaveDatas.isEmpty()) {
            return;
        }
        if (mLeadRectFs == null && mViewHeight > 0) {
            meanSort();
            // "enlarge the refresh bar" or "widen the refresh bar."
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
        // Increase the width of the refresh bar
        if (mLastX == 0) {
            canvas = mHolder.lockCanvas(new Rect(mLeadNameWidth, 0, (int) (mLeadNameWidth + oneXwidth * 5 + 20), mViewHeight));
        } else {
            // Increase the width of the refresh bar
            canvas = mHolder.lockCanvas(new Rect((int) mLastX, 0, (int) (mLastX + oneXwidth * 5 + 20), mViewHeight));
        }

        if (canvas == null) {
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
                mCurYs[i] = mCenterLineYs[i] - val * 0.001f * gain * perMillimeter;
                //Add an additional dot at the connection point of the
                // two screens to avoid discontinuity in the waveform caused by accuracy loss during screen lock.
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

     Get the width used to draw the waveform in the specified region.

     @return The width of the waveform.
     */
    private float getWaveWidth(int index) {

// For non-6x2 + 1 mode, each region has the same size.

// For 6x2 + 1 mode, the standalone rhythm lead waveform region has the maximum width.
        float maxRectFWidth = mLeadRectFs[index].right
                - mLeadRectFs[index].left - mLeadNameWidth;

        return maxRectFWidth;
    }

    private float mScrollY; // Vertical offset of the waveform when it can be vertically moved, ranging from - (maxHeight - mViewHeight) to 0.

    /**

     Get the current number of leads.
     @return The lead count.
     */
    public int getLeadCount() {
        return mLeadCount;
    }
    /**

     Set the number of leads (12 or 18).
     @param leadCount The lead count.
     */
    public void setLeadCount(int leadCount) {
        this.mLeadCount = leadCount;
    }
    /**

     Set the waveform data.
     @param ecgDataBuf The buffer containing the ECG waveform data.
     */
    public void setEcgDataBuf(ConcurrentLinkedQueue<Short> ecgDataBuf) {
        mWaveDatas = ecgDataBuf;
    }

    float[] mCenterLineYs;


    /**
     * Mean sorting
     */
    public void meanSort() {

        maxHeight = mViewHeight;
        float perLeadHeight;
        int allRow = mRow + mDisplayRhythmLeadCount;
        int rectFCount = mRow * mColumn + mDisplayRhythmLeadCount;
        mLeadRectFs = new RectF[rectFCount];
        mCenterLineYs = new float[rectFCount];
        perLeadHeight = (mViewHeight - paddingTop) // 2016-09-30 Keep consistent with intelligent sorting
                / allRow;

        float perColumnWidth = mViewWidth / mColumn;

        for (int i = 0; i < mRow; i++) {
            // mLeadsCenterLineY[i] = paddingTop + perLeadHeight * (i + 0.5f);
            mLeadRectFs[i] = new RectF();
            int whichColumn;
            if (mRow == 0) { // If perColumnLeadCount = 0, it means only rhythm leads are displayed
                whichColumn = mColumn;
            } else {
                whichColumn = i / mRow;
            }

            mLeadRectFs[i].left = perColumnWidth * whichColumn + paddingLeft;
            mLeadRectFs[i].right = (perColumnWidth * whichColumn + perColumnWidth - paddingRight);
            if (mRow == 0) {
                mLeadRectFs[i].top = paddingTop + perLeadHeight * i;
            } else {
                mLeadRectFs[i].top = paddingTop + perLeadHeight * (i % mRow);
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
        perLeadHeight = (mViewHeight - paddingTop)//2016-09-30  Maintain consistency with intelligent sorting
                / allRow;

        perColumnWidth = mViewWidth / mColumn;

        for (int i = 0; i < mRow * mColumn; i++) {
            // mLeadsCenterLineY[i] = paddingTop + perLeadHeight * (i + 0.5f);
            mLeadRectFs[i] = new RectF();
            int whichColumn;
            if (mRow == 0) {// If perColumnLeadCount = 0, it means that only rhythm leads are displayed.
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
        // Called when the size of the control changes
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
                if (!refreshWave) {
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
