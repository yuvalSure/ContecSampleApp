package serial.jni;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GLActivityNew extends Activity {
    private ReviewWave newView;
    private DataUtils data;
    private Context mContext;
    private static UsbManager mUsbManager;
    private static UsbDevice mUsbDevice;
    private String strCase;
    /**
     * 2018-05 isFinishActivity
     * This variable is set based on the actual usage scenario. When returning to close the current waveform display activity, there may be a Bluetooth connection in progress and a message of connection failure will be returned.
     * It is necessary to check whether the relevant processing operations have been executed to avoid multiple executions of opening a certain activity or handling other variables and states, causing unnecessary errors.
     */
    private boolean isFinishActivity;
    private int m = 0;
    private int n = 0;
    private int p = 0;
    private int q = 0;
    private Button btnStartConnect;
    private Button btnStopConnect;

    private Button btnStartECGRenderer;
    private Button btnStopECGRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.surfaceview);
        mContext = this;

        DisplayMetrics dm = new DisplayMetrics();
        // 取得窗口属性
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;// 屏幕宽度
        int height = dm.heightPixels;// 屏幕高度
        Log.e("Activity WxH", width + "x" + height);
        Log.e("Density", "" + dm.densityDpi);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Button b1 = (Button) this.findViewById(R.id.btn01);
        Button b2 = (Button) this.findViewById(R.id.btn02);
        Button b3 = (Button) this.findViewById(R.id.btn03);
        Button b4 = (Button) this.findViewById(R.id.btn04);
        Button b5 = (Button) this.findViewById(R.id.btn05);
        Button b6 = (Button) this.findViewById(R.id.btn06);
        Button b7 = (Button) this.findViewById(R.id.btn07);
        Button b8 = (Button) this.findViewById(R.id.btn08);
        btnStartConnect = (Button) findViewById(R.id.btnStartConnect);
        btnStopConnect = (Button) findViewById(R.id.btnStopConnect);

        btnStartECGRenderer = (Button) findViewById(R.id.btnStartRenderer);
        btnStopECGRenderer = (Button) findViewById(R.id.btnStopRenderer);

        btnStartECGRenderer.setEnabled(false);

        btnStartConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnStartConnect.setEnabled(false);
                data.gatherStart(new nativeMsg());
//                bindDev.connect();
            }
        });
        btnStopConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnStartECGRenderer.setEnabled(false);
                newView.stopRenderer();
                data.gatherEnd();
                btnStartConnect.setEnabled(true);
//                bindDev.disconnect();
            }
        });

        btnStartECGRenderer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEcgQueue = new ConcurrentLinkedQueue<Short>();
                newView.setEcgDataBuf(mEcgQueue);
                newView.startRenderer();
            }
        });
        btnStopECGRenderer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newView.stopRenderer();
                mEcgQueue = null;
            }
        });
        //all
        b1.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Analyze the data file and store the result as XML.
/*
                int ret = data.ecgAnalyzeToXml(
                        Environment.getExternalStorageDirectory() + "/"
                                + strCase,
                        Environment.getExternalStorageDirectory()
                                + "/BECG_advice.xml",
                        Environment.getExternalStorageDirectory()
                                + "/conclusion.cn");
                */
//Customize heart rate upper and lower limits.
                int ret = data.ecgAnalyzeToXml("/mnt/sdcard/AECG",
                        "/mnt/sdcard/BECG_advice.xml",
                        "/mnt/sdcard/conclusion.cn", 50, 80);

                Log.e("ANA", "ecgAnalyzeToXml ret = " + ret);
            }
        });
        //ac
        b2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Set the gain to 20 for the data
// Convert the data file to aecg format case
// int ret = data.ecgDataToAECG(
// Environment.getExternalStorageDirectory() + "/"
// + strCase + ".c8k",
// Environment.getExternalStorageDirectory() + "/BECG.xml");
                int ret = data.ecg15DataToAECG(
                        Environment.getExternalStorageDirectory() + "/"
                                + "AECG.c8k",
                        Environment.getExternalStorageDirectory() + "/BECG.xml");
