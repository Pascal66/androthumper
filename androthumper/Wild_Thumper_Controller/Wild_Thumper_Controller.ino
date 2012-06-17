#include <Wire.h>
#include <Servo.h>

#include "IOpins.h"
#include "Constants.h"


//-------------------------------------------------------------- define global variables --------------------------------------------

unsigned short Volts;
unsigned int LeftAmps;
unsigned int RightAmps;
unsigned long chargeTimer;
unsigned long leftOverloadTime;
unsigned long rightOverloadTime;
boolean shoveLeft = true;
boolean shoveRight = true;
int leftShoveCount = 0;
int rightShoveCount = 0;
byte requestCode,recieveCode;
int highVolts;
int startVolts;
int Leftspeed=0;
int Rightspeed=0;
int Speed;
int Steer;
byte Charged=BATT_CHARGED;                                               // 0=Flat battery  1=Charged battery
int Leftmode=1;                                               // 0=reverse, 1=brake, 2=forward
int Rightmode=1;                                              // 0=reverse, 1=brake, 2=forward
byte Leftmodechange=0;                                        // Left input must be 1500 before brake or reverse can occur
byte Rightmodechange=0;                                       // Right input must be 1500 before brake or reverse can occur
int LeftPWM;                                                  // PWM value for left  motor speed / brake
int RightPWM;                                                 // PWM value for right motor speed / brake
int leftI2CSpeed;
int leftI2CMode;
int rightI2CSpeed;
int rightI2CMode;
int leftRcSpeed;
int leftRcMode;
int rightRcSpeed;
int rightRcMode;
int LastReadLeftPWM;
int LastReadRightPWM;
int data;
int newi2c = 0;
int servo[7];

byte BATT_LOW_MSG[2] = {-1,-1};

//-------------------------------------------------------------- define servos ------------------------------------------------------


Servo Servo0;                                                 // define servos
Servo Servo1;                                                 // define servos
Servo Servo2;                                                 // define servos
Servo Servo3;                                                 // define servos
Servo Servo4;                                                 // define servos
Servo Servo5;                                                 // define servos
Servo Servo6;                                                 // define servos

void setup()
{
  //------------------------------------------------------------ Initialize Servos ----------------------------------------------------

//  Servo0.attach(S0);                                          // attach servo to I/O pin
//  Servo1.attach(S1);                                          // attach servo to I/O pin
//  Servo2.attach(S2);                                          // attach servo to I/O pin
//  Servo3.attach(S3);                                          // attach servo to I/O pin
//  Servo4.attach(S4);                                          // attach servo to I/O pin
//  Servo5.attach(S5);                                          // attach servo to I/O pin
//  Servo6.attach(S6);                                          // attach servo to I/O pin

  //------------------------------------------------------------ Set servos to default position ---------------------------------------

//  Servo0.writeMicroseconds(DServo0);                          // set servo to default position
//  Servo1.writeMicroseconds(DServo1);                          // set servo to default position
//  Servo2.writeMicroseconds(DServo2);                          // set servo to default position
//  Servo3.writeMicroseconds(DServo3);                          // set servo to default position
//  Servo4.writeMicroseconds(DServo4);                          // set servo to default position
//  Servo5.writeMicroseconds(DServo5);                          // set servo to default position
//  Servo6.writeMicroseconds(DServo6);                          // set servo to default position

  //------------------------------------------------------------ Initialize I/O pins --------------------------------------------------

  pinMode (Charger,OUTPUT);                                   // change Charger pin to output
  digitalWrite (Charger,CHARGER_DISABLE);                                   // disable current regulator to charge battery
  Serial.begin(9600);
  if (Cmode==1){
    Serial.begin(Brate);                                      // enable serial communications if Cmode=1
    Serial.flush();                                           // flush buffer
  }else if(Cmode == 2){        //I2C
    digitalWrite(I2Cdata,HIGH);
    digitalWrite(I2Cclk,HIGH);
    Wire.onReceive(HandleOnRecieve);
    Wire.onRequest(HandleOnRequest);
    Wire.begin(1);
    Serial.println("Started I2C, internal pull-ups, registered recievers, slaved address: 1");
  }

}


