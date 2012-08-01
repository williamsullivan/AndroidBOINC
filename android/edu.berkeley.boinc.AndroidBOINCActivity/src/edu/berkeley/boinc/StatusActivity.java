package edu.berkeley.boinc;

import edu.berkeley.boinc.client.ClientStatus;
import edu.berkeley.boinc.client.Monitor;
import edu.berkeley.boinc.definitions.CommonDefs;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class StatusActivity extends Activity {
	
	private final String TAG = "StatusActivity";
	
	private Monitor monitor;
	
	private Boolean mIsBound = false;

	private BroadcastReceiver mClientStatusChangeRec = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context,Intent intent) {
			String action = intent.getAction();
			Log.d(TAG+"-localClientStatusRecNoisy","received action " + action);
			loadLayout(); //initial layout set up
		}
	};
	private IntentFilter ifcsc = new IntentFilter("edu.berkeley.boinc.clientstatuschange");
	

	private ServiceConnection mConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder service) {
	    	Log.d(TAG,"onServiceConnected");
	        monitor = ((Monitor.LocalBinder)service).getService();
		    mIsBound = true;
		    loadLayout();
	    }

	    public void onServiceDisconnected(ComponentName className) {
	        monitor = null;
	    }
	};

	void doBindService() {
		if(!mIsBound) {
			getApplicationContext().bindService(new Intent(this, Monitor.class), mConnection, 0); //calling within Tab needs getApplicationContext() for bindService to work!
		}
	}

	void doUnbindService() {
	    if (mIsBound) {
	        unbindService(mConnection);
	        mIsBound = false;
	    }
	}
	
	public void onCreate(Bundle savedInstanceState) {
		//bind to monitor in order to call its functions and access ClientStatus singleton
		doBindService();
		super.onCreate(savedInstanceState);
	}
	
	public void onResume() {
		//register noisy clientStatusChangeReceiver here, so only active when Activity is visible
		Log.d(TAG+"-onResume","register receiver");
		registerReceiver(mClientStatusChangeRec,ifcsc);
		loadLayout();
		super.onResume();
	}
	
	public void onPause() {
		//unregister receiver, so there are not multiple intents flying in
		Log.d(TAG+"-onPause","remove receiver");
		unregisterReceiver(mClientStatusChangeRec);
		super.onStop();
	}

	@Override
	protected void onDestroy() {
	    super.onDestroy();
	    doUnbindService();
	}
	
	private void loadLayout() {
		//load layout, if service is available and ClientStatus can be accessed.
		//if this is not the case, "onServiceConnected" will call "loadLayout" as soon as the service is bound
		if(mIsBound) {
			ClientStatus status = Monitor.getClientStatus();
			switch(status.setupStatus){
			case 0:
				setContentView(R.layout.status_layout_launching);
				break;
			case 1: 
				switch(status.computingStatus) {
				case 0:
					setContentView(R.layout.status_layout_computing_disabled);
					findViewById(R.id.enableImage).setOnClickListener(mEnableClickListener);
					findViewById(R.id.enableText).setOnClickListener(mEnableClickListener);
					break;
				case 1: //suspended for reason
					setContentView(R.layout.status_layout_suspended);
					findViewById(R.id.disableImage).setOnClickListener(mDisableClickListener);
					findViewById(R.id.disableText).setOnClickListener(mDisableClickListener);
					TextView t=(TextView)findViewById(R.id.suspend_reason);
					switch(status.computingSuspendReason) {
					case 1:
						t.setText(R.string.suspend_batteries);
						break;
					case 2:
						t.setText(R.string.suspend_useractive);
						break;
					case 4:
						t.setText(R.string.suspend_userreq);
						break;
					case 8:
						t.setText(R.string.suspend_tod);
						break;
					case 16:
						t.setText(R.string.suspend_bm);
						break;
					case 32:
						t.setText(R.string.suspend_disksize);
						break;
					case 64:
						t.setText(R.string.suspend_cputhrottle);
						break;
					case 128:
						t.setText(R.string.suspend_noinput);
						break;
					case 256:
						t.setText(R.string.suspend_delay);
						break;
					case 512:
						t.setText(R.string.suspend_exclusiveapp);
						break;
					case 1024:
						t.setText(R.string.suspend_cpu);
						break;
					case 2048:
						t.setText(R.string.suspend_network_quota);
						break;
					case 4096:
						t.setText(R.string.suspend_os);
						break;
					case 8192:
						t.setText(R.string.suspend_wifi);
						break;
					default:
						t.setText(R.string.suspend_unknown);
						break;
					}
					break;
				case 2: //idle
					setContentView(R.layout.status_layout_suspended);
					TextView t2=(TextView)findViewById(R.id.suspend_reason);
				    t2.setText(R.string.suspend_idle);
					break;
				case 3:
					setContentView(R.layout.status_layout_computing);
					findViewById(R.id.disableImage).setOnClickListener(mDisableClickListener);
					findViewById(R.id.disableText).setOnClickListener(mDisableClickListener);
					break;
				}
				break;
			case 2:
				setContentView(R.layout.status_layout_error);
				Log.d(TAG,"layout: status_layout_error");
				break;
			}
		}
	}
	
	//gets called when user clicks on retry of error_layout
	//has to be public in order to get triggered by layout component
	public void reinitClient(View view) {
		if(!mIsBound) return;
		Log.d(TAG,"reinitClient");
		monitor.restartMonitor(); //start over with setup of client
	}
	
	private OnClickListener mEnableClickListener = new OnClickListener() {
	    public void onClick(View v) {
	    	Log.d(TAG,"mEnableClickListener - onClick");
			if(!mIsBound) return;
			Log.d(TAG,"enableComputation");
			monitor.setRunMode(CommonDefs.RUN_MODE_AUTO);
	    }
	};
	
	private OnClickListener mDisableClickListener = new OnClickListener() {
	    public void onClick(View v) {
	    	Log.d(TAG,"mDisableClickListener - onClick");
			if(!mIsBound) return;
			Log.d(TAG,"disableComputation");
			monitor.setRunMode(CommonDefs.RUN_MODE_NEVER);
	    }
	};

}
