{
  "startPoint": {
    "x": 24.98720930232558,
    "y": 125.01279069767443,
    "heading": "linear",
    "startDeg": 144,
    "endDeg": 144,
    "degrees": 144,
    "locked": false
  },
  "lines": [
    {
      "id": "asr-01-drive-to-shoot",
      "name": "Drive to shoot pose",
      "endPoint": {
        "x": 45,
        "y": 104,
        "heading": "linear",
        "reverse": false,
        "startDeg": 144,
        "endDeg": 144,
        "degrees": 144
      },
      "controlPoints": [],
      "color": "#D85975",
      "waitBeforeMs": 0,
      "waitAfterMs": 0,
      "waitBeforeName": "",
      "waitAfterName": ""
    },
    {
      "id": "asr-02-shoot-to-row1",
      "name": "Drive to row 1",
      "endPoint": {
        "x": 45.32906976744186,
        "y": 58.68662790697675,
        "heading": "linear",
        "reverse": false,
        "startDeg": 180,
        "endDeg": 180
      },
      "controlPoints": [],
      "color": "#D85975",
      "waitBeforeMs": 0,
      "waitAfterMs": 0,
      "waitBeforeName": "",
      "waitAfterName": ""
    },
    {
      "id": "asr-03-sweep-row1",
      "name": "Sweep row 1",
      "endPoint": {
        "x": 23.329069767441858,
        "y": 58.52209302325581,
        "heading": "linear",
        "reverse": false,
        "startDeg": 180,
        "endDeg": 180
      },
      "controlPoints": [],
      "color": "#D85975",
      "waitBeforeMs": 0,
      "waitAfterMs": 0,
      "waitBeforeName": "",
      "waitAfterName": ""
    },
    {
      "id": "asr-04-retreat-row1",
      "name": "Retreat from row 1",
      "endPoint": {
        "x": 23.329069767441858,
        "y": 70.69651162790697,
        "heading": "linear",
        "reverse": false,
        "startDeg": 180,
        "endDeg": 180
      },
      "controlPoints": [],
      "color": "#D85975",
      "waitBeforeMs": 0,
      "waitAfterMs": 0,
      "waitBeforeName": "",
      "waitAfterName": ""
    },
    {
      "id": "asr-05-collect-at-wall",
      "name": "Collect at wall",
      "endPoint": {
        "x": 18.83546511627907,
        "y": 70.69651162790699,
        "heading": "linear",
        "reverse": false,
        "startDeg": 180,
        "endDeg": 180
      },
      "controlPoints": [],
      "color": "#D85975",
      "waitBeforeMs": 0,
      "waitAfterMs": 500,
      "waitBeforeName": "",
      "waitAfterName": "Hold at wall"
    },
    {
      "id": "asr-06-retreat-wall",
      "name": "Retreat from wall",
      "endPoint": {
        "x": 23.16453488372093,
        "y": 70.86104651162789,
        "heading": "linear",
        "reverse": false,
        "startDeg": 180,
        "endDeg": 180
      },
      "controlPoints": [],
      "color": "#D85975",
      "waitBeforeMs": 0,
      "waitAfterMs": 0,
      "waitBeforeName": "",
      "waitAfterName": ""
    },
    {
      "id": "asr-07-return-seg1",
      "name": "Return to shoot (seg 1)",
      "endPoint": {
        "x": 45.164534883720926,
        "y": 70.69651162790699,
        "heading": "linear",
        "reverse": false,
        "startDeg": 180,
        "endDeg": 180
      },
      "controlPoints": [],
      "color": "#D85975",
      "waitBeforeMs": 0,
      "waitAfterMs": 0,
      "waitBeforeName": "",
      "waitAfterName": ""
    },
    {
      "id": "asr-08-return-seg2",
      "name": "Return to shoot (seg 2)",
      "endPoint": {
        "x": 45,
        "y": 104,
        "heading": "linear",
        "reverse": false,
        "startDeg": 180,
        "endDeg": 144
      },
      "controlPoints": [],
      "color": "#D85975",
      "waitBeforeMs": 0,
      "waitAfterMs": 0,
      "waitBeforeName": "",
      "waitAfterName": ""
    },
    {
      "id": "asr-09-shoot-to-row2",
      "name": "Drive to row 2",
      "endPoint": {
        "x": 45.16453488372094,
        "y": 82.68662790697674,
        "heading": "linear",
        "reverse": false,
        "startDeg": 180,
        "endDeg": 180
      },
      "controlPoints": [],
      "color": "#D85975",
      "waitBeforeMs": 0,
      "waitAfterMs": 0,
      "waitBeforeName": "",
      "waitAfterName": ""
    },
    {
      "id": "asr-10-sweep-row2",
      "name": "Sweep row 2",
      "endPoint": {
        "x": 23.493604651162787,
        "y": 82.52209302325582,
        "heading": "linear",
        "reverse": false,
        "startDeg": 180,
        "endDeg": 180
      },
      "controlPoints": [],
      "color": "#D85975",
      "waitBeforeMs": 0,
      "waitAfterMs": 0,
      "waitBeforeName": "",
      "waitAfterName": ""
    },
    {
      "id": "asr-11-park",
      "name": "Park",
      "endPoint": {
        "x": 54,
        "y": 124,
        "heading": "linear",
        "reverse": false,
        "startDeg": 180,
        "endDeg": 180
      },
      "controlPoints": [],
      "color": "#D85975",
      "waitBeforeMs": 0,
      "waitAfterMs": 0,
      "waitBeforeName": "",
      "waitAfterName": ""
    }
  ],
  "shapes": [],
  "sequence": [
    {
      "kind": "path",
      "lineId": "asr-01-drive-to-shoot"
    },
    {
      "kind": "wait",
      "id": "wait-flywheel-spinup",
      "name": "Flywheel spinup",
      "durationMs": 2000,
      "locked": false
    },
    {
      "kind": "wait",
      "id": "wait-fire-preloads",
      "name": "Fire preloads",
      "durationMs": 5000,
      "locked": false
    },
    {
      "kind": "wait",
      "id": "wait-turn-to-180-a",
      "name": "Turn to 180 deg",
      "durationMs": 500,
      "locked": false
    },
    {
      "kind": "path",
      "lineId": "asr-02-shoot-to-row1"
    },
    {
      "kind": "path",
      "lineId": "asr-03-sweep-row1"
    },
    {
      "kind": "path",
      "lineId": "asr-04-retreat-row1"
    },
    {
      "kind": "path",
      "lineId": "asr-05-collect-at-wall"
    },
    {
      "kind": "path",
      "lineId": "asr-06-retreat-wall"
    },
    {
      "kind": "path",
      "lineId": "asr-07-return-seg1"
    },
    {
      "kind": "path",
      "lineId": "asr-08-return-seg2"
    },
    {
      "kind": "wait",
      "id": "wait-turn-to-144",
      "name": "Turn to 144 deg",
      "durationMs": 500,
      "locked": false
    },
    {
      "kind": "wait",
      "id": "wait-fire-collected",
      "name": "Fire collected",
      "durationMs": 5000,
      "locked": false
    },
    {
      "kind": "wait",
      "id": "wait-turn-to-180-b",
      "name": "Turn to 180 deg",
      "durationMs": 500,
      "locked": false
    },
    {
      "kind": "path",
      "lineId": "asr-09-shoot-to-row2"
    },
    {
      "kind": "path",
      "lineId": "asr-10-sweep-row2"
    },
    {
      "kind": "path",
      "lineId": "asr-11-park"
    }
  ],
  "pathChains": [
    {
      "id": "chain-asr-main",
      "name": "Main Chain",
      "color": "#D85975",
      "lineIds": [
        "asr-01-drive-to-shoot",
        "asr-02-shoot-to-row1",
        "asr-03-sweep-row1",
        "asr-04-retreat-row1",
        "asr-05-collect-at-wall",
        "asr-06-retreat-wall",
        "asr-07-return-seg1",
        "asr-08-return-seg2",
        "asr-09-shoot-to-row2",
        "asr-10-sweep-row2",
        "asr-11-park"
      ]
    }
  ],
  "settings": {
    "xVelocity": 75,
    "yVelocity": 65,
    "aVelocity": 3.141592653589793,
    "kFriction": 0.1,
    "rWidth": 18,
    "rHeight": 18,
    "safetyMargin": 1,
    "maxVelocity": 40,
    "maxAcceleration": 30,
    "maxDeceleration": 30,
    "fieldMap": "decode.webp",
    "robotImage": "/robot.png",
    "theme": "auto",
    "showGhostPaths": false,
    "showOnionLayers": false,
    "onionLayerSpacing": 3,
    "onionColor": "#dc2626",
    "onionNextPointOnly": false,
    "showHeadingArrow": false,
    "headingArrowLength": 50,
    "headingArrowColor": "#ffffff",
    "headingArrowThickness": 2,
    "pathOpacity": 1
  },
  "version": "1.2.1",
  "timestamp": "2026-04-21T12:11:36.933Z"
}