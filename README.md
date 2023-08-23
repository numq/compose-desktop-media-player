## About

This repository includes various media player implementation methods that can be useful if you want to add multimedia
components to your Jetpack Compose project.

### AWT component

The standard way to create a media player using the media player library component and container for the AWT component
provided by Jetpack Compose `SwingPanel`.

### Frame grabbing

A more flexible method that captures video frames as an array of bytes, converts to the required format, and then
displays.

### [JavaFX](https://openjfx.io)

```kotlin
plugins {
    id("org.openjfx.javafxplugin") version "19"
}

javafx {
    version = "19"
    modules("javafx.media", "javafx.swing")
}
```

- [AWT component](https://github.com/numq/jetpack-compose-desktop-media-player/blob/master/src/main/kotlin/javafx/JfxComponentController.kt)
- [Frame grabbing](https://github.com/numq/jetpack-compose-desktop-media-player/blob/master/src/main/kotlin/javafx/JfxFrameController.kt)

### [VLCJ](https://github.com/caprica/vlcj)

```kotlin
implementation("uk.co.caprica:vlcj:4.8.2")
```

- [AWT component](https://github.com/numq/jetpack-compose-desktop-media-player/blob/master/src/main/kotlin/vlcj/VlcjComponentController.kt)
- [Frame grabbing](https://github.com/numq/jetpack-compose-desktop-media-player/blob/master/src/main/kotlin/vlcj/VlcjFrameController.kt)
