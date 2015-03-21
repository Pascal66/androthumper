# Introduction #

[AndroThumper](http://code.google.com/p/androthumper/) is robotics chassis and hardware controller controlled via a [IOIO](http://proto-pic.co.uk/ioio-for-android-new/). It can use bluetooth zeemotes to drive, it can accept command packets from a server, and is planned to have RC override features. It can send back a camera feed to the server, along with sensor details from the accelerometer, GPS (location and status) and Compass.


### Robot Details ###
The robot is based on a [6WD Wild Thumper chassis](http://proto-pic.co.uk/dagu-wild-thumper-6wd-all-terrain-chassis-black-34-1/). It uses the  [Wild Thumper motor controller](http://proto-pic.co.uk/wild-thumper-controller-board/), talking to the IOIO via UART. Its powered by one or more 7.2v RC car batteries.

You can read more about the robot in the [hardware](http://code.google.com/p/androthumper/wiki/Hardware) section.

### Client Details ###
The client should run on any Android device. It sends packets to the server containing image slices, sensor information, and GPS/Location information. It receives packets from the server to control its features, such as enabling the camera feed, receiving GPS waypoints, and to send commands to the motor driver to make it move.

### Server Details ###
The server is written in Java using Swing for a GUI. It listens on ports 9000 to 9005. It can listen to controller event from XBox 360 controllers and forward this information to the phone to drive it. There is an Android server in the works, but development on this is not a priority, thus slow

It uses a mapping library from [Unfolding Maps](http://unfoldingmaps.org/) to provide an easy way to display routing and position information, and allows you to insert a start point and destination, and using [MapQuest](http://developer.mapquest.com/web/products/deprecated/java) APIs, will display a road route in waypoints

You can read more about the servers and clients in the [software](http://code.google.com/p/androthumper/wiki/Software) section.

<a href='Hidden comment: 
Private TODO:
NEED COMPASS.
implement sending a list of waypoints. Use codes for new_list [], insert_at [points][locations],restart,stop,pause,resume,go
write server methods to draw waypoints on map, perhaps allow creating route with mouse, click 2 points then again to insert, show current location on map, track phone over waypoints.
write "driver" threads for things like navigation/exploration/autonomy.
'></a>