# Autorecorder
This is a Java application that automates OBS Studio and FFMpeg on Windows to record game sessions and create highlight clips. It can also upload clips to YouTube.

# Setup
1. Install [OBS Studio](https://obsproject.com/download) and configure it however you like. Autorecorder will launch OBS Studio and start a new recording when it detects that a game has launched.
2. Install some kind of Java Runtime Environment that supports Java 11. This project was developed on [OpenJDK 11 for x64 Windows](https://adoptium.net/?variant=openjdk11), so that should work.
3. Clone this repo and open with IntelliJ to resolve dependencies, or [download the automatic updater](https://github.com/trdesilva/autorecorder/releases/download/v1.4.0/updater.jar).
4. Run the updater JAR to generate the launcher script in %LOCALAPPDATA%\autorecorder. (If you cloned the repo, you can skip this step and the next one because you can build Autorecorder yourself.)
5. Run the launcher script. The launcher will download and run the latest Autorecorder release. You can make Windows run the launcher on startup if you want to update and launch Autorecorder when you log in to Windows.
6. Edit your Autorecorder settings to tell it where to find OBS, and where your recordings and clips will be stored. You can do this through Autorecorder itself or by editing the JSON file it generates in %LOCALAPPDATA%.
7. Put a Google OAuth client secret JSON file at %LOCALAPPDATA%\autorecorder\client_secret.json. If you're forking the project, you'll need to get your own client secret and make sure it can request tokens for the `youtube.upload` scope in the YouTube Data API.
8. Leave Autorecorder running whenever you want it to record your game sessions.
