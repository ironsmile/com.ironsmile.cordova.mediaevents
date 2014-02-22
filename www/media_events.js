/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/

/**
* Shamelessly copied from batterystatus plugin at
* https://git-wip-us.apache.org/repos/asf/cordova-plugin-battery-status.git
*/

/**
 * This class is responsible for firing audio events such as
 * becomingnoisy, focusgain, focusloss, focuslosstransient, focuslosstransietcanduck
 * At the moment only becomingnoisy is implemented.
 * @constructor
 */
var cordova = require('cordova'),
    exec = require('cordova/exec');

function handlers() {
    return mediaevents.channels.becomingnoisy.numHandlers;
}

var MediaEvents = function() {

    // Create new event handlers on the window (returns a channel instance)
    this.channels = {
        becomingnoisy: cordova.addWindowEventHandler("becomingnoisy")
    };
    
    for (var key in this.channels) {
        this.channels[key].onHasSubscribersChange = MediaEvents.onHasSubscribersChange;
    }
};
/**
 * Event handlers for when callbacks get registered.
 * Keep track of how many handlers we have so we can start and stop the native listener
 * appropriately (and hopefully save on battery life!).
 */
MediaEvents.onHasSubscribersChange = function() {
    // If we just registered the first handler, make sure native listener is started.
    if (this.numHandlers === 1 && handlers() === 1) {
        exec(mediaevents._on_event, mediaevents._error, "MediaEvents", "start", []);
    } else if (handlers() === 0) {
        exec(null, null, "MediaEvents", "stop", []);
    }
};

/**
 * Fired on every event
 *
 * @param {Object} info            keys: type
 */
MediaEvents.prototype._on_event = function(info) {
    console.log("New event:");
    console.log(info);
    if (info.type) {
        cordova.fireWindowEvent(info.type, info);  
    };
};

/**
 * Error callback
 */
MediaEvents.prototype._error = function(e) {
    console.log("Error initializing MediaEvents: " + e);
};

var mediaevents = new MediaEvents();

module.exports = mediaevents;
