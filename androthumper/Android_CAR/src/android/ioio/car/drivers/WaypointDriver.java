package android.ioio.car.drivers;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import android.ioio.car.listeners.MyCompassListener;
import android.ioio.car.listeners.MyLocationListener;
import android.location.Location;

public class WaypointDriver implements Driver{

	private List<Location> waypoints = new LinkedList<Location>();
	private Location currentLocation;
	private int currentHeading;
	private boolean running = false,waiting = false;
	private Thread drivingThread;

	public WaypointDriver(DriverManager driverManager){
		waypoints = new LinkedList<Location>();

		//register for GPS updates
		driverManager.getThreadManager().getGPSThread().addMyLocationListener(new MyLocationListener() {
			@Override
			public void gotNewLocation(Location L) {
				synchronized (currentLocation) {
					currentLocation = L;
					if(waiting){
						currentLocation.notify();
					}
				};

			}
		});
		//and for compass updates
		driverManager.getThreadManager().getIOIOThread().addCompassListener(new MyCompassListener() {
			@Override
			public void gotCompassHeading(int heading) {
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
		running = true;
		drivingThread.start();
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
			}
			
			while(running){
				if(currentLocation == null){
					try {
						waiting = true;
						currentLocation.wait();
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
					}
				}else{
					//are not at next location. Compare desired heading against actual
					desiredHeading = currentLocation.bearingTo(nextLocation);
					/*
					 * TODO
					 * if desired heading and actual heading are within some limit, keep going
					 * else, by degree of difference, apply more drive to appropriate side
					 */
				}
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
