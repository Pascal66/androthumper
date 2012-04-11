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
package android.ioio.car;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.zip.Deflater;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.util.Log;
import constants.Conts;

/**
 * This class manages the camera, and sends any data it can back to the server. Inspiration and the
 * core implementation of this class was provided by University of California, Irvine by Nicolas Oros, Ph.D.
   (http://www.cogsci.uci.edu/~noros/).
 * @author Alex Flynn
 *
 */
public class Cam_thread implements Runnable{
	
	private Camera mCamera;
	/**The size of the header in each packet. */
	private static int HEADER_SIZE = Conts.PacketSize.CAMERA_HEADER_SIZE;
	/**The size of available data n each packet. */
	public static int DATAGRAM_MAX_SIZE = Conts.PacketSize.CAMERA_PACKET_SIZE - Conts.PacketSize.CAMERA_HEADER_SIZE;	
	private int frame_nb = 0;	
	/**The address of the server to send packets to. */
	private InetAddress serverAddr;
	/**A socket to send packets from. */
	private DatagramSocket socket;	
	/**A {@link Deflater} used to compress frame data. */
	private Deflater compressor;

	/**A bitmap to store the processed image in. */
	private Bitmap mBitmap;
	/**The current packet to send. */
	private int packetCount;
	/**The number of packets for a frame. */
	private int nb_packets;
	/**The amount of bytes in the packet available for data. */
	private int size;
	private int count;
	/**An output stream to store the bitmap data in. */
	private ByteArrayOutputStream byteStream;
	/**An output stream to store the compressed data in. */
	private ByteArrayOutputStream compressedOutputStream;
	/**A byte[] to hold the output from the {@link #byteStream}. */
	private byte[] picData;
	/**A byte[] to hold the data to be put into the packet. */
	private byte[] data;
	private byte[] compressedData,buf;
	/**{@link Mat} to hold the OpenCV images. */ 
	private Mat m,dest;
	private static final String TAG = "IP_cam";
	/**The colour the circles to draw around features. */
	//private Scalar circleCol;
	/**Packet to hold data to be sent to server. */
	private DatagramPacket packet;

	private boolean STOP_THREAD,inUse = false;

	/**The IP address of the server to send feed. */
	private String ip_address;
	/**The host activity. */
	private MainActivity app;
	/**The Utils thread to control the camera. */
	private UtilsThread utils;
	/**A list of features provided by OpenCV. */
	//private LinkedList<Point> features;
	/**A queue of frames to process. */
	//private BlockingQueue<byte[]> dataQueue;
	/**Temporary data store from the camera. */
	private byte[] datadata;
	/**The thread doing the processing of the frames from the camera. */
	private Thread processingThread;

	public Cam_thread(MainActivity app, String ip, UtilsThread utils){
		this.app = app;
		mCamera = Camera.open();
		this.utils = utils;
		ip_address = ip;
		compressor = new Deflater();
		compressor.setLevel(Deflater.BEST_COMPRESSION);
		byteStream = new ByteArrayOutputStream();
//		circleCol = new Scalar(255, 0, 0);
//		dataQueue = new ArrayBlockingQueue<byte[]>(2);
		processingThread = new Thread(this);
		processingThread.start();
	}

