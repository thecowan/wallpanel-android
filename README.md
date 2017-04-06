# HomeDash
HomeDash is an Android browser for displaying web based dashboards with MQTT integration and 
a motion sensor that can wake the screen.

## HowTo
Open the app. On the Welcome screen you can tap the graphic at the top to launch your 
dashboard. Configuration options are available on the main screen, as well as on the 
Motion Detector Settings and Other Homedash Settings subscreens.

To return to the Welcome screen pull down or up from the screen border and press Back

To close the background services swipe out the app from the Android task manager.

## Sensors
HomeDash can publish the following sensors from the device to MQTT:
* Motion
* Pressure
* Light
* Battery / Charging

## MQTT Remote Control
You can remotely control the dashboard by publishing to
topic `homedash/command` (the base topic can be changed, especially if you have more than one dashboard)

* Load a URL:
 `{"url":"http://someurl.org"}`
* Load a URL and make it the new launch default:
 `{"url":"http://someurl.org","save":"true"}`
* Run JavaScript in current page:
`{"jsExec":"Alert('Hello World');"}`
* Wake the screen up:
`{"wakeup":"true"}`
* Clear the browser cache:
`{"clearBrowserCache":"true"}`
* Reload the current page:
`{"reload":"true"}`

It's also possible to send multiple comamnds together:
* `{"clearBrowserCache":"true","reload":"true","wakeup":"true"}`

## HomeAssistant configuration
```YAML
- platform: mqtt
    state_topic: "homedash/sensor/battery"
    name: "homedash battery"
    unit_of_measurement: "%"
    value_template: '{{ value_json.value }}'

  - platform: mqtt
    state_topic: "homedash/sensor/brightness"
    name: "homedash brightness"
    unit_of_measurement: "lx"
    value_template: '{{ value_json.value }}'

  - platform: mqtt
    state_topic: "homedash/sensor/pressure"
    name: "homedash pressure"
    unit_of_measurement: "mb"
    value_template: '{{ value_json.value }}'

```

The motion sensor only sends a message on motion but doesn't reset. 
The motion sensor state can be reset with an automation. Since Home-Assistant doesn't 
provide a service to set a sensor state we use an REST API call.
```YAML
binary_sensor:
  - platform: mqtt
    state_topic: "homedash/sensor/motion"
    name: "Motion"
    payload_on: '{"sensor":"cameraMotionDetector","unit":"Boolean","value":"true"}'
    device_class: motion
 
// the ugly part: 
// reset the motion sensor
 - alias: Turn off motion detection
   trigger:
     platform: state
     entity_id: binary_sensor.motion
     to: 'on'
     for:
       seconds: 10
   action:
     service: rest_command.reset_motion_sensor
 
 rest_command:
   reset_motion_sensor:
     url: 'http://localhost:8123/api/states/binary_sensor.motion'
     payload: '{"state": "off" }'
     method: 'post'
```
