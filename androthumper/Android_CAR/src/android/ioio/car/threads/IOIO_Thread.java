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
package android.ioio.car.threads;

import ioio.lib.api.DigitalInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.IOIOFactory;
import ioio.lib.api.Uart;
import ioio.lib.api.Uart.Parity;
import ioio.lib.api.Uart.StopBits;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.api.exception.IncompatibilityException;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import android.ioio.car.listeners.MyCompassListener;
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
public class IOIO_Thread implements Runnable {
	/** Subclasses should use this field for controlling the IOIO. */
	protected IOIO ioio;
	int size_p;

	InetAddress serverAddr;
	DatagramSocket socket;	
	//String ip_address;

	//MainActivity main_app;
	boolean START = true;
	int a_nb=0;

	private IOIOThread ioioThread;
	private Thread listenerThread;
	//private UtilsThread utils;
	private boolean listening = true;
	private byte[] input = new byte[Conts.PacketSize.MOVE_PACKET_SIZE];
	private final String TAG = "IOIO";

	private ThreadManager manager;
	private List<MyCompassListener> compassListeners;
	private float heading;
	
	IOIO_Thread(ThreadManager manager){
		this.manager = manager;
		compassListeners = new LinkedList<MyCompassListener>();
		listenerThread = new Thread(this);
		ioioThread = new IOIOThread();
		//listenerThread.start();
		ioioThread.start();

//		try {
//			socket = new DatagramSocket();
//			DatagramPacket pingPacket = new DatagramPacket(new byte[]{1}, 1, InetAddress.getByName(manager.getIpAddress()), Conts.Ports.MOVE_INCOMMING_PORT);
//			socket.send(pingPacket);
//			Log.e(TAG,"Sent ping.");
//		} catch (SocketException e) {
//			e.printStackTrace();
//		} catch (UnknownHostException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

	public void abort(){
		listening = false;
		ioioThread.abort();
	}

	public void start(){
		listenerThread = new Thread(this);
		ioioThread = new IOIOThread();
		//listenerThread.start();
		ioioThread.start();
	}

	@Override
	public void run(){
		DatagramPacket packet = new DatagramPacket(input, Conts.PacketSize.MOVE_PACKET_SIZE);
		while(listening){
			try {
				socket.receive(packet);
				input = packet.getData();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Proved a way to inject data, I.E from zeemote.
	 * @param input - The byte[] of data to receive.
	 */
	public void override(byte[] input){
		this.input = input;
	}
	
	public void addCompassListener(MyCompassListener listener){
		compassListeners.add(listener);
	}

	/**
	 * IOIO Thread. This contains the declarations and implementations of methods specific to the communication
	 * and controlling of the ioio board. The implementation that follows is specific to a required set-up. As such,
	 * you must either duplication my project set-up, or edit this to match your own.
	 * @author Alex Flynn
	 *
	 */
	private class IOIOThread implements Runnable{
		private Thread thread;
		private boolean stop = false,setBaud = false,resetted = false,m1HasMoved = false, m0hasMoved = false;
		private OutputStream os,compassOs;
		private InputStream is,compassIs;
		private int m0speed,m1speed;
		private DigitalOutput testLED;
		private Uart driver;
		private Uart compass;
		private long compassLastReadTime = System.currentTimeMillis();
		private DigitalInput errorInput;
		private DigitalOutput reset;
		private double batteryLevel = 0;
		
		private byte[] READ_BATTERY = new byte[]{'B','L'};
		private int ACCEL_VAL = 35;
		private int COMPASS_READ_TIME = 200;
		
		private boolean connected = false,stopRequest = false;

		@Override
		public void run() {
			while(!stop){
				ioio = IOIOFactory.create();
				try {
					Log.e("IOIO","waiting");
					ioio.waitForConnect();
					Log.e("IOIO","done");
					setup();
					while(!stop){
						loop();
						Thread.sleep(10);
					}
				} catch (ConnectionLostException e) {
					//e.printStackTrace();
					if(connected){
						manager.getUtilitiesThread().sendCommand(Conts.UtilsCodes.IOIO.LOST_IOIO_CONNECTION);
						connected = false;
					}
				} catch (IncompatibilityException e) {
					//e.printStackTrace();
					if(connected){
						connected = false;
						manager.getUtilitiesThread().sendCommand(Conts.UtilsCodes.IOIO.LOST_IOIO_CONNECTION);
					}
				} catch (InterruptedException e) {
					//e.printStackTrace();
					if(connected){
						connected = false;
						manager.getUtilitiesThread().sendCommand(Conts.UtilsCodes.IOIO.LOST_IOIO_CONNECTION);
					}
				}
			}
		}

		/**
		 * Construct the input/output pins
		 */
		private void setup(){
			try {
				testLED = ioio.openDigitalOutput(IOIO.LED_PIN);
				driver = ioio.openUart(5, 4, 9600, Parity.NONE, StopBits.ONE);
				errorInput = ioio.openDigitalInput(2, DigitalInput.Spec.Mode.PULL_DOWN);
				reset = ioio.openDigitalOutput(3, true);
				compass = ioio.openUart(9, 10, 9600, Parity.NONE, StopBits.TWO);
				compassOs = compass.getOutputStream();
				compassIs = compass.getInputStream();
				os = driver.getOutputStream();
				is = driver.getInputStream();
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
			//Simply turn on the test LED. This is used to signify right/left controllers (it only lights
			// on left) and that the ioio is still listening.
			if(input[Conts.Controller.Buttons.BUTTON_A] == 0){
				testLED.write(true);
			}else{
				testLED.write(false);
				
				try {
					os.write(READ_BATTERY);
					if(is.available()>0){
						int highByte = is.read();
						int lowByte = is.read();
						int thumperBattery = highByte << 8 | lowByte;
						double realBatt = thumperBattery/68.3f;
						if(batteryLevel != realBatt){
							batteryLevel = realBatt;
							manager.getUtilitiesThread().sendMessage("Battery level: "+batteryLevel);
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			getCompassData();
			
			//RESET
//			if(input[Conts.Controller.Buttons.BUTTON_X] == 1){
//				reset.write(false);
//				setBaud = false;
//				resetted = true;
//			}else{
//				reset.write(true);
//				if(resetted){
//					try {
//						resetted = false;
//						Thread.sleep(200);
//					} catch (InterruptedException e1) {
//						e1.printStackTrace();
//					}
//				}
//			}

//			if(input[Conts.Controller.Buttons.BUTTON_LS] == 1){
//				//read values back from my motor driver
//				try {
//					os.write(new byte[]{(byte) 0x83,2});
//					Thread.sleep(100);
//					is = driver.getInputStream();
//					int onError = is.read();
//					os.write(new byte[]{(byte) 0x83,3});
//					Thread.sleep(100);
//					is = driver.getInputStream();
//					int timeout = is.read();
//					os.write(new byte[]{(byte) 0x83, 4});
//					Thread.sleep(100);
//					int m0Accel = is.read();
//					os.write(new byte[]{(byte) 0x83, 5});
//					Thread.sleep(100);
//					int m1Accel = is.read();
//					os.write(new byte[]{(byte) 0x83, 6});
//					Thread.sleep(100);
//					int m0BrakeTime = is.read();
//					os.write(new byte[]{(byte) 0x83, 7});
//					Thread.sleep(100);
//					int m1BrakeTime = is.read();
//					os.write(new byte[]{(byte) 0x83, 8});
//					Thread.sleep(100);
//					int m0CurrentLimit = is.read();
//					os.write(new byte[]{(byte) 0x83, 9});
//					Thread.sleep(100);
//					int m1CurrentLimit = is.read();
//					os.write(new byte[]{(byte) 0x83, 10});
//					Thread.sleep(100);
//					int m0CurrentLimitResponse = is.read();
//					os.write(new byte[]{(byte) 0x83, 11});
//					Thread.sleep(100);
//					int m1CurrentLimitResponse = is.read();
//					os.write(new byte[]{(byte) 0x83, 1});
//					Thread.sleep(100);
//					int pwm = is.read();
//					
//					Log.e("INFO: Timeout: ",""+timeout);
//					Log.e("INFO: PWM: ",""+pwm);
//					Log.e("INFO: Shut down on error: ",""+onError);
//					Log.e("INFO: M0 acceleration: ",""+m0Accel);
//					Log.e("INFO: M1 acceleration: ",""+m1Accel);
//					Log.e("INFO: M0 brake time: ",""+m0BrakeTime);
//					Log.e("INFO: M1 brake time: ",""+m1BrakeTime);
//					Log.e("INFO: M0 current limit: ",""+m0CurrentLimit);
//					Log.e("INFO: M1 current limit: ",""+m1CurrentLimit);
//					Log.e("INFO: M0 current limit response: ",""+m0CurrentLimitResponse);
//					Log.e("INFO: M1 current limit response: ",""+m1CurrentLimitResponse);
//				} catch (IOException e) {
//					e.printStackTrace();
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//			}
			
//			if(input[Conts.Controller.Buttons.BUTTON_RS] == 1){
//				try {
//					//Write parameters to my motor driver
//					os.write(new byte[]{(byte) 0x84, 4, 0, 0x55, 0x2A});
//					Thread.sleep(100);
//					int m0AccelResult = is.read();
//					os.write(new byte[]{(byte) 0x84, 5, 0, 0x55, 0x2A});
//					Thread.sleep(100);
//					int m1AccelResult = is.read();
//					os.write(new byte[]{(byte) 0x84, 6, 50, 0x55, 0x2A});
//					Thread.sleep(100);
//					int m0BrakeResult = is.read();
//					os.write(new byte[]{(byte) 0x84, 7, 50, 0x55, 0x2A});
//					Thread.sleep(100);
//					int m1BrakeResult = is.read();
//					os.write(new byte[]{(byte) 0x84, 8, 44, 0x55, 0x2A});
//					Thread.sleep(100);
//					int m0CurrentLimitResult = is.read();
//					os.write(new byte[]{(byte) 0x84, 9, 44, 0x55, 0x2A});
//					Thread.sleep(100);
//					int m1CurrentLimitResult = is.read();
//					
//					Log.e("INFO: M0 accel result: ",""+m0AccelResult);
//					Log.e("INFO: M1 accel result: ",""+m1AccelResult);
//					Log.e("INFO: M0 brake result: ",""+m0BrakeResult);
//					Log.e("INFO: M1 brake result: ",""+m1BrakeResult);
//					Log.e("INFO: M0 current limit: ",""+m0CurrentLimitResult);
//					Log.e("INFO: M1 current limit: ",""+m1CurrentLimitResult);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
			

//			try {
//				//The driver's error pin is high. Read the error, and send it to the server.
//				if(errorInput.read()){
//					os.write(0x82);
//					ByteArrayOutputStream baos = new ByteArrayOutputStream(Conts.PacketSize.UTILS_CONTROL_PACKET_SIZE);
//					DataOutputStream dos = new DataOutputStream(baos);
//					dos.write(Conts.UTILS_MESSAGE_TYPE_DRIVER_ERROR);
//					Byte out;
//					//if(is.available() >= 1){
//						out = (byte) is.read();
//						dos.write(out);
//						Log.e("IOIO ERROR: ","BYTE: "+out);
//						dos.close();
//						manager.getUtilitiesThread().sendData(Arrays.copyOf(baos.toByteArray(), Conts.PacketSize.UTILS_CONTROL_PACKET_SIZE));
//					//}
//					reset.write(false);
//					resetted = true;
//					setBaud = false;
//					m1HasMoved = false;
//					m0hasMoved = false;
//					Thread.sleep(1000);
//				}
//			} catch (InterruptedException e1) {
//				e1.printStackTrace();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}

			//Set the baud rate. Must be done before any motor commands.
//			if(input[Conts.Controller.Buttons.BUTTON_B] == 1){
//				if(!setBaud){
//					try {
//						Log.e("IOIO","sent baud");
//						os.write(0xAA);
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//					setBaud = true;
//				}
//			}
//			
//			if(input[Conts.Controller.Buttons.BUTTON_LB] == 1){
//				m1HasMoved = false;
//			}
//			if(input[Conts.Controller.Buttons.BUTTON_RB] == 1){
//				m0hasMoved = false;
//			}
//
//			if(stopRequest){
//				input[Conts.Controller.Channel.LEFT_CHANNEL] = 0;
//				input[Conts.Controller.Channel.RIGHT_CHANNEL] = 0;
//				stop = true;
//			}
//			
			if(input[Conts.Controller.Channel.LEFT_CHANNEL] == 0){
				
			}else if(input[Conts.Controller.Channel.LEFT_CHANNEL] > 0){
				/*
				 * TODO
				 * make byte buffer, set places for left speed, and get mode with conts.chann.left_mode
				 */
			}
			
			if(input[Conts.Controller.Channel.RIGHT_CHANNEL] == 0){
				
			}else if(input[Conts.Controller.Channel.RIGHT_CHANNEL] > 0){
				
			}
			
			
//			if(setBaud){
//				try {
//					//RIGHT STICK
//					if(input[11] < -1){
//						//forward
//						if(-input[11] < m0speed){
//							m0speed = -input[11];
//						}else if(-input[11] > m0speed){
//							m0speed+=ACCEL_VAL;
//						}
//						if(m0speed > -input[11]){
//							m0speed = -input[11];
//						}
//						os.write(new byte[]{(byte) 0x8E,(byte) m0speed});
//						m0hasMoved = true;
//					}else if(input[11] > 1){
//						//backward
//						if(input[11] < m0speed){
//							m0speed = input[11];
//						}else if(input[11] > m0speed){
//							m0speed+=ACCEL_VAL;
//						}
//						if(m0speed > input[11]){
//							m0speed = input[11];
//						}
//						os.write(new byte[]{(byte) 0x8C,(byte) m0speed});
//						m0hasMoved = true;
//					}else{
//						if(m0hasMoved){
//							os.write(new byte[]{(byte) 0x87,64});
//						}else{
//							os.write(new byte[]{(byte) 0x87, 0});
//						}
//					}
//
//					//LEFT STICK
//					if(input[10] < -1){
//						//forward
//						if(-input[10] < m1speed){
//							m1speed = -input[10];
//						}else if(-input[10] > m1speed){
//							m1speed+=ACCEL_VAL;
//						}
//						if(m1speed > -input[10]){
//							m1speed = -input[10];
//						}
//						os.write(new byte[]{(byte) 0x88,(byte) m1speed});
//						m1HasMoved = true;
//					}else if(input[10] > 1){
//						//backward
//						if(input[10] < m1speed){
//							m1speed = input[10];
//						}else if(input[10] > m1speed){
//							m1speed+=ACCEL_VAL;
//						}
//						if(m1speed > input[10]){
//							m1speed = input[10];
//						}
//						os.write(new byte[]{(byte) 0x8A,(byte) m1speed});
//						m1HasMoved = true;
//					}else{
//						if(m1HasMoved){
//							os.write(new byte[]{(byte) 0x86,64});
//						}else{
//							os.write(new byte[]{(byte) 0x86, 0});
//						}
//					}
//
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
		}
		
		public void getCompassData(){
			if((System.currentTimeMillis() - compassLastReadTime) > COMPASS_READ_TIME){
				try{
//					compassOs.write(0x11);
//					int version = compassIs.read();
					
					compassOs.write(0x12);
					int realHeading = compassIs.read();
					heading = (realHeading / 255f) * 360;
					
//					byte[] utilsData = new byte[Conts.PacketSize.UTILS_CONTROL_PACKET_SIZE];
//					utilsData[0] = Conts.UtilsCodes.DataType.COMPASS_DATA;
//					utilsData[1] = (byte) realHeading;
//					manager.getUtilitiesThread().sendData(utilsData);

					compassLastReadTime = System.currentTimeMillis();
					for(MyCompassListener listener:compassListeners){
						listener.gotCompassHeading(heading);
					}
				}catch(IOException e){
					
				}
			}
		}

		public void abort(){
			stopRequest = true;
		}
		public void start(){
			thread = new Thread(this);
			thread.start();
		}
	}
	
	public String byteArrayToString(byte[] data){
		StringBuilder bul = new StringBuilder();
		
		for(byte b:data){
			bul.append(b);bul.append(",");
		}
		return bul.toString();
	}
}