/* ----------------------------------------------------------- *\
      <> IO pin definitions
\* ----------------------------------------------------------- */
#define enableArm 3
#define armLed    13
#define GripA     2
#define GripB     4
#define WristA    5
#define WristB    6
#define ElbowA    7
#define ElbowB    8
#define ShoulderA 11
#define ShoulderB 12
#define BaseA     14
#define BaseB     15

#define leftPWM   9
#define rightPWM  10
#define rForward  16
#define rReverse  17
#define lForward  18
#define lReverse  19

/* ----------------------------------------------------------- *\
      <> Command ID definitions
\* ----------------------------------------------------------- */
#define GripID     48
#define WristID    49
#define ElbowID    50
#define ShoulderID 51
#define BaseID     52
#define LedID      53

#define dirID_FL   54
#define dirID_FR   55
#define dirID_RL   56
#define dirID_RR   57
#define dirID_SL   58
#define dirID_SR   59
#define dirID_FS   60
#define timer_ID   61
#define timerCmd   36

/* ----------------------------------------------------------- *\
      <> Misc Constants
\* ----------------------------------------------------------- */
#define maxSpd     250
#define minSpd     0
#define pwmCutoff  50
#define tmrEnabled 1
#define shutdownDelay 200

/* ----------------------------------------------------------- *\
      <> Globals
\* ----------------------------------------------------------- */
const char token = '#';
const boolean debugMode = false;
char val;
byte data[2];
boolean collectData = false, enabled = false, ledState = false;
int loopCount = 0, tmpPinA, tmpPinB;
unsigned long timeNow, timeStamp;

/* ----------------------------------------------------------- *\
      <> setup
\* ----------------------------------------------------------- */
void setup() {
  for(int i = 2; i < 20; i++) {
    pinMode(i, OUTPUT); 
  }
  Serial.begin(57600);
}

/* ----------------------------------------------------------- *\
      <> main loop
\* ----------------------------------------------------------- */
void loop() {
  
  if(Serial.available() > 0) {
    val = Serial.read();
    if(collectData) {
      data[loopCount] = val;
      loopCount++;
      if (loopCount >= 2) {
        processCommand(data[0], data[1]);
        data[0] = data[1] = 0;
        collectData = false;
      }
    }
    else if (val == token) {
      loopCount = 0;
      collectData = true;
    }
  }
  
  // Handle watchdog timeout event
  if(tmrEnabled && enabled) {
    timeNow = millis();
    if ((timeNow - timeStamp) >= shutdownDelay) {
      shutdown();
    }
  }
  
}
/* ----------------------------------------------------------- *\
      <> shutdown
\* ----------------------------------------------------------- */
void shutdown() {
  for(int i = 2; i < 20; i++) {
    digitalWrite(i, LOW);
  }
  enabled = false;
}

/* ----------------------------------------------------------- *\
      <> Execute command received from the bluetooth channel
\* ----------------------------------------------------------- */
void processCommand(byte id, byte cmd) {
  
  if(debugMode) {
    Serial.print("id: ");
    Serial.print(id);
    Serial.print(", cmd: ");
    Serial.println(cmd);
  }
  if (id <= BaseID && id >= GripID)  {
    switch (id) {
      case GripID:
        tmpPinA = GripA; tmpPinB = GripB;
        break;
      case WristID:
        tmpPinA = WristA; tmpPinB = WristB;
        break;
      case ElbowID:
        tmpPinA = ElbowA; tmpPinB = ElbowB;
        break;
      case ShoulderID:
        tmpPinA = ShoulderA; tmpPinB = ShoulderB;
        break;
      case BaseID:
        tmpPinA = BaseA; tmpPinB = BaseB;
        break;
    } 
    switch(cmd) {
      case 'f':
        digitalWrite(tmpPinA, HIGH);
        digitalWrite(tmpPinB, LOW);
        break;
      case 'r':
        digitalWrite(tmpPinA, LOW);
        digitalWrite(tmpPinB, HIGH);
        break;
      case 's':
        digitalWrite(tmpPinA, LOW);
        digitalWrite(tmpPinB, LOW);
        break;
    }
  } 
  else if (id == LedID) {
    switch (cmd) {
      case 'h':
        digitalWrite(armLed, HIGH);
        ledState = true;
        break;
      case 'l':
        digitalWrite(armLed, LOW);
        ledState = false;
        break;
    }
  }
  else if (id <= dirID_FS && id >= dirID_FL) {
    if (!enabled) {
      cmd = 0; 
    }
    else if (cmd <= pwmCutoff) { 
      cmd = minSpd;
    }
    switch (id) {
      case dirID_FL: case dirID_RL:
        analogWrite(leftPWM, constrain(cmd, minSpd, maxSpd));
        break;
      case dirID_FR: case dirID_RR:
        analogWrite(rightPWM, constrain(cmd, minSpd, maxSpd));
        break;
      case dirID_SL:
        digitalWrite(leftPWM, LOW);
        break;
      case dirID_SR:
        digitalWrite(rightPWM, LOW);
        break;
      case dirID_FS:
        digitalWrite(leftPWM, LOW);
        digitalWrite(rightPWM, LOW);
        break;
    }
    switch (id) {
      case dirID_FL:
        digitalWrite(lForward, HIGH);
        digitalWrite(lReverse, LOW);
        break;
      case dirID_FR:
        digitalWrite(rForward, HIGH);
        digitalWrite(rReverse, LOW);
        break;
      case dirID_RL:
        digitalWrite(lReverse, HIGH);
        digitalWrite(lForward, LOW);
        break;
      case dirID_RR:
        digitalWrite(rReverse, HIGH);
        digitalWrite(rForward, LOW);
        break;
      case dirID_SL:
        digitalWrite(lForward, LOW);
        digitalWrite(lReverse, LOW);
        break;
      case dirID_SR:
        digitalWrite(rReverse, LOW);
        digitalWrite(rForward, LOW);
        break;
      case dirID_FS:
        digitalWrite(lForward, LOW);
        digitalWrite(lReverse, LOW);
        digitalWrite(rReverse, LOW);
        digitalWrite(rForward, LOW);
        break;
    }
  }

  // Watchdog timer update command
  else if (id == timer_ID && cmd == timerCmd) {
    timeStamp = millis();
    if(!enabled) {
      digitalWrite(enableArm, HIGH);
      if(ledState) {
        digitalWrite(armLed, HIGH);
      }
      enabled = true;
    }
  }
}
