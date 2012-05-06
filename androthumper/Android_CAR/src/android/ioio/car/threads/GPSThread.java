package android.ioio.car.threads;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import constants.Conts;

import android.R.integer;
import android.content.Context;
import android.ioio.car.listeners.MyLocationListener;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.GpsStatus.Listener;
import android.os.Bundle;

/**
 * The class has two jobs. It can listen for location updates, and listen for GPS status', then send them on to the server
 * if required. 
 * @author Alex Flynn
 *
 */
public class GPSThread implements LocationListener, Listener{

	/**A socket to send the packets. */
	private DatagramSocket socket;
	/**A handle to the location manager. */
	private LocationManager locationManager;
	/**The current/last location. */
	private Location currentLocation = null;
	/**A packet to hold data to send. */
	private DatagramPacket packet;
	/**A handle to the activity. */
	//private MainActivity app;
	/**The current/last GPS status data. */
	private GpsStatus gpsStatus;
	/**A list of the top 6 satellites. */
	private LinkedList<GpsSatellite> top6Sats;
	/**byte[] of data to send the location data in. */
	private byte[] locationData = new byte[Conts.PacketSize.GPS_POSITION_PACKET_SIZE];
	/**byte[] of data to send GPS status data in. */
	private byte[] statusData = new byte[Conts.PacketSize.GPS_POSITION_PACKET_SIZE];
	/**byte stream for location data. */
	private ByteArrayOutputStream locationOutput;
	/**byte stream for status data. */
	private ByteArrayOutputStream statusOutput;
	/**data stream for location data. */
	private DataOutputStream locationDataOutput;
	/**data stream for status data. */
	private DataOutputStream statusDataOutput;
	private ThreadManager manager;
	
	private List<MyLocationListener> listeners;

	GPSThread(ThreadManager manager){
		this.manager = manager;
		try {
			listeners = new LinkedList<MyLocationListener>();
			socket = new DatagramSocket();
			packet = new DatagramPacket(new byte[]{1}, 1, InetAddress.getByName(manager.getIpAddress()), Conts.Ports.GPS_INCOMMING_PORT);

			locationManager = (LocationManager)manager.getMainActivity().getSystemService(Context.LOCATION_SERVICE);
			top6Sats = new LinkedList<GpsSatellite>();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		if(location != null){
			boolean changed = false;

			//If we have no previous location, send this one.
			if(currentLocation == null){
				currentLocation = location;
				changed = true;
			}else{
				//If it is more accurate..
				if(location.hasAccuracy() && (location.getAccuracy() <= currentLocation.getAccuracy())){
					currentLocation = location;
					changed = true;
				//Or we hit timeout of a 30 seconds
				}else if(location.getTime() > (currentLocation.getTime() + 30000)){
					currentLocation = location;
					changed = true;
				}
			}

			if(changed){
				
				for(MyLocationListener listener:listeners){
					listener.gotNewLocation(currentLocation);
				}

				if(locationOutput != null){
					locationOutput.reset();
				}else{
					locationOutput = new ByteArrayOutputStream(Conts.PacketSize.GPS_POSITION_PACKET_SIZE);
				}
	
				locationDataOutput = new DataOutputStream(locationOutput);
				
				//Write the location code, then write the location data to the byte[]
				try {
					locationDataOutput.write(Conts.UtilsCodes.DataType.GPS_POSITION_DATA);
					locationDataOutput.writeDouble(currentLocation.getLatitude());
					locationDataOutput.writeDouble(currentLocation.getLongitude());
					locationDataOutput.writeDouble(currentLocation.getAltitude());

					locationDataOutput.writeFloat(currentLocation.getSpeed());
					locationDataOutput.writeFloat(currentLocation.getAccuracy());
				} catch (IOException e) {
					e.printStackTrace();
				}
				locationData = locationOutput.toByteArray();
				
				packet.setData(locationData);
				try {
					socket.send(packet);
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}
	}
	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}
	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

	/**Register location updates from both the network and GPS providers. */
	public void enableLocation(){
		manager.getMainActivity().runOnUiThread(new Runnable(){
			@Override
			public void run(){
				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, GPSThread.this);
				locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, GPSThread.this);

				onLocationChanged(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
				onLocationChanged(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
			}
		});
	}
	/**Remove any location listeners. */
	public void disableLocation(){
		manager.getMainActivity().runOnUiThread(new Runnable(){
			@Override
			public void run(){
				locationManager.removeUpdates(GPSThread.this);
			}
		});
	}
	
	/**Enable the GPS status listener. */
	public void enableGPSStatus(){
		manager.getMainActivity().runOnUiThread(new Runnable(){
			@Override
			public void run(){
				locationManager.addGpsStatusListener(GPSThread.this);
			}
		});
	}
	/**Disable the GPS listener. */
	public void disableGPSStatus(){
		manager.getMainActivity().runOnUiThread(new Runnable(){
			@Override
			public void run(){
				locationManager.removeGpsStatusListener(GPSThread.this);
			}
		});
	}

	@Override
	public void onGpsStatusChanged(int event) {
		if(event == GpsStatus.GPS_EVENT_SATELLITE_STATUS){
			gpsStatus = locationManager.getGpsStatus(gpsStatus);
			Iterator<GpsSatellite> satsIt = gpsStatus.getSatellites().iterator();
			top6Sats.clear();
			while(satsIt.hasNext()){
				top6Sats.add(satsIt.next());
			}

			//Order the list in decending order of signal-to-noise ratio
			int found = 0; float smallest = Float.MIN_VALUE; GpsSatellite current = null;
			GpsSatellite[] top = new GpsSatellite[6];
			while(found < 6 && top6Sats.size() > 0){
				for(int i = 0; i < top6Sats.size(); i++){
					GpsSatellite sat = top6Sats.get(i);

					if(sat.getSnr() > smallest){
						smallest = sat.getSnr();
						current = sat;
					}
				}
				top[found] = current;
				top6Sats.remove(current);
				found++;
				smallest = Float.MIN_VALUE;
			}

			sendStatus(top);
		}

	}

	/**Send the GPS status packet to the server. */
	public void sendStatus(GpsSatellite[] sats){
		if(statusOutput != null){
			statusOutput.reset();
		}else{
			statusOutput = new ByteArrayOutputStream(Conts.PacketSize.GPS_POSITION_PACKET_SIZE);
		}
		
		statusDataOutput = new DataOutputStream(statusOutput);

		//Write the status code, then write the details of the top 6 satellites into the byte[]
		try {
			statusDataOutput.write(Conts.UtilsCodes.DataType.GPS_STATUS_DATA);

			for(int i = 0; i < sats.length; i++){
				if(sats[i] != null){
					statusDataOutput.write(sats[i].getPrn());
					statusDataOutput.writeFloat(sats[i].getSnr());
					statusDataOutput.write(sats[i].usedInFix() ? 1 : 0);
				}else{
					statusDataOutput.write(-1);
					statusDataOutput.writeFloat(-1);
					statusDataOutput.write(-1);
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		statusData = statusOutput.toByteArray();

		packet.setData(statusData);
		try {
			socket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void addMyLocationListener(MyLocationListener listener){
		listeners.add(listener);
	}
}
