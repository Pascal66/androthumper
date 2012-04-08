package threads;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import constants.Conts;

import ui.Window;

/**
 * Implements a thread that constantly listens for sensor data from the client, 
 * and displays it on the Window.
 * @author Alex Flynn
 *
 */
public class SensorThread implements Runnable{
	/**A Thread to constantly listen for data. */
	private Thread listeningThread;
	/**A socket to listen for packets on. */
	private DatagramSocket socket;
	/**A packet to hold the data received. */
	private DatagramPacket packet;
	/**A flag to signify if the thread is allowed to run. */
	private boolean running = true;
	/**The floats to hold the sensor data. */
	private float orientX, orientY, orientZ,accelX, accelY, accelZ;
	/**The Window to provide the data to. */
	private Window window;
	
	public SensorThread(Window window){
		this.window = window;
		listeningThread = new Thread(this);
		
		try {
			socket = new DatagramSocket(Conts.Ports.SENSOR_INCOMING_PORT, InetAddress.getLocalHost());
			packet = new DatagramPacket(new byte[Conts.PacketSize.SENSORS_PACKET_SIZE], Conts.PacketSize.SENSORS_PACKET_SIZE);
			listeningThread.start();
			
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	
	@Override
	public void run() {
		Window.PrintToLog("Sensor thread listening...");
		
		while(running){
			try {
				socket.receive(packet);
				byte[] data = packet.getData();
				
				short nb = (short) ((data[0] & 0xff) << 8 | (data[1] & 0xff)); 
				orientX = (float)nb / 100;
				
				nb = (short) ((data[2] & 0xff) << 8 | (data[3] & 0xff)); 
				orientY = (float)nb / 100;
				
				nb = (short) ((data[4] & 0xff) << 8 | (data[5] & 0xff)); 
				orientZ = (float)nb / 100;
				
				nb = (short) ((data[6] & 0xff) << 8 | (data[7] & 0xff)); 
				accelX = (float)nb / 100;
				
				nb = (short) ((data[8] & 0xff) << 8 | (data[9] & 0xff)); 
				accelY = (float)nb / 100;
				
				nb = (short) ((data[10] & 0xff) << 8 | (data[11] & 0xff)); 
				accelZ = (float)nb / 100;
				
				window.receivedSensorValues(orientX, orientY, orientZ, accelX, accelY, accelZ);
			} catch (IOException e) {
				e.printStackTrace();
				running = false;
			}
		}
		
		Window.PrintToLog("Sensor thread terminating.");
	}

}