/*
Use ecg18DataToAECG method for aecg conversion with 18 leads
int ret = data.ecg18DataToAECG(
Environment.getExternalStorageDirectory() + "/"
+ "20170419152220.c8k",
Environment.getExternalStorageDirectory() + "/BECG.xml");
*/
                Log.e("aecg", "ecgDataToAECG ret = " + ret);
            }
        });
        //emg
        b3.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

//				data.setSpeed(DataUtils.DISPLAY_SPEED_50);
                if (n % 5 == 0) {
                    data.setFilter(0);
                    Toast.makeText(mContext, "All Filters Off", Toast.LENGTH_LONG).show();
                } else if (n % 5 == 1) {
                    data.setFilter(1);
                    Toast.makeText(mContext, "Power Line Filter", Toast.LENGTH_LONG).show();
                } else if (n % 5 == 2) {
                    data.setFilter(2);
                    Toast.makeText(mContext, "EMG Filter", Toast.LENGTH_LONG).show();
                } else if (n % 5 == 3) {
                    data.setFilter(4);
                    Toast.makeText(mContext, "Baseline Filter", Toast.LENGTH_LONG).show();
                } else if (n % 5 == 4) {
                    data.setFilter(7);
                    Toast.makeText(mContext, "All Filters On", Toast.LENGTH_LONG).show();
                }
                n++;
            }
        });
        //bl
        b4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                data.cancelCase();// Cancel the file being saved.
/*                if (!isFinishActivity) {
                    isFinishActivity = true;
                    Intent intent = new Intent();
                    intent.setClass(mContext, DeviceListActivity.class);
                    startActivity(intent);
                    finish();
                }*/
//                finish();
//                data.BluNIBPCtrlCmd((byte)1,(byte)0);
                data.packUpLoadEx(Environment.getExternalStorageDirectory() + "/", strCase);
//                data.BluStepResetCmd();
/*
                for (byte[] bs : mDeviceInfos) {
                    String name = new String(bs,7,10);
                    Log.e("ECGSpo2BindedThread", "info:" + name);
                    if(name.contains("SpO206")){
                        bindDev.setBindSpO2(bs);
                    }
                }
              */
            }
        });
        //no
        b5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                data.saveCase(Environment.getExternalStorageDirectory() + "/",
                        strCase, 20);// Storage file parameters are path, file name, and storage duration in seconds.
            }
        });
        //displaymode
        b6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (q % 4 == 0) {
                    data.setDisplayMode(DataUtils.DISPLAY_MODE_12x1);
                    Toast.makeText(mContext, "12x1 display, only 12 leads valid!", Toast.LENGTH_LONG).show();
                } else if (q % 4 == 1) {
                    data.setDisplayMode(DataUtils.DISPLAY_MODE_6x2);
                    Toast.makeText(mContext, "6x2 display, only 12 leads valid!", Toast.LENGTH_LONG).show();
                } else if (q % 4 == 2) {
                    data.setDisplayMode(DataUtils.DISPLAY_MODE_2x6_LIMB);
                    Toast.makeText(mContext, "6 leads (limb leads) display", Toast.LENGTH_LONG).show();
                } else if (q % 4 == 2) {
                    data.setDisplayMode(DataUtils.DISPLAY_MODE_2x6_CHEST);
                    Toast.makeText(mContext, "6 leads (chest leads) display", Toast.LENGTH_LONG).show();
                }
                q++;
            }
        });
        //speed
        b7.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                if (p % 5 == 0) {
                    data.setSpeed(DataUtils.DISPLAY_SPEED_25);
                    Toast.makeText(mContext, "25mm/s", Toast.LENGTH_LONG).show();
                } else if (p % 5 == 1) {
                    data.setSpeed(DataUtils.DISPLAY_SPEED_50);
                    Toast.makeText(mContext, "50mm/s", Toast.LENGTH_LONG).show();
                } else if (p % 5 == 2) {
                    data.setSpeed(DataUtils.DISPLAY_SPEED_125);
                    Toast.makeText(mContext, "12.5mm/s", Toast.LENGTH_LONG).show();
                } else if (p % 5 == 3) {
                    data.setSpeed(DataUtils.DISPLAY_SPEED_5);
                    Toast.makeText(mContext, "5mm/s", Toast.LENGTH_LONG).show();
                } else if (p % 5 == 4) {
                    data.setSpeed(DataUtils.DISPLAY_SPEED_10);
                    Toast.makeText(mContext, "10mm/s", Toast.LENGTH_LONG).show();
                }
                p++;

            }
        });
        //gain
        b8.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (m % 6 == 0) {
                    data.setGain(DataUtils.DISPLAY_GAIN_5);
                    Toast.makeText(mContext, "5mm/mV", Toast.LENGTH_LONG).show();
                } else if (m % 6 == 1) {
                    data.setGain(DataUtils.DISPLAY_GAIN_10);
                    Toast.makeText(mContext, "10mm/mV", Toast.LENGTH_LONG).show();
                } else if (m % 6 == 2) {
                    data.setGain(DataUtils.DISPLAY_GAIN_20);
                    Toast.makeText(mContext, "20mm/mV", Toast.LENGTH_LONG).show();
                } else if (m % 6 == 3) {
                    data.setGain(DataUtils.DISPLAY_GAIN_2_5);
                    Toast.makeText(mContext, "2.5mm/mV", Toast.LENGTH_LONG).show();
                    ;
                } else if (m % 6 == 4) {
                    data.setGain(DataUtils.DISPLAY_GAIN_Limb10_Chest5);
                    Toast.makeText(mContext, "肢导10mm/mV,胸导5mm/mV", Toast.LENGTH_LONG).show();
                } else if (m % 6 == 5) {
                    data.setGain(DataUtils.DISPLAY_GAIN_Limb20_Chest10);
                    Toast.makeText(mContext, "肢导20mm/mV,胸导10mm/mV", Toast.LENGTH_LONG).show();
                }
                m++;
            }
        });
        // The data object contains all the operations related to electrocardiogram (ECG) acquisition.
// glView is responsible for displaying.
// Bluetooth collection.
//*
        Intent para = null;
        para = getIntent();
        if (para != null) {
            String address = para.getExtras().getString("device_address");

            data = new DataUtils(mContext, address, DataUtils.ECG_LEAD_WILSON, false,
                    new BluConnectionStateListener() {
                        @Override
                        public void OnBluConnectionInterrupted() {
                            // TODO Auto-generated method stub
                            mHandler.obtainMessage(MESSAGE_CONNECT_INTERRUPTED,
                                    -1, -1).sendToTarget();
                        }

                        @Override
                        public void OnBluConnectSuccess() {
                            // TODO Auto-generated method stub
                            mHandler.obtainMessage(MESSAGE_CONNECT_SUCCESS, -1,
                                    -1).sendToTarget();
                        }

                        @Override
                        public void OnBluConnectStart() {
                            // TODO Auto-generated method stub
                            mHandler.obtainMessage(MESSAGE_CONNECT_START)
                                    .sendToTarget();
                        }

                        @Override
                        public void OnBluConnectFaild() {
                            // TODO Auto-generated method stub
                            mHandler.obtainMessage(MESSAGE_CONNECT_FAILED, -1,
                                    -1).sendToTarget();
                        }
                    });
//            bindDev(address);
        }
        //*/
        // Collecting demonstration files.
//         data = new DataUtils(Environment.getExternalStorageDirectory().getPath()+"/demo.ecg");
//         btnStartECGRenderer.setEnabled(true);
/*
		// USB 8000G Device Support
		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		data = new DataUtils(mUsbManager, new USBConnectionStateListener() {

			@Override
			public void OnUSBConnectionError(int state) {
				// TODO Auto-generated method stub
				switch (state) {

				case USBConnectionStateListener.ERROR_REMOVE_DEVICE:
					mHandler.obtainMessage(MESSAGE_USB_CONNECT_REMOVE_DEVICE)
							.sendToTarget();
					break;
				case USBConnectionStateListener.ERROR_NO_USB_PERMISSION:
					mHandler.obtainMessage(
							MESSAGE_USB_CONNECT_NO_USB_PERMISSION)
							.sendToTarget();
					break;
				case USBConnectionStateListener.ERROR_INTERRUPTED:
					mHandler.obtainMessage(MESSAGE_USB_CONNECT_INTERRUPTED)
							.sendToTarget();
					break;
				case USBConnectionStateListener.ERROR_SETTING_DEVICE:
					mHandler.obtainMessage(
							MESSAGE_USB_CONNECT_ERROR_SETTING_DEVICE)
							.sendToTarget();
				case USBConnectionStateListener.ERROR_OPEN_DEVICE:
					mHandler.obtainMessage(
							MESSAGE_USB_CONNECT_ERROR_OPEN_DEVICE)
							.sendToTarget();
					break;
				}
			}

			@Override
			public void OnUSBConnectSuccess() {
				// TODO Auto-generated method stub
				mHandler.obtainMessage(MESSAGE_USB_CONNECT_SUCCESS)
						.sendToTarget();
			}

			@Override
			public void OnUSBConnectStart() {
				// TODO Auto-generated method stub
				mHandler.obtainMessage(MESSAGE_USB_CONNECT_START)
						.sendToTarget();
			}

			@Override
			public void OnUSBConnectFaild() {
				// TODO Auto-generated method stub
				mHandler.obtainMessage(MESSAGE_USB_CONNECT_FAILED)
						.sendToTarget();
			}
		});

*/
        // 18导
/*
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        data = new DataUtils(mUsbManager, new USBConnectionStateListener() {

            @Override
            public void OnUSBConnectionError(int state) {
                // TODO Auto-generated method stub
                switch (state) {

                    case USBConnectionStateListener.ERROR_REMOVE_DEVICE:
                        mHandler.obtainMessage(MESSAGE_USB_CONNECT_REMOVE_DEVICE)
                                .sendToTarget();
                        break;
                    case USBConnectionStateListener.ERROR_NO_USB_PERMISSION:
                        mHandler.obtainMessage(
                                MESSAGE_USB_CONNECT_NO_USB_PERMISSION)
                                .sendToTarget();
                        break;
                    case USBConnectionStateListener.ERROR_INTERRUPTED:
                        mHandler.obtainMessage(MESSAGE_USB_CONNECT_INTERRUPTED)
                                .sendToTarget();
                        break;
                    case USBConnectionStateListener.ERROR_SETTING_DEVICE:
                        mHandler.obtainMessage(
                                MESSAGE_USB_CONNECT_ERROR_SETTING_DEVICE)
                                .sendToTarget();
                    case USBConnectionStateListener.ERROR_OPEN_DEVICE:
                        mHandler.obtainMessage(
                                MESSAGE_USB_CONNECT_ERROR_OPEN_DEVICE)
                                .sendToTarget();
                        break;
                }
            }

            @Override
            public void OnUSBConnectSuccess() {
                // TODO Auto-generated method stub
                mHandler.obtainMessage(MESSAGE_USB_CONNECT_SUCCESS)
                        .sendToTarget();
            }

            @Override
            public void OnUSBConnectStart() {
                // TODO Auto-generated method stub
                mHandler.obtainMessage(MESSAGE_USB_CONNECT_START)
                        .sendToTarget();
            }

            @Override
            public void OnUSBConnectFaild() {
                // TODO Auto-generated method stub
                mHandler.obtainMessage(MESSAGE_USB_CONNECT_FAILED)
                        .sendToTarget();
            }
        }, DataUtils.ECG_USB_SERIAL_BAUDRATE_460800,DataUtils.ECG_LEAD_18_TYPE_15);
        //*/

        // The following operations regarding glView are necessary operations. Please do not make any changes.
        data.setDisplayMode(DataUtils.DISPLAY_MODE_12x1);
        data.setGain(DataUtils.DISPLAY_GAIN_5);
        data.setSpeed(DataUtils.DISPLAY_SPEED_25);

        newView = (ReviewWave) this.findViewById(R.id.NewWave);
        newView.initDisplay();
