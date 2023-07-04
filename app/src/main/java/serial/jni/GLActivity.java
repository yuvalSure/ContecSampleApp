package serial.jni;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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

import serial.jni.ExtensionDeviceBind.DevBindStateListener;

public class GLActivity extends Activity {
    private GLView glView;
    private DataUtils data;
    private Context mContext;
    private static UsbManager mUsbManager;
    private static UsbDevice mUsbDevice;
    private String strCase;
    /**
     * 2018-05 isFinishActivity
     * 此变量根据实际使用场景设置，当返回执行关闭当前波形显示acticity时，由于可能蓝牙正在连接中，会返回连接失败的消息
     * 需要判断相关处理操作是否已经执行，避免多次执行打开某个activity的操作或对其它变量及状态的处理，造成不必要的错误
     */
    private boolean isFinishActivity;
    private int m = 0;
    private int n = 0;
    private int p = 0;
    private int q = 0;
    private Button btnStartConnect ;
    private Button btnStopConnect ;

    private Button btnStartECGRenderer ;
    private Button btnStopECGRenderer ;

    private Button btnConnectBindSpo2 ;
    private Button btnDisConnectBindSpo2 ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.glsurfaceview);
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

        btnConnectBindSpo2 = (Button) findViewById(R.id.btnConnectBindSpo2);
        btnDisConnectBindSpo2 = (Button) findViewById(R.id.btnDisConnectBindSpo2);

        btnStartECGRenderer.setEnabled(true);
        btnStartConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnStartConnect.setEnabled(false);
                data.gatherStart(new nativeMsg());
            }
        });
        btnStopConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnStartECGRenderer.setEnabled(false);
                data.gatherEnd();
                glView.stopRenderer();
                btnStartConnect.setEnabled(true);
            }
        });

        btnStartECGRenderer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEcgQueue = new ConcurrentLinkedQueue<Short>();
                glView.setEcgDataBuf(mEcgQueue);
                glView.startRenderer();
            }
        });
        btnStopECGRenderer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                glView.stopRenderer();
                mEcgQueue = null;
            }
        });

        btnConnectBindSpo2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bindDev.connect();
            }
        });
        btnDisConnectBindSpo2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bindDev.disconnect();
            }
        });

        //all
        b1.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // 分析数据文件，结果存储为xml
/*
                int ret = data.ecgAnalyzeToXml(
                        Environment.getExternalStorageDirectory() + "/"
                                + strCase,
                        Environment.getExternalStorageDirectory()
                                + "/BECG_advice.xml",
                        Environment.getExternalStorageDirectory()
                                + "/conclusion.cn");
                */
//自定义心率上下限
                int ret = data.ecgAnalyzeToXml("/mnt/sdcard/AECG",
                        "/mnt/sdcard/BECG_advice.xml",
                        "/mnt/sdcard/conclusion.cn",50,60);

                Log.e("ANA", "ecgAnalyzeToXml ret = " + ret);
            }
        });
        //ac
        b2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // data.setGain(DataUtils.DISPLAY_GAIN__20);
                // 数据文件转换成aecg格式病例
                // int ret = data.ecgDataToAECG(
                // Environment.getExternalStorageDirectory() + "/"
                // + strCase + ".c8k",
                // Environment.getExternalStorageDirectory() + "/BECG.xml");
                int ret = data.ecgDataToAECG(
                        Environment.getExternalStorageDirectory() + "/"
                                + "20170419152220.c8k",
                        Environment.getExternalStorageDirectory() + "/BECG.xml");
/*
                18导时使用ecg18DataToAECG这个方法进行aecg转换
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
                    Toast.makeText(mContext, "滤波全关", Toast.LENGTH_LONG).show();
                } else if (n % 5 == 1) {
                    data.setFilter(1);
                    Toast.makeText(mContext, "工频滤波", Toast.LENGTH_LONG).show();
                } else if (n % 5 == 2) {
                    data.setFilter(2);
                    Toast.makeText(mContext, "肌电滤波", Toast.LENGTH_LONG).show();
                } else if (n % 5 == 3) {
                    data.setFilter(4);
                    Toast.makeText(mContext, "基线滤波", Toast.LENGTH_LONG).show();
                } else if (n % 5 == 4) {
                    data.setFilter(7);
                    Toast.makeText(mContext, "滤波全开", Toast.LENGTH_LONG).show();
                }
                n++;
            }
        });
        //bl
        b4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                data.cancelCase();// 取消正在保存的文件
/*                if (!isFinishActivity) {
                    isFinishActivity = true;
                    Intent intent = new Intent();
                    intent.setClass(mContext, DeviceListActivity.class);
                    startActivity(intent);
                    finish();
                }*/