void loop(){
  //------------------------------------------------------------ Check battery voltage and current draw of motors ---------------------

  Volts=analogRead(Battery);                                  // read the battery voltage
  LeftAmps=analogRead(LmotorC);                               // read left motor current draw
  RightAmps=analogRead(RmotorC);                              // read right motor current draw

  //Serial.print(LeftAmps);
  //Serial.print("    ");
  //Serial.println(RightAmps);

  if (LeftAmps>LEFT_MAX_AMPS){                                 // is motor current draw exceeding safe limit
    analogWrite (LmotorA,0);                                  // turn off motors
    analogWrite (LmotorB,0);                                  // turn off motors
    leftOverloadTime=millis();                                // record time of overload
    //Serial.println("left overload");
  }

  if (RightAmps>RIGHT_MAX_AMPS){                              // is motor current draw exceeding safe limit 
    analogWrite (RmotorA,0);                                  // turn off motors
    analogWrite (RmotorB,0);                                  // turn off motors
    rightOverloadTime=millis();                                   // record time of overload
    //Serial.println("right overload");
  }

//  TODO
//  drawing a lot of current causes voltage to drop, making driver think battery is dead.
//  modify to either lower the low battery voltage, or make it so that if the current draw
//  is big, ignore this. Or, write a serial command to enter charging mode, ignoring any 
//  low voltage untill the IOIO drops off the phone, notifying the server.
  
  if ((Volts<LOW_BATT_VOLTAGE) && ((Charged==BATT_LOW) && (BATT_AUTO_CHECK==1))){                         // check condition of the battery
    enableCharge();
  }else{

  //------------------------------------------------------------ CHARGE BATTERY -------------------------------------------------------

  if (Charged==BATT_LOW){                 // if battery is flat?
    checkForInput();
    checkCharge();
  }else{//----------------------------------------------------------- GOOD BATTERY speed controller opperates normally ----------------------
    checkForInput();

    // --------------------------------------------------------- Code to drive dual "H" bridges --------------------------------------
    doMove();
  }
  }
}

void checkCharge(){
  if((Volts < LOW_BATT_VOLTAGE)){
    if(Volts-startVolts > 33){            //has charger been connected (voltage increaased by at least 0.5V)?
      if (Volts>highVolts){               // has battery voltage increased?
        highVolts=Volts;                  // record the highest voltage. Used to detect peak charging.
        chargeTimer=millis();             // when voltage increases record the time
      }
    }

    if (Volts>PEAK_BATT_VOLTAGE){                                          // battery voltage must be higher than this before peak charging can occur.
      if ((highVolts-Volts)>5 || (millis()-chargeTimer)>chargetimeout){     // has voltage begun to drop or levelled out?
        Serial.println("charged");
        Charged=BATT_CHARGED;                                            // battery voltage has peaked
        digitalWrite (Charger,CHARGER_DISABLE);                             // turn off current regulator
      }
    }
  }else{
    Serial.println("charged");
    Charged=BATT_CHARGED;                                            // battery voltage has peaked
    digitalWrite (Charger,CHARGER_DISABLE);                             // turn off current regulator
  } 
}

void enableCharge(){
  Serial.println("enable charge");
    // change battery status from charged to flat

    //---------------------------------------------------------- FLAT BATTERY speed controller shuts down until battery is recharged ----
    //---------------------------------------------------------- This is a safety feature to prevent malfunction at low voltages!! ------
    Charged=BATT_LOW;                                         // battery is flat
    highVolts=Volts;                                          // record the voltage
    startVolts=Volts;
    chargeTimer=millis();                                     // record the time

    digitalWrite (Charger,CHARGER_ENABLE);                    // enable current regulator to charge battery
}

void checkForInput(){
   switch(Cmode){
    case 0:                                                   // RC mode via D0 and D1
      RCmode();
      break;
    case 1:                                                       // Serial mode via D0(RX) and D1(TX)
      SCmode();
      break;
    case 2:                                                   // I2C mode via A4(SDA) and A5(SCL)
      I2Cmode();
      break;
    }
}

