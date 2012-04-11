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
	/**The utils thread that controls the other threads. */
	private UtilsThread utilsThread;
	/**The GPS and location feed to the server. */
	private GPSThread gpsThread;
	
	/**
	 * Get a new ThreadManager, creating instances of all the threads, and starting them
	 * @param app - The host application
	 * @param ip - The ip of the server to connect to
	 */
	public ThreadManager(MainActivity app, String ip_address){		
		utilsThread = new UtilsThread(app, ip_address);
		the_cam = new Cam_thread(app,ip_address,utilsThread);
		the_sensors = new Sensors_thread(app,ip_address,utilsThread);
		ioio_thread_ = new IOIO_Thread(app, ip_address, utilsThread);
		gpsThread = new GPSThread(ip_address, utilsThread, app);
		ioio_thread_.start();	
		the_cam.start_thread();
	}
    
	/**
	 * Stop all the threads
	 */
    public void stop(){
    	the_cam.stop_thread();
    	the_sensors.stop();
		ioio_thread_.abort();
    	utilsThread.stop();
    	gpsThread.disableLocation();
    	gpsThread.disableGPSStatus();
    }
    
    /**
     * Provide a way to inject data into the IOIO thread. I.E, by zeemotes.
     * @param data - The controller data. Must be formatted according to what the IOIO wants!
     */
    public void overrideMovement(byte[] data){
    	ioio_thread_.override(data);
    }
}
