# ECAE Portable Welcome Experience

Browser-based prototype for the ECAE Android portable screens.

## Current behavior

1. The normal ECAE content rotates continuously.
2. The front camera runs without showing a camera preview.
3. MediaPipe checks whether a visible face is present.
4. After a person is detected, the ECAE welcome message appears.
5. After the person leaves, the system waits three seconds.
6. The welcome message disappears.
7. The normal rotating content remains available.

## Privacy

The prototype performs face-presence detection only.

It does not:

- Identify the person
- Recognize identities
- Analyze facial expressions
- Save camera images
- Upload photographs
- Record video

## Browser requirements

Use the GitHub Pages HTTPS address.

Allow camera access when requested.

Chrome or Microsoft Edge is recommended for testing.

## Repository files

- index.html
- style.css
- script.js
- README.md

## Future APK work

The browser version can later be wrapped in an Android WebView application and configured for startup and kiosk mode.
