/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/

/**
* Shamelessly copied from batterystatus plugin at
* https://git-wip-us.apache.org/repos/asf/cordova-plugin-battery-status.git
*/

package com.ironsmile.cordova.mediaevents;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.media.AudioManager;


public class MediaEventListener extends CordovaPlugin {

    private static final String LOG_TAG = "MediaEvents";

    BroadcastReceiver receiver;

    private class FocusListener implements AudioManager.OnAudioFocusChangeListener {

        AudioManager audioManager;

        public FocusListener (Context ctx) {
            this.audioManager = (AudioManager)ctx.getSystemService(Context.AUDIO_SERVICE);
            int result = this.audioManager.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                sendFocusEvent(AudioManager.AUDIOFOCUS_LOSS);
            }
        }

        @Override
        public void onAudioFocusChange (int focusChange) {
            sendFocusEvent(focusChange);
        }

        public void abandonFocus() {
            if (this.audioManager != null) {
                this.audioManager.abandonAudioFocus(this);
                this.audioManager = null;                
            }
        }

        public void onDestroy() {
            abandonFocus();
        }
    }

    FocusListener focusListener;
    

    private CallbackContext eventCallbackContext = null;

    /**
     * Constructor.
     */
    public MediaEventListener() {
        this.receiver = null;
        this.focusListener = null;  
    }

    /**
     * Executes the request.
     *
     * @param action            The action to execute.
     * @param args              JSONArry of arguments for the plugin.
     * @param callbackContext   The callback context used when calling back into js
     * @return                  True if the action was valid, false if not.
     */
    public boolean execute(String action, JSONArray args, 
                            CallbackContext callbackContext) {
        if (action.equals("start")) {
            if (this.eventCallbackContext != null) {
                callbackContext.error("Media event listener already running.");
                return true;
            }
            this.eventCallbackContext = callbackContext;

            // We need to listen to audio events
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

            if (this.receiver == null) {
                this.receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        sendMediaEvent(intent);
                    }
                };
                cordova.getActivity().registerReceiver(this.receiver, intentFilter);
            }

            if (this.focusListener == null) {
                Context ctx = cordova.getActivity().getBaseContext();
                this.focusListener = this.new FocusListener(ctx);
            }

            // Don't return any result now, since status results will be sent when 
            // events come in from broadcast receiver
            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
            return true;
        }

        else if (action.equals("stop")) {
            removeMediaEventListener();
            // release status callback in JS side
            this.sendUpdate(new JSONObject(), false);
            this.eventCallbackContext = null;
            callbackContext.success();
            return true;
        }

        return false;
    }

    /**
     * Stop EventListener
     */
    public void onDestroy() {
        removeMediaEventListener();
    }

    /**
     * Stop EventListener
     */
    public void onReset() {
        removeMediaEventListener();
    }

    /**
     * Stop the event receiver and set it to null.
     */
    private void removeMediaEventListener() {
        if (this.receiver != null) {
            try {
                this.cordova.getActivity().unregisterReceiver(this.receiver);
                this.receiver = null;
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error unregistering media event receiver: " + 
                        e.getMessage(), e);
            }
        }
        if (this.focusListener != null) {
            this.focusListener.abandonFocus();
            this.focusListener = null;
        }
    }

    /**
     * Creates a JSONObject with the current event type
     *
     * @param eventMediaIntent the current audio event
     * @return a JSONObject containing the type of the event
     */
    private JSONObject constructMediaEvent(Intent eventMediaIntent) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("type", "becomingnoisy");
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
        return obj;
    }

    /**
     * Updates the JavaScript side whenever new event is received
     *
     * @param mediaEventIntent the current event information
     * @return
     */
    private void sendMediaEvent(Intent mediaEventIntent) {
        sendUpdate(this.constructMediaEvent(mediaEventIntent), true);
    }

    /**
     * Updates the JavaScript side with new audio focus event
     *
     * @param focusEvent the received event
     * @return
     */
    private void sendFocusEvent(int focusEvent) {
       JSONObject obj = new JSONObject();
        try {
            switch(focusEvent) {
                case AudioManager.AUDIOFOCUS_GAIN:
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE:
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                    obj.put("type", "audiofocusgain");
                break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    obj.put("type", "audiofocusloss");
                break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    obj.put("type", "audiofocuslosstransient");
                break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    obj.put("type", "audiofocuslosstransientcanduck");
                break;
            }
            
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
        sendUpdate(obj, true);
    }

    /**
     * Create a new plugin result and send it back to JavaScript
     *
     * @param connection the network info to set as navigator.connection
     */
    private void sendUpdate(JSONObject info, boolean keepCallback) {
        if (this.eventCallbackContext != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, info);
            result.setKeepCallback(keepCallback);
            this.eventCallbackContext.sendPluginResult(result);
        }
    }
}
