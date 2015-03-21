# IOIO #

The IOIO is the device that makes this project come alive. While it would have been equally possible using an Arduino or something similar, using a IOIO allows me to use Java to program is processing loop, and lets me use all of the advantages of the Android operating system, such as internet access, accelerometer/GPS sensors, and camera.

Using Java for the program loop is much more powerful than using C in Arduino, proving access to any Java library that is compatible with Android.

My IOIO is on the latest bootloader (allowing Bluetooth connection options) and the latest application library.


---


## Connecting devices ##

  * **Wild Thumper Controller**:
    * The WTC is connected via TWI.
    * Pins:
      * _TWI MODULE_   : 2
      * _ADDRESS_      : 1


  * **Compass Module**
    * The compass is connected via UART, allowing sending and receiving on two wires.
    * Pins:
      * _RX_          : 9
      * _TX_          : 10
      * _BAUD_        : 9600
      * _PARITY_      : None
      * _STOP BITS_   : Two

  * **GPS Module**
    * he GPS is connected via UART, allowing sending and receiving on two wires.
    * Pins:
      * _RX_          : 3
      * _TX_          : 4
      * _BAUD_        : 9600
      * _PARITY_      : None
      * _STOP BITS_   : One


---


#### Nice things about the IOIO ####

Some nice things here

#### Bad things about the IOIO ####

Some bad things here