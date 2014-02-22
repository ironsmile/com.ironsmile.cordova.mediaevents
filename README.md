Cordova Plugin for Media Events
=================

This cordova plugin gives you the ability to listen to media events from the platform.

Currently implemented events:

* **becomingnoisy** - Fired when your media playback is set to become too noisy. For example when the headphones are pulled out.

Events that will be implemented in the future:

* **focusgain** - when your application has received the audio focus. You can now use the audio freely.
* **focusloss** - when your app has lost the audio focus. Stop audio playback and cleanup all associated resourced.
* **focuslosstransient** - When your app has lost the audio focus for very short amount of time. Stop audio playback but you can keep media resourced loaded.
* **focuslosstransientcanduck** - You app has lost audio focus but it is acceptable for it to continue playing at lower volume.


Platforms
=================

* Android

For the moment I do not think to implement it for any other platform.


Install
=================

1. ```cordova plugin add https://github.com/ironsmile/com.ironsmile.cordova.mediaevents.git```

2. Edit your ```platforms/android/AndroidManifest.xml``` and add ```<action android:name="android.media.AUDIO_BECOMING_NOISY" />``` into the <intent-filter> tag.


Usage Example
=================

JavaScript

```
function onBecomingNoisy(event) {
    // Do something on becomenoisy event
}

var app = {

    initialize: function() {
        this.bindEvents();
    },

    bindEvents: function() {
        document.addEventListener('deviceready', this.onDeviceReady, false);
    },

    onDeviceReady: function() {
        app.bindPluginEvents();
    },

    bindPluginEvents: function() {
        window.addEventListener("becomingnoisy", onBecomingNoisy, false);
    }
};
```

HTML
```
<script type="text/javascript">
    app.initialize();
</script>
```


Credits
=================

Heavily inspired by the cordova [battery status plugin](https://git-wip-us.apache.org/repos/asf/cordova-plugin-battery-status.git).
