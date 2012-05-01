package constants;
public class Conts {
	/**Code for server alerting of driver error. */
	public static final byte UTILS_MESSAGE_TYPE_DRIVER_ERROR = 2;
	
	public static class Ports{
		/**Port on server to listen for camera packets. */
		public static final int CAMERA_INCOMMING_PORT = 9000;
		/**Port on server to listen for sensor packets. */
		public static final int SENSOR_INCOMING_PORT = 9001;
		/**Port on server to listen for utilities packets. */
		public static final int UTILS_INCOMMING_PORT = 9002;
		/**Port on server to listen for movement packets. */
		public static final int MOVE_INCOMMING_PORT = 9003;
		/**Port on server to listen for location packets. */
		public static final int GPS_INCOMMING_PORT = 9004;
	}
	
	public static class PacketSize{
		/**Size of camera packet header. */
		public static final int CAMERA_HEADER_SIZE = 5;
		/**Total size of camera packet. */
		public static final int CAMERA_PACKET_SIZE = 2000;
		/**Size of sensor packet. */
		public static final int SENSORS_PACKET_SIZE = 12;
		/**Size of utilities packet. First byte is code, rest is up to you. */
		public static final int UTILS_CONTROL_PACKET_SIZE = 2000;
		/**Size of the movement packet. 10 bytes for buttons,  1 byte for steering, 1 byte for gas/brake. One spare.*/
		public static final int MOVE_PACKET_SIZE = 12;
		/** CODE + 8 bytes for double lat, 8 bytes for double lng, 8 bytes for double altitude, 4 bytes for float speed, 4 bytes for float accuracy*/
		public static final int GPS_POSITION_PACKET_SIZE = 33;
		/** CODE + 6 sats * (4bytes for SNR, 1 byte for used in prn, 1byte for used (1 used, 0 not used)*/
		public static final int GPS_STATUS_PACKET_SIZE = 37;
		/**One for code, one for new driver. */
		public static final int CHANGE_DRIVER_PACKET_SIZE = 2;
	}
	
	public static class UtilsCodes{
		/**Code to enable location updates. */
		public static final byte ENABLE_GPS = 0;
		/**Code to disable location updates. */
		public static final byte DISABLE_GPS = 1;
		
		/**Code to enable GPS status updates. */
		public static final byte ENABLE_GPS_STATUS = 6;
		/**Code to disable GPS status updates. */
		public static final byte DISABLE_GPS_STATUS = 7;
		
		public static final byte SEND_GPS_WAYPOINTS = 8;
		
		/**Code to enable camera feed. */
		public static final byte ENABLE_CAM = 2;
		/**Code to disable camera feed. */
		public static final byte DISABLE_CAM = 3;
		
		/**Code to enable sensors. */
		public static final byte ENABLE_SENSORS = 4;
		/**Code to disable sensors. */
		public static final byte DISABLE_SENSORS = 5;
		
		/**Code using to define a location packet. */
		public static final int GPS_POSITION_CODE = 1;
		/**Code used to define a status packet. */
		public static final int GPS_STATUS_CODE = 0;
		
		public static final int COMPASS_DATA = 9;
		
		/**Code used to test the connection. Nothing should use this!. */
		public static final byte UTILS_CONNECTION_TEST = -1;
		/**Code used to tell the server the client has lost connection to the IOIO. */
		public static final byte LOST_IOIO_CONNECTION = -2;
		/**Code used to tell the server gained IOIO connection. */
		public static final byte GOT_IOIO_CONNECTION = -3;
		
		public static final byte CHANGE_DRIVER = -4;
		public static final byte BASIC_SERVER_DRIVER = -5;
		public static final byte WAYPOINT_DRIVER = -6;
	}
	
	public static class Controller{
		
		public static class Buttons{
			public static final int BUTTON_A = 0;
			public static final int BUTTON_B = 1;
			public static final int BUTTON_X = 2;
			public static final int BUTTON_Y = 3;
			public static final int BUTTON_START = 7;
			public static final int BUTTON_SEL = 6;
			public static final int BUTTON_RB = 5;
			public static final int BUTTON_LB = 4;
			public static final int BUTTON_LS = 8;
			public static final int BUTTON_RS = 9;
		}
		public static class Axis{
			
		}
	}
}
