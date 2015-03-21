# IOIOThread #

This is the class where the connection to the IOIO is established, its variables are set up, and the IOIO's main loop executes. The IOIOThread has no direct socket connection to the Server, thus is only available to be interacted with to the Drivers and the Threads. Since the IOIOThread is where the hardware interaction will take place, it is also where listeners are registered for hardware updates such as from the compass or the GPS.

## Talking to IOIOThread ##

IOIOThread uses a `byte[]` to set input flags, such as which buttons are pressed, what position the joysticks are in, and which direction they pushed. The length of this array is stored in Conts, as are the button positions, the left/right channel positions, and the left/right direction positions. No index in this array should be accessed with magic numbers.

IOIOThread exposes this input via an  [override method](http://code.google.com/p/androthumper/source/browse/trunk/androthumper/Android_CAR/src/android/ioio/car/threads/IOIO_Thread.java#166).

Other classes can also register instances of listeners for updates, such as the WaypointDriver can register a MyCompassListener and receive new compass values every tick of the IOIO.

## Connecting to the IOIO ##

The IOIO is managed in a private inner thread. This allows the IOIO to continue to run isolated from the main thread, and will allow it to pause/fail without bringing down the whole app.

An instance to the IOIO is created with the static method IOIOFactory.create(). Then, using ioio.waitForConnect(), the ioio thread will block until a connection to the IOIO has been established. At this point, the setup method is called, which opens the desired pins in desired modes, then the actual processing loop. All this is performed in try/catch blocks, so if at any point the IOIO becomes disconnected, the error is caught and the outer thread restarts (wait for connect, setup, etc).