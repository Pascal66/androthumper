package threads;
/*******************************************************************************************************
Copyright (c) 2011 Regents of the University of California.
All rights reserved.

This software was developed at the University of California, Irvine.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in
   the documentation and/or other materials provided with the
   distribution.

3. All advertising materials mentioning features or use of this
   software must display the following acknowledgment:
   "This product includes software developed at the University of
   California, Irvine by Nicolas Oros, Ph.D.
   (http://www.cogsci.uci.edu/~noros/)."

4. The name of the University may not be used to endorse or promote
   products derived from this software without specific prior written
   permission.

5. Redistributions of any form whatsoever must retain the following
   acknowledgment:
   "This product includes software developed at the University of
   California, Irvine by Nicolas Oros, Ph.D.
   (http://www.cogsci.uci.edu/~noros/)."

THIS SOFTWARE IS PROVIDED ``AS IS'' AND WITHOUT ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIED
WARRANTIES OF MERCHANTIBILITY AND FITNESS FOR A PARTICULAR PURPOSE.
IN NO EVENT SHALL THE UNIVERSITY OR THE PROGRAM CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************************************/


import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Runnable;
import java.lang.Thread;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URL;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import constants.Conts;

import ui.Window;

/**
 * This class receives byte[]s from a client, creates an image, and sets it on the GUI
 * @author Alex
 *
 */
public class CamThread implements Runnable{

	/**The host frame, where to send the image. */
	private Window window;
	/**The looping receiving thread. */
	private Thread t;
	/**An inflater used to uncompress the received byte[]. */
	private Inflater uncompressor;
	public static int DATA_MAX_SIZE = Conts.PacketSize.CAMERA_PACKET_SIZE - Conts.PacketSize.CAMERA_HEADER_SIZE;
	/**Flag to signify whether the thread is allowed to run or not. */
	private boolean running = true;

	public CamThread(Window window2){
		window = window2;
		uncompressor = new Inflater();

		try{
			t = new Thread(this);
			t.start();
		}catch (Exception e){e.printStackTrace();}
	}

	public void run(){
		Window.PrintToLog("Cam wait.");

		handleConnection_UDP();
	}

	/**
	 * Find the internal/external address of the server and display it on the log.
	 * Then constant loops while allowed receiving sets of packets, creating images from
	 * complete sets, then setting the image on the GUI.
	 */
	public void handleConnection_UDP() {
		int current_frame = -1,frame_nb,nb_packets,packet_nb,size_packet;
		int slicesStored = 0;
		byte[] imageData = null, data = null,buff = null;
		DatagramSocket socket=null;
		ByteArrayOutputStream bos = null;

		try {		         	
//			URL whatismyip = new URL("http://automation.whatismyip.com/n09230945.asp");
//			BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
//
//			String ip = in.readLine(); //you get the IP as a String
//			Window.PrintToLog("ip1 = "+ip);

			InetAddress serverAddr = InetAddress.getLocalHost();
			Window.PrintToLog("ip2 = " + serverAddr.getHostAddress());
			socket = new DatagramSocket(Conts.Ports.CAMERA_INCOMMING_PORT, serverAddr);

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
				}

				//Still on current bitmap
				if(frame_nb == current_frame){
					System.arraycopy(data, Conts.PacketSize.CAMERA_HEADER_SIZE, imageData, packet_nb * DATA_MAX_SIZE, size_packet);
					slicesStored++;				
				}

				/* If image is complete display it */
				if (slicesStored == nb_packets) {					
					if(bos == null){
						bos = new ByteArrayOutputStream();
						buff = new byte[1024];
					}
					bos.reset();
					uncompressor.reset();
					uncompressor.setInput(imageData);
					int totalRead = 0;
					while(!uncompressor.finished()){
						uncompressor.inflate(buff, 0, 1024);
						bos.write(buff);

					}
					//ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
					window.setImage(bos.toByteArray());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			//socket.close();
		} catch (DataFormatException e) {
			e.printStackTrace();
		} 
	}

	/**Stop the thread from receiving packets. */
	public void stop(){
		running = false;
	}
}
