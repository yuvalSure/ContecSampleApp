package serial.jni;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


public class DeviceListActivity extends Activity {

	private static final int REQUEST_ENABLE_BT = 3;
	public  String EXTRA_DEVICE_ADDRESS = "device_address";
    // Member fields
    private BluetoothAdapter mBtAdapter = null;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;

    private Button mScanButton = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.device_list);
        
        // Initialize the button to perform device discovery
        mScanButton = (Button) findViewById(R.id.button_scan);
        mScanButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	mNewDevicesArrayAdapter.clear();
            	v.setEnabled(false);
                doDiscovery();
            }
        });

        
        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        // Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);
        String noDevices = "none_found";
        mNewDevicesArrayAdapter.add(noDevices);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);
        
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBtAdapter == null) {
            Toast.makeText(this, "bt_not_exist", Toast.LENGTH_LONG).show();
        	finish();
            return;
        }
        if (!mBtAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }        
    }
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	     
		// Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        //Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (KeyEvent.KEYCODE_BACK == keyCode) {

			System.exit(0);

		}
		return super.onKeyDown(keyCode, event);
	}
    private void doDiscovery() {

        setProgressBarIndeterminateVisibility(true);
        setTitle("scanning");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
                return;
            }
        }

        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        mBtAdapter.startDiscovery();
    }


    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
  
            mBtAdapter.cancelDiscovery();
            String info = ((TextView) v).getText().toString();
            if (info.length()<17)	{return;}
            String address = info.substring(info.length() - 17);
            Intent intent;		
//            intent = new Intent(DeviceListActivity.this, GLActivity.class);
            intent = new Intent(DeviceListActivity.this, GLActivityNew.class);
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
            startActivity(intent);
            finish();
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
    	@Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
           
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if ((device.getName() != null) /*&& (device.getName().contains("ECGWS")||device.getName().contains("8000GW")||device.getName().contains("CB100"))*/) {
					mNewDevicesArrayAdapter.remove(device.getName() + "\n"+ device.getAddress());
					mNewDevicesArrayAdapter.add(device.getName() + "\n"+ device.getAddress());
                }
            }   
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle("select device");

                if (mNewDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = "none_found";
                    mNewDevicesArrayAdapter.add(noDevices);
                }
                findViewById(R.id.button_scan).setEnabled(true);
            }           
        }
    };
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		switch (requestCode) {
		case REQUEST_ENABLE_BT:
			if (resultCode != Activity.RESULT_OK) {
				Toast.makeText(this, "bt_not_enabled", Toast.LENGTH_SHORT).show();
				finish();
			}
		}  
	}
}
