package threads;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
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
		ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length+4);
		DataOutputStream dos = new DataOutputStream(baos);
		try {
			dos.writeInt(data.length);
			dos.write(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		sendingQueue.add(baos.toByteArray());
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
			int[] bits = new int[8];
			
			System.out.println("Recieved IOIO error:"+data[1]);
			break;
		case Conts.UtilsCodes.IOIO.LOST_IOIO_CONNECTION:
			Window.PrintToLog("lost ioio.");
			break;
		case Conts.UtilsCodes.IOIO.GOT_IOIO_CONNECTION:
			Window.PrintToLog("got ioio.");
			break;
		case Conts.UtilsCodes.DataType.COMPASS_DATA:
			float heading = (360f / 255f) * (data[1] & 0xFF);
			System.out.println("rael heading: "+(data[1] & 0xFF));
			System.out.println("heading: "+heading);
			break;
		case Conts.UtilsCodes.DataType.SEND_MESSAGE_DATA:
			try {
				ByteArrayInputStream bais = new ByteArrayInputStream(data, 1, data.length-1);
				DataInputStream dis = new DataInputStream(bais);
				int size = dis.readInt();
				StringBuilder builder = new StringBuilder();
				for(int i = 0; i < size; i++){
					builder.append(dis.readChar());
				}
				System.out.println(builder.toString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
					Window.PrintToLog("LOST CONNECTION");
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
					Window.PrintToLog("LOST CONNECTION");
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
			
			while(stillConnected){
				Thread.sleep(2000);
				sendCommand(Conts.UtilsCodes.UTILS_CONNECTION_TEST);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static int[] getErrorBits(int error){
		int num;
		int[] bits = new int[9];
		
		if(error<0){
			bits[7] = 1;
			error = -error;
			error=128-error;
			bits[8]=128+error;
		}else{
			bits[8]=error;
		}
		
		while(error != 0){
			if(error >= 128){
				error-=128;
				bits[7] = 1;
			}else if(error >= 64){
				error-=64;
				bits[6] = 1;
			}else if(error >= 32){
				error-=32;
				bits[5] = 1;
			}else if(error >= 16){
				error-=16;
				bits[4] = 1;
			}else if(error >= 8){
				error-=8;
				bits[3] = 1;
			}else if(error >= 4){
				error-=4;
				bits[2] = 1;
			}else if(error >= 2){
				error-=2;
				bits[1] = 1;
			}else if(error == 1){
				error-=1;
				bits[0] = 1;
			}
		}
		
		return bits;
	}

}
