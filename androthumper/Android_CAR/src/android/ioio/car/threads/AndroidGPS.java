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

import android.content.Context;
import android.ioio.car.listeners.MyGPSListener;
import android.ioio.car.providers.GPSProvider;
import android.ioio.car.providers.ProviderManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import constants.Conts;
import constants.GpsData.GpsDataWrapper;

/**
 * The class has two jobs. It can listen for location updates, and listen for GPS status', then send them on to the server
 * if required. 
 * @author Alex Flynn
 *
 */
public class AndroidGPS implements LocationListener, Listener{

	/**A handle to the location manager. */
	private LocationManager locationManager;
	/**The current/last location. */
	private Location currentLocation = null;
	/**The current/last GPS status data. */
	private GpsStatus gpsStatus;
	/**A list of the top 6 satellites. */
	private LinkedList<GpsSatellite> top6Sats;
	/**byte stream for status data. */
	private ByteArrayOutputStream statusOutput;
	/**data stream for status data. */
	private DataOutputStream statusDataOutput;
	private ThreadManager manager;
	
	private GPSProvider provider;
	private boolean enabled = false;
	private byte[] gpsStatusData;
	private long lastLocationUpdateTime;
	private float compassHeading;

	AndroidGPS(ThreadManager manager, GPSProvider provider){
		this.provider = provider;
		provider.assignDevice(this);
		this.manager = manager;
		
		locationManager = (LocationManager)manager.getMainActivity().getSystemService(Context.LOCATION_SERVICE);
		top6Sats = new LinkedList<GpsSatellite>();
	}

	@Override
	public void onLocationChanged(Location location) {
		if(location != null){
			lastLocationUpdateTime = SystemClock.elapsedRealtime();
			currentLocation = location;
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
	
	/**Enable both the location updates and the GPS status updates. */
	public void enable(){
		manager.getMainActivity().runOnUiThread(new Runnable(){
			@Override
			public void run(){
				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, AndroidGPS.this);
				locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, AndroidGPS.this);

				onLocationChanged(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
				onLocationChanged(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
			}
		});
		
		manager.getMainActivity().runOnUiThread(new Runnable(){
			@Override
			public void run(){
				locationManager.addGpsStatusListener(AndroidGPS.this);
			}
		});
		enabled = true;
	}
	
	/**Disable both the location and GPS status updates. */
	public void disable(){
		manager.getMainActivity().runOnUiThread(new Runnable(){
			@Override
			public void run(){
				locationManager.removeUpdates(AndroidGPS.this);
			}
		});
		
		manager.getMainActivity().runOnUiThread(new Runnable(){
			@Override
			public void run(){
				locationManager.removeGpsStatusListener(AndroidGPS.this);
			}
		});
		enabled = false;
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

			gpsStatusData = gpsStatusToByteArray(top);
			
			boolean lock = ((SystemClock.elapsedRealtime() - lastLocationUpdateTime) > 3000);
			provider.provideData(this, new AndroidGpsData(currentLocation,gpsStatusData, lock));
		}

	}
	
	public void provideCompassHeading(float heading){
		this.compassHeading = heading;
	}

	/**
	 * Convert the list of satellites to a byte array, each with their PNR, SNR, and lock.
	 * @param sats - GpsSatellite[] - The ordered list of satellites to convert
	 * @return - byte[] - A byte[] containing the information
	 */
	private byte[] gpsStatusToByteArray(GpsSatellite[] sats){
		if(statusOutput != null){
			statusOutput.reset();
		}else{
			statusOutput = new ByteArrayOutputStream(Conts.PacketSize.GPS_PACKET_SIZE);
		}
		
		statusDataOutput = new DataOutputStream(statusOutput);

		//Write the status code, then write the details of the top 6 satellites into the byte[]
		try {
			statusDataOutput.write(sats.length);

			for(int i = 0; i < sats.length; i++){
				if(sats[i] != null){
					statusDataOutput.write(sats[i].getPrn());
					statusDataOutput.writeFloat(sats[i].getSnr());
					statusDataOutput.write(sats[i].usedInFix() ? 1 : 0);
				}else{
//					statusDataOutput.write(-1);
//					statusDataOutput.writeFloat(-1);
//					statusDataOutput.write(-1);
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return statusOutput.toByteArray();
	}
	
	public boolean isEnabled(){
		return enabled;
	}
	
	public class AndroidGpsData implements constants.GpsData{
		private Location currentLocation;
		private byte[] gpsData;
		private boolean lock;
		
		private ByteArrayOutputStream baos;
		private DataOutputStream dos;
		
		public AndroidGpsData(Location currentLocation, byte[] gpsData, boolean lock){
			this.gpsData = gpsData;
			this.lock = lock;
			this.currentLocation = currentLocation;
			
		}
		
		@Override
		public byte[] getRawData(){
			if(baos == null){
				baos = new ByteArrayOutputStream();
				dos = new DataOutputStream(baos);
			}
			baos.reset();
			
			try {
				//dos.write(Conts.UtilsCodes.DataType.GPS_POSITION_DATA);
				dos.write(GpsDataWrapper.PROVIDER_DEVICE);
				dos.writeDouble(currentLocation.getLatitude());
				dos.writeDouble(currentLocation.getLongitude());
				dos.writeDouble(currentLocation.getAltitude());

				dos.writeFloat(currentLocation.getSpeed());
				dos.writeFloat(currentLocation.getAccuracy());
				dos.writeFloat(compassHeading);
				
				if(gpsStatusData != null){
					dos.write(1);
					dos.write(gpsData);
				}else{
					dos.write(0);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			byte[] data = baos.toByteArray();
			return data;
		}
		
		@Override
		public boolean gotLock(){
			return lock;
		}
		
		@Override
		public double getAccuracy(){
			return currentLocation.getAccuracy();
		}

		@Override
		public double getLongitude() {
			return currentLocation.getLongitude();
		}

		@Override
		public double getLatitude() {
			return currentLocation.getLatitude();
		}

		@Override
		public double getAltitude() {
			return currentLocation.getAltitude();
		}

		@Override
		public float getSpeed() {
			return currentLocation.getSpeed();
		}
		
		public float getRealHeading(){
			return 0;//TODO
		}
	}
}
