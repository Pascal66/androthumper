package android.ioio.car.drivers;

import android.util.Log;

import com.zeemote.zc.event.ButtonEvent;

import constants.Conts;

public class ZeemoteDriver implements Driver{
	
	private DriverManager driverManager;
	private boolean running = false;
	private byte[] input = new byte[12];
	
	public ZeemoteDriver(DriverManager driverManager){
		this.driverManager = driverManager;
	}

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
	
	public boolean leftJoystick(int x, int y){
		input[10] = (byte)-y;
		return commit();
	}
	public boolean rightJoystick(int x, int y){
		input[11] = (byte)-y;
		return commit();
	}
	
	private boolean commit(){
		if(running){
			//TODO FIX FOR NEW DRIVER
			//driverManager.getThreadManager().getIOIOThread().override(input);
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
