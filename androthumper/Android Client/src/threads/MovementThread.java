package threads;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Enumeration;

import android.ioio.client.Main;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import constants.Conts;

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
	private Main host;

	public MovementThread(Main host){
		this.host = host;
		pingingThread = new Thread(this);
		pingingThread.start();
	}

	/**
	 * Construct the packet with the data and send it to the sever. Also performs the mixing of the left/right
	 * side to give the feel of driving a normal car.
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

			if(RStick[1] > 0.5){
				input[10] = (byte)-rSpeed;
				input[11] = (byte)-lSpeed;
			}else{
				input[10] = (byte)rSpeed;
				input[11] = (byte)lSpeed;
			}

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
		InetAddress finalAddress = null;
		try {
			URL whatismyip = new URL("http://automation.whatismyip.com/n09230945.asp");

			WifiManager myWifiManager = (WifiManager) host.getSystemService(host.WIFI_SERVICE);
			WifiInfo myWifiInfo = myWifiManager.getConnectionInfo();
			int ipAddress = myWifiInfo.getIpAddress();
			int reverseIp = Integer.reverseBytes(ipAddress);

			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			outer:
				while(interfaces.hasMoreElements()){
					NetworkInterface network = interfaces.nextElement();
					Enumeration<InetAddress> addresses = network.getInetAddresses();
					while(addresses.hasMoreElements()){
						InetAddress address = addresses.nextElement();
						int iip = byteArrayToInt(address.getAddress(), 0);

						if(iip == ipAddress || iip == reverseIp){
							finalAddress = address;
							break outer;
						}
					}
				}


			socket = new DatagramSocket(Conts.Ports.MOVE_INCOMMING_PORT, finalAddress);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}catch (IOException e) {
			e.printStackTrace();
		}

		host.printToLog("Move wait.");
		DatagramPacket packet = new DatagramPacket(new byte[1], 1);
		try {
			socket.receive(packet);
			host.printToLog("Move got.");
			phoneAddress = packet.getAddress();
			phonePort = packet.getPort();
			this.packet = new DatagramPacket(new byte[]{1}, 1, phoneAddress, phonePort);
			waitingForPing = false;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private int byteArrayToInt(byte[] arr, int offset) {
		if (arr == null || arr.length - offset < 4)
			return -1;

		int r0 = (arr[offset] & 0xFF) << 24;
		int r1 = (arr[offset + 1] & 0xFF) << 16;
		int r2 = (arr[offset + 2] & 0xFF) << 8;
		int r3 = arr[offset + 3] & 0xFF;
		return r0 + r1 + r2 + r3;
	}
}
