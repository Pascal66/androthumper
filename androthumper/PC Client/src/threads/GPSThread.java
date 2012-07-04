package threads;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import constants.Conts;
import constants.GpsData;
import constants.GpsData.GpsDataWrapper;

import ui.Window;

/** Listen on a specific port for GPS information from the phone */
public class GPSThread implements Runnable{
	
	/**A socket to receive information on. */
	private DatagramSocket socket;
	/**A packet to store the information in. */
	private DatagramPacket packet;
	/**A Thread to listen for the ping from the client. */
	private Thread listeningThread = null;
	/**A flag to signify if we are allowed to run or not. */
	private boolean running = true;
	/**The host. */
	private Window window;
	/**Any previous recieved GPS data. */
	private GpsData data;
	private boolean debug = false;
	
	public GPSThread(Window window2){
		this.window = window2;
		startListening();
	}

	private void startListening(){
		try {
			socket = new DatagramSocket(Conts.Ports.GPS_INCOMMING_PORT, InetAddress.getLocalHost());
			
			listeningThread = new Thread(this);
			listeningThread.start();
			Window.PrintToLog("GPS listening.");
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {

		packet = new DatagramPacket(new byte[Conts.PacketSize.GPS_PACKET_SIZE], Conts.PacketSize.GPS_PACKET_SIZE);
		while(running){
			try {
				socket.receive(packet);
				byte[] data = packet.getData();
				
				GpsDataWrapper gpsData = new GpsDataWrapper(data);
				if(debug){
					System.out.println(gpsData);
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}

}