void doMove(){
      if (Charged==BATT_CHARGED)                                           // Only power motors if battery voltage is good
    {
      checkOverride();
      if ((millis()-leftOverloadTime)>OVERLOAD_PAUSE_TIME_MILLIS )             
      {
//        if(shoveLeft && leftShoveCount < 5){
//          LeftPWM = 80;
//          leftShoveCount++;
//        }else{
//          leftShoveCount=0;
//          shoveLeft = false;
//          LeftPWM = LastReadLeftPWM;
//        }
        switch (Leftmode)                                     // if left motor has not overloaded recently
        {
        case 2:                                               // left motor forward
          analogWrite(LmotorA,0);
          analogWrite(LmotorB,LeftPWM);
          break;

        case 1:                                               // left motor brake
          analogWrite(LmotorA,LeftPWM);
          analogWrite(LmotorB,LeftPWM);
          break;

        case 0:                                               // left motor reverse
          analogWrite(LmotorA,LeftPWM);
          analogWrite(LmotorB,0);
          break;
        }
      }else{
        //Serial.println("Left current spike!");
      }
      if ((millis()-rightOverloadTime)>OVERLOAD_PAUSE_TIME_MILLIS )
      {
//        if(shoveRight && rightShoveCount < 5){
//          RightPWM = 80;
//          rightShoveCount++;
//        }else{
//          rightShoveCount=0;
//          shoveRight = false;
//          RightPWM = LastReadRightPWM;
//        }
        switch (Rightmode)                                    // if right motor has not overloaded recently
        {
        case 2:                                               // right motor forward
          analogWrite(RmotorA,0);
          analogWrite(RmotorB,RightPWM);
          break;

        case 1:                                               // right motor brake
          analogWrite(RmotorA,RightPWM);
          analogWrite(RmotorB,RightPWM);
          break;

        case 0:                                               // right motor reverse
          analogWrite(RmotorA,RightPWM);
          analogWrite(RmotorB,0);
          break;
        }
      }else{
       //Serial.println("Right current spike!"); 
      }
    }
    else                                                      // Battery is flat
    {
      //Serial.println("Battery low, stopping all motors!");
      analogWrite (LmotorA,0);                                // turn off motors
      analogWrite (LmotorB,0);                                // turn off motors
      analogWrite (RmotorA,0);                                // turn off motors
      analogWrite (RmotorB,0);                                // turn off motors
    }
}

void RCmode()
{
  //------------------------------------------------------------ Code for RC inputs ---------------------------------------------------------

  Speed=pulseIn(RCleft,HIGH,25000);                           // read throttle/left stick
  Steer=pulseIn(RCright,HIGH,25000);                          // read steering/right stick


  if (Speed==0) Speed=1500;                                   // if pulseIn times out (25mS) then set speed to stop
  if (Steer==0) Steer=1500;                                   // if pulseIn times out (25mS) then set steer to centre

  if (abs(Speed-1500)<RCdeadband) Speed=1500;                 // if Speed input is within deadband set to 1500 (1500uS=center position for most servos)
  if (abs(Steer-1500)<RCdeadband) Steer=1500;                 // if Steer input is within deadband set to 1500 (1500uS=center position for most servos)

  if (Mix==1)                                                 // Mixes speed and steering signals
  {
    Steer=Steer-1500;
    Leftspeed=Speed-Steer;
    Rightspeed=Speed+Steer;
  }
  else                                                        // Individual stick control
  {
    Leftspeed=Speed;
    Rightspeed=Steer;
  }
  /*
  Serial.print("Left:");
  Serial.print(Leftspeed);
  Serial.print(" -- Right:");
  Serial.println(Rightspeed);
  */
  Leftmode=2;
  Rightmode=2;
  if (Leftspeed>(Leftcenter+RCdeadband)) Leftmode=0;          // if left input is forward then set left mode to forward
  if (Rightspeed>(Rightcenter+RCdeadband)) Rightmode=0;       // if right input is forward then set right mode to forward

  LeftPWM=abs(Leftspeed-Leftcenter)*10/scale;                 // scale 1000-2000uS to 0-255
  LeftPWM=min(LeftPWM,255);                                   // set maximum limit 255

  RightPWM=abs(Rightspeed-Rightcenter)*10/scale;              // scale 1000-2000uS to 0-255
  RightPWM=min(RightPWM,255);                                 // set maximum limit 255
}

