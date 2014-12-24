package org.ljh.BlueSerial;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar.OnSeekBarChangeListener;

/**
 * Jeffrey Lau 
 * 20131105
 */

@SuppressLint("HandlerLeak")
public class MainActivity extends Activity {
	// Debugging
	private static final String TAG = "BluetoothRocker";
	private static final boolean D = true;
	
	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
	
	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";
	
	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;
	
	// Layout Views
	private TextView vtv1;
	private ToggleButton vtb1;
	private ToggleButton vtb2;
	private Button vb1;
	private Button vb2;
	private SeekBar speedbar;
	private SurfaceView vsfv1;
	private SurfaceHolder sfh1;
	private Canvas canvas;
	private Paint paint;
	
	private int centerY;
	private int centerX;
	private float SmallRCX;
	private float SmallRCY;
	//private int BigRCR = 128;
	private int BigRCR = 154;
	private float SmallRCR = 64;
	//private int RCR = 64;
	private float RCR = 90;
			
	
	private int historyY;
	private int historyX;
	
	//Sensor
	private SensorManager sensorManager;
	//private float[] accelerometerValues;
	private sensorEL sEl;
	
	// Name of the connected device
	private String mConnectedDeviceName = null;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the chat services
	private BluetoothChatService mChatService = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);
		
		vtv1 = (TextView) findViewById(R.id.textView1);
		
		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
		    Toast.makeText(this, "蓝牙不可用", Toast.LENGTH_LONG).show();
		    finish();
		    return;
		}
		//SurfaceView
		vsfv1 = (SurfaceView) this.findViewById(R.id.surfaceView1);
		sfh1 = vsfv1.getHolder();
		
		paint = new Paint();
		paint.setAntiAlias(true);
		//
		DisplayMetrics dm = getResources().getDisplayMetrics();
		
		//width 50%, height 30%
		centerX = dm.widthPixels / 2;
		centerY = dm.heightPixels - (int)Math.round(dm.heightPixels * 0.3);
		SmallRCX = centerX;
		SmallRCY = centerY;
		
		//width 70%
		BigRCR = (int)Math.round(centerX * 0.7);
		SmallRCR = 70;
		RCR = (int)(BigRCR - SmallRCR);
		
		Log.v("centerX", String.valueOf(centerX));
		Log.v("centerY", String.valueOf(centerY));
		//
		Sfv_draw_Thread draw_th = new Sfv_draw_Thread();
		Thread th = new Thread(draw_th);
		th.start();
		//
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
	}
	
	@Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");
        // If BT is not on, request that it be enabled.
        // setupEvent() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            if (mChatService == null) setupEvent();
        }
    }

	@Override
	public synchronized void onResume() {
		super.onResume();
		if(D) Log.e(TAG, "+ ON RESUME +");
		// Performing this check in onResume() covers the case in which BT was
		// not enabled during onStart(), so we were paused to enable it...
		// onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
		if (mChatService != null) {
		    // Only if the state is STATE_NONE, do we know that we haven't started already
		    if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
		      // Start the Bluetooth chat services
		      mChatService.start();
		    }
		}
	}
	
	@Override
	public synchronized void onPause() {
	    super.onPause();
	    if(D) Log.e(TAG, "- ON PAUSE -");
	}
	
	@Override
	public void onStop() {
	    super.onStop();
	    if(D) Log.e(TAG, "-- ON STOP --");
	}
	
	@Override
	public void onDestroy() {
	    super.onDestroy();
	    // Stop the Bluetooth services
	    if (mChatService != null) mChatService.stop();
	    if(D) Log.e(TAG, "--- ON DESTROY ---");
	}

    private void setupEvent() {
		if(D) Log.d(TAG, "setupEvent()");
		
		vtb1 = (ToggleButton) findViewById(R.id.toggleButton1);
		vtb1.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				byte[] message = new byte[6];
				message[0] = (byte)170;//0xAA
				message[1] = 85;//0x55
				message[4] = 0;//0x0
				message[5] = 0;//0x0
				if(isChecked){
					message[2] = 1;
					message[3] = 1;
				}else{
					message[2] = 1;
					message[3] = 0;
				}
				sendData(message);
			}
		});
		vtb2 = (ToggleButton) findViewById(R.id.toggleButton2);
		vtb2.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				byte[] message = new byte[6];
				message[0] = (byte)170;//0xAA
				message[1] = 85;//0x55
				message[4] = 0;//0x0
				message[5] = 0;//0x0
				if(isChecked){
					message[2] = 2;
					message[3] = 1;
				}else{
					message[2] = 2;
					message[3] = 0;
				}
				sendData(message);
			}
		});
		vb1 = (Button) findViewById(R.id.button1);
		vb1.setOnClickListener(new OnClickListener() {
		    public void onClick(View v) {
		    	byte[] message = new byte[6];
				message[0] = (byte)170;//0xAA
				message[1] = 85;//0x55
				message[4] = 0;//0x0
				message[5] = 0;//0x0
				message[2] = 3;
				message[3] = 1;
				sendData(message);
		    }
		});
		vb2 = (Button) findViewById(R.id.button2);
		vb2.setOnClickListener(new OnClickListener() {
		    public void onClick(View v) {
		    	byte[] message = new byte[6];
				message[0] = (byte)170;//0xAA
				message[1] = 85;//0x55
				message[4] = 0;//0x0
				message[5] = 0;//0x0
				message[2] = 4;
				message[3] = 1;
				sendData(message);
		    }
		});
		
		speedbar = (SeekBar) findViewById(R.id.seekBar1);
        speedbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
        	@Override 
        	public void onStopTrackingTouch(SeekBar seekBar) { }
        	
        	@Override 
        	public void onStartTrackingTouch(SeekBar seekBar) { }
        	
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				//speed.setText(""+progress);
				byte[] message = new byte[6];
				message[0] = (byte)170;//0xAA
				message[1] = 85;//0x55
				message[2] = 5;//cmd
				message[3] = (byte)progress;
				message[4] = 0;//0x0
				message[5] = 0;//0x0
				sendData(message);
			}
		});
		// Initialize the BluetoothChatService to perform bluetooth connections
		mChatService = new BluetoothChatService(this, mHandler);
	}
    
    private void sendData(byte[] message) {
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            //Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        if (message.length > 0) {
            mChatService.write(message);
        }
    }
	// The Handler that gets information back from the BluetoothChatService
	private final Handler mHandler = new Handler(){
		@Override
	    public void handleMessage(Message msg) {
	        switch (msg.what) {
	        case MESSAGE_STATE_CHANGE:
	            if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
	            switch (msg.arg1) {
	            case BluetoothChatService.STATE_CONNECTED:
	                vtv1.setText(R.string.title_connected_to);
	                vtv1.append(mConnectedDeviceName);
	                break;
	            case BluetoothChatService.STATE_CONNECTING:
	            	vtv1.setText(R.string.title_connecting);
	                break;
	            case BluetoothChatService.STATE_LISTEN:
	            case BluetoothChatService.STATE_NONE:
	            	vtv1.setText(R.string.title_not_connected);
	                break;
	            }
	            break;
	        case MESSAGE_WRITE:
	            //byte[] writeBuf = (byte[]) msg.obj;
	            // construct a string from the buffer
	            //String writeMessage = new String(writeBuf);
	            //mConversationArrayAdapter.add("Me:  " + writeMessage);
	            break;
	        case MESSAGE_READ:
	            byte[] readBuf = (byte[]) msg.obj;
	            // construct a string from the valid bytes in the buffer
	            //String readMessage = new String(readBuf, 0, msg.arg1);
	            String readMessage = String.valueOf(readBuf);
	            Toast.makeText(getApplicationContext(), readMessage, Toast.LENGTH_SHORT).show();
	            break;
	        case MESSAGE_DEVICE_NAME:
	            // save the connected device's name
	            mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
	            Toast.makeText(getApplicationContext(), "连接到 "
	                           + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
	            break;
	        case MESSAGE_TOAST:
	            Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
	                           Toast.LENGTH_SHORT).show();
	            break;
	        }
	    }
	};
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if(D) Log.d(TAG, "onActivityResult " + resultCode);
	    switch (requestCode) {
	    case REQUEST_CONNECT_DEVICE:
	        // When DeviceListActivity returns with a device to connect
	        if (resultCode == Activity.RESULT_OK) {
	            // Get the device MAC address
	            String address = data.getExtras()
	                                 .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
	            // Get the BLuetoothDevice object
	            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
	            // Attempt to connect to the device
	            mChatService.connect(device);
	        }
	        break;
	    case REQUEST_ENABLE_BT:
	        // When the request to enable Bluetooth returns
	        if (resultCode == Activity.RESULT_OK) {
	            // Bluetooth is now enabled, so set up a chat session
	            setupEvent();
	        } else {
	            // User did not enable Bluetooth or an error occured
	        	if(D) Log.d(TAG, "BT disabled");
	            Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
	            finish();
	        }
	    }
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_scan:
            // Launch the DeviceListActivity to see devices and do scan
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
        // red mi have something bug in here
        case R.id.menu_settings:
        	if (item.isChecked()) {
        		item.setChecked(false);
				sensorStop();
				settings(0);
			}else {
				item.setChecked(true);
        		sensorStart();
        		settings(1);
			}
            return true;
        case R.id.menu_about:
            about();
            return true;
        }
        return false;
    }
	private void settings(int action) {
        if(D) Log.d(TAG, "setting");
        if (action == 1) {
        	Toast.makeText(this, "传感器已经打开", Toast.LENGTH_SHORT).show();
		}else {
			Toast.makeText(this, "传感器已经关闭", Toast.LENGTH_SHORT).show();
		}
    }
	private void about() {
        if(D) Log.d(TAG, "about");
        new AboutDialog(this).show();
        //Toast.makeText(this, "  BlueSerial\nDesigned by Jeffrey Lau", Toast.LENGTH_SHORT).show();
    }
	class AboutDialog extends AlertDialog {
        protected AboutDialog(Context context) {
			super(context);
			final View view = getLayoutInflater().inflate(R.layout.about, null);
        	//setButton(context.getText(R.string.close), (OnClickListener) null);  
            setIcon(R.drawable.logo);  
            setTitle("BlueSerial" );  
            setView(view); 
		}
    }
	
	//
	class Sfv_draw_Thread implements Runnable {
		@Override
		public void run() {
			while (true) {
				draw();
				try {
					Thread.sleep(100);
				} catch (Exception ex) {}
			}
		}
	}
	public void draw() {
		try {
			canvas = sfh1.lockCanvas();
			canvas.drawColor(Color.WHITE);
			//circle
			paint.setColor(0x70000000);
			canvas.drawCircle(centerX, (centerY - vsfv1.getTop()), BigRCR, paint);
			
			paint.setColor(Color.BLUE);
			canvas.drawCircle(SmallRCX, (SmallRCY - vsfv1.getTop()), SmallRCR, paint);
		} catch (Exception e) {
			// TODO: handle exception
		} finally {
			try {
				if (canvas != null)
					sfh1.unlockCanvasAndPost(canvas);
			} catch (Exception e2) {
			}
		}
	}
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();
		if (Math.sqrt(Math.pow((centerX - (int)event.getX()), 2) + Math.pow((centerY - (int)event.getY()), 2)) >= BigRCR+60) {
			return true;
		}
		if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
			// 当触屏区域不在活动范围内
			if (Math.sqrt(Math.pow((centerX - (int)event.getX()), 2) + Math.pow((centerY - (int)event.getY()), 2)) >= RCR) {
				//得到摇杆与触屏点所形成的角度
				double tempRad = getRad(centerX, centerY, event.getX(), event.getY());
				//保证内部小圆运动的长度限制
				getXY(centerX, centerY, RCR, tempRad);
			} else {//如果小球中心点小于活动区域则随着用户触屏点移动即可
				SmallRCX = (int) event.getX();
				SmallRCY = (int) event.getY();
				Log.v("ljh", "SmallRCX "+SmallRCX);
				Log.v("ljh", "SmallRCY "+SmallRCY);
			}
		} else if (action == MotionEvent.ACTION_UP) {
			//当释放按键时摇杆要恢复摇杆的位置为初始位置
			SmallRCX = centerX;
			SmallRCY = centerY;
		}
		sendrockerdata(SmallRCX, SmallRCY);
		return true;
	}
	/***
	 * 得到两点之间的弧度
	 */
	public double getRad(float px1, float py1, float px2, float py2) {
		//得到两点X的距离
		float x = px2 - px1;
		//得到两点Y的距离
		float y = py1 - py2;
		//算出斜边长
		float xie = (float) Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
		//得到这个角度的余弦值（通过三角函数中的定理 ：邻边/斜边=角度余弦值）
		float cosAngle = x / xie;
		//通过反余弦定理获取到其角度的弧度
		float rad = (float) Math.acos(cosAngle);
		//注意：当触屏的位置Y坐标<摇杆的Y坐标我们要取反值-0~-180
		if (py2 < py1) {
			rad = -rad;
		}
		return rad;
	}
	/**
	 * 
	 * @param R
	 *            圆周运动的旋转点
	 * @param centerX
	 *            旋转点X
	 * @param centerY
	 *            旋转点Y
	 * @param rad
	 *            旋转的弧度
	 */
	public void getXY(float centerX, float centerY, float R, double rad) {
		//获取圆周运动的X坐标 
		SmallRCX = (float) (R * Math.cos(rad)) + centerX;
		//获取圆周运动的Y坐标
		SmallRCY = (float) (R * Math.sin(rad)) + centerY;
		Log.v("ljh", "SmallRCX "+SmallRCX);
		Log.v("ljh", "SmallRCY "+SmallRCY);
	}
	
	public void sendrockerdata(float x, float y){
		int x_int, y_int,tmp=0, corepos = 100;
		x_int = (int)x;
		y_int = (int)y;
		if (historyX == x_int && historyY == y_int) {
			return;
		}
		byte[] message = new byte[6];
		message[0] = (byte)170;//0xAA
		message[1] = 85;//0x55
		message[2] = 15;
		message[5] = 0;
		//message[8] = (byte)170;//0xAA
		//message[9] = 85;//0x55
		//message[12] = (byte)170;//0xAA
		//message[13] = 85;//0x55
		//message[16] = (byte)170;//0xAA
		//message[17] = 85;//0x55
		//message[20] = (byte)170;//0xAA
		//message[21] = 85;//0x55
		
		//x
		if (x_int == centerX) {
			message[3] = 0;
		}
		if (x_int < centerX) {
			//tmp = Math.round((centerX-x_int)/RCR*corepos);
			//message[3] = (byte)(corepos-tmp);
			tmp = Math.round((centerX-x_int)/RCR*corepos);
			message[3] = (byte)(-tmp);
			//message[18] = 13;//L
			//message[19] = (byte)tmp;
		}
		if (x_int > centerX) {
			//tmp = Math.round((x_int-centerX)/RCR*corepos);
			//message[3] = (byte)(corepos+tmp);
			tmp = Math.round((x_int-centerX)/RCR*corepos);
			message[3] = (byte)(tmp);
			//message[22] = 14;//R
			//message[23] = (byte)tmp;
		}
		//y
		if (y_int == centerY) {
			message[4] = 0;
		}
		if (y_int < centerY) {
			tmp = Math.round((centerY-y_int)/RCR*corepos);
			message[4] = (byte)(tmp);
			//message[10] = 11;//F
			//message[11] = (byte)tmp;
		}
		if (y_int > centerY) {
			tmp = Math.round((y_int-centerY)/RCR*corepos);
			message[4] = (byte)(-tmp);
			//message[14] = 12;//B
			//message[15] = (byte)tmp;
		}
		//
		sendData(message);
		historyX = x_int;
		historyY = y_int;
	}
	//将data字节型数据转换为0~255 (0xFF 即BYTE)
	public int getUnsignedByte (byte data){
        return data&0x0FF ;
	}
	//sensor
	public void sensorStart(){
		//Sensor
		sEl = new sensorEL();
		sensorManager.registerListener(sEl,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	public void sensorStop(){
		sensorManager.unregisterListener(sEl);
	}
	
	class sensorEL implements SensorEventListener {
		@Override
		public void onSensorChanged(SensorEvent event)
		{
			byte[] message = new byte[6];
			message[0] = (byte)170;//0xAA
			message[1] = 85;//0x55
			switch (event.sensor.getType())
			{
				case Sensor.TYPE_ACCELEROMETER:
					message[2] = 21;
					message[3] = (byte)Math.round(-event.values[0]*10);//x *13
					message[4] = (byte)Math.round(-event.values[1]*10);//y
					message[5] = (byte)Math.round(event.values[2]*10);//z
					break;
			}
			sendData(message);
		}
		
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy)
		{
			// TODO Auto-generated method stub
		}
	}
	//
	
	private long exitTime = 0;
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN){   
	        if((System.currentTimeMillis()-exitTime) > 2000){  
	            Toast.makeText(getApplicationContext(), "再按一次退出程序", Toast.LENGTH_SHORT).show();                                
	            exitTime = System.currentTimeMillis();   
	        } else {
	        	if (mChatService != null) mChatService.stop();
	            finish();
	            System.exit(0);
	        }
	        return true;   
	    }
	    return super.onKeyDown(keyCode, event);
	}
}
