/*
 * Copyright (c) 2022 Thomas DeSilva.
 * Distributed under GPLv3.
 */

package io.github.trdesilva.autorecorder;

import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.github.trdesilva.autorecorder.record.GameListener;
import io.github.trdesilva.autorecorder.ui.cli.MainCli;
import io.github.trdesilva.autorecorder.ui.gui.MainWindow;
import io.github.trdesilva.autorecorder.ui.gui.inject.GuiModule;
import io.github.trdesilva.autorecorder.ui.status.Event;
import io.github.trdesilva.autorecorder.ui.status.EventQueue;
import io.github.trdesilva.autorecorder.ui.status.EventType;
import io.github.trdesilva.autorecorder.video.inject.VideoModule;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;

public class Main
{
    public static void main(String[] args) throws Exception
    {
        System.out.println("starting");
        Set<String> argSet = Sets.newHashSet(args);
        
        if(argSet.contains("-?") || argSet.contains("--help"))
        {
            System.out.print("Arguments:\n\t-d or --debug: Enable debug output\n\t--cli: Run in CLI mode\n");
            return;
        }
        
        boolean isDebugMode = false;
        if(argSet.contains("-d") || argSet.contains("--debug"))
        {
            isDebugMode = true;
        }
        
        Injector injector = Guice.createInjector(new VideoModule(), new GuiModule(isDebugMode));
        
        Settings settings = injector.getInstance(Settings.class);
        settings.populate();
        
        EventQueue events = injector.getInstance(EventQueue.class);
        
        if(!new File(settings.getFfmpegPath()).exists())
        {
            events.postEvent(new Event(EventType.DEBUG, "copying ffmpeg.exe from resources"));
            try
            {
                Files.copy(ClassLoader.getSystemClassLoader().getResourceAsStream("ffmpeg.exe"),
                           Settings.SETTINGS_DIR.resolve("ffmpeg.exe"));
            }
            catch(IOException e)
            {
                events.postEvent(new Event(EventType.DEBUG, "failed to copy ffmpeg.exe"));
            }
        }
        if(!new File(settings.getFfprobePath()).exists())
        {
            events.postEvent(new Event(EventType.DEBUG, "copying ffprobe.exe from resources"));
            try
            {
                Files.copy(ClassLoader.getSystemClassLoader().getResourceAsStream("ffprobe.exe"),
                           Settings.SETTINGS_DIR.resolve("ffprobe.exe"));
            }
            catch(IOException e)
            {
                events.postEvent(new Event(EventType.DEBUG, "failed to copy ffprobe.exe"));
            }
        }
        
        GameListener listener = injector.getInstance(GameListener.class);
        
        if(argSet.contains("--cli"))
        {
            System.out.println("running in CLI mode");
            listener.start();
            
            MainCli cli = injector.getInstance(MainCli.class);
            cli.run();
            
            listener.stop();
        }
        else
        {
            MainWindow mainWindow = injector.getInstance(MainWindow.class);
            mainWindow.start();
        }
    }
}