//		glView.setBackground(Color.TRANSPARENT, Color.rgb(111, 110, 110));//2018-06-21 注释掉,避免Bitmap出现异常

        newView.setMsg(mHandler);
        newView.setZOrderOnTop(true);
        newView.getHolder().setFormat(PixelFormat.TRANSLUCENT);

        textHR = (TextView) this.findViewById(R.id.textHR);
        textLF = (TextView) this.findViewById(R.id.textLeadOff);

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss",
                Locale.getDefault());
        Date curDate = new Date(System.currentTimeMillis());// 获取当前时间
        // strCase = formatter.format(curDate);
        strCase = "AECG";
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // When executing this function, avoid performing operations such as force quitting,
        // repeatedly pressing the back button, or any other actions
        // that would affect the management of the Android lifecycle.
        newView.onPause();
        data.gatherEnd();
        data.gatherRelease();
    }

    @Override
    protected void onResume() {
        super.onResume();
        newView.onResume();
//        data.gatherStart(new nativeMsg());

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // TODO Auto-generated method stub
        if (KeyEvent.KEYCODE_BACK == keyCode) {
            if (!isFinishActivity) {
                isFinishActivity = true;
                Intent intent = new Intent();
//                intent.setClass(mContext, DeviceListActivity.class);
//                intent.setClass(mContext, UsbDeviceListActivity.class);
//                startActivity(intent);
                finish();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private TextView textHR;
    private TextView textLF;
    private static final int MESSAGE_UPDATE_HR = 0;
    private static final int MESSAGE_UPDATE_LeadOff = 1;
    public static final int MESSAGE_CONNECT_START = 0x100;
    public static final int MESSAGE_CONNECT_SUCCESS = 0x200;
    public static final int MESSAGE_CONNECT_FAILED = 0x300;
    public static final int MESSAGE_CONNECT_INTERRUPTED = 0x400;

    public static final int MESSAGE_USB_CONNECT_START = 0xA010;
    public static final int MESSAGE_USB_CONNECT_SUCCESS = 0xA020;
    public static final int MESSAGE_USB_CONNECT_FAILED = 0xA030;
    public static final int MESSAGE_USB_CONNECT_INTERRUPTED = 0xA040;
    public static final int MESSAGE_USB_CONNECT_NO_USB_PERMISSION = 0xA050;
    public static final int MESSAGE_USB_CONNECT_ERROR_SETTING_DEVICE = 0xA060;
    public static final int MESSAGE_USB_CONNECT_REMOVE_DEVICE = 0xA070;
    public static final int MESSAGE_USB_CONNECT_ERROR_OPEN_DEVICE = 0xD050;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_UPDATE_HR:
                    // textHR.setText(""+data.getHR());
                    // Log.e("HR",msg.obj.toString()+"bpm" );
                    textHR.setText(msg.obj.toString() + "bpm");
                    break;
                case MESSAGE_UPDATE_LeadOff:
                    textLF.setText(msg.obj.toString());
                    break;
                case MESSAGE_CONNECT_START:
                    Log.e("BL", "Connect Start");
                    Toast.makeText(mContext, "Connect Start", Toast.LENGTH_SHORT)
                            .show();
                    break;
                case MESSAGE_CONNECT_SUCCESS:
//				Log.e("BL", "Connect Success");
                    btnStartECGRenderer.setEnabled(true);
                    Toast.makeText(mContext, "Connect Success", Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_CONNECT_INTERRUPTED:
//				Log.e("BL", "INT");
                    btnStartConnect.setEnabled(true);
                    btnStartECGRenderer.setEnabled(false);
                    newView.stopRenderer();
                    mEcgQueue = null;
                    Toast.makeText(mContext, "Connect INT", Toast.LENGTH_SHORT).show();
/*
                    if (!isFinishActivity) {
                        isFinishActivity = true;
                        Intent interrupt = new Intent(GLActivity.this,
                                DeviceListActivity.class);
                        startActivity(interrupt);
                        finish();
                    }
                    */
                    break;
                case MESSAGE_CONNECT_FAILED:
                    Log.e("BL", "Connnect Failed");
                    btnStartConnect.setEnabled(true);
 /*
                    if (!isFinishActivity) {
                        isFinishActivity = true;
                        Intent intent = new Intent(GLActivity.this,
                                DeviceListActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    */
                    break;
                case MESSAGE_USB_CONNECT_REMOVE_DEVICE:
                    Log.e("BL", "MESSAGE_USB_CONNECT_REMOVE_DEVICE");
                    Toast.makeText(mContext, "The USB device has been removed.", Toast.LENGTH_SHORT).show();
                    finish();
                    break;
//                case MyRenderer.MESSAGE_GATHER_START:
//                    Toast.makeText(mContext, "开始绘图", Toast.LENGTH_SHORT).show();
//                    break;
                case MESSAGE_USB_CONNECT_FAILED:
                    btnStartConnect.setEnabled(true);
                    Toast.makeText(mContext, "USB IOE", Toast.LENGTH_SHORT).show();
    /*
                    Intent mintent = new Intent(GLActivity.this,
                            UsbDeviceListActivity.class);
                    startActivity(mintent);
                    finish();
                    */
                    break;
                case MESSAGE_USB_CONNECT_START:
                    Toast.makeText(mContext, "USB START", Toast.LENGTH_SHORT)
                            .show();
                    break;
                case MESSAGE_USB_CONNECT_SUCCESS:
                    btnStartECGRenderer.setEnabled(true);
                    Toast.makeText(mContext, "USB SUCCESS", Toast.LENGTH_SHORT)
                            .show();
                    break;
                case MESSAGE_USB_CONNECT_ERROR_OPEN_DEVICE:
                    btnStartConnect.setEnabled(true);
                    btnStartECGRenderer.setEnabled(false);
                    newView.stopRenderer();
                    mEcgQueue = null;
                    Toast.makeText(mContext, "USB OPEN ERR", Toast.LENGTH_SHORT)
                            .show();
                    break;
                case MESSAGE_USB_CONNECT_INTERRUPTED:
                    btnStartConnect.setEnabled(true);
                    btnStartECGRenderer.setEnabled(false);
                    newView.stopRenderer();
                    mEcgQueue = null;
                    Toast.makeText(mContext, "USB INTERRUPTED", Toast.LENGTH_SHORT)
                            .show();
                    break;
            }
        }
    };

    class nativeMsg extends NativeCallBack {

        @Override
        public void callHRMsg(short hr) {// Heart rate
            mHandler.obtainMessage(MESSAGE_UPDATE_HR, hr).sendToTarget();
        }

        @Override
        public void callLeadOffMsg(String flagOff) {// lead detachment
            // Log.e("LF", flagOff);
            mHandler.obtainMessage(MESSAGE_UPDATE_LeadOff, flagOff).sendToTarget();
        }

        @Override
        public void callProgressMsg(short progress) {// 文件存储进度百分比 progress%
            Log.e("progress", "" + progress);
        }

        @Override
        public void callCaseStateMsg(short state) {
            if (state == 0) {
                Log.e("Save", "start");// Start storing the file.
            } else {
                Log.e("Save", "end");// Storage completed
            }
        }

        @Override
        public void callHBSMsg(short hbs) {// Heart rate hbs = 1 indicates heartbeat
// Log.e("HeartBeat", "Sound"+hbs);
        }

        @Override
        public void callBatteryMsg(short per) {// Collection box battery level
            // Log.e("Battery", ""+per);
        }

        @Override
        public void callCountDownMsg(short per) {// Remaining storage duration
            // Log.e("CountDown", ""+per);
        }

        @Override
        public void callWaveColorMsg(boolean flag) {
            Log.e("WaveColor", "" + flag);
            if (flag) {
                // After the waveform stabilizes, the color becomes green
                newView.setRendererColor(0, 1.0f, 0, 0);
                // The following operations can automatically start saving files
                // data.saveCase(Environment.getExternalStorageDirectory() + "/",strCase, 20);// Store file parameters for path, filename, storage seconds
            }
        }


        @Override
        public void callEcgWaveDataMsg(short[] wave) {
            // TODO Auto-generated method stub
/*
            if (mEcgQueue != null) {
                for (int i = 48; i < 60; i++) {
                    mEcgQueue.offer(wave[i]);
                }
//				Log.e("callEcgWaveDataMsg", " " + mEcgQueue.size());
            }
            */

            if (mEcgQueue != null) {
                Vector<EcgPoint> points = convertEcg(wave);
                for (int i = 0; i < points.size(); i++) {
                    for (int j = 0; j < 12; j++) {
                        mEcgQueue.offer(points.get(i).ecg[j]);
                    }
                }
            }

        }

        /*
             Real-time ECG waveform data callback. Be careful not to do time-consuming operations in this function (running time-consuming
             should be less than 5ms), otherwise it will affect the real-time data processing speed, please refer to the sample program for the processing method.

             5 sampling point data are returned each time, the structure is as shown in the figure below

        */
        @Override
        public void callEcg18WaveDataMsg(short[] wave) {
            // TODO Auto-generated method stub
            if (mEcgQueue != null) {
                for (int i = 72; i < 90; i++) {
                    mEcgQueue.offer(wave[i]);
                }
            }
        }

        @Override
        public void callEcg15WaveDataMsg(short[] wave) {
            // TODO Auto-generated method stub
            if (mEcgQueue != null) {
                for (int i = 60; i < 75; i++) {
                    mEcgQueue.offer(wave[i]);
                }
            }
        }

        @Override
        public void callVcgWaveDataMsg(short[] wave) {
            // TODO Auto-generated method stub

        }

        @Override
        public void callVcgWaveRPosMsg(int[] flag) {
            // TODO Auto-generated method stub

        }


        @Override
        public void callNibpStateMsg(byte flag, byte type) {
            // TODO Auto-generated method stub
            super.callNibpStateMsg(flag, type);
        }

        @Override
        public void callNibpResultMsg(short sys, short dia, short mea,
                                      short pr, byte err) {
            // TODO Auto-generated method stub
            super.callNibpResultMsg(sys, dia, mea, pr, err);
            Log.e("callNibpResultMsg", "sys : " + sys + " dia : " + dia + " mea : " + mea);
            data.BluNIBPConfirmCmd();
        }

        @Override
        public void callSpO2ResultMsg(short spo2, short pr, byte state) {
            // TODO Auto-generated method stub
            super.callSpO2ResultMsg(spo2, pr, state);
            if (spo2 > 0) {
                Log.e("callSpO2ResultMsg", "spo2 : " + spo2 + "  pr : " + pr);
            }
        }

        @Override
        public void callNibpCuffMsg(short val) {
            // TODO Auto-generated method stub
            super.callNibpCuffMsg(val);
            Log.e("callNibpCuffMsg", "cuff : " + val);
        }

        @Override
        public void callSpO2ConntectStateMsg(short state) {
            // TODO Auto-generated method stub
            super.callSpO2ConntectStateMsg(state);
        }

        @Override
        public void callQueryParamsMsg(byte index, byte val) {
            super.callQueryParamsMsg(index, val);
            Log.e("callQueryParamsMsg", "index : " + index + "val : " + val);
        }

        @Override
        public void callStepResetMsg() {
            super.callStepResetMsg();
            Log.e("callStepResetMsg", "reset step");
        }

        @Override
        public void callStepDataMsg(int steps, short turn, short percent) {
            super.callStepDataMsg(steps, turn, percent);
            Log.e("callStepDataMsg", "steps : " + steps + "turns : " + turn + "percent : " + percent);
        }
    }

    static void show(Context context, UsbDevice usbDevice, UsbManager usbManager) {
        mUsbDevice = usbDevice;
        mUsbManager = usbManager;
        final Intent intent = new Intent(context, GLActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(intent);
    }

    private ConcurrentLinkedQueue<Short> mEcgQueue;
    private ExtensionDeviceBind bindDev;

    //2020-05-13 Sampling conversion 1000 --> 256
    private int srcDataIndex = 0;
    private int selectDataIndex = 0;
    private final int srcDataBufferSize = 6;
    private EcgPoint[] srcDataBuffer = new EcgPoint[srcDataBufferSize];

    private class EcgPoint {
        public short[] ecg;

        public EcgPoint() {
            ecg = new short[12];
        }
    }

    private EcgPoint[] convertShortArrayToEcgPointArray(short[] data) {
        EcgPoint[] des = new EcgPoint[5];
        for (int i = 0; i < 5; i++) {
            EcgPoint point = new EcgPoint();
            for (int j = 0; j < 12; j++) {
                point.ecg[j] = data[12 * i + j];
            }
            des[i] = point;
        }
        return des;
    }

    private Vector<EcgPoint> convertFSMP(EcgPoint[] src) {
        for (int i = 0; i < 5; i++) {
            srcDataBuffer[srcDataIndex % srcDataBufferSize] = src[i];
            srcDataIndex++;
        }
        Vector<EcgPoint> points = new Vector<>();
        float currentIndex = selectDataIndex * 3.90625f;
        while (currentIndex <= ((srcDataIndex - 1) * 1.0f)) {
            EcgPoint point = new EcgPoint();
            int index1 = (int) currentIndex;
            int index2 = index1 + 1;
            for (int i = 0; i < 12; i++) {
                short val = (short) (srcDataBuffer[index1 % srcDataBufferSize].ecg[i] + (srcDataBuffer[index2 % srcDataBufferSize].ecg[i] - srcDataBuffer[index1 % srcDataBufferSize].ecg[i]) * (currentIndex - index1 * 1.0f));
                point.ecg[i] = val;
            }
            points.add(point);
            selectDataIndex++;
            currentIndex = selectDataIndex * 3.90625f;
        }
        return points;
    }

    private Vector<EcgPoint> convertEcg(short[] data) {
        EcgPoint[] points = convertShortArrayToEcgPointArray(data);
        Vector<EcgPoint> vector = convertFSMP(points);
        return vector;
    }

    List<byte[]> mDeviceInfos;


    private void bindDev(String address) {
        bindDev = new ExtensionDeviceBind(address, new ExtensionDeviceBind.DevBindStateListener() {

            @Override
            public void onScanSuccess(List<byte[]> deviceInfos) {
                // TODO Auto-generated method stub
                mDeviceInfos = deviceInfos;
                for (byte[] bs : deviceInfos) {
                    String name = new String(bs, 7, 10);
                    Log.e("ECGSpo2BindedThread", "info:" + name);
                    if (name.contains("SpO206")) {
                        bindDev.setBindSpO2(bs);
                    }
                }
            }

            @Override
            public void onConnectState(int state) {
                // TODO Auto-generated method stub
                Log.e("ECGSpo2BindedThread", "connect state: " + state);
            }

            @Override
            public void onBindSuccess(String info) {
                // TODO Auto-generated method stub
                Log.e("ECGSpo2BindedThread", "success ecgDeviceinfo: " + info);
            }

            @Override
            public void onBindFailed(int error, String ecgDeviceInfo) {
                // TODO Auto-generated method stub
                Log.e("ECGSpo2BindedThread", "err " + error + "\n"
                        + "ecgDeviceinfo: " + ecgDeviceInfo);
            }

            @Override
            public void onQueryParamsSuccess(byte index, byte val) {

            }

            @Override
            public void onQueryParamsFailed() {

            }
        });

    }
}
