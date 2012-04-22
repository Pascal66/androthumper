package android.ioio.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;

import com.zeemote.zc.event.ButtonEvent;
import com.zeemote.zc.ui.HiddenState;
import com.zeemote.zc.ui.IProcessingDialogStateListener;
import com.zeemote.zc.ui.MessageDialogState;
import com.zeemote.zc.ui.ProcessingDialogState;
import com.zeemote.zc.ui.State;
import com.zeemote.zc.ui.StateManager;
import com.zeemote.zc.ui.UserChoiceState;

import constants.Conts;

import threads.MovementThread;
import threads.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import threads.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.text.Spannable;
import android.text.method.MovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VerticalSeekBar;
import android.widget.AdapterView.OnItemClickListener;

public class Main extends Activity {

	private Button controlButton;
	private VerticalSeekBar leftSeekBar;
	private VerticalSeekBar rightSeekBar;
	private static TextView infoText;
	private Utils utils;
	private MovementThread move;
	private ImageView image;
	private boolean[] buttons;
	private float[] triggers,rStick,lStick;
	private AlertDialog dialog;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		image = (ImageView)findViewById(R.id.main_imageview_cam);

		infoText = (TextView)findViewById(R.id.main_label_info);
		infoText.setMovementMethod(new ScrollingMovementMethod());
		infoText.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				infoText.setText("");
				return true;
			}
		});

		controlButton = (Button)findViewById(R.id.main_button_control);
		controlButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				printToLog("test line");
			}
		});

		final MyZSpyApplication app = (MyZSpyApplication)getApplicationContext();
		app.addMyListener(this);

		utils = new Utils(this);
		move = new MovementThread(this);
		new Camera(this);
	}

	public void setImage(final Bitmap bitmap){
		Runnable r = new Runnable() {
			@Override
			public void run() {
				image.setImageBitmap(bitmap);	
			}
		};
		runOnUiThread(r);
	}

	public void printToLog(final String line){
		this.runOnUiThread(new Runnable(){
			@Override
			public void run(){
				infoText.append(line+"\n");
				if(infoText.getLineCount() > 5){
					infoText.scrollTo(0, (infoText.getLineCount()-5)*infoText.getLineHeight());
				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflator = getMenuInflater();
		inflator.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case R.id.main_menu_cam:
			if(utils.camEnabled){
				utils.sendCommand(Conts.UtilsCodes.DISABLE_CAM);
				utils.camEnabled = false;
			}else{
				utils.sendCommand(Conts.UtilsCodes.ENABLE_CAM);
				utils.camEnabled = true;
			}
			break;
		case R.id.main_menu_zeemotes:
			reconnectWithDialog(0);
		}
		return true;
	}

	/**
	 * Process a button down event from a joystick
	 * @param event - The button event
	 * @param cont - The controller the event came from. 0=left, 1=right
	 */
	public void buttonDown(ButtonEvent event){
		switch(event.getButtonGameAction()){
		case 5:
			buttons[Conts.Controller.Buttons.BUTTON_B] = true;
			break;
		case 6:
			//buttons[5] = true;
			break;
		case 7:
			buttons[Conts.Controller.Buttons.BUTTON_RS] = true;
			break;
		case 8:
			buttons[Conts.Controller.Buttons.BUTTON_RB] = true;
			break;
		}

		move.doMove(buttons, lStick, rStick, triggers);
	}

	/**
	 * Process a button up event from a joystick
	 * @param event - The button event
	 * @param cont - The controller the event came from. 0=left, 1=right
	 */
	public void buttonUp(ButtonEvent event) {
		switch(event.getButtonGameAction()){
		case 5:
			buttons[Conts.Controller.Buttons.BUTTON_X] = false;
			break;
		case 6:
			buttons[Conts.Controller.Buttons.BUTTON_A] = false;
			break;
		case 7:
			buttons[Conts.Controller.Buttons.BUTTON_LS] = false;
			break;
		case 8:
			buttons[Conts.Controller.Buttons.BUTTON_LB] = false;
			break;
		}

		move.doMove(buttons, lStick, rStick, triggers);
	}

	/**
	 * Process the movement of the left joystick. At the moment, only concerned
	 * about Y values
	 * @param val - The value of the joystick
	 */
	public void moveJoystick(int valX, int valY){
		rStick[0] = (float)valX / (float)100;
		rStick[1] = (float)valY / (float)100;
		
		move.doMove(buttons, lStick, rStick, triggers);
	}

	/**
	 * Start the connection process for two zeemotes.
	 * @param cont
	 */
	public void reconnectWithDialog(final int cont){
		new AsyncTask<Void, Void, Void>(){
			State currentState;
			final MyZSpyApplication app = (MyZSpyApplication)getApplicationContext();
			StateManager stateManager = StateManager.getStateManager(app.getController());

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
							Toast.makeText(Main.this, ((MessageDialogState)s).getMessage(), (int) MessageDialogState.DEFAULT_TIMEOUT).show();
						}
					});
					State newState = ((MessageDialogState)s).dismiss();
					processState(newState, cont);
					break;
				case MessageDialogState.WARNING_TYPE:
					Log.e("processing_state_messagetype","warn "+((MessageDialogState)s).getMessage());
					final AlertDialog.Builder builder = new AlertDialog.Builder(Main.this);
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
				final ProgressDialog progress = new ProgressDialog(Main.this);
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
				final AlertDialog.Builder builder = new AlertDialog.Builder(Main.this);
				//final AlertDialog dialog = null;
				builder.setTitle(((UserChoiceState)s).getTitle());
				ListView lv = new ListView(Main.this);
				ArrayAdapter<String> adapter = new ArrayAdapter<String>(Main.this, android.R.layout.simple_list_item_1,new LinkedList<String>());
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
//			if(cont == 0){
//				reconnectWithDialog(cont + 1);
//			}
		}
	}
}