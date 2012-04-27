package android.ioio.car;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.http.util.ByteArrayBuffer;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import constants.Conts;

/**
 * This class provides means to communication in two ways to the server. It can recieve and transmit messages at the same time.
 * It is used for the turning on/off of sensors, and for the phone to send error messages or anything else back to the 
 * server in a byte[] 2000 elements long.
 * @author Alex Flynn
 *
 */
public class UtilsThread{

	/**The host activity. */
	private Activity host;

	/**A boolean flag to indicate if the threads are allowed to run. */
	private boolean running = true;
	/**A boolean flag to indicate whether the app should send the camera feed to the server. */
	private boolean camEnabled = false;
	/**A boolean flag to indicate whether to listen for and send location updates to the server */
	private boolean gpsEnabled = false;
	/**A boolean flag to indicate whether to listen to and send accelerometer/orientation sensor data to the server. */
	private boolean sensorsEnabled = false;
	/**A boolean flag to indicate whether to listen to and send GPS status updates to the server. */
	private boolean gpsStatusEnabled = false;
	/**A boolean flag to indicate whether the socket is still connected to the server. */
	private boolean stillConnected = false;

	/**A socket to be connected to the server to. */
	private Socket socket;
	/**A Thread used to listen for incoming packets on the incoming socket. */
	private Thread listeningThread;
	/**A Thread used to send packets from the sending socket. */
	private Thread sendingThread;
	/**A Thread used to check if the connection to the server is still active. */
	private Thread checkerThread;
	/**The output stream to send data to the server. */
	private OutputStream socketOutput;
	/**The input stream to read data from the server. */
	private InputStream socketInput;
	private ByteArrayBuffer bab;
	private int bytesReceived = 0;

	/**A queue used to buffer output to send. */
	private BlockingQueue<byte[]> sendingQueue;

	/**A reference to the GPS thread to enable/disable. */
	private GPSThread gps;
	/**A reference to the Sensors thread to enable/disable. */
	private Sensors_thread sensors;

	public UtilsThread(Activity host, String ip){
		this.host = host;
		try {
			sendingQueue = new ArrayBlockingQueue<byte[]>(20);
			socket = new Socket();
			SocketAddress address = new InetSocketAddress(InetAddress.getByName(ip), Conts.Ports.UTILS_INCOMMING_PORT);
			socket.connect(address, 3000);

			socketInput = socket.getInputStream();
			socketOutput = socket.getOutputStream();
			stillConnected = true;

			checkerThread = new Thread(checkerRunnable);
			//			checkerThread.start();

			listeningThread = new Thread(listenRunnable);
			listeningThread.start();

			sendingThread = new Thread(sendRunnable);
			sendingThread.start();

			Log.e("utils","end of create");
		}catch (SocketException e) {
			e.printStackTrace();
			stillConnected = false;
			Log.e("utils","error");
		} catch (UnknownHostException e) {
			e.printStackTrace();
			stillConnected = false;
			Log.e("utils","error");
		} catch (IOException e) {
			e.printStackTrace();
			stillConnected = false;
			Log.e("utils","error");
		}
	}

	/**
	 * Send a single command to the client.
	 * @param command - the command to send. See {@link Conts} //TODO break into inner classes for types
	 */
	public void sendCommand(byte command){
		if(isConnected()){
			byte[] data = new byte[Conts.PacketSize.UTILS_CONTROL_PACKET_SIZE];
			data[0] = command;
			sendData(data);
		}
	}

	/**
	 * Send a byte[] to the server. The length of the byte[] MUST match that which is expected by the server 
	 * detailed in {@link Conts#UTILS_CONTROL_PACKET_SIZE}
	 * @param data - {@link byte}[] of information
	 * @return True if the length matches, and it has been added to the queue. False if otherwise.
	 */
	public boolean sendData(byte[] data){
		if(data.length != Conts.PacketSize.UTILS_CONTROL_PACKET_SIZE){
			return false;
		}
		sendingQueue.add(data);
		return true;
	}

