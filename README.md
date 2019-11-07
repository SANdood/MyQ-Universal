# MyQ-Universal
Port of @brbeaird's MyQ Lite 3.1.0 to run on both Hubitat & SmartThings, plus the addition of Acceleration/Three Axis Sensor support

### Change Log
#### 3.1.2bab 11/03/2019 
  * Don't mark door `open` or `opening` when Accelerations becomes `active` and the door is closed - wait for the Contact Sensor (or 3D Sensor) become `open` before marking the door `opening`/`open`. This should avoid cases where the wind (or something) shakes the door.
  * Reset all door attributes to `closed` if door goes `inactive` without Contact sensor becoming `open`
#### 11/07/2019
  * Added `HubConnect MyQ Garage Door Opener` custom driver to enable all attributes and commands to be replicated across multiple hubs using HubConnect. This is a universal Driver/DTH version that can be used on both Hubitat and SmartThings Remote Client Hubs. See HubConnect documentation for usage information.
***NOTE:*** *The Lock Door and No Sensor versions have been ported, but not tested. Please report issues to me via GitHub*

The optimal use of this driver is with some form of a Contact / Tilt Sensor plus an Activity (acceleration) Sensor, but there are multiple options with this new implementation:

* Use ONLY a Tilt Sensor (like the EcoLink), or a Contact Sensor with its Magnet by itself. You won't get "opening", "closing", or "waiting" status with this configuration, but you will know when the door is open and closed.
* Use a Tilt Sensor, plus a separate Activity (`acceleration`) sensor. With this setup (and each of the configuration below), you will get the full range of status updates:
   - "closed" --> "opening" --> "open" --> "waiting" --> "closing" --> "closed)
* Use any of the SmartThings/SmartSense Multipurpose sensors for BOTH of these purposes. To do this, you will need to 
   1. Configure the multisensor for use on a Garage Door - this is an option for all of the SmartThings/SmartSense drivers on both SmartThings and Hubitat.
     - If possible, turn OFF the `xyz` reporting, as it's not needed in this configuration
   2. Figure out which orientation to mount the multisensor on your garage door so that it reports "closed" when the sensor is vertical, and "open" when it is horizontal.
   3. Mount the sensor as high as possible so that it reports "open" as soon as possible, and "closed" after the door is fully closed.
   4. Configure the Sensor in MyQ Lite setup as the "Contact Sensor", and then select the option to use this same sensor as the "Activity Sensor"
    - You could also use a separate Activity Sensor, but its best to use the same sensor for both
* If you don't have a sensor that can be configured as a Contact or Tilt Sensor (`contact`), then you can still use MyQ Lite. You'll need a multi-sensor that reports Activity (`acceleration`) and 3D orientation (`threeAxis` *aka* `xyz`). 
   1. Ideally, you mount this sensor on your garage door with the long side on the right when the door is closed. 
   2. The software will figure out the orientation, and then report "open" and "closed" based on the changes in the axis that changes the most when the door opens and closes 
     - Note: this may require the door to open and close once or twice before it gets in sync, but usually it figures it out without that.
