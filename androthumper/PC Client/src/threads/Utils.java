package threads;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import constants.Conts;

import ui.Window;

/**
 * This class provides means to communication in two ways to the server. It can recieve and transmit messages at the same time.
 * It is used for the turning on/off of sensors, and for the phone to send error messages or anything else back to the 
 * server in a byte[] 2000 elements long.
 * @author Alex Flynn
 *
 */
public class Utils{

	/**Flag to show if we have received ping from client. */
	private boolean recievedPing = false;
	/**Flag to signify if the threads are allowed to run. */
	private boolean running = true;
	/**Thread for listening to the {@link #listeningSocket}. */
	private Thread listeningThread;
	/**Thread for sending the packets to the client with the {@link #sendSocket}. */
	private Thread sendingThread;
	/**Socket to listen for packets from the client. */
	private DatagramSocket listeningSocket;
	/**Socket to send packets to the client. */
	private DatagramSocket sendSocket;
	/**A packet to hold the data to send to the client. */
	private DatagramPacket sendPacket;
	/**A packet to hold the data recieved from the client. */
	private DatagramPacket listenPacket;
	/**A queue to hold the information to send to he client. */
	private BlockingQueue<byte[]> sendingQueue;

	public Utils(Window window2){
		try {
			listeningSocket = new DatagramSocket(Conts.Ports.UTILS_INCOMMING_PORT,InetAddress.getLocalHost());
			sendSocket = new DatagramSocket();
			
			sendingQueue = new ArrayBlockingQueue<byte[]>(20);
			sendPacket = new DatagramPacket(new byte[]{1}, 1);
			listenPacket = new DatagramPacket(new byte[Conts.PacketSize.UTILS_CONTROL_PACKET_SIZE], Conts.PacketSize.UTILS_CONTROL_PACKET_SIZE);
			
			listeningThread = new Thread(listenRunnable);
			listeningThread.start();
			
			sendingThread = new Thread(sendRunnable);
			sendingThread.start();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Send a single command to the client.
	 * @param command - the command to send. See {@link Conts} //TODO break into inner classes for types
	 */
	public void sendCommand(byte command){
		if(!isWaitingForPing()){
			byte[] data = new byte[Conts.PacketSize.UTILS_CONTROL_PACKET_SIZE];
			data[0] = command;
			sendData(data);
		}
	}
	
	/**
	 * Send a byte[] of data to the client. Must be length specified in {@link Conts#UTILS_CONTROL_PACKET_SIZE}
	 * and start with a code.
	 * @param data - the byte[] of data to send.
	 * @return True if the data was added to the queue to send, else false.
	 */
	public boolean sendData(byte[] data){
		if(data.length != Conts.PacketSize.UTILS_CONTROL_PACKET_SIZE){
			return false;
		}
		sendingQueue.add(data);
		return true;
	}

	/**
	 * Is the Utils waiting for a ping from the client?
	 * @return True if so, false otherwise.
	 */
	public boolean isWaitingForPing(){
		return !recievedPing;
	}
	
	/**
	 * Process the byte[] recieved from the client.
	 * @param data - the byte[] to process.
	 */
	private void processData(byte[] data){
		switch(data[0]){
		case Conts.UTILS_MESSAGE_TYPE_DRIVER_ERROR:
			System.out.println("Recieved IOIO error:"+data[1]);
			break;
		}
	}
	
	/**
	 * A runnable for the {@link #listeningThread} to run.
	 */
	private Runnable listenRunnable = new Runnable(){
		@Override
		public void run() {
			while(running){
				if(!recievedPing){
					Window.PrintToLog("Utils waiting for ping.");
				}
				
				try {
					listeningSocket.receive(listenPacket);
					if(!recievedPing){
						Window.PrintToLog("Utils recieved ping.");
						byte[] data = listenPacket.getData();
						ByteArrayInputStream bais = new ByteArrayInputStream(data);
						DataInputStream dis = new DataInputStream(bais);
						int port = dis.readInt();
						dis.close();
						sendPacket.setPort(port);
						sendPacket.setAddress(listenPacket.getAddress());
						recievedPing = true;
					}
					
					processData(listenPacket.getData());
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	};
	
	/**
	 * A runnable for the {@link #sendingThread} to run.
	 */
	private Runnable sendRunnable = new Runnable(){
		@Override
		public void run() {
			while(running){
				try {
					sendPacket.setData(sendingQueue.take());
					sendSocket.send(sendPacket);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	};

}
