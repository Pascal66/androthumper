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
 * This thread recieves movement data stuctures from the window provided by the controller thread,
 * puts them into a packet, and sends them to the client (phone)
 * @author Alex
 *
 */
public class MovementThread implements Runnable{

	/**The address of the phone to send the packets to. */
	private InetAddress phoneAddress;
	/**The port of the phone to send the packet to. */
	private int phonePort;
	/**The socket from which to send the packets from. */
	private DatagramSocket socket;
	/**The packet to attatch data to and send to the client. */
	private DatagramPacket packet;
	/**The byte[] of data to send. */
	private byte[] input = new byte[Conts.PacketSize.MOVE_PACKET_SIZE];
	/**The thread to wait for a ping */
	private Thread pingingThread;
	/**A flag to signify whether we have received a ping from the client yet. */
	private boolean waitingForPing = true;
	
	private static final float BRAKE_THRESH = 0.2f;
	private static final float ACCEL_THRESH = 0.2f;

	public MovementThread(Window window2){
		try {
			socket = new DatagramSocket(Conts.Ports.MOVE_INCOMMING_PORT, InetAddress.getLocalHost());
			pingingThread = new Thread(this);
			pingingThread.start();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Construct the packet with the data and send it to the sever.
	 * @param buttons - The array of button values (1=pressed, 0=released)
	 * @param LStick - The float x/y values of the left stick
	 * @param RStick - the float x/y values of the right stick
	 * @param triggers - The float values of the left and right trigger (unreliable.avoid)
	 */
	public void doMove(boolean[] buttons, float[] LStick, float[] RStick, float[] triggers){
		if(!waitingForPing){
			for(int i = 0; i < buttons.length; i++){
				if(buttons[i]){
					input[i] = 1;
				}else{
					input[i] = 0;
				}
			}

			int speed = (int)((127/(float)100) * (triggers[1] * 100));
			
			int rSpeed,lSpeed;
			lSpeed = rSpeed = speed;
			
			if(LStick[0] > 0.1){
				//RIGHT (slow left)
				if(speed < 5){
					if(buttons[Conts.Controller.Buttons.BUTTON_Y]){
						//lock left
						rSpeed = (int)(127/(float)100 * (LStick[0]*100));
					}else{
						lSpeed = (int)(127/(float)100 * (-LStick[0]*100));
						rSpeed = (int)(127/(float)100 * (LStick[0]*100));
					}
				}else{
					lSpeed = lSpeed - (int) (lSpeed * LStick[0]);
				}
			}else if(LStick[0] < -0.1){
				//LEFT (slow right)
				if(speed < 5){
					if(buttons[Conts.Controller.Buttons.BUTTON_Y]){
						//Lock right
						lSpeed = (int)(127/(float)100 * (-LStick[0]*100));
					}else{
						lSpeed = (int)(127/(float)100 * (-LStick[0]*100));
						rSpeed = (int)(127/(float)100 * (LStick[0]*100));
					}
				}else{
					rSpeed = rSpeed - (int) (rSpeed * -LStick[0]);
				}
			}
			
			input[10] = (byte)rSpeed;
			input[11] = (byte)lSpeed;
			
//			input[10] = (byte) (100 * -LStick[0]);
//			if(RStick[1] > MovementThread.BRAKE_THRESH){
//				input[11] = (byte) (-100* RStick[1]);
//			}else if(triggers[1] > MovementThread.ACCEL_THRESH){
//				input[11] = (byte) (100* triggers[1]);
//			}else{
//				input[11] = 0;
//			}
			System.out.println("RIGHT: "+(byte)rSpeed+" LEFT: "+(byte)lSpeed);
			//packet = new DatagramPacket(input, Conts.PacketSize.MOVE_PACKET_SIZE, phoneAddress, phonePort);
			packet.setData(input);
			try {
				socket.send(packet);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void run() {
		//Wait to recieve a ping from a client, then store the address and send the data back to it.
		Window.PrintToLog("Move wait.");
		DatagramPacket packet = new DatagramPacket(new byte[1], 1);
		try {
			socket.receive(packet);
			Window.PrintToLog("Move got.");
			phoneAddress = packet.getAddress();
			phonePort = packet.getPort();
			this.packet = new DatagramPacket(new byte[]{1}, 1, phoneAddress, phonePort);
			waitingForPing = false;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