//                finish();
                data.BluNIBPCtrlCmd((byte)1,(byte)0);
            }
        });
        //no
        b5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                data.saveCase(Environment.getExternalStorageDirectory() + "/",
                        strCase, 10);// 存储文件 参数为路径，文件名，存储秒数
            }
        });
        //displaymode
        b6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (q % 4 == 0) {
                    data.setDisplayMode(DataUtils.DISPLAY_MODE_12x1);
                    Toast.makeText(mContext, "12x1显示，仅12导有效！", Toast.LENGTH_LONG).show();
                } else if (q % 4 == 1) {
                    data.setDisplayMode(DataUtils.DISPLAY_MODE_6x2);
                    Toast.makeText(mContext, "6x2显示，仅12导有效！", Toast.LENGTH_LONG).show();
                } else if (q % 4 == 2) {
                    data.setDisplayMode(DataUtils.DISPLAY_MODE_2x6_LIMB);
                    Toast.makeText(mContext, "6导（肢体导联）显示", Toast.LENGTH_LONG).show();
                }  else if (q % 4 == 2) {
                    data.setDisplayMode(DataUtils.DISPLAY_MODE_2x6_CHEST);
                    Toast.makeText(mContext, "6导（胸导导联）显示", Toast.LENGTH_LONG).show();
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
                    Toast.makeText(mContext, "2.5mm/mV", Toast.LENGTH_LONG).show();;
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
        // data对象包含所有心电采集相关操作
        // glView负责显示
        // 蓝牙采集
/*
        Intent para = null;
        para = getIntent();
        if (para != null) {
            String address = para.getExtras().getString("device_address");
            data = new DataUtils(mContext, address,DataUtils.ECG_LEAD_WILSON,false,
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
            bindDev = new ExtensionDeviceBind(address, new DevBindStateListener() {

                @Override
                public void onScanSuccess(List<byte[]> deviceInfos) {
                    // TODO Auto-generated method stub
                    for (byte[] bs : deviceInfos) {
                        String name = new String(bs,7,10);
                        Log.e("ECGSpo2BindedThread", "info:" + name);
                        if(name.contains("SpO206")){
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
                public void onBindSuccess(String ecgDeviceInfo) {
                    // TODO Auto-generated method stub
                    Log.e("ECGSpo2BindedThread", "ecgDeviceinfo: " + ecgDeviceInfo);
                }

                @Override
                public void onBindFailed(int error, String ecgDeviceInfo) {
                    // TODO Auto-generated method stub
                    Log.e("ECGSpo2BindedThread", "err " + error + "\n"
                            + "ecgDeviceinfo: " + ecgDeviceInfo);
                }
            });
        }
        */
        // 演示文件采集
         data = new DataUtils(Environment.getExternalStorageDirectory().getPath()+"/demo.ecg");
/*
		// USB 8000G 设备支持
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
        }, 460800,DataUtils.ECG_LEAD_18_TYPE_18);
        */

        // 以下关于glView操作为必要操作，请不要更改
        data.setDisplayMode(DataUtils.DISPLAY_MODE_12x1);

        glView = (GLView) this.findViewById(R.id.GLWave);
        glView.initDisplay();
//		glView.setBackground(Color.TRANSPARENT, Color.rgb(111, 110, 110));//2018-06-21 注释掉,避免Bitmap出现异常

//        glView.setMsg(mHandler);
        glView.setZOrderOnTop(true);
        glView.getHolder().setFormat(PixelFormat.TRANSLUCENT);

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
        // 此函数执行时，不要执行强制退出、多次点击返回按键等影响android生命周期管理的操作！
        glView.onPause();
        data.gatherEnd();
    }

    @Override
    protected void onResume() {
        super.onResume();
        glView.onResume();
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
                    glView.stopRenderer();
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
                    Toast.makeText(mContext, "USB设备被移除", Toast.LENGTH_SHORT).show();
                    finish();
                    break;
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
                    glView.stopRenderer();
                    mEcgQueue = null;
                    Toast.makeText(mContext, "USB OPEN ERR", Toast.LENGTH_SHORT)
                            .show();
                    break;
                case MESSAGE_USB_CONNECT_INTERRUPTED:
                    btnStartConnect.setEnabled(true);
                    btnStartECGRenderer.setEnabled(false);
                    glView.stopRenderer();
                    mEcgQueue = null;
                    Toast.makeText(mContext, "USB INTERRUPTED", Toast.LENGTH_SHORT)
                            .show();
                    break;
            }
        }
    };

    class nativeMsg extends NativeCallBack {

        @Override
        public void callHRMsg(short hr) {// 心率
            mHandler.obtainMessage(MESSAGE_UPDATE_HR, hr).sendToTarget();
        }

        @Override
        public void callLeadOffMsg(String flagOff) {// 导联脱落
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
                Log.e("Save", "start");// 开始存储文件
            } else {
                Log.e("Save", "end");// 存储完成
            }
        }

        @Override
        public void callHBSMsg(short hbs) {// 心率 hbs = 1表示有心跳
            // Log.e("HeartBeat", "Sound"+hbs);
        }

        @Override
        public void callBatteryMsg(short per) {// 采集盒电量
            // Log.e("Battery", ""+per);
        }

        @Override
        public void callCountDownMsg(short per) {// 剩余存储时长
            // Log.e("CountDown", ""+per);
        }

        @Override
        public void callWaveColorMsg(boolean flag) {
            Log.e("WaveColor", "" + flag);
            if (flag) {
                // 波形稳定后颜色变为绿色
                glView.setRendererColor(0, 1.0f, 0, 0);
                // 以下操作可以实现自动开始保存文件
                // data.saveCase(Environment.getExternalStorageDirectory() + "/",strCase, 20);// 存储文件 参数为路径，文件名，存储秒数
            }
        }
        @Override
        public void callEcgWaveDataMsg(short[] wave) {
            // TODO Auto-generated method stub
            if (mEcgQueue != null) {
                for (int i = 48; i < 60; i++) {
                    mEcgQueue.offer(wave[i]);
                }
//				Log.e("callEcgWaveDataMsg", " " + mEcgQueue.size());
            }
        }
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
            Log.e("callNibpResultMsg", "sys : " + sys +" dia : " + dia + " mea : " + mea);
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
}
