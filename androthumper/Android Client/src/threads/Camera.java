package threads;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import constants.Conts;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.ioio.client.Main;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class Camera implements Runnable{

	private Main host;
	private Thread t;

	/**An inflater used to uncompress the received byte[]. */
	private Inflater uncompressor;
	public static int DATA_MAX_SIZE = Conts.PacketSize.CAMERA_PACKET_SIZE - Conts.PacketSize.CAMERA_HEADER_SIZE;
	/**Flag to signify whether the thread is allowed to run or not. */
	private boolean running = true;
	private InetAddress finalAddress = null;
	private Bitmap bitmap;

	public Camera(Main host){
		this.host = host;
		uncompressor = new Inflater();
		
		t = new Thread(null, this, "cam", 92033748L);

		WifiManager myWifiManager = (WifiManager) host.getSystemService(host.WIFI_SERVICE);
		WifiInfo myWifiInfo = myWifiManager.getConnectionInfo();
		int ipAddress = myWifiInfo.getIpAddress();
		int reverseIp = Integer.reverseBytes(ipAddress);

		Enumeration<NetworkInterface> interfaces;
		try {
			interfaces = NetworkInterface.getNetworkInterfaces();
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
		} catch (SocketException e) {
			e.printStackTrace();
		}

		host.printToLog("Cam waiting");
		t.start();
	}

	@Override
	public void run(){
		handleConnection_UDP();
	}

	public void handleConnection_UDP() {
		int current_frame = -1,frame_nb,nb_packets,packet_nb,size_packet,totalSize = 0;
		int slicesStored = 0;
		byte[] imageData = null, data = null,buff = null;
		DatagramSocket socket=null;
		ByteArrayOutputStream bos = null;

		try {		         	
			socket = new DatagramSocket(Conts.Ports.CAMERA_INCOMMING_PORT, finalAddress);

			byte[] buffer = new byte[Conts.PacketSize.CAMERA_PACKET_SIZE];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

			while (running){		
				socket.receive(packet);				
				data = packet.getData();			
				frame_nb = (int)data[0];
				nb_packets = (int)data[1];
				packet_nb = (int)data[2];
				size_packet = (int) ((data[3] & 0xff) << 8 | (data[4] & 0xff));

				//Start of a new bitmap
				if((packet_nb==0) && (current_frame != frame_nb)){
					current_frame = frame_nb;
					slicesStored = 0;				
					imageData = new byte[nb_packets * DATA_MAX_SIZE];
					totalSize=0;
				}

				//Still on current bitmap
				if(frame_nb == current_frame){
					System.arraycopy(data, Conts.PacketSize.CAMERA_HEADER_SIZE, imageData, packet_nb * DATA_MAX_SIZE, size_packet);
					totalSize+=size_packet;
					slicesStored++;				
				}

				/* If image is complete display it */
				if (slicesStored == nb_packets) {					
					if(bos == null){
						bos = new ByteArrayOutputStream(4096);
						buff = new byte[1024];
					}
					bos.reset();
					try{
						Log.e("bytes","total bytes: "+totalSize);

						uncompressor.reset();
						uncompressor.setInput(imageData, 0, totalSize);
						int counter = 0;
						while(!uncompressor.finished()){
							if(counter > 10){
								Log.e("som","thing");
							}
							uncompressor.inflate(buff, 0, 1024);
							bos.write(buff);
							counter++;

						}
						//ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
						byte[] data1 = bos.toByteArray();
						uncompressor.end();
						uncompressor = new Inflater();
						bitmap = BitmapFactory.decodeByteArray(data1, 0, data1.length);
						host.setImage(bitmap);
					}catch(DataFormatException e){
						Log.e("message: ",e.getMessage());
						e.printStackTrace();
						
//						bos.reset();
						uncompressor.reset();
					}
				}
			}
		} catch (IOException e) {
			Log.e("message: ",e.getMessage());
			e.printStackTrace();
			//socket.close();
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
