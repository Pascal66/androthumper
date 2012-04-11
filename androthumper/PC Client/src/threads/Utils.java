package threads;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
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

	/**Flag to signify if the threads are allowed to run. */
	private boolean running = true;
	/**Flag to signify if the socket is still connected. */
	private boolean stillConnected = false;
	/**Thread for listening to the {@link #listeningSocket}. */
	private Thread listeningThread;
	/**Thread for sending the packets to the client with the {@link #sendSocket}. */
	private Thread sendingThread;
	/**Thread to set up the initial connection. */
	private Thread connectionThread;
	/**A queue to hold the information to send to he client. */
	private BlockingQueue<byte[]> sendingQueue;
	/**The input stream from the socket.*/
	private InputStream socketInput;
	/**The output stream to the socket. */
	private OutputStream socketOutput;
	
	private ServerSocket serverSocket;

	public Utils(Window window2){
		try {
			sendingQueue = new ArrayBlockingQueue<byte[]>(20);
			serverSocket = new ServerSocket(Conts.Ports.UTILS_INCOMMING_PORT, 0, InetAddress.getLocalHost());
			connectionThread = new Thread(connectionRunnable);
			connectionThread.start();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
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
	public boolean isConnected(){
		return stillConnected;
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
			while(running && stillConnected){
				try {
					byte[] data = new byte[Conts.PacketSize.UTILS_CONTROL_PACKET_SIZE];
					socketInput.read(data);
					processData(data);
				} catch (IOException e) {
					stillConnected = false;
					System.out.println("Lost connection.");
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
			while(running && stillConnected){
				try {
					socketOutput.write(sendingQueue.take());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}catch (IOException e) {
					stillConnected = false;
					e.printStackTrace();
				}
			}
		}
	};
	
	/**Runnable for {@link #connectionThread}. This accepts the first connection and
	 * starts listening/sending on it.*/
	private Runnable connectionRunnable = new Runnable() {
		@Override
		public void run() {
			try {
				Window.PrintToLog("Utils wait.");
				startComms(serverSocket.accept());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	};
	/**Start the communication. Get the streams from the socket and start the threads. */
	private void startComms(Socket socket){
		try {
			socketInput = socket.getInputStream();
			socketOutput = socket.getOutputStream();
			
			stillConnected = true;
			Window.PrintToLog("Utils connected.");
			
			listeningThread = new Thread(listenRunnable);
			sendingThread = new Thread(sendRunnable);
			
			listeningThread.start();
			sendingThread.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}