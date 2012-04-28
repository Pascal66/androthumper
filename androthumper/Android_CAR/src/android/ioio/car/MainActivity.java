/*******************************************************************************************************
Copyright (c) 2011 Regents of the University of California.
All rights reserved.

This software was developed at the University of California, Irvine.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in
   the documentation and/or other materials provided with the
   distribution.

3. All advertising materials mentioning features or use of this
   software must display the following acknowledgment:
   "This product includes software developed at the University of
   California, Irvine by Nicolas Oros, Ph.D.
   (http://www.cogsci.uci.edu/~noros/)."

4. The name of the University may not be used to endorse or promote
   products derived from this software without specific prior written
   permission.

5. Redistributions of any form whatsoever must retain the following
   acknowledgment:
   "This product includes software developed at the University of
   California, Irvine by Nicolas Oros, Ph.D.
   (http://www.cogsci.uci.edu/~noros/)."

THIS SOFTWARE IS PROVIDED ``AS IS'' AND WITHOUT ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIED
WARRANTIES OF MERCHANTIBILITY AND FITNESS FOR A PARTICULAR PURPOSE.
IN NO EVENT SHALL THE UNIVERSITY OR THE PROGRAM CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*******************************************************************************************************/
package android.ioio.car;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.ioio.car.drivers.DriverManager;
import android.ioio.car.threads.ThreadManager;
import android.ioio.car.threads.UtilsThread;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.zeemote.zc.event.ButtonEvent;
import com.zeemote.zc.ui.HiddenState;
import com.zeemote.zc.ui.IProcessingDialogStateListener;
import com.zeemote.zc.ui.MessageDialogState;
import com.zeemote.zc.ui.ProcessingDialogState;
import com.zeemote.zc.ui.State;
import com.zeemote.zc.ui.StateManager;
import com.zeemote.zc.ui.UserChoiceState;

import constants.Conts;

/**
 * This is the main entry point of the program. It handles the connection to the Zeemotes,
 * as well as starting all the other threads.
 * @author Alex Flynn
 *
 */
public class MainActivity extends Activity implements SurfaceHolder.Callback {   	
	private ThreadManager threadManager;
	private DriverManager driverManager;
	private UtilsThread utilsThread;
	private ToggleButton togglebutton,controlToggle;
	private EditText ip_text;
    private SurfaceView view;
	private SurfaceHolder holder;
	private AlertDialog dialog;
	private Button connectZeemotes;
	private boolean override = false;
	private boolean gotControllers = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		view = (SurfaceView)findViewById(R.id.surfaceView1);
		view.getHolder().addCallback(this);
		view.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		ip_text = (EditText) findViewById(R.id.IP_edit_txt);
		togglebutton = (ToggleButton) findViewById(R.id.CameraButton);	
		togglebutton.setOnCheckedChangeListener(new toggleListener());
		
