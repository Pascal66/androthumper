package android.ioio.car.drivers;

import android.util.Log;

import com.zeemote.zc.event.ButtonEvent;

import constants.Conts;

public class ZeemoteDriver implements Driver{
	
	private DriverManager driverManager;
	private boolean running = false;
	private byte[] input = new byte[Conts.PacketSize.MOVE_PACKET_SIZE];
	private boolean debug = false;
	
	public ZeemoteDriver(DriverManager driverManager){
		this.driverManager = driverManager;
	}

	/**Tell the driver one of the buttons on the right controller has been released. */
	public boolean rightButtonUp(ButtonEvent event){
		switch(event.getButtonGameAction()){
		case 5:
			input[Conts.Controller.Buttons.BUTTON_B] = 0;
			break;
		case 6:
			//input[5] = 0;
			break;
		case 7:
			input[Conts.Controller.Buttons.BUTTON_RS] = 0;
			break;
		case 8:
			input[Conts.Controller.Buttons.BUTTON_RB] = 0;
			break;
		}
		return commit();
	}
	/**Tell the driver one of the buttons on the right controller has been pressed. */
	public boolean rightButtonDown(ButtonEvent event){
		switch(event.getButtonGameAction()){
		case 5:
			input[Conts.Controller.Buttons.BUTTON_B] = 1;
			break;
		case 6:
			//input[5] = 1;
			break;
		case 7:
			input[Conts.Controller.Buttons.BUTTON_RS] = 1;
			break;
		case 8:
			input[Conts.Controller.Buttons.BUTTON_RB] = 1;
			break;
		}
		return commit();
	}
	/**Tell the driver one of the buttons on the left controller has been released. */
	public boolean leftButtonUp(ButtonEvent event){
		switch(event.getButtonGameAction()){
		case 5:
			input[Conts.Controller.Buttons.BUTTON_X] = 0;
			break;
		case 6:
			input[Conts.Controller.Buttons.BUTTON_A] = 0;
			break;
		case 7:
			input[Conts.Controller.Buttons.BUTTON_LS] = 0;
			break;
		case 8:
			input[Conts.Controller.Buttons.BUTTON_LB] = 0;
			break;
		}
		return commit();
	}
	/**Tell the driver one of the buttons on the left controller has been pressed. */
	public boolean leftButtonDown(ButtonEvent event){
		switch(event.getButtonGameAction()){
		case 5:
			input[Conts.Controller.Buttons.BUTTON_X] = 1;
			break;
		case 6:
			input[Conts.Controller.Buttons.BUTTON_A] = 1;
			break;
		case 7:
			input[Conts.Controller.Buttons.BUTTON_LS] = 1;
			break;
		case 8:
			input[Conts.Controller.Buttons.BUTTON_LB] = 1;
			break;
		}
		return commit();
	}
	
	/**Tell the driver that the left joystick has moved. */
	public boolean leftJoystick(int x, int y){
		if(y < 0){
			input[Conts.Controller.Channel.LEFT_CHANNEL] = (byte)-y;
			input[Conts.Controller.Channel.LEFT_MODE] = Conts.Controller.Channel.MODE_FORWARDS;
		}else{
			input[Conts.Controller.Channel.LEFT_MODE] = Conts.Controller.Channel.MODE_REVERSE;
		}
		return commit();
	}
	/**Tell the driver the right joystick has moved. */
	public boolean rightJoystick(int x, int y){
		if(y < 0){
			input[Conts.Controller.Channel.RIGHT_CHANNEL] = (byte)-y;
			input[Conts.Controller.Channel.RIGHT_MODE] = Conts.Controller.Channel.MODE_FORWARDS;
		}else{
			input[Conts.Controller.Channel.RIGHT_CHANNEL] = (byte)y;
			input[Conts.Controller.Channel.RIGHT_MODE] = Conts.Controller.Channel.MODE_REVERSE;
		}
		return commit();
	}
	
	/**Commit the input data to the IOIO thread via the driver manager. */
	private boolean commit(){
		if(running){
			if(debug){
				Log.e("Zeemote driver","Sending input: "+Conts.Tools.getStringFromByteArray(input));
			}
			driverManager.getThreadManager().getIOIOThread().override(input);
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public void start() {
		running = true;
	}

	@Override
	public void stop() {
		running = false;
	}

	@Override
	public void restart() {	
		running = true;
	}
}
