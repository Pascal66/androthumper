/*******************************************************************************************************
Copyright (c) 2011

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
   software must display the following acknowledgement:
   "This product includes software developed at the University of
   Aberystwyth, by Alex Flynn.
   (http://www.alexflynn2391.mx.x10)."

4. The name of the University may not be used to endorse or promote
   products derived from this software without specific prior written
   permission.

5. Redistributions of any form whatsoever must retain the following
   Acknowledgement:
   "This product includes software developed at the University of
   Aberystwyth, by Alex Flynn.
   (http://www.alexflynn2391.mx.x10)."

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
package android.ioio.car.threads;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.IOIOFactory;
import ioio.lib.api.Uart;
import ioio.lib.api.Uart.Parity;
import ioio.lib.api.Uart.StopBits;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.api.exception.IncompatibilityException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import android.ioio.car.hardware.GpsModule;
import android.ioio.car.listeners.MyCompassListener;
import android.ioio.car.listeners.MyGPSListener;
import android.util.Log;
import constants.Conts;

/**
 * This class handles the communication to the ioio board. It contains project-specific implementations
 * of methods that handle the interaction with the robot hardware. Inspiration and the
 * core implementation of this class was provided by University of California, Irvine by Nicolas Oros, Ph.D.
 * (http://www.cogsci.uci.edu/~noros/).
 * @author Alex Flynn
 *
 */
public class IOIO_Thread{
	/** The class that does the actual controlling and manipulation of the ioio. */
	private IOIOThread ioioThread;
	/**Input data that the ioioThread uses. */
	private byte[] input = new byte[Conts.PacketSize.MOVE_PACKET_SIZE];
	/**New input data that is provided by outside sources. */
	private byte[] newInput = new byte[Conts.PacketSize.MOVE_PACKET_SIZE];
	/**The manager for this thread. */
	private ThreadManager manager;
	/**A list of {@link MyCompassListener}'s. */
	private List<MyCompassListener> compassListeners;
	/**A list of {@link MyGPSListener}'s. */
	private List<MyGPSListener> gpsListeners;
	/**The current heading reported by the compass. */
	private float heading;

	/**Create a new IOIO managing class, and start it. */
	IOIO_Thread(ThreadManager manager){
		this.manager = manager;
		compassListeners = new LinkedList<MyCompassListener>();
		start();
	}

	/**Abort the connection to the ioio and remove any listeners. */
	public void abort(){
		ioioThread.abort();
		compassListeners.clear();
	}

	/**Start the connection and running of the ioio. */
	public void start(){
		ioioThread = new IOIOThread();
		ioioThread.start();
	}

	/**
	 * Proved a way to inject data, I.E from zeemote.
	 * @param input - The byte[] of data to receive.
	 */
	public void override(byte[] input){
		if(input.length != Conts.PacketSize.MOVE_PACKET_SIZE){
			Log.e("IOIO","Wrong input, got: "+Conts.Tools.getStringFromByteArray(input));
		}else{
			this.newInput = input;
		}
	}

	/**Add a compass listener to recieve the new headings from the ioio. */
	public void addCompassListener(MyCompassListener listener){
		compassListeners.add(listener);
	}
	public void addGpsListener(MyGPSListener listener){
		gpsListeners.add(listener);
	}

	/**
	 * IOIO Thread. This contains the declarations and implementations of methods specific to the communication
	 * and controlling of the ioio board. The implementation that follows is specific to a required set-up. As such,
	 * you must either duplicate my project set-up, or edit this to match your own.
	 * @author Alex Flynn
	 *
	 */
	private class IOIOThread implements Runnable{
		/**The transmit pin of the compass UART module */
		private static final int COMPASS_PIN_TX = 10;
		/**The receive pin of the compass UART module. */
		private static final int COMPASS_PIN_RX = 9;
		/**The communications baud rate of the compass UART module. */
		private static final int COMPASS_BAUD = 9600;
		/**The delay between reading compass headings to give the compass time to compensate tilt. */
		private static final int COMPASS_READ_TIME = 200;
		
