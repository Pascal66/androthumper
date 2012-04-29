package android.ioio.car.drivers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.ioio.car.threads.ThreadManager;
import android.widget.Toast;

import constants.Conts;

/**
 * This driver recieves data from the server's XBox controller, and uses it to drive to IOIO override the IOIO thread.
 * @author Alex
 *
 */
public class BasicServerDriver implements Driver{

	private DriverManager driverManager;
	private DatagramSocket socket;
	private Thread listeningThread;
	private boolean listening = false;
	
	BasicServerDriver(DriverManager manager){
		this.driverManager = manager;
		
		//Send a ping to the sever to let it know where we are listening to
		try {
			socket = new DatagramSocket();
			DatagramPacket pingPacket = new DatagramPacket(new byte[]{1}, 1, InetAddress.getByName(driverManager.getThreadManager().getIpAddress()), Conts.Ports.MOVE_INCOMMING_PORT);
			socket.send(pingPacket);
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**A runnable containing the listening/processing packet loop.*/
	private Runnable listeningRunnable = new Runnable() {
		@Override
		public void run() {
			DatagramPacket packet = new DatagramPacket(new byte[Conts.PacketSize.MOVE_PACKET_SIZE], Conts.PacketSize.MOVE_PACKET_SIZE);
			while(listening){
				try {
					socket.receive(packet);
					//The data is already formatted for the IOIO by the server, so just forward it straight to the ioio
					driverManager.getThreadManager().getIOIOThread().override(packet.getData());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	};
	
	@Override
	public void start(){
		listening = true;
		listeningThread = new Thread(listeningRunnable);
		listeningThread.start();
		
		Runnable r = new Runnable() {
			@Override
			public void run() {
				Toast.makeText(driverManager.getThreadManager().getMainActivity(), "Started basic server driver.", Toast.LENGTH_SHORT).show();
			}
		};
		
		driverManager.getThreadManager().getMainActivity().runOnUiThread(r);
	}
	@Override
	public void stop(){
		listening = false;
	}
	@Override
	public void restart(){
		try {
			socket = new DatagramSocket();
			DatagramPacket pingPacket = new DatagramPacket(new byte[]{1}, 1, InetAddress.getByName(driverManager.getThreadManager().getIpAddress()), Conts.Ports.MOVE_INCOMMING_PORT);
			socket.send(pingPacket);
			start();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
