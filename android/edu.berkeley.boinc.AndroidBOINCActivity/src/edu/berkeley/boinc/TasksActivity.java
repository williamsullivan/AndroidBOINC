package edu.berkeley.boinc;

import java.util.ArrayList;

import edu.berkeley.boinc.adapter.TasksListAdapter;
import edu.berkeley.boinc.client.ClientStatus;
import edu.berkeley.boinc.client.Monitor;
import edu.berkeley.boinc.rpc.Result;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

public class TasksActivity extends Activity {
	
	private final String TAG = "TasksActivity";
	
	private ClientStatus status; //client status, new information gets parsed by monitor, changes notified by "clientstatus" broadcast. read Result from here, to get information about tasks.

	private ListView lv;
	private TasksListAdapter listAdapter;
	
	private ArrayList<Result> data = new ArrayList<Result>(); //Adapter for list data
	private Boolean setup = false;

	private BroadcastReceiver mClientStatusChangeRec = new BroadcastReceiver() {
		
		private final String TAG = "TasksActivity-Receiver";
		@Override
		public void onReceive(Context context,Intent intent) {
			Log.d(TAG,"onReceive");
			loadData(); // refresh list view
		}
	};
	private IntentFilter ifcsc = new IntentFilter("edu.berkeley.boinc.clientstatuschange");
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tasks_layout); 
		
		//get singleton client status from monitor
		status = Monitor.getClientStatus();
		
        //load data model
		loadData();
		
        
        Log.d(TAG,"onCreate");
	}
	
	public void onResume() {
		super.onResume();
		//register noisy clientStatusChangeReceiver here, so only active when Activity is visible
		Log.d(TAG+"-onResume","register receiver");
		registerReceiver(mClientStatusChangeRec,ifcsc);
		loadData();
	}
	
	public void onPause() {
		//unregister receiver, so there are not multiple intents flying in
		Log.d(TAG+"-onPause","remove receiver");
		unregisterReceiver(mClientStatusChangeRec);
		super.onPause();
	}
	
	
	private void loadData() {

		//setup list and adapter
		ArrayList<Result> tmpA = status.getTasks();
		if(tmpA!=null) { //can be null before first monitor status cycle (e.g. when not logged in or during startup)
		
			//deep copy, so ArrayList adapter actually recognizes the difference
			data.clear();
			for (Result tmp: tmpA) {
				data.add(tmp);
			}
			
			if(!setup) {// first time we got proper results, setup adapter
				lv = (ListView) findViewById(R.id.tasksList);
		        listAdapter = new TasksListAdapter(TasksActivity.this,R.id.tasksList,data);
		        lv.setAdapter(listAdapter);
		        
		        setup = true;
			} 
		
			Log.d(TAG,"loadData: array contains " + data.size() + " results.");
			listAdapter.notifyDataSetChanged(); //force list adapter to refresh
		}else {
			Log.d(TAG, "loadData array is null");
		}
		
	}
}