// ------------------------------------------------------------ Code for Serial Communications --------------------------------------
void SCmode(){

 // FL = flush serial buffer
 // AN = report Analog inputs 1-5
 // SV = next 7 integers will be position information for servos 0-6
 // BL = write battery level out serial
 // HB = "H" bridge data - next 4 bytes will be:
 //      left  motor mode 0-2
 //      left  motor PWM  0-255
 //      right motor mode 0-2
 //      right motor PWM  0-255
   
 
  if (Serial.available()>1){                               // command available
    int A=Serial.read();
    int B=Serial.read();
    int command=A*256+B; 
    switch (command){
      case 18243:                                            //GC - enable charge mode.
        enableCharge();
        break;
      case 21315:                                            //SC - stop charge
        checkCharge();
        break;
      case 16972:                                            //BL - write battery level to output serial
        //Serial.println(highByte(Volts));
        //Serial.println(lowByte(Volts));
        Serial.write(highByte(Volts));
        Serial.write(lowByte(Volts));
        break;
      case 17996:                                             // FL (17996 - 76)/256=70
        Serial.flush();                                       // flush buffer
        break;
        
      case 16718:                                             // AN - return values of analog inputs 1-5
        for (int i=1;i<6;i++)                                 // index analog inputs 1-5
        {
          data=analogRead(i);                                 // read 10bit analog input 
          Serial.write(highByte(data));                       // transmit high byte
          Serial.write(lowByte(data));                        // transmit low byte
        }
        break;
              
       case 21334:                                            // SV - receive postion information for servos 0-6
         for (int i=0;i<15;i++)                               // read 14 bytes of data
         {
           Serialread();                                      
           servo[i]=data;
         }
         Servo0.writeMicroseconds(servo[0]*256+servo[1]);     // set servo position
         Servo1.writeMicroseconds(servo[2]*256+servo[3]);     // set servo position
         Servo2.writeMicroseconds(servo[4]*256+servo[5]);     // set servo position
         Servo3.writeMicroseconds(servo[6]*256+servo[7]);     // set servo position
         Servo4.writeMicroseconds(servo[8]*256+servo[9]);     // set servo position
         Servo5.writeMicroseconds(servo[10]*256+servo[11]);   // set servo position
         Servo6.writeMicroseconds(servo[12]*256+servo[13]);   // set servo position
         break;
       
       case 18498:                                            // HB - mode and PWM data for left and right motors
       
         if(Charged==BATT_LOW || Volts<LOW_BATT_VOLTAGE){
           Serial.println("Battery is low!");
           Serial.write(BATT_LOW_MSG,2);
//           Charged=BATT_LOW;
//           analogWrite (LmotorA,0);                                // turn off motors
//           analogWrite (LmotorB,0);                                // turn off motors
//           analogWrite (RmotorA,0);                                // turn off motors
//           analogWrite (RmotorB,0);                                // turn off motors
         }else{
           Serialread();
           Leftmode=data;
           //Serial.print("left mode: ");
           //Serial.println(data);
           Serialread();
         
           if(data < 80 && LeftPWM < 10 && data > LeftPWM){
             shoveLeft = true; 
           }

           LeftPWM=data;
           LastReadLeftPWM = data;
           Serialread();
           Rightmode=data;
           Serialread();
         
           if(data < 80 && RightPWM < 10 && data > RightPWM){
              shoveRight = true; 
           }
           RightPWM=data;
           LastReadRightPWM = data;
           //Serial.println("done reading data");
         } 
         break;
         
       default:                                                // invalid command
         Serial.flush();                                       // flush buffer
    }
  }
}

void Serialread() {//---------------------------------------------------------- Read serial port until data has been received -----------------------------------
  do{
    data=Serial.read();
  } while (data<0);
}

void I2Cmode(){//----------------------------------------------------------- Your code goes here ------------------------------------------------------------
//  Wire.onReceive(HandleOnRecieve);
//  Wire.onRequest(HandleOnRequest);
//  Wire.begin(1);
}

