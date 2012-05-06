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
	/**The location of the client. */
	private double latitude,longitude,altitude;
	/**The speed and accuracy of the lock. */
	private float speed,accuracy;
	private Window window;
	/**A Stream to read from the packet with. */
	private ByteArrayInputStream bytein;
	/**A stream to extract data from the byte stream with. */
	private DataInputStream dataInput;
	
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
		//using biggest because cant use packet with more than one size
		packet = new DatagramPacket(new byte[Conts.PacketSize.GPS_STATUS_PACKET_SIZE], Conts.PacketSize.GPS_STATUS_PACKET_SIZE);
		while(running){
			try {
				socket.receive(packet);
				byte[] data = packet.getData();
				
				bytein = new ByteArrayInputStream(data);
				dataInput = new DataInputStream(bytein);
				switch(dataInput.readByte()){
				case Conts.UtilsCodes.DataType.GPS_POSITION_DATA:
					latitude = dataInput.readDouble();
					longitude = dataInput.readDouble();
					altitude = dataInput.readDouble();
					speed = dataInput.readFloat();
					accuracy = dataInput.readFloat();
					
					window.postToGPSLog("GPS Update: Lat: "+latitude+" Long: "+longitude+" speed: "+speed+" accuracy: "+accuracy);
					window.setLocation(latitude, longitude);
					break;
				case Conts.UtilsCodes.DataType.GPS_STATUS_DATA:
					int[] prns = new int[6];
					float[] snrs = new float[6];
					boolean[] used = new boolean[6];
					for(int i = 0; i < 6; i++){
						prns[i] = dataInput.read();
						snrs[i] = dataInput.readFloat();
						int b = dataInput.read();
						if(b == 1){
							used[i] = true;
						}else{
							used[i] = false;
						}
					}

					window.recievedGpsStatus(prns, snrs, used);
					break;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}

}
