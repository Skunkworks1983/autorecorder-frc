# Autorecorder
This is a Java application that automates OBS Studio and FFMpeg on Windows to record FRC Driver Station dashboards. It is a fork of [Autorecorder](https://github.com/trdesilva/autorecorder).

# Setup
1. Install [OBS Studio](https://obsproject.com/download) and configure it however you like. Autorecorder FRC will launch OBS Studio and start a new recording when it detects that the Elastic dashboard is running while either FMS is connected or the robot is enabled.
2. Install some kind of Java Runtime Environment that supports Java 11. This project was developed on [OpenJDK 11 for x64 Windows](https://adoptium.net/?variant=openjdk11), so that should work.
3. Clone this repo and open with IntelliJ to resolve dependencies, or [download the automatic updater](https://github.com/Skunkworks1983/autorecorder-frc/releases/download/v1.0.0/updater.jar).
4. Run the updater JAR to generate the launcher script in %LOCALAPPDATA%\autorecorder-frc. (If you cloned the repo, you can skip this step and the next one because you can build Autorecorder yourself.)
5. Run the launcher script. The launcher will download and run the latest Autorecorder FRC release. You can make Windows run the launcher on startup if you want to update and launch Autorecorder FRC when you log in to Windows.
6. Edit your Autorecorder settings to tell it where to find OBS, and where your recordings and clips will be stored. You can do this through Autorecorder FRC itself or by editing the JSON file it generates in %LOCALAPPDATA%.
7. Leave Autorecorder FRC running whenever you want it to record your dashboard.

# FAQ

## Why should I use Autorecorder FRC?
Autorecorder FRC makes it easy to correlate logs with video from camera feeds and drive team reports from the field. Unlike other logging or telemetry solutions, it doesn't require any code changes, and it's capable of recording what your drive team is saying during matches.

## What do I have to agree to in order to use it?
You can find Autorecorder's terms of use (including its privacy policy) [here](https://trdesilva.github.io/autorecorder/license). Autorecorder is published under the GNU Public License.