		connectZeemotes = (Button)findViewById(R.id.main_button_connectzeemote);
		connectZeemotes.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//0 recurses, and finnshes again by calling itself with 1, then ends
				reconnectWithDialog(0);
			}
		});
		
		controlToggle = (ToggleButton)findViewById(R.id.main_toggle_zeemote);
		controlToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				override = isChecked;
				if(isChecked){
					if(gotControllers){
						final MyZSpyApplication app = (MyZSpyApplication)getApplicationContext();
						app.addMyListener(driverManager.getZeemoteDriver());
						Toast.makeText(MainActivity.this, "Started Zeemote driver.", Toast.LENGTH_SHORT).show();
					}else{
						Toast.makeText(MainActivity.this, "Connect controllers first.", Toast.LENGTH_SHORT).show();
					}
				}
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onStop(){
		super.onStop();
		if(threadManager != null){
			threadManager.stopAll();
			driverManager.stopAll();
		}
		this.finish();
	}
	
	private class toggleListener implements OnCheckedChangeListener{
		@Override
		public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
			if(isChecked){
				if(threadManager == null){
					threadManager = new ThreadManager(MainActivity.this, ip_text.getText().toString());
		        	driverManager = new DriverManager(threadManager);
		        	utilsThread = new UtilsThread(threadManager,driverManager);
				}else{
					threadManager.restartAll();
					utilsThread.restart();
				}
	            Toast.makeText(MainActivity.this, "Start streaming", Toast.LENGTH_SHORT).show();
			}else{
	        	threadManager.stopAll();
	        	driverManager.stopAll();
	            Toast.makeText(MainActivity.this, "Stop streaming", Toast.LENGTH_SHORT).show();
			}
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		this.holder = holder;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		this.holder = holder;
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {}
	
	/**
	 * Return the surface holder for the camera to draw to
	 * @return - The surface holder
	 */
	public SurfaceHolder getSurfaceHolder(){
		return holder;
	}
	
	/**
	 * Start the connection process for two zeemotes.
	 * @param cont
	 */
	public void reconnectWithDialog(final int cont){
		new AsyncTask<Void, Void, Void>(){
			State currentState;
			final MyZSpyApplication app = (MyZSpyApplication)getApplicationContext();
			StateManager stateManager = StateManager.getStateManager(app.getController(cont));
			
			@Override
			protected void onPreExecute(){
				currentState = stateManager.getCurrentState();
				currentState = ((HiddenState)currentState).showMenu();
			}

			@Override
			protected Void doInBackground(Void... params) {
				Looper.prepare();
				processState(currentState, cont);
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				Log.e("ASyncTask","connection thread finished");
				super.onPostExecute(result);
			}
			
		}.execute();
	}
	
	/**
	 * Recursively process the state the Zeemote process is in.
	 * @param s - The state to process
	 * @param cont - Controller to connect
	 */
	public void processState(final State s, final int cont){
		if(s != null && s.getStateType() != State.HIDDEN_STATE){
			switch(s.getStateType()){
			case State.HIDDEN_STATE:
				Log.e("process_state","hidden");
				State menuState = ((HiddenState)s).showMenu();
				processState(menuState, cont);
				break;
			case State.MESSAGE_DIALOG_STATE:
				Log.e("processing_state","message");
				switch(((MessageDialogState) s).getDialogType()){
					case MessageDialogState.INFO_TYPE:
						Log.e("processing_state_messagetype","info "+((MessageDialogState)s).getMessage());
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Toast.makeText(MainActivity.this, ((MessageDialogState)s).getMessage(), (int) MessageDialogState.DEFAULT_TIMEOUT).show();
							}
						});
						State newState = ((MessageDialogState)s).dismiss();
						processState(newState, cont);
						break;
					case MessageDialogState.WARNING_TYPE:
						Log.e("processing_state_messagetype","warn "+((MessageDialogState)s).getMessage());
						final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
						builder.setTitle("Warning!");
						builder.setMessage(((MessageDialogState)s).getMessage());
						builder.setPositiveButton("Retry", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								State newState = ((MessageDialogState)s).dismiss();
								//State menuState = ((HiddenState)newState).showMenu();
								processState(newState, cont);
							}
						});
						builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								((MessageDialogState)s).dismiss();
							}
						});
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								dialog = builder.show();	
							}
						});
						break;
				}
				break;
			case State.PROCESSING_DIALOG_STATE:
				Log.e("process_state","processing");
				final ProgressDialog progress = new ProgressDialog(MainActivity.this);
				progress.setTitle("Processing...");
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						progress.show();
					}
				});
				//progress.show();
				((ProcessingDialogState)s).setProcessingDialogStateListener(new IProcessingDialogStateListener() {
					@Override
					public void processingDone(ProcessingDialogState arg0) {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								progress.dismiss();
							}
						});
						Log.e("process_state_processdone","done");
						State newState = ((ProcessingDialogState)s).next();
						processState(newState, cont);
					}
					
					@Override
					public void messageUpdated(ProcessingDialogState arg0, final String arg1) {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								progress.setMessage(arg1);
								Log.e("process_state_messageUpdated",arg1);
							}
						});
					}
				});
				break;
			case State.USER_CHOICE_STATE:
				Log.e("process_state","choice");
				final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				//final AlertDialog dialog = null;
				builder.setTitle(((UserChoiceState)s).getTitle());
				ListView lv = new ListView(MainActivity.this);
				ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1,new LinkedList<String>());
				lv.setAdapter(adapter);
				lv.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> arg0, View arg1,
							int arg2, long arg3) {
						State newState = ((UserChoiceState)s).select(arg2);
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								dialog.dismiss();	
							}
						});
						processState(newState, cont);
					}	
				});
				for(String str:((UserChoiceState)s).getChoiceList()){
					adapter.add(str);
				}
				adapter.notifyDataSetChanged();
				builder.setView(lv);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						dialog = builder.show();	
					}
				});
				break;
			}
		}else{
			Log.e("process_state","recieved null state!");
			if(cont == 0){
				reconnectWithDialog(cont + 1);
			}
			if(cont == 1){
				gotControllers = true;
			}
		}
	}
}

