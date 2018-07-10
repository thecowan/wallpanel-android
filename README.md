# WallPanel
WallPanel (Formerly HomeDash) is an Android browser for displaying web based dashboards which has 
other features that integrate into your home automation.

## Quick Start
Install the application to your device and run it. It will go straight to a default page just to
say it's working. To go/return to settings, swipe from the top or bottom of the screen and press
Back. To close the background services swipe out the app from the Android task manager.

## API
You can control the app remotely via MQTT or HTTP(REST). For a description of the API calls, visit
https://github.com/WallPanel-Project/wallpanel-api/blob/master/README.md

## Sensors
If MQTT is enabled and Sensor Reading Frequency is set, the app will post sensors per the API 
description. Currently only motion, pressure, light, and battery are posted.

### Home Assistant Examples
```YAML
sensor:
  - platform: mqtt
    state_topic: "wallpanel/mywallpanel/sensor/battery"
    name: "wallpanel battery"
    unit_of_measurement: "%"
    value_template: '{{ value_json.value }}'

  - platform: mqtt
    state_topic: "wallpanel/mywallpanel/sensor/brightness"
    name: "wallpanel brightness"
    unit_of_measurement: "lx"
    value_template: '{{ value_json.value }}'

  - platform: mqtt
    state_topic: "wallpanel/mywallpanel/sensor/pressure"
    name: "wallpanel pressure"
    unit_of_measurement: "mb"
    value_template: '{{ value_json.value }}'
```
```YAML
binary_sensor:
  - platform: mqtt
    state_topic: "wallpanel/mywallpanel/sensor/motion"
    name: "Motion"
    payload_on: '{"value":true}'
    payload_off: '{"value":false}'
    device_class: motion 
```