		/**Combination of bytes that tell WTC to write the battery level to its output stream. */
		private final byte[] READ_BATTERY = new byte[]{'B','L'};
		/**Combination of bytes that tell the WTC to start charging. */
		private final byte[] START_CHARGE = new byte[]{'G','C'};
		/**Combination of bytes that tell the motor driver how to control its motors. */
		private byte[] MOTOR_DATA = new byte[6];
		/**Byte of {@link #MOTOR_DATA} to set the left speed. */
		private static final int MOTOR_LEFT_SPEED=3;
		/**Byte of {@link #MOTOR_DATA} to set the right speed. */
		private static final int MOTOR_RIGHT_SPEED=5;
		/**Byte of {@link #MOTOR_DATA} to set the left mode. */
		private static final int MOTOR_LEFT_MODE=2;
		/**Byte of {@link #MOTOR_DATA} to set the right mode. */
		private static final int MOTOR_RIGHT_MODE = 4;
		
		private IOIO ioio;
		private Thread thread;
		private OutputStream motorOs,compassOs;
		private InputStream motorIs,compassIs;
		private DigitalOutput testLED;
		private Uart driver;
		private Uart compass;
		private long compassLastReadTime = System.currentTimeMillis();
		private double batteryLevel = 0;
		private GpsModule gpsModule;

		private boolean connected = false,stopRequest = false,stop = false,driverChanged = false, isCharging = false;

		/*
		 *  Inspiration and the structure of this connect/setup loop was provided by 
		 *  University of California, Irvine by Nicolas Oros, Ph.D.
		 * (http://www.cogsci.uci.edu/~noros/).
		 */
		@Override
		public void run() {
			while(!stop){
				ioio = IOIOFactory.create();
				try {
					ioio.waitForConnect();
					setup();
					while(!stop){
						loop();
						Thread.sleep(10);
					}
				} catch (ConnectionLostException e) {
					if(connected){
						manager.getUtilitiesThread().sendMessage("Disconnected from IOIO!");
						manager.getUtilitiesThread().sendCommand(Conts.UtilsCodes.IOIO.LOST_IOIO_CONNECTION);
						connected = false;
					}
				} catch (IncompatibilityException e) {
					if(connected){
						connected = false;
						manager.getUtilitiesThread().sendMessage("Something is incompatible.");
						manager.getUtilitiesThread().sendCommand(Conts.UtilsCodes.IOIO.LOST_IOIO_CONNECTION);
					}
				} catch (InterruptedException e) {
//					if(connected){
//						connected = false;
//						manager.getUtilitiesThread().sendCommand(Conts.UtilsCodes.IOIO.LOST_IOIO_CONNECTION);
//					}
				}
			}
		}

		/**
		 * Construct the input/output pins
		 */
		private void setup(){
			try {
				testLED = ioio.openDigitalOutput(IOIO.LED_PIN);
				driver = ioio.openUart(7, 6, 9600, Parity.NONE, StopBits.ONE);
				compass = ioio.openUart(COMPASS_PIN_RX, COMPASS_PIN_TX, COMPASS_BAUD, Parity.NONE, StopBits.TWO);
				compassOs = compass.getOutputStream();
				compassIs = compass.getInputStream();
				motorOs = driver.getOutputStream();
				motorIs = driver.getInputStream();

				//Commands to send to the motor driver. Documented in WTC source.
				MOTOR_DATA[0]='H';MOTOR_DATA[1]='B';
				MOTOR_DATA[MOTOR_LEFT_SPEED] = 0;MOTOR_DATA[MOTOR_LEFT_MODE] = 0;MOTOR_DATA[MOTOR_RIGHT_SPEED] = 0; MOTOR_DATA[MOTOR_RIGHT_MODE] = 0;
				
				gpsModule = new GpsModule(manager,ioio);
				gpsModule.startListening();
			} catch (ConnectionLostException e) {
				e.printStackTrace();
			}
		}

