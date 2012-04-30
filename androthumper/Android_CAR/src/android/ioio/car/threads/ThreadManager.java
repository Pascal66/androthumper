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

import android.ioio.car.MainActivity;


/**
 * This class simply holds all of the other threads, such as that managing GPS, sensors, or the camera
 *
 */
public class ThreadManager{
	
	/**The camera feed to the server. */
	private Cam_thread the_cam;
	/**The sensor feed to the server. */
	private Sensors_thread the_sensors;	
	/**The ioio thread managing hardware. */
	private IOIO_Thread ioio_thread_;
	private UtilsThread utilsThread;
	/**The GPS and location feed to the server. */
	private GPSThread gpsThread;
	
	private MainActivity app;
	private String ipAddress;
	
	/**
	 * Get a new ThreadManager, creating instances of all the threads, and starting them
	 * @param app - The host application
	 * @param ip - The ip of the server to connect to
	 */
	public ThreadManager(MainActivity app, String ip_address){	
		this.ipAddress = ip_address;
		this.app = app;
		//TODO write restart
		the_cam = new Cam_thread(this);
		the_sensors = new Sensors_thread(this);
		ioio_thread_ = new IOIO_Thread(this);
		gpsThread = new GPSThread(this);	
	}
    
	/**
	 * Stop all the threads
	 */
    public void stopAll(){
    	the_cam.stop_thread();
    	the_sensors.stop();
		ioio_thread_.abort();
    	gpsThread.disableLocation();
    	gpsThread.disableGPSStatus();
    }
    
    /**Restart all the threads. */
    public void restartAll(){
		the_cam.restart();
		the_sensors.restart();
		ioio_thread_ = new IOIO_Thread(this);	
    }
    
    /**
     * Provide a way to inject data into the IOIO thread. I.E, by zeemotes.
     * @param data - The controller data. Must be formatted according to what the IOIO wants!
     */
    public void overrideMovement(byte[] data){
    	//TODO replace by zeemote driver
    	ioio_thread_.override(data);
    }
    
    public Cam_thread getCamThread(){
    	return this.the_cam;
    }
    public Sensors_thread getSensorThread(){
    	return this.the_sensors;
    }
    public IOIO_Thread getIOIOThread(){
    	return this.ioio_thread_;
    }
    public void giveUtilities(UtilsThread utils){
    	this.utilsThread = utils;
    }
    public UtilsThread getUtilitiesThread(){
    	return this.utilsThread;
    }
    public GPSThread getGPSThread(){
    	return this.gpsThread;
    }
    
    public MainActivity getMainActivity(){
    	return this.app;
    }
    public String getIpAddress(){
    	return ipAddress;
    }
}
