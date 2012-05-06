package android.ioio.car.drivers;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

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
						newHeading = -180+(currentHeading-180);
					}else{
						newHeading = currentHeading;
					}
					
					float difference = desiredHeading-newHeading;
					
					if((System.currentTimeMillis() - lastMessageTime) > 500){
						lastMessageTime = System.currentTimeMillis();
						if(!driverManager.getThreadManager().getUtilitiesThread().sendMessage(("heading to: "+desiredHeading+", current: "+currentHeading+", modified to: "+newHeading+" correcting by: "+difference))){
							if(!driverManager.getThreadManager().getUtilitiesThread().sendMessage("message lost")){
								Log.e("Waypoint driver","could not send");
							}
						}
					}
					
					/*
					 * negative is left, positive is right
					 */
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
}