		/**
		 * This is the main ioio loop, which is ran every tick.
		 */
		private void loop() throws ConnectionLostException{
			if(!connected){
				manager.getUtilitiesThread().sendCommand(Conts.UtilsCodes.IOIO.GOT_IOIO_CONNECTION);
			}
			connected = true;
			input = Arrays.copyOf(newInput, Conts.PacketSize.MOVE_PACKET_SIZE);
			//Turn on the test LED. This is used to signify right/left controllers (it only lights
			// on left) and that the ioio is still listening.
			if(input[Conts.Controller.Buttons.BUTTON_A] == 0){
				testLED.write(true);
			}else{
				testLED.write(false);
				getBatteryLevel();
			}

			getCompassData();
			gpsModule.tick();

			//Check for any messages from WTC
			try {
				if(motorIs.available()>0){
					byte[] message = new byte[2];
					motorIs.read(message);
					switch(message[0]){
					case -1:
						switch(message[1]){
						case -1:
							//-1,-1
							//Uh oh, WTC says the battery is getting low.
							manager.getUtilitiesThread().sendMessage("WTC reports low volts!");
							//TODO toggle switch to allow switch to auto-charge.
							sendMessageToDriver(START_CHARGE);
							isCharging = true;
							break;
						}
						break;
					}
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			if(!isCharging && !stopRequest){
				if(MOTOR_DATA[MOTOR_LEFT_SPEED] != input[Conts.Controller.Channel.LEFT_CHANNEL]){
					MOTOR_DATA[MOTOR_LEFT_SPEED] = input[Conts.Controller.Channel.LEFT_CHANNEL];
					driverChanged = true;
				}
				if(MOTOR_DATA[MOTOR_LEFT_MODE] != input[Conts.Controller.Channel.LEFT_MODE]){
					MOTOR_DATA[MOTOR_LEFT_MODE] = input[Conts.Controller.Channel.LEFT_MODE];
					driverChanged = true;
				}
				if(MOTOR_DATA[MOTOR_RIGHT_SPEED] != input[Conts.Controller.Channel.RIGHT_CHANNEL]){
					MOTOR_DATA[MOTOR_RIGHT_SPEED] = input[Conts.Controller.Channel.RIGHT_CHANNEL];
					driverChanged = true;
				}
				if(MOTOR_DATA[MOTOR_RIGHT_MODE] != input[Conts.Controller.Channel.RIGHT_MODE]){
					MOTOR_DATA[MOTOR_RIGHT_MODE] = input[Conts.Controller.Channel.RIGHT_MODE];
					driverChanged = true;
				}else if(stopRequest){
					MOTOR_DATA[MOTOR_LEFT_SPEED] = 0;
					MOTOR_DATA[MOTOR_RIGHT_SPEED] = 0;
					driverChanged = true;
					stopRequest = false;
					stop = true;
				}

				if(driverChanged){
					driverChanged = false;
					//Log.e("IOIO","Write motor buff: "+byteArrayToString(MOTOR_DATA));
					sendMessageToDriver(MOTOR_DATA);
				}	
			}else{
				//We are charging. Check if we are still charging. If we are, sleep. Else, wake up
				//toggle on UI for asleep, uncheck to call interrupt. 
				if(getBatteryLevel() > 8){
					//Done!
					isCharging = false;
					manager.getUtilitiesThread().sendMessage("Done charging!");
				}else{
					try {
						Thread.sleep(60000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}

		/**Read the data from the compass module. */
		public void getCompassData(){
			if((System.currentTimeMillis() - compassLastReadTime) > COMPASS_READ_TIME){
				try{
					compassOs.write(0x12);
					int realHeading = compassIs.read();
					heading = (realHeading / 255f) * 360;

					compassLastReadTime = System.currentTimeMillis();
					for(MyCompassListener listener:compassListeners){
						listener.gotCompassHeading(heading);
					}
				}catch(IOException e){}
			}
		}

		/**Send a byte[] message to the motor driver via the output stream in UART module. */
		private void sendMessageToDriver(byte[] data){
			try {
				motorOs.write(data);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		/**Get the level of batteries from the motor driver. */
		private double getBatteryLevel(){
			try {
				motorOs.write(READ_BATTERY);
				if(motorIs.available()>0){
					int highByte = motorIs.read();
					int lowByte = motorIs.read();
					int thumperBattery = highByte << 8 | lowByte;
					double realBatt = thumperBattery/68.3f;
					batteryLevel = realBatt;
					manager.getUtilitiesThread().sendMessage("Battery level: "+batteryLevel);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return batteryLevel;
		}

		/**Ask the ioio to stop. Issue stop request, stopping all connected devices. */
		private void abort(){
			stopRequest = true;
		}
		/**Connect to all devices and start the main loop. */
		private void start(){
			thread = new Thread(this);
			thread.start();
		}
	}
}