	private void init(){
		try {			 
			serverAddr = InetAddress.getByName(ip_address);
			socket = new DatagramSocket();
			packet = new DatagramPacket(new byte[]{1}, 1, serverAddr,Conts.Ports.CAMERA_INCOMMING_PORT);
			Camera.Parameters parameters = mCamera.getParameters(); 
			parameters.setPreviewSize(640,480);

			//Get a list of preview sizes support. Useful for different devices.
			//List<Size> sizes = parameters.getSupportedPictureSizes();
			//for(Size s:sizes){
			//Log.e(TAG,"Picture Size: "+s.width+" x "+s.height);
			//}
			
			parameters.setSceneMode(Camera.Parameters.SCENE_MODE_SPORTS);
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
			parameters.setColorEffect(Camera.Parameters.EFFECT_NONE);
			mCamera.setParameters(parameters);
			mCamera.setPreviewDisplay(app.getSurfaceHolder());
			mCamera.setPreviewCallback(new cam_PreviewCallback());  
			mCamera.startPreview();
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	public void start_thread(){
		init();
	}

	public void stop_thread(){
		STOP_THREAD = true;
		socket.close();
	}

	/**
	 * Send the bitmap image to the server.
	 */
	public void send_data_UDP(Bitmap image){

		if(image != null){    	 
			byteStream.reset();
			image.compress(Bitmap.CompressFormat.JPEG, 30, byteStream);	// !!!!!!!  change compression rate to change packets size	

			picData = byteStream.toByteArray();
			//Log.e(TAG, "UNCOMPRESSED SIZE: " + picData.length);
			compressor.reset();
			compressor.setInput(picData);
			compressor.finish();
			compressedOutputStream = new ByteArrayOutputStream(picData.length);
			buf = new byte[picData.length];
			while(!compressor.finished()){
				count = compressor.deflate(buf);
				compressedOutputStream.write(buf, 0, count);
			}
			compressedData = compressedOutputStream.toByteArray();

			//Number of packets used for this bitmap UNCOMPRESSED
			//nb_packets = (int) Math.ceil(picData.length / (float)DATAGRAM_MAX_SIZE);

			//How many packets it takes for our compressed data, including headers for each packet
			//Log.e(TAG,"COMPRESSED SIZE: "+compressedData.length);
			nb_packets = (int)Math.ceil(compressedData.length / ((float)DATAGRAM_MAX_SIZE));
			size = DATAGRAM_MAX_SIZE;
			/* Loop through slices of the bitmap*/

			for(packetCount = 0; packetCount < nb_packets; packetCount++){
				
				//If we are on the last packet... or we only need one packet
				if(packetCount >=0 && packetCount == nb_packets-1){
					//Set the size of this packet to be whatever we have not used up in the previous packets
					size = compressedData.length - packetCount * DATAGRAM_MAX_SIZE;
				}

				/* Set additional header */
				data = new byte[HEADER_SIZE + size];
				data[0] = (byte)frame_nb;
				data[1] = (byte)nb_packets;
				data[2] = (byte)packetCount;
				//smallest 8 bits
				data[3] = (byte)(size >> 8);
				//biggest 8 bits
				data[4] = (byte)size;
				
				/* Copy current slice to byte array */
				System.arraycopy(compressedData, packetCount * DATAGRAM_MAX_SIZE, data, HEADER_SIZE, size);		

				try {			
					packet.setData(data);
					socket.send(packet);
				}catch (Exception e){
					e.printStackTrace();
				}	
			}	
			frame_nb++;

			if(frame_nb == 128){
				frame_nb=0;	
			}
		}
	}    

	// Preview callback used whenever new frame is available...send image via UDP !!!
	private class cam_PreviewCallback implements PreviewCallback {

		@Override
		public void onPreviewFrame(byte[] data, Camera camera){
			if(STOP_THREAD == true){
				mCamera.setPreviewCallback(null);
				mCamera.stopPreview();
				mCamera.release();
				mCamera = null;
				return;
			}

			if (mBitmap == null){
				int format = camera.getParameters().getPreviewFormat();
				Log.e(TAG,"format: "+format);

				mBitmap = Bitmap.createBitmap(camera.getParameters().getPreviewSize().width, camera.getParameters().getPreviewSize().height, Bitmap.Config.ARGB_8888);
				m = new Mat(mBitmap.getHeight() + mBitmap.getHeight() / 2, mBitmap.getWidth(), CvType.CV_8UC1);
				dest = new Mat();
			}

			if(utils.getUseCamera()){
//				m.put(0, 0, data);
//				if(features == null){
//					features = new LinkedList<Point>();
//				}
//				features.clear();
//				Imgproc.goodFeaturesToTrack(m, features, 50, 0.1, 5);
//				for(Point p:features){
//					Core.circle(m, p, 5, circleCol, 1);
//				}

//				Imgproc.cvtColor(m, dest, Imgproc.COLOR_YUV420sp2RGB,4);
//				Utils.matToBitmap(dest, mBitmap);
//				send_data_UDP(mBitmap);
				
				//If the data is not currently in use by the image processing, assign it to the latest frame.
				if(!inUse){
					datadata = data;
				}
			}else{
				datadata = null;
			}
		}
	}

	@Override
	public void run() {
		while(!STOP_THREAD){
			//byte[] data = dataQueue.take();
			if(datadata != null){
				inUse = true;
				m.put(0, 0, datadata);
				inUse = false;
				
				//***FEATURE FINDING***
				//if(features == null){
				//	features = new LinkedList<Point>();
				//}
				//features.clear();
				//Imgproc.goodFeaturesToTrack(m, features, 50, 0.1, 5);
				//for(Point p:features){
				//	Core.circle(m, p, 5, circleCol, 1);
				//}

				//Convert and send the image.
				Imgproc.cvtColor(m, dest, Imgproc.COLOR_YUV420sp2RGB,4);
				Utils.matToBitmap(dest, mBitmap);
				send_data_UDP(mBitmap);
			}else{
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}
}
