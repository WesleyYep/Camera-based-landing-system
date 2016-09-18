# Camera Based Landing System for Air Drones 
Designed for Part IV Project 105, University of Auckland, 2016  
Wesley Yep and Heying Cai

Structure
------------  
- DroneApplication folder consists of the Java Application's that run on board the drone and on the base station
- org.mavlink package contains the Mavlink generator libraries forked from https://github.com/ghelle/MAVLinkJava

Setup
------------  
- Setup a wireless hotspot from laptop or other device. This can be done either via command line (recommended) or through a mobile hotspot application
- Connect a monitor and keyboard/mouse to the raspberry pi. Connect to the hotspot that you created. Can edit the wpa_supplicant file to ensure raspberry pi always automatically connects to your hotspot on startup
- Make a note of the wlan ip address of the raspberry pi when connected to your hotspot
- Create new Java Project in Eclipse (or other IDE)
- Add the files from the DroneApplication folder into the root of the new project
- Fix build path errors by adding required libraries plus OpenCV

Image Processing System
------------
- Start up the wireless hotspot
- ssh into the raspberry pi using it’s wlan ip address (can use git bash cmd prompt if using windows)
- Change directory to ~/Downloads/jarForPi
- Run mavproxy onboard the drone, once it starts successfully you can stop it (ctrl-c). For some unknown reason this step is needed to get the HKPilot to begin sending mavlink messages
- Start the base station Java application (GroundStation.java)
- Run “java -jar mavlinktest.jar” from the raspberry pi
- You should see that the mode changes to “81.0” on the GUI. This means the connection is successful and messages are being received.
- Hold drone above the landing pattern and you should see the pattern location being detected. If not, try changing the range sliders on the GUI
- Tick the “Use big pattern” checkbox to use the full size 80cm square landing pattern. Otherwise don’t tick it for the half-size pattern

Autonomous Landing
------------
- Follow steps 1-8 of previous section
- Ensure “big pattern” checkbox is selected and “test mode” checkbox is not selected
- Use GUI to switch to stabilize mode. Press the arm button, the Pixhawk main LED should turn solid green (assuming GPS has got signal)
- Use radio controller to control the drone to the desired height above landing pattern
- Switch to loiter mode from the GUI, this should allow the drone to hover in once place
- Select the “test” checkbox to begin the automated landing process. Keep an eye on the drone and be ready to deselect the checkbox and use the remote controller if the drone has unexpected behaviour
- If all goes well, the drone should land onto the pattern

Drone Calibration
------------
- Download Mission Planner and open it
- Connect  HKpilot32 to laptop via USB cable
- Click the “connect” button on the top right to start connection
- Also you can connect wirelessly through the raspberry pi Wi-Fi as described in http://ardupilot.org/dev/docs/raspberry-pi-via-mavlink.html 
- After connection, go to “INITIAL_SETUP” from the top menu and then click on “Mandatory Hardware” from the left menu.
- Perform calibration and change parameters if needed
- When the drone can not balance well, calibrate sensors such as accelerometer and compass.
- When the motor or radio control can not perform well, calibrate radio receiver and motor.

Troubleshooting
------------

Drone flies around in a circle fast when switched to loiter mode
- GPS needs to stabilize. Leave drone flat without moving for 3 minutes before flying, LED light should be flashing green
  
Drone LED flashes yellow/red alternately
- EKF variance, so drone cannot switch to GPS dependent mode
- Restart drone
- If this doesn’t work, need to redo gyroscope and compass calibration on Mission Planner
  
Drone LED flashes yellow
- Low battery
- Can still work
- Just be careful and stop once rapid beeping occurs
  
Drone drifts in a particular direction as soon as it lifts off ground
- Adjust remote control offset sliders
- If that doesn’t work, try redo radio calibration on Mission Planner
- If that doesn’t work, try calibrate gyroscope and level calibration
  
Image processing detection doesn’t pick up the pattern
- Try adjusting the sliders for saturation and value on the GUI
  
Drone flips over as soon as started up
- Propellers may be on upside down
- Propellers may be inserted into the incorrect pins (or wrong order) on the HKPilot32
  
No data is being received to the GUI application (eg. current mode)
- Stop the application
- run Mavproxy on raspberry pi until it says the flight mode
- then rerun the base station app and drone app
  
Can’t connect to drone via ssh
- Wait a few minutes and try again
- Ensure Wi-Pi module light is blue
- Try connect monitor to the raspberry pi and check the correct ip address is being used
