# ECAE Portable Welcome

This Android application displays changing GitHub Pages content on an Android portable screen.

The front camera operates without displaying a camera preview.

When a visible face is detected:

1. Videos and audio inside the displayed page are paused.
2. The ECAE welcome overlay appears.

When the person leaves:

1. The system waits three seconds.
2. The overlay closes.
3. Previously playing media resumes from the same position.

The application does not identify people, recognize identities, analyze expressions, save photographs, or upload camera images.

## Content address

Change the CONTENT_URL value in MainActivity.kt before building the APK.

## APK build

Open the Actions tab.

Run the Build ECAE Portable Welcome APK workflow.

Download the ECAE-Portable-Welcome-APK artifact.