void checkOverride(){
  int override = pulseIn(RCOverride,HIGH,25000);

  if(override > 1800){
    //Serial.println("using Mode2");
    //steering = ch2 (A2)
    //speed = ch3 (D0)
    
    int steer = pulseIn(RCInput3,HIGH,25000);
    int sped = pulseIn(RCInput1,HIGH,25000);
    
    if (abs(steer-1500)<RCdeadband) steer=1500;                
    if (abs(sped-1500)<RCdeadband) sped=1500;   
    
    leftRcMode = rightRcMode = 2;
    
    if(sped < (Leftcenter+RCdeadband)){
      leftRcMode = rightRcMode = 0;
    }
    
    sped = abs(sped-Leftcenter)*10/scale;
    sped = min(sped,255);
    leftRcSpeed = rightRcSpeed = sped;
    if(sped < 20){
      //rotate
      if(steer < (Leftcenter+RCdeadband)){
        //right
        rightRcMode = 0;
        leftRcMode = 2;
      }else{
        //left
        leftRcMode = 0;
        rightRcMode = 2;
      }
      rightRcSpeed = leftRcSpeed = (abs(steer-Leftcenter)*10/scale);
    }else{
      if(steer < (Leftcenter+RCdeadband)){
        //right
        rightRcSpeed = sped - (abs(steer-Leftcenter)*10/scale);
        rightRcSpeed = max(rightRcSpeed,0);
      }else{
        //left
        leftRcSpeed = sped - (abs(steer-Leftcenter)*10/scale);
        leftRcSpeed = max(leftRcSpeed,0);
      }
    }
    
    //Serial.print("RM: ");Serial.print(rightRcMode);Serial.print(" RS: ");Serial.print(rightRcSpeed);Serial.print(" LM: ");Serial.print(leftRcMode);Serial.print(" LS: ");Serial.print(leftRcSpeed);Serial.print(" steer: ");Serial.print(steer);Serial.print(" speed: ");Serial.println(sped);
    Rightmode = rightRcMode;
    Leftmode = leftRcMode;
    LeftPWM = leftRcSpeed;
    RightPWM = rightRcSpeed;
  }else if(override > 1400){
    //Serial.println("Using Mode1");
    //right = ch1 (D1)
    //left = ch3 (D0)
    int left =  pulseIn(RCInput1,HIGH,25000);
    int right = pulseIn(RCInput2,HIGH,25000);
    
    leftRcMode=2;
    rightRcMode=2;
    if (left<(Leftcenter+RCdeadband)) leftRcMode=0;          // if left input is forward then set left mode to forward
    if (right<(Rightcenter+RCdeadband)) rightRcMode=0;       // if right input is forward then set right mode to forward

    leftRcSpeed=abs(left-Leftcenter)*10/scale;                 // scale 1000-2000uS to 0-255
    leftRcSpeed=min(leftRcSpeed,255);                                   // set maximum limit 255

    rightRcSpeed=abs(right-Rightcenter)*10/scale;              // scale 1000-2000uS to 0-255
    rightRcSpeed=min(rightRcSpeed,255);                                 // set maximum limit 255
    
    //Serial.print("left: ");Serial.print(left);Serial.print(" right: ");Serial.println(right);
    //Serial.print("RM: ");Serial.print(rightRcMode);Serial.print(" RS: ");Serial.print(rightRcSpeed);Serial.print(" LM: ");Serial.print(leftRcMode);Serial.print(" LS: ");Serial.println(leftRcSpeed);
    RightPWM = rightRcSpeed;
    LeftPWM = leftRcSpeed;
    Leftmode = leftRcMode;
    Rightmode = rightRcMode;
  }else{
    //Serial.println("Using I2C");
    if(newi2c == 1){
      Serial.print("new I2C: ");Serial.print(leftI2CMode);Serial.print(" , ");Serial.print(leftI2CSpeed);Serial.print(" , ");Serial.print(rightI2CMode);Serial.print(" , ");Serial.println(rightI2CSpeed);
    }
    LeftPWM = leftI2CSpeed;
    Leftmode = leftI2CMode;
    RightPWM = rightI2CSpeed;
    Rightmode = rightI2CMode;
    newi2c = 0;
    //Serial.println("Got I2C commands");
  }
  
}

//This is when the master has addressed this controller as a slave, and has send 'bytes' amount of data. First byte is always a command code.
void HandleOnRecieve(int bytes){
  recieveCode = Wire.read();
  //Serial.println("Recieved I2C data");
  switch(recieveCode){
   case Hello:
     //Master has said hello...
     break; 
   case MotorComms:
     //Serial.println("Recieved I2C motor commands");
     //Recieved new data about the motors
     leftI2CMode = Wire.read();
     leftI2CSpeed = Wire.read();
     rightI2CMode = Wire.read();
     rightI2CSpeed = Wire.read();
     newi2c = 1;
     //Serial.print(Leftmode);Serial.print(" , ");Serial.print(LeftPWM);Serial.print(" , ");Serial.print(Rightmode);Serial.print(" , ");Serial.println(RightPWM);
     //doMove();
     break;
  }
  
  if(requestCode == Hello){
    //Master says hello.. but thats it.
  }else{
    for(int i = 1; i < bytes; i++){
      byte sentByte = Wire.read();
    }
  }
}

//This is when the master has requested some data from this controller which is set up as a slave.
void HandleOnRequest(){
  switch(requestCode){
   case Hello:
     //Master has said hello... lets say something back
       byte b[4];
       b[0] = 1;
       b[1] = 2;
       b[2] = 3;
       b[3] = 4;
       Wire.write(b,4);
    break; 
  }
}  