	/**
	 * Process the data that was sent by the server.
	 * @param data - {@link byte}[] of data.
	 */
	private void processData(byte[] data){
		switch(data[0]){
		case Conts.UtilsCodes.UTILS_CONNECTION_TEST:
			break;
		case Conts.UtilsCodes.ENABLE_CAM:
			if(!camEnabled){
				camEnabled = true;
			}
			break;
		case Conts.UtilsCodes.DISABLE_CAM:
			if(camEnabled){
				camEnabled = false;
			}
			break;
		case Conts.UtilsCodes.ENABLE_GPS:
			if(!gpsEnabled){
				gps.enableLocation();
				gpsEnabled = true;
			}
			break;
		case Conts.UtilsCodes.DISABLE_GPS:
			if(gpsEnabled){
				gps.disableLocation();
				gpsEnabled = false;
			}
			break;
		case Conts.UtilsCodes.ENABLE_GPS_STATUS:
			if(!gpsStatusEnabled){
				gps.enableGPSStatus();
				gpsStatusEnabled = true;
			}
			break;
		case Conts.UtilsCodes.DISABLE_GPS_STATUS:
			if(gpsStatusEnabled){
				gps.disableGPSStatus();
				gpsStatusEnabled = false;
			}
			break;
		case Conts.UtilsCodes.ENABLE_SENSORS:
			if(!sensorsEnabled){
				sensorsEnabled = true;
				sensors.enableSensors();
			}
			break;
		case Conts.UtilsCodes.DISABLE_SENSORS:
			if(sensorsEnabled){
				sensorsEnabled = false;
				sensors.disableSensors();
			}
			break;
		case Conts.UtilsCodes.SEND_GPS_WAYPOINTS:
			DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data, 1, data.length-1));

			Vector<float[]> points = new Vector<float[]>();
			while(true){
				try {
					float[] data1 = new float[2];
					data1[0] = dis.readFloat();
					data1[1] = dis.readFloat();

				} catch (IOException e) {
					break;
				}
				break;
			}
			/*
			 * Send info to GPS waypoint driver
			 */
		}
	}

	/**
	 * Stop the {@link #listeningThread} and {@link #sendingThread}
	 */
	public void stop(){
		running = false;
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**True if the server has requested the camera feed, false if otherwise. */
	public boolean getUseCamera(){
		return camEnabled;
	}
	/**True if the server has requested location updates. */
	public boolean getUseGPS(){
		return gpsEnabled;
	}
	/**True if the server has requested sensor updates. */
	public boolean getUseSensors(){
		return sensorsEnabled;
	}
	/**True if the server has requested GPS status updates. */
	public boolean getUseGpsStatus(){
		return gpsStatusEnabled;
	}

	/**Register the {@link GPSThread} for control. */
	public void registerForGPS(GPSThread gps){
		this.gps = gps;
	}
	/**Register the {@link Sensors_thread} for control. */
	public void registerForSensor(Sensors_thread sensor){
		this.sensors = sensor;
	}

	/**Runnable for the {@link #listeningThread}*/
	private Runnable listenRunnable = new Runnable() {
		@Override
		public void run() {
			while(running && stillConnected){
				try{
					if(bab == null){
						bab = new ByteArrayBuffer(Conts.PacketSize.UTILS_CONTROL_PACKET_SIZE);
					}

					byte[] data = new byte[Conts.PacketSize.UTILS_CONTROL_PACKET_SIZE];
					int result = socketInput.read(data);
					if(result > 0){
						bab.append(data, bytesReceived, result);
						bytesReceived+=result;
						if(bytesReceived == Conts.PacketSize.UTILS_CONTROL_PACKET_SIZE){
							processData(bab.toByteArray());
							bab.clear();
							bytesReceived = 0;
						}
					}
				}catch(IOException e){
					lostConnection();
					e.printStackTrace();
				}
			}
		}
	};
	/**Runnable for the {@link #sendingThread}*/
	private Runnable sendRunnable = new Runnable() {
		@Override
		public void run() {
			while(running && stillConnected){
				try {
					socketOutput.write(sendingQueue.take());
				} catch (IOException e) {
					lostConnection();
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	};
	/**Runnable for {@link #checkerThread}. */
	private Runnable checkerRunnable = new Runnable() {
		@Override
		public void run() {
			while(stillConnected){
				try{
					socketOutput.write(-1);
					Thread.sleep(1000);
				}catch(IOException e){
					lostConnection();
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	};

	public boolean isConnected(){
		return stillConnected;
	}

	private void lostConnection(){
		stillConnected = false;
		host.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(host, "LOST CONNECTION", Toast.LENGTH_SHORT).show();
			}
		});

		/*
		 * TODO
		 * Write methods to stop IOIO, reset sockets, attempt re-connection every X seconds.
		 * use broadcast receiver to listen for network changes:
		 * http://stackoverflow.com/questions/3307237/how-can-i-monitor-the-network-connection-status-in-android
		 */
	}
}
