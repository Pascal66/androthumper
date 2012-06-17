package android.ioio.car.hardware;

import android.ioio.car.threads.IOIO_Thread;
import android.util.Log;
import constants.Conts;
import ioio.lib.api.IOIO;
import ioio.lib.api.TwiMaster;
import ioio.lib.api.exception.ConnectionLostException;

public class WTC {

	/**Combination of bytes that tell the motor driver how to control its motors. */
	private byte[] MOTOR_DATA = new byte[5];
	/**Byte of {@link #MOTOR_DATA} to set the left speed. */
	private static final int MOTOR_LEFT_SPEED=2;
	/**Byte of {@link #MOTOR_DATA} to set the right speed. */
	private static final int MOTOR_RIGHT_SPEED=4;
	/**Byte of {@link #MOTOR_DATA} to set the left mode. */
	private static final int MOTOR_LEFT_MODE=1;
	/**Byte of {@link #MOTOR_DATA} to set the right mode. */
	private static final int MOTOR_RIGHT_MODE = 3;
	
	/**How long to wait before sending another load of data down the bus. */
	private static final int TICK_TIME = 50;
	/**The address of the WTC on the bus. */
	private static final int WTC_BUS_ADDR = 1;
	
	/**The instance of the bus to be worked on. */
	private TwiMaster busMaster;
	/**The IOIO thread creating this. */
	private IOIO_Thread ioio;
	/**The last time a chunk of data was sent. */
	private long lastSendtime;
	/**boolean flag indicating whether wtc has a response from a command. */
	private boolean hasResponse = false;
	/**flag indicating whether the WTC has received a new command yet to be sent. */
	private boolean newCommand = false;
	/**Byte[] command for the WTC. */
	private byte[] command = null;
	/**Byte[] response for a command. */
	private byte[] expectedResponse = null;
	/**Byte[] response for the motor command. This should always be an empty array. */
	private byte[] motorResponse = new byte[0];
	private boolean debug = false;
	
	public WTC(TwiMaster busMaster){
		MOTOR_DATA[0]=2;
		MOTOR_DATA[MOTOR_LEFT_SPEED] = 0;MOTOR_DATA[MOTOR_LEFT_MODE] = 0;MOTOR_DATA[MOTOR_RIGHT_SPEED] = 0; MOTOR_DATA[MOTOR_RIGHT_MODE] = 0;
	
		this.busMaster = busMaster;
		lastSendtime = System.currentTimeMillis();
	}
	
	/**
	 * Provide motor input for the WTC. This will not be sent now, but will be sent via tick if appropriate.
	 * @param input - byte[] - The motor data to send to WTC.
	 */
	public void provideInput(byte[] input){
		MOTOR_DATA[MOTOR_LEFT_SPEED] = 	input[Conts.Controller.Channel.LEFT_CHANNEL];
		MOTOR_DATA[MOTOR_LEFT_MODE] = 	input[Conts.Controller.Channel.LEFT_MODE];
		MOTOR_DATA[MOTOR_RIGHT_SPEED] = input[Conts.Controller.Channel.RIGHT_CHANNEL];
		MOTOR_DATA[MOTOR_RIGHT_MODE] = 	input[Conts.Controller.Channel.RIGHT_MODE];
	}
	
	/**
	 * Ping the WTC, and send the data if appropriate.
	 */
	public boolean tick(){
		if(System.currentTimeMillis() - lastSendtime > TICK_TIME){
			sendData();
			lastSendtime = System.currentTimeMillis();
			return true;
		}
		return false;
	}
	
	public void forceTick(){
		sendData();
	}
	
	/**
	 * Send a command to WTC. Example asking for battery level, or current level, or w.e
	 * @param command - byte[] - the commnd to be sent to WTC.
	 * @param expectedResponse - byte[] - any expected response from that command.
	 * @return - boolean - true if the command was accepted. False if otherwise, and you should try again in a bit.
	 */
	public boolean sendCommand(byte[] command, byte[] expectedResponse){
		if(newCommand){
			//There is already a command waiting to be sent
			return false;
		}else{
			//Good to go
			newCommand = true;
			this.command = command;
			this.expectedResponse = expectedResponse;
			return true;
		}
	}
	/**
	 * Has a command been sent that had a response? Has that response arrived?
	 * @return - boolean - True if there is a response from the last command, false otherwise
	 */
	public boolean hasResponse(){
		return hasResponse;
	}
	
	/**
	 * Return the last response that was created from the last command sent.
	 * @return - byte[] - The response data from the WTC.
	 */
	public byte[] getResponse(){
		if(hasResponse){
			hasResponse = false;
			return expectedResponse;
		}
		return null;
	}
	
	/**Send the data to the WTC. */
	private void sendData(){
		try {
			if(debug){
				Log.e("Sending: ",""+Conts.Tools.getStringFromByteArray(MOTOR_DATA));
			}
			busMaster.writeRead(WTC_BUS_ADDR, false, MOTOR_DATA, MOTOR_DATA.length, motorResponse, 0);
			
			if(newCommand){
				busMaster.writeRead(WTC_BUS_ADDR, false, command, command.length, expectedResponse, expectedResponse.length);
				newCommand = false;
				hasResponse = true;
			}
		} catch (ConnectionLostException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
