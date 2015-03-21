# Android Client #

The Android Client is a heavily threaded application designed to interact with a connected IOIO, receive commands from bluetooth Zeemotes or a connected server, and act upon them or forward them to the IOIO.

[AndroThumper](http://code.google.com/p/androthumper/) can be driven around and controlled without an internet connection by connecting two Zeemotes. 

&lt;screenshot&gt;

 Each Zeemote then commands each side of motors with the joysticks.

## Managers ##

To manage the multiple threads and drivers in [AndroThumper](http://code.google.com/p/androthumper/) there is the concept of [Managers](http://code.google.com/p/androthumper/wiki/Android_Client_Managers). Each manager is responsible for holding instances of each of the objects it is managing, such as the [ThreadManager](http://code.google.com/p/androthumper/wiki/Android_Client_Managers_Thread) holds the [camera thread](http://code.google.com/p/androthumper/wiki/Android_Client_Thread_Camera), and the [DriverManager](http://code.google.com/p/androthumper/wiki/Android_Client_Managers_Drivers) holds the WaypointDriver. The [Managers](http://code.google.com/p/androthumper/wiki/Android_Client_Managers) provide a way for the Threads to call each others methods. The [DriverManager](http://code.google.com/p/androthumper/wiki/Android_Client_Managers_Drivers) has an instance of the [ThreadManager](http://code.google.com/p/androthumper/wiki/Android_Client_Managers_Thread), and allows the drivers to control the threads, for example, allowing the WaypointDriver to control the [IOIO Thread](http://code.google.com/p/androthumper/wiki/Android_Client_Thread_IOIO).

  * [Driver manager](http://code.google.com/p/androthumper/wiki/Android_Client_Managers_Drivers)
  * [Thread manager](http://code.google.com/p/androthumper/wiki/Android_Client_Managers_Thread)