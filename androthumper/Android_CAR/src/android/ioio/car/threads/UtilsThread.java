package android.ioio.car.threads;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.http.util.ByteArrayBuffer;

import android.app.Activity;
import android.ioio.car.drivers.DriverManager;
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
	private ByteArrayOutputStream bab;
	private int bytesReceived = 0;

	/**A queue used to buffer output to send. */
	private BlockingQueue<byte[]> sendingQueue;

	private ThreadManager threadManager;
	private DriverManager driverManager;

	public UtilsThread(ThreadManager threadManager,DriverManager driverManager){
		this.threadManager = threadManager;
		this.driverManager = driverManager;
		threadManager.giveUtilities(this);
		try {
			sendingQueue = new ArrayBlockingQueue<byte[]>(20);
			socket = new Socket();
			SocketAddress address = new InetSocketAddress(InetAddress.getByName(threadManager.getIpAddress()), Conts.Ports.UTILS_INCOMMING_PORT);
			//Connect the socket with a timeout, so IO exception is thrown if the connection is not made in time
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
		if(!sendingQueue.offer(data)){
			Log.e("Utils Thread","COULD NOT SEND DATA, FULL");
		}
		return true;
	}
	
	public boolean sendMessage(String message){
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(Conts.PacketSize.UTILS_CONTROL_PACKET_SIZE);
			DataOutputStream dis = new DataOutputStream(baos);
			dis.write(Conts.UtilsCodes.DataType.SEND_MESSAGE_DATA);
			dis.writeInt(message.length());
			dis.writeChars(message);
			byte[] data = baos.toByteArray();
			byte[] result = new byte[Conts.PacketSize.UTILS_CONTROL_PACKET_SIZE];
			System.arraycopy(data, 0, result, 0, data.length);
			return sendData(result);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Process the data that was sent by the server.
	 * @param data - {@link byte}[] of data.
	 */
	private void processData(byte[] data){
		switch(data[0]){
		case Conts.UtilsCodes.UTILS_CONNECTION_TEST:
			break;
		case Conts.UtilsCodes.Command.Enable.ENABLE_CAM:
			if(!camEnabled){
				camEnabled = true;
				sendMessage("PHONE - Enable camera");
			}
			break;
		case Conts.UtilsCodes.Command.Disable.DISABLE_CAM:
			if(camEnabled){
				camEnabled = false;
				sendMessage("PHONE - Disable camera");
			}
			break;
		case Conts.UtilsCodes.Command.Enable.ENABLE_GPS:
			if(!gpsEnabled){
				threadManager.getGPSThread().enableLocation();
				gpsEnabled = true;
				sendMessage("PHONE - Enable location");
			}
			break;
		case Conts.UtilsCodes.Command.Disable.DISABLE_GPS:
			if(gpsEnabled){
				threadManager.getGPSThread().disableLocation();
				gpsEnabled = false;
				sendMessage("PHONE - Disable location");
			}
			break;
		case Conts.UtilsCodes.Command.Enable.ENABLE_GPS_STATUS:
			if(!gpsStatusEnabled){
				threadManager.getGPSThread().enableGPSStatus();
				gpsStatusEnabled = true;
				sendMessage("PHONE - Enable GPS status");
			}
			break;
		case Conts.UtilsCodes.Command.Disable.DISABLE_GPS_STATUS:
			if(gpsStatusEnabled){
				threadManager.getGPSThread().disableGPSStatus();
				gpsStatusEnabled = false;
				sendMessage("PHONE - Disable GPS Status");
			}
			break;
		case Conts.UtilsCodes.Command.Enable.ENABLE_SENSORS:
			if(!sensorsEnabled){
				sensorsEnabled = true;
				threadManager.getSensorThread().enableSensors();
				sendMessage("PHONE - Enable sensors");
			}
			break;
		case Conts.UtilsCodes.Command.Disable.DISABLE_SENSORS:
			if(sensorsEnabled){
				sensorsEnabled = false;
				threadManager.getSensorThread().disableSensors();
				sendMessage("PHONE - Disable sensors");
			}
			break;
		case Conts.UtilsCodes.Command.CHANGE_DRIVER:
			driverManager.stopAll();
			switch(data[1]){
			case Conts.Driver.BASIC_SERVER_DRIVER:
				driverManager.getBasicServerDriver().start();
				sendMessage("PHONE - Start basic server driver");
				break;
			case Conts.Driver.WAYPOINT_DRIVER:
				//driverManager.getWaypointDriver().start();
				//TODO not here? Separate start/stop
				break;
			}
			break;
		case Conts.Driver.WaypointDriver.START_DRIVER:
			driverManager.getWaypointDriver().start();
			sendMessage("PHONE - Start waypoint driver");
			break;
		case Conts.Driver.WaypointDriver.STOP_DRIVER:
			driverManager.getWaypointDriver().stop();
			sendMessage("PHONE - Stop waypoint driver");
			break;
		case Conts.UtilsCodes.DataType.SEND_GPS_WAYPOINTS:
			DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data, 1, data.length-1));

			Vector<double[]> points = null;
			try {
				int size = dis.readInt();
				points = new Vector<double[]>();
				for(int i = 0; i<size; i++){
					double[] data1 = new double[2];
					data1[0] = dis.readFloat();
					data1[1] = dis.readFloat();
					points.add(data1);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			driverManager.getWaypointDriver().setNewWaypoints(points);
			sendMessage("PHONE - Recieved new waypoints");
			break;
		default:
			Log.e("UTILS THREAD","Not sure what that was... "+Conts.Tools.getStringFromByteArray(data));
		}
	}

	/**
	 * Stop the {@link #listeningThread} and {@link #sendingThread}
	 */
	public void stop(){
		running = false;

		if(stillConnected){
			try {
				socketInput.close();
				socketOutput.close();
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	public void restart(){
		try {
			bytesReceived = 0;
			socket = new Socket();
			SocketAddress address = new InetSocketAddress(InetAddress.getByName(threadManager.getIpAddress()), Conts.Ports.UTILS_INCOMMING_PORT);
			//Connect the socket with a timeout, so IO exception is thrown if the connection is not made in time
			socket.connect(address, 3000);

			socketInput = socket.getInputStream();
			socketOutput = socket.getOutputStream();
			stillConnected = true;
			running = true;

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

	//	/**Register the {@link GPSThread} for control. */
	//	public void registerForGPS(GPSThread gps){
	//		this.gps = gps;
	//	}
	//	/**Register the {@link Sensors_thread} for control. */
	//	public void registerForSensor(Sensors_thread sensor){
	//		this.sensors = sensor;
	//	}

	/**Runnable for the {@link #listeningThread}*/
	private Runnable listenRunnable = new Runnable() {
		int sizeOfData = -1;
		@Override
		public void run() {
			while(running && stillConnected){
				try{
					if(bab == null){
						bab = new ByteArrayOutputStream(Conts.PacketSize.UTILS_CONTROL_PACKET_SIZE);
					}

					byte[] data = new byte[Conts.PacketSize.UTILS_CONTROL_PACKET_SIZE];
					int result = socketInput.read(data);
					if(result > 0){
						if(running){
							//bab.append(data, bytesReceived, result);
							bab.write(data, 0, result);
							bytesReceived+=result;
							if(bytesReceived > 4 && sizeOfData == -1){
								byte[] data1 = bab.toByteArray();
								byte[] size = new byte[4];
								System.arraycopy(data1, 0, size, 0, 4);
								ByteArrayInputStream bais = new ByteArrayInputStream(size);
								DataInputStream dis = new DataInputStream(bais);
								sizeOfData = dis.readInt();
								bab.reset();
								bab.write(data, 4, data1.length-4);
								bytesReceived-=4;
							}else{
								bab.write(data, 0, result);
							}
							if(bytesReceived == sizeOfData){
								processData(bab.toByteArray());
								//bab.clear();
								bab.reset();
								bytesReceived = 0;
								sizeOfData = -1;
							}
						}
						/**
						 * TODO
						 * rewrite with first bytes being size of current information, allowing lots of wayponits and huge messages
						 */
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
		if(running && stillConnected){
			stillConnected = false;
			threadManager.getMainActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(threadManager.getMainActivity(), "LOST CONNECTION", Toast.LENGTH_SHORT).show();
				}
			});
		}

		/*
		 * TODO
		 * Write methods to stop IOIO, reset sockets, attempt re-connection every X seconds.
		 * use broadcast receiver to listen for network changes:
		 * http://stackoverflow.com/questions/3307237/how-can-i-monitor-the-network-connection-status-in-android
		 */
	}
}
