# Managers #

Keeping track of multiple Threads and letting them all interact with each other can be a little tricky. To make it easier, I am using Managers. This is similar to the Facade design pattern. Each Thread or driver holds an instance of its Manager, and it can use it to call methods in the other Drivers, or in threads via the driver manager's handle on the thread manager

Both of the Managers are only available from inside the [main activity](http://code.google.com/p/androthumper/source/browse/trunk/androthumper/Android_CAR/src/android/ioio/car/MainActivity.java), and are only initialised when the Toggle button [is pressed](http://code.google.com/p/androthumper/source/browse/trunk/androthumper/Android_CAR/src/android/ioio/car/MainActivity.java#166). When the toggle button is unchecked, The Managers tell all their instances to stop. If toggling the button again, the Managers tell their instances to restart to their default states.


## [Driver Manager](http://code.google.com/p/androthumper/wiki/Android_Client_Managers_Drivers) ##

The DriverManager holds all the instances of the Driver objects, such as the ZeemoteDriver and the WaypointDriver. The DriverManager also has a handle to the instance of the ThreadManager to control the IOIO and other things.

## [Thread Manager](http://code.google.com/p/androthumper/wiki/Android_Client_Managers_Thread) ##

The ThreadManager holds all the instances of the Threads, such as the [camera thread](http://code.google.com/p/androthumper/source/browse/trunk/androthumper/Android_CAR/src/android/ioio/car/threads/Cam_thread.java) and the [IOIO thread](http://code.google.com/p/androthumper/source/browse/trunk/androthumper/Android_CAR/src/android/ioio/car/threads/IOIO_Thread.java). It allows the threads to call methods in the other Threads, allowing functionality such as providing any Thread the ability to send a string back to the server via the [UtilsThread](http://code.google.com/p/androthumper/source/browse/trunk/androthumper/Android_CAR/src/android/ioio/car/threads/UtilsThread.java).