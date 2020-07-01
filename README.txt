application for android to control the Cosmic Ray Facility measurements with SM2 Micromegas Modules

using the build.sh script the application can be installed 
("I" has to be used as option for installation)

therefore in the parent directory android-sdk has to be installed 
(or the ANDROID_HOME path has to be set properly)

options in the application:

    choose an operation :
        - status  : checks status of measurements and of slowcontrol
        - reset   : restarts the slowcontrol
        - anode   : sets the anodes of the module to the specified voltage
        - cathode : sets the cathodes of the module to the specified voltage
        - start   : initiates a new measurement using the name tag specified
        - stop    : ends the current measurement
        
    username and password of the measurement-group-user has to be provided for all operations
    
    anode voltages should be 525 , 540 , 560 , 570 , 580 (default)
    cathode voltage is default at 300
    measurement tags have the form : m<modeNumber>_<anodeVoltage>V
    
    only if activated the operation can be sent
    
main source-code resides in : 
                                        src/com/CRFcontrol/MainActivity.java
xml-file for layout can be found in : 
                                        res/layout/activity_main.xml
for network communication external library is needed :
                                        libs/jsch-0.1.55.jar
