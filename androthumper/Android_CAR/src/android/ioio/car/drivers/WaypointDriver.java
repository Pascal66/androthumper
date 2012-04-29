package android.ioio.car.drivers;

import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import android.ioio.car.listeners.MyCompassListener;
import android.ioio.car.listeners.MyLocationListener;
import android.location.Location;

public class WaypointDriver implements Driver{

	List<Location> waypoints = new LinkedList<Location>();
	Location currentLocation;
	int currentHeading;
	
	public WaypointDriver(DriverManager driverManager){
		waypoints = new LinkedList<Location>();
		
		//register for GPS updates
		driverManager.getThreadManager().getGPSThread().addMyLocationListener(new MyLocationListener() {
			@Override
			public void gotNewLocation(Location L) {
				currentLocation = L;
			}
		});
		//and for compass updates
		driverManager.getThreadManager().getIOIOThread().addCompassListener(new MyCompassListener() {
			@Override
			public void gotCompassHeading(int heading) {
				currentHeading = heading;
			}
		});
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

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void restart() {
		// TODO Auto-generated method stub
		
	}
}
