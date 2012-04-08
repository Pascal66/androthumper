package android.ioio.car;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import android.util.Log;

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

	/**A Thread used to listen for incoming packets on the incoming socket. */
	private Thread listeningThread;
	/**A Thread used to send packets from the sending socket. */
	private Thread sendingThread;
	
	/**A socket used to listen for packets on. */
	private DatagramSocket listeningSocket;
	/**A socket used to send sockets from. */
	private DatagramSocket sendingSocket;
	/**A packet used to store information about received packets. */
	private DatagramPacket listeningPacket;
	/**A packet used to store information to send. */
	private DatagramPacket sendingPacket;

	/**A queue used to buffer output to send. */
	private BlockingQueue<byte[]> sendingQueue;

	/**A reference to the GPS thread to enable/disable. */
	private GPSThread gps;
	/**A reference to the Sensors thread to enable/disable. */
	private Sensors_thread sensors;

	/**A logging tag. */
	private final String TAG = "UtilsThread";

	public UtilsThread(String ip){
		try {
			sendingQueue = new ArrayBlockingQueue<byte[]>(20);

			DatagramChannel channel = DatagramChannel.open();
			listeningSocket = channel.socket();
			listeningSocket.bind(null);
			sendingSocket = new DatagramSocket();

			listeningPacket = new DatagramPacket(new byte[Conts.PacketSize.UTILS_CONTROL_PACKET_SIZE], Conts.PacketSize.UTILS_CONTROL_PACKET_SIZE);
			sendingPacket = new DatagramPacket(new byte[Conts.PacketSize.UTILS_CONTROL_PACKET_SIZE], Conts.PacketSize.UTILS_CONTROL_PACKET_SIZE, InetAddress.getByName(ip), Conts.Ports.UTILS_INCOMMING_PORT);

			listeningThread = new Thread(listenRunnable);
			listeningThread.start();

			sendingThread = new Thread(sendRunnable);
			sendingThread.start();

			ping();
		}catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Send a ping to the PC. This packet contains the port that the {@link #listeningSocket} is listening, so the PC knows
	 * where to send packets to.
	 */
	private void ping(){
		ByteArrayOutputStream baos = new ByteArrayOutputStream(Conts.PacketSize.UTILS_CONTROL_PACKET_SIZE);
		DataOutputStream dos = new DataOutputStream(baos);

		try {
			dos.writeInt(listeningSocket.getLocalPort());
			dos.close();
			sendData(baos.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		sendData(Arrays.copyOf(baos.toByteArray(), Conts.PacketSize.UTILS_CONTROL_PACKET_SIZE));
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
		}
	}

	/**
	 * Stop the {@link #listeningThread} and {@link #sendingThread}
	 */
	public void stop(){
		running = false;
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
			while(running){
				try {
					listeningSocket.receive(listeningPacket);
					processData(listeningPacket.getData());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	};
	/**Runnable for the {@link #sendingThread}*/
	private Runnable sendRunnable = new Runnable() {
		@Override
		public void run() {
			while(running){
				try {
					sendingPacket.setData(sendingQueue.take());
					sendingSocket.send(sendingPacket);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	};
}
