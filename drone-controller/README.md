# Drone Controller

This module is an interface for controlling of the drone. 
It contains the Drone interface for low level control of the drone (takeoff, land, pitch, yaw, roll, gaz, etc)
It also contains the Drone controller interface for higher level control of the drone ( fly to this coordinate)

It has no dependencies on the parrot sdk or on android.
The android-controller module implements the Drone interface in this module.
