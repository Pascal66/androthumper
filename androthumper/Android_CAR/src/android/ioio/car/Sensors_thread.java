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

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import constants.Conts;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/* code based on: 
 * IBMEyes.java
 * sample code for IBM Developerworks Article
 * Author: W. Frank Ableson
 * fableson@msiservices.com 
 */

/**
 * This class provides sensor updates from an acceleromter and orientation sensor, and, if required,
 * sends them to the server
 *
 */
public class Sensors_thread implements SensorEventListener {
	
	/**Logging tag. */
    private static final String tag = "Sensors";
	/**The socket to send the packets from. */
	private DatagramSocket socket;	
	/**A packet to store information to send to the server. */
	private DatagramPacket packet;
	private float x_O, y_O, z_O, x_A, y_A, z_A;
	private short ix_O, iy_O, iz_O, ix_A, iy_A, iz_A;
	private boolean changed = false;
	private UtilsThread utils;
	
	private SensorManager mSensorManager = null;	

    public Sensors_thread(MainActivity app, String ip, UtilsThread utils){
    	utils.registerForSensor(this);
    	this.utils = utils;
    	mSensorManager = (SensorManager)app.getSystemService(Context.SENSOR_SERVICE);   
    	
    	try{
    		socket = new DatagramSocket();
    		packet = new DatagramPacket(new byte[]{1}, 1, InetAddress.getByName(ip), Conts.Ports.SENSOR_INCOMING_PORT);
    	}catch (Exception e){
    		e.printStackTrace();
    	}
	}
    
    /**Stop listening by closing the socket, causing {@link #disableSensors()}
     * @see {@link #disableSensors()}
     */
    public void stop(){
    	socket.close();
    }
    
    /**Send data packet to the server. */
	private void send_data_UDP() {
		try {			
        	ix_O = (short) (x_O);
        	iy_O = (short) (y_O);
        	iz_O = (short) (z_O);
        	ix_A = (short) (x_A);
        	iy_A = (short) (y_A);
        	iz_A = (short) (z_A);        	
			
			byte[] data = new byte[12];
			data[0] = (byte) (ix_O >> 8);
			data[1] = (byte) ix_O;    			
			data[2] = (byte) (iy_O >> 8);
			data[3] = (byte) iy_O;    			
			data[4] = (byte) (iz_O >> 8);
			data[5] = (byte) iz_O;    			
			data[6] = (byte) (ix_A >> 8);
			data[7] = (byte) ix_A;    			
			data[8] = (byte) (iy_A >> 8);
			data[9] = (byte) iy_A;    			
			data[10] = (byte) (iz_A >> 8);
			data[11] = (byte) iz_A;
			
			try {			
				packet.setData(data);
				socket.send(packet);
			} catch (Exception e) {	
				e.printStackTrace();
			}	
		}catch (Exception e) {
			e.printStackTrace();
			disableSensors();
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		float[] values = event.values;
		if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
			if((values[0] * 100) != x_A){
				x_A = values[0] *100;
				changed = true;
			}
    		if((values[1] * 100) != y_A){
    			y_A = values[1] *100;
    			changed = true;
    		}
    		if((values[2] * 100) != z_A){
    			z_A = values[2] *100;
    			changed = true;
    		}
		}else if(event.sensor.getType() == Sensor.TYPE_ORIENTATION){
    		if((values[0] * 100) != x_O){
    			x_O = values[0] *100;
    			changed = true;
    		}
    		if((values[1] * 100) != y_O){
    			y_O = values[1] *100;
    			changed = true;
    		}
    		if((values[2] * 100) != z_O){
    			z_O = values[2] *100;
    			changed = true;
    		}
		}
		
		if(changed){
			if(utils.isConnected()){
				send_data_UDP();
				changed = false;
			}else{
				disableSensors();
			}
		}
	}
	
	/**Register the sensor listeners. */
	public void enableSensors(){
		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_NORMAL);
	}
	/**Unregister the sensor listeners. */
	public void disableSensors(){
		mSensorManager.unregisterListener(this);
	}
}

