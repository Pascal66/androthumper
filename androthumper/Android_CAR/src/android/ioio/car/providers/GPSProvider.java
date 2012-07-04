package android.ioio.car.providers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.LinkedList;

import android.ioio.car.hardware.GpsModule;
import android.ioio.car.listeners.MyCompassListener;
import android.ioio.car.listeners.MyGPSListener;
import android.ioio.car.threads.AndroidGPS;
import constants.Conts;
import constants.GpsData;

/**
 * This provider combines the GPS data from multiple sources, and provides it to listeners.
 * The hardware GPS module is preferred over the device's GPS sensor, but it may not always be available.
 * This provider attempts to provide data from the module, but will fall back onto the device when it needs to.
 * If the module gains a lock while the device is being used, the device will no longer be used, and the module
 * will be used instead. If a lock with the module is lost, the device's GPS sensor will be used again until a lock
 * with the module has been gained again.
 * 
 * Unifies information to a standard, provides that information, and if desired, sends information to the server.
 * 
 * @author Alex Flynn
 *
 */
public class GPSProvider {

	/**The GPS module providing GPS data. */
	private GpsModule gpsModule;
	/**The device's GPS providing data. */
	private AndroidGPS device;
	
	/**The last data provided by the GPS module. */
	private GpsData lastData = null;
	/**The last data provided by the device. */
	private GpsData lastDeviceData = null;
	private GpsData dataToSend = null;
	/**Boolean enabled flag. */
	private boolean enabled = false;
	/**Boolean sending to server. */
	private boolean isSendingToServer  =false;
	
	private DatagramSocket socket;
	private DatagramPacket packet;
	
	/**Any listeners that want to know GPS data. */
	private LinkedList<MyGPSListener> gpsListeners;
	
	public GPSProvider(){
		gpsListeners = new LinkedList<MyGPSListener>();
	}
	
	/**Add the GPS module to the GPS provider. */
	public void assignDevice(GpsModule module){
		this.gpsModule = module;
	}
	/**Add the device's GPS sensors to the GPS provider. */
	public void assignDevice(AndroidGPS device){
		this.device = device;
	}
	
	/**Provide this provider with new GPS data from the module. */
	public void provideData(GpsModule module, GpsData data){
		lastData = data;
		if(data.gotLock() && device.isEnabled()){
			//The module has a lock, so disable the device's GPS
			device.disable();
		}else if(!data.gotLock() && !device.isEnabled()){
			//The module doesnt have a lock and the device's GPS is not used, so enable it
			device.enable();
		}
		compareAndProvide();
	}
	/**Provide this provider with new GPS data from the device. */
	public void provideData(AndroidGPS device, GpsData data){
		lastDeviceData = data;
		//This has a lower priority, so will never want to disable the module.
		compareAndProvide();
	}
	
	/**Add a listener to this provider. */
	public void addGpsListener(MyGPSListener listener){
		gpsListeners.add(listener);
	}
	
	/**Enable this GPS provider. */
	public void enable(){
		if(enabled){
			return;
		}
		
		if(gpsModule != null){
			gpsModule.startListening();
		}
		device.enable();
		enabled = true;
	}
	/**Disable this GPS provider. */
	public void disable(){
		if(!enabled){
			return;
		}
		
		//Can be null if IOIO is disconnected
		if(gpsModule != null){
			gpsModule.stopListening();
		}
		device.disable();
		enabled = false;
	}
	
	private void compareAndProvide(){
		if(lastData.getAccuracy() < lastDeviceData.getAccuracy()){dataToSend = lastData;}else{dataToSend = lastDeviceData;}
		
		for(MyGPSListener l:gpsListeners){
			l.gotNewGPSData(dataToSend);
		}
		
		if(isSendingToServer){
			try {
				packet.setData(dataToSend.getRawData());
				socket.send(packet);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public boolean isSendingToServer(){
		return isSendingToServer;
	}
	public void enableSendingToServer(){
		if(!isSendingToServer){
			initServerConnection();
		}
	}
	public void disableSendingToServer(){
		if(isSendingToServer){
			closeServerConnection();
		}
	}
	
	private void initServerConnection(){
		try {
			socket = new DatagramSocket();
			packet = new DatagramPacket(new byte[]{1}, 1, InetAddress.getByName(ProviderManager.getIpAddress()), Conts.Ports.GPS_INCOMMING_PORT);
			isSendingToServer = true;
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	private void closeServerConnection(){
		socket.close();
		isSendingToServer = false;
	}
	public void provideCompassHeading(float heading){
		device.provideCompassHeading(heading);
		gpsModule.provideCompassHeading(heading);
	}
}
