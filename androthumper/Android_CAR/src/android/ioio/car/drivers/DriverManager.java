package android.ioio.car.drivers;

import android.ioio.car.threads.ThreadManager;

public class DriverManager {

	private ThreadManager threadManager;
	private BasicServerDriver basicServerDriver;
	private WaypointDriver waypointDriver;
	private ZeemoteDriver zeemoteDriver;
	
	public DriverManager(ThreadManager threadManager){
		this.threadManager = threadManager;
		
		zeemoteDriver = new ZeemoteDriver(this);
		basicServerDriver = new BasicServerDriver(this);
		waypointDriver = new WaypointDriver(this);
	}
	
	/**Stop all the drivers. */
	public void stopAll(){
		basicServerDriver.stop();
		waypointDriver.stop();
		zeemoteDriver.stop();
		//TODO
	}
	
	public ZeemoteDriver getZeemoteDriver(){
		return this.zeemoteDriver;
	}
	public BasicServerDriver getBasicServerDriver(){
		return this.basicServerDriver;
	}
	public WaypointDriver getWaypointDriver(){
		return this.waypointDriver;
	}
	public ThreadManager getThreadManager(){
		return this.threadManager;
	}
}
