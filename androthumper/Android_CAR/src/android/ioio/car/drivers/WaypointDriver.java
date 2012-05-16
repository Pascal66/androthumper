package android.ioio.car.drivers;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import constants.Conts;

import android.ioio.car.listeners.MyCompassListener;
import android.ioio.car.listeners.MyLocationListener;
import android.location.Location;
import android.util.Log;

public class WaypointDriver implements Driver{

	private List<Location> waypoints = new LinkedList<Location>();
	private Location currentLocation;
	private float currentHeading;
	private boolean running = false,waiting = false;
	private Thread drivingThread;
	private DriverManager driverManager;
	private long lastMessageTime;
	private float maxSpeed = 64;
	private boolean setBaud = false;
	private byte[] input = new byte[12];

	public WaypointDriver(DriverManager driverManager){
		this.driverManager = driverManager;
		waypoints = new LinkedList<Location>();
		currentLocation = new Location("empty");
		currentLocation.setLatitude(-200);
		currentLocation.setLongitude(-200);

		//register for GPS updates
		driverManager.getThreadManager().getGPSThread().addMyLocationListener(new MyLocationListener() {
			@Override
			public void gotNewLocation(Location L) {
				Log.e("recieved:","");
				synchronized (currentLocation) {
					currentLocation = L;
					if(waiting){
						currentLocation.notify();
						Log.e("notified:","");
					}
				};

			}
		});
		//and for compass updates
		driverManager.getThreadManager().getIOIOThread().addCompassListener(new MyCompassListener() {
			@Override
			public void gotCompassHeading(float heading) {
				currentHeading = heading;
			}
		});

		drivingThread = new Thread(drivingRunnable);
	}

	public void clearPoints(){
		waypoints.clear();
	}
	public void setNewWaypoints(Vector<double[]> points){
		waypoints.clear();
		for(double[] point:points){
			Location l = new Location("waypoint provider");
			l.setLatitude(point[0]);
			l.setLongitude(point[1]);
			waypoints.add(l);
		}
	}

	@Override
	public void start() {
		lastMessageTime = System.currentTimeMillis();
		if(!running){
			running = true;
			drivingThread.start();
		}
		/*
		 * thread:
		 * wait for location
		 * is location within its radius of first point?
		 * yes - done, next point
		 * no - 
		 * get heading from current pos to first point
		 * go
		 * is point in radius?
		 * yes - done, next point
		 * no - keep going, refresh heading, compensate
		 */
	}

	@Override
	public void stop() {
		running = false;
		input[Conts.Controller.Channel.LEFT_CHANNEL] = 0;
		input[Conts.Controller.Channel.RIGHT_CHANNEL] = 0;
		//TODO FIX FOR NEW DRIVER
		//driverManager.getThreadManager().getIOIOThread().override(input);
	}

	@Override
	public void restart() {
		// TODO Auto-generated method stub

	}

	private Runnable drivingRunnable = new Runnable() {
		@Override
		public void run() {
			float desiredHeading;
			Location nextLocation = null;
			Iterator<Location> pointIter = waypoints.iterator();
			if(pointIter.hasNext()){
				nextLocation = pointIter.next();
			}else{
				//no points to drive to TODO
				Log.e("waypoint","done");
				running=false;
			}

			while(running){
				if(currentLocation.getLatitude() == -200){
					try {
						waiting = true;
						synchronized (currentLocation) {
							currentLocation.wait();
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				if(isNear(currentLocation, nextLocation)){
					//are at the location, onto next
					if(pointIter.hasNext()){
						nextLocation = pointIter.next();
					}else{
						//ran out of points TODO
						Log.e("waypoint","hit point");
					}
				}else{
					//are not at next location. Compare desired heading against actual
					desiredHeading = currentLocation.bearingTo(nextLocation);
					//					if(desiredHeading < 0){
					//						desiredHeading = 360-desiredHeading;
					//					}

					float newHeading=0;
					if(currentHeading > 180){
						//newHeading = -180+(currentHeading-180);
						newHeading = currentHeading-360;
					}else{
						newHeading = currentHeading;
					}

					float difference = newHeading-desiredHeading;
					
					if(difference < -180){
						difference = 360-Math.abs(difference);
					}

					if((System.currentTimeMillis() - lastMessageTime) > 500){
//						lastMessageTime = System.currentTimeMillis();
//						driverManager.getThreadManager().getUtilitiesThread().sendMessage("sending command: "+getInputAsString(input));
//						if(!driverManager.getThreadManager().getUtilitiesThread().sendMessage(("heading to: "+desiredHeading+", current: "+currentHeading+", modified to: "+newHeading+" correcting by: "+difference))){
//							if(!driverManager.getThreadManager().getUtilitiesThread().sendMessage("message lost")){
//								Log.e("Waypoint driver","could not send");
//							}
//						}
					}

					/*
					 * negative is right, positive is left
					 * the closer to +- 180, the less it needs to be
					 */
					//TODO FIX FOR NEW DRIVER
//					if(setBaud){
//						input[10] = (byte)maxSpeed;
//						input[11] = (byte)maxSpeed;
//						if(Math.abs(difference) > 120){
//							if(difference > 0){
//								input[Conts.Controller.Channel.LEFT_CHANNEL] = (byte)(maxSpeed * -0.7);
//								input[Conts.Controller.Channel.RIGHT_CHANNEL] = (byte)(maxSpeed * 0.7);
//							}else{
//								input[Conts.Controller.Channel.LEFT_CHANNEL] = (byte)(maxSpeed * 0.7);
//								input[Conts.Controller.Channel.RIGHT_CHANNEL] = (byte)(maxSpeed * -0.7);
//							}
//						}
//						else if(difference > 0){
//							//go left, slow down left wheel
//							input[Conts.Controller.Channel.LEFT_CHANNEL] = (byte)((maxSpeed / 180f)*(180-difference));
//						}else if(difference < 0){
//							//go right, slow right
//							difference = Math.abs(difference);
//							input[Conts.Controller.Channel.RIGHT_CHANNEL] = (byte)((maxSpeed / 180f)*(180-difference));
//						}
//					}else{
//						input[Conts.Controller.Buttons.BUTTON_B] = 1;
//						setBaud = true;
//						driverManager.getThreadManager().getIOIOThread().override(input);
//						//driverManager.getThreadManager().getUtilitiesThread().sendMessage("Sent baud.");
//						input[Conts.Controller.Buttons.BUTTON_B] = 0;
//						try {
//							Thread.sleep(100);
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//						}
//					}
//					//driverManager.getThreadManager().getUtilitiesThread().sendMessage("sending command: "+getInputAsString(input));
//					driverManager.getThreadManager().getIOIOThread().override(input);
				}
				Thread.yield();
			}
		}
	};

	private boolean isNear(Location source, Location destination){
		if(source.distanceTo(destination) < source.getAccuracy()){
			return true;
		}
		return false;
	}
	private String getInputAsString(byte[] input){
		StringBuilder sb = new StringBuilder();
		for(byte b:input){
			sb.append(b);sb.append(",");
		}
		return sb.toString();
	}
}
