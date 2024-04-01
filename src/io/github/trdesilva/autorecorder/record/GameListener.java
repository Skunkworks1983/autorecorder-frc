/*
 * Copyright (c) 2022 Thomas DeSilva.
 * Distributed under GPLv3.
 */

package io.github.trdesilva.autorecorder.record;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import edu.wpi.first.networktables.BooleanSubscriber;
import edu.wpi.first.networktables.IntegerSubscriber;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.NetworkTablesJNI;
import edu.wpi.first.util.CombinedRuntimeLoader;
import edu.wpi.first.util.WPIUtilJNI;
import edu.wpi.first.wpilibj.DriverStation;
import io.github.trdesilva.autorecorder.Settings;
import io.github.trdesilva.autorecorder.event.Event;
import io.github.trdesilva.autorecorder.event.EventConsumer;
import io.github.trdesilva.autorecorder.event.EventProperty;
import io.github.trdesilva.autorecorder.event.EventQueue;
import io.github.trdesilva.autorecorder.event.EventType;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class GameListener implements AutoCloseable, EventConsumer
{
    private static final Set<EventType> EVENT_TYPES = Sets.immutableEnumSet(EventType.SETTINGS_CHANGE,
                                                                            EventType.MANUAL_RECORDING_START,
                                                                            EventType.MANUAL_RECORDING_END);
    private final Obs obs;
    private final Settings settings;
    private final EventQueue events;
    
    private final ConcurrentHashMap<Path, Optional<String>> exeCheckResults;
    private final AtomicBoolean recording;
    // when set, don't allow the listener to start recordings
    private final AtomicBoolean forceDisableListening;
    // when set, don't allow the listener to stop recordings
    private final AtomicBoolean forceRecording;
    private final AtomicReference<String> currentGame;
    private final NetworkTableInstance networkTables;
    private final NetworkTable fmsInfo;
    private final IntegerSubscriber fmsInfoSubscriber;
    
    private Thread thread;
    
    @Inject
    public GameListener(Obs obs, Settings settings, EventQueue events)
    {
        this.obs = obs;
        this.settings = settings;
        this.events = events;
        
        exeCheckResults = new ConcurrentHashMap<>();
        recording = new AtomicBoolean(false);
        forceDisableListening = new AtomicBoolean(false);
        forceRecording = new AtomicBoolean(false);
        currentGame = new AtomicReference<>();

        networkTables = NetworkTableInstance.getDefault();
        fmsInfo = networkTables.getTable("/FMSInfo");
        fmsInfoSubscriber = fmsInfo.getIntegerTopic("FMSControlData").subscribe(0);
        networkTables.startClient4("Autorecorder FRC");
        networkTables.setServer("localhost");
        networkTables.startDSClient();
        
        events.addConsumer(this);
    }
    
    public void startListener()
    {
        events.postEvent(new Event(EventType.DEBUG, "Starting listener thread"));
        thread = new Thread(() ->
                            {
                                while(true)
                                {
                                    if(!recording.get() && !forceDisableListening.get())
                                    {
                                        ProcessHandle.allProcesses().forEach(ph -> {
                                            // some game exes are identified by more of their path than just filename
                                            if(ph.info().command().isPresent())
                                            {
                                                Path command = Paths.get(ph.info().command().orElse(""));
                                                if(!exeCheckResults.containsKey(command))
                                                {
                                                    Optional<String> programOptional = Optional.empty();
                                                    for(int i = 1; i <= command.getNameCount(); i++)
                                                    {
                                                        String program = command.subpath(
                                                                                        command.getNameCount() - i,
                                                                                        command.getNameCount())
                                                                                .toString();
                                                        if(settings.getGames()
                                                                   .contains(settings.formatExeName(program)))
                                                        {
                                                            programOptional = Optional.of(program);
                                                            break;
                                                        }
                                                    }
                                
                                                    exeCheckResults.put(command, programOptional);
                                                }
                            
                                                if(exeCheckResults.containsKey(command) && exeCheckResults.get(command)
                                                                                                          .isPresent()
                                                && (isFmsAttached() || isRobotEnabled()))
                                                {

                                                    String program = exeCheckResults.get(command).get();
                                                    startRecording(program);
                                                }
                                            }
                                        });
                                    }
                                    else if(!forceRecording.get())
                                    {
                                        if(!settings.getGames().contains(settings.formatExeName(currentGame.get()))
                                                || (ProcessHandle.allProcesses()
                                                                .noneMatch(
                                                                        ph -> Paths.get(ph.info().command().orElse(""))
                                                                                   .endsWith(currentGame.get()))
                                                    )
                                                || !(isFmsAttached() || isRobotEnabled()))
                                        {
                                            stopRecording();
                                            // allow listener to start recordings again now that the game we force-stopped has terminated
                                            forceDisableListening.set(false);
                                            // currentGame exists for the listener to keep track of its state, so updating here is fine
                                            currentGame.set(null);
                                        }
                                    }
                
                                    try
                                    {
                                        Thread.sleep(1000);
                                    }
                                    catch(InterruptedException e)
                                    {
                                        events.postEvent(new Event(EventType.DEBUG, "Game listening ended"));
                                        return;
                                    }
                                }
                            });
        thread.setName("Listener thread");
        thread.start();
    }
    
    public void stopListener()
    {
        thread.interrupt();
        if(recording.get())
        {
            events.postEvent(new Event(EventType.DEBUG, "thread shutting down, stopping recording"));
            obs.stop();
        }
    }
    
    private void startRecording(String program)
    {
        try
        {
            if(recording.get())
            {
                events.postEvent(new Event(EventType.DEBUG, String.format("Tried to record %s but already recording %s", program, currentGame.get())));
                return;
            }
            recording.set(true);
            obs.start();
            currentGame.set(program);
            events.postEvent(new Event(EventType.RECORDING_START,
                                       "Recording " + program,
                                       Map.of(EventProperty.GAME_NAME, program)));
        }
        catch(IOException e)
        {
            events.postEvent(new Event(EventType.FAILURE,
                                       "Couldn't start OBS; waiting for settings change before attempting to record again"));
            recording.set(false);
            currentGame.set(null);
            stopListener();
        }
    }
    
    private void stopRecording()
    {
        if(!recording.get())
        {
            events.postEvent(new Event(EventType.DEBUG, "Tried to stop recording, but recording is already stopped"));
            return;
        }
        
        events.postEvent(new Event(EventType.RECORDING_END,
                                   "Stopped recording " + currentGame.get()));
        obs.stop();
        recording.set(false);
    }
    
    @Override
    public void close() throws Exception
    {
        stopListener();
    }
    
    @Override
    public void post(Event event)
    {
        if(event.getType().equals(EventType.SETTINGS_CHANGE))
        {
            exeCheckResults.clear();
        }
        else if(event.getType().equals(EventType.MANUAL_RECORDING_START))
        {
            events.postEvent(new Event(EventType.DEBUG, "FMS connected: " + isFmsAttached()));
            events.postEvent(new Event(EventType.DEBUG, "Enabled: " + isRobotEnabled()));
            // if we're resuming an automatic recording, restore the listener to its normal state
            if(forceDisableListening.get())
            {
                events.postEvent(new Event(EventType.DEBUG, "Resuming automatic recording"));
                forceDisableListening.set(false);
            }
            else // and if we're starting a manual recording, stop the listener from ending the manual recording
            {
                events.postEvent(new Event(EventType.DEBUG, "Starting manual recording"));
                forceRecording.set(true);
                startRecording("(Manual)");
            }
        }
        else if(event.getType().equals(EventType.MANUAL_RECORDING_END))
        {
            // if we're stopping a manual recording, restore the listener to its normal state
            if(forceRecording.get())
            {
                events.postEvent(new Event(EventType.DEBUG, "Stopping manual recording"));
                forceRecording.set(false);
                currentGame.set(null);
            }
            else // and if we're stopping an automatic recording, stop the listener from starting new recordings
            {
                events.postEvent(new Event(EventType.DEBUG, "Stopping automatic recording"));
                // don't clear currentGame here so the listener can reenable itself after currentGame terminates
                forceDisableListening.set(true);
            }
            stopRecording();
        }
    
        if(!thread.isAlive())
        {
            startListener();
        }
    }
    
    @Override
    public Set<EventType> getSubscriptions()
    {
        return EVENT_TYPES;
    }
    
    boolean isFmsAttached()
    {
        return (fmsInfoSubscriber.get() & 16) == 16;
    }
    
    boolean isRobotEnabled()
    {
        return (fmsInfoSubscriber.get() & 1) == 1;
    }
}
