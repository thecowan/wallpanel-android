# homeDash
homeDash is an Android browser for displaying dashboards with MQTT integration 
and a motion sensor.

## Howto
After configuration press the little play button on the top right.

To return to the configuration screen pull down or up from the screen border and press back.

To close the background service swipe out the app from the Android task manager.


## Sensors
* Motion
* Pressure
* Light
* Battery / Charging

## MQTT Remote Control
topic `/homedash/command`

* Load URL
 `{"url":"http://someurl.org"}`
* Load URL and make it the new startup default
 `{"url":"http://someurl.org","save":"true"}`
* Run JavaScript in current page
`{"jsExec":"Alert('Hello World');"}`
* Screen wake up 
`{"wakeup":"true"}`
* Clear browser cache
`{"clearBrowserCache":"true"}`
* Reload the current page
`{"reload":"true"}`
* It's possible to send multiple commands together
`{"clearBrowserCache":"true","reload":"true","wakeup":"true"}`

## Home-Assistant configuration
```YAML
- platform: mqtt
    state_topic: "/homedash/sensor/battery"
    name: "homedash battery"
    unit_of_measurement: "%"
    value_template: '{{ value_json.value }}'

  - platform: mqtt
    state_topic: "/homedash/sensor/brightness"
    name: "homedash brightness"
    unit_of_measurement: "lx"
    value_template: '{{ value_json.value }}'

  - platform: mqtt
    state_topic: "/homedash/sensor/pressure"
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
    state_topic: "/homedash/sensor/motion"
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
