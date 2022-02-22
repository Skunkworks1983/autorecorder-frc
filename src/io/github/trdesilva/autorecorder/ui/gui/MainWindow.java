/*
 * Copyright (c) 2022 Thomas DeSilva.
 * Distributed under GPLv3.
 */

package io.github.trdesilva.autorecorder.ui.gui;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.trdesilva.autorecorder.Settings;
import io.github.trdesilva.autorecorder.clip.ClipQueue;
import io.github.trdesilva.autorecorder.record.GameListener;
import io.github.trdesilva.autorecorder.ui.gui.clip.ClipListPanel;
import io.github.trdesilva.autorecorder.ui.gui.clip.UploadPanel;
import io.github.trdesilva.autorecorder.ui.gui.record.ClippingPanel;
import io.github.trdesilva.autorecorder.ui.gui.record.RecordingListPanel;
import io.github.trdesilva.autorecorder.ui.gui.settings.SettingsPanel;
import io.github.trdesilva.autorecorder.ui.status.StatusMessage;
import io.github.trdesilva.autorecorder.ui.status.StatusQueue;
import io.github.trdesilva.autorecorder.ui.status.StatusType;
import io.github.trdesilva.autorecorder.upload.UploadQueue;
import net.miginfocom.swing.MigLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;

@Singleton
public class MainWindow implements Navigator
{
    public static final int PREFERRED_WIDTH = 800;
    public static final int PREFERRED_HEIGHT = 600;
    
    private final JPanel mainPanel;
    private final CardLayout mainLayout;
    private final JFrame mainFrame;
    
    private final ClippingPanel clippingPanel;
    private final UploadPanel uploadPanel;
    private final StatusPanel statusPanel;
    
    private final StatusQueue statusQueue;
    private final ClipQueue clipQueue;
    private final UploadQueue uploadQueue;
    
    private final GameListener gameListener;
    private final WindowCloseHandler windowCloseHandler;
    
    @Inject
    public MainWindow(Settings settings, RecordingListPanel recordingListPanel, ClipListPanel clipListPanel,
                      SettingsPanel settingsPanel, ClippingPanel clippingPanel, UploadPanel uploadPanel,
                      LicensePanel licensePanel, StatusPanel statusPanel, StatusQueue statusQueue, ClipQueue clipQueue,
                      UploadQueue uploadQueue, GameListener gameListener, WindowCloseHandler windowCloseHandler)
    {
        // components
        mainFrame = new JFrame("Autorecorder");
        // TODO figure out minimizing to tray
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        JPanel metaPanel = new JPanel();
        metaPanel.setLayout(new MigLayout("fill, insets 0 0 0 0", "[grow]", "[grow][30!]"));
        
        mainPanel = new JPanel();
        
        mainLayout = new CardLayout();
        mainPanel.setLayout(mainLayout);
        
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Recordings", recordingListPanel);
        tabbedPane.add("Clips", clipListPanel);
        tabbedPane.add("Settings", settingsPanel);
        tabbedPane.setSelectedIndex(0);
        tabbedPane.setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
        
        this.clippingPanel = clippingPanel;
        this.uploadPanel = uploadPanel;
        
        mainPanel.add(tabbedPane, "mainView");
        mainPanel.add(clippingPanel, "clippingView");
        mainPanel.add(uploadPanel, "uploadView");
        mainPanel.add(licensePanel, "licenseView");
        if(settings.isTermsAccepted())
        {
            mainLayout.show(mainPanel, "mainView");
        }
        else
        {
            mainLayout.show(mainPanel, "licenseView");
            tabbedPane.setSelectedIndex(tabbedPane.indexOfTab("Settings"));
        }
        
        this.statusPanel = statusPanel;
        this.statusQueue = statusQueue;
        
        this.clipQueue = clipQueue;
        this.uploadQueue = uploadQueue;
        
        this.gameListener = gameListener;
        
        this.windowCloseHandler = windowCloseHandler;
        
        metaPanel.add(mainPanel, "cell 0 0, grow");
        metaPanel.add(statusPanel, "cell 0 1, growx");
        
        mainFrame.setIconImage(
                Toolkit.getDefaultToolkit().createImage(ClassLoader.getSystemResource("autorecordericon.png")));
        
        mainFrame.getContentPane().add(metaPanel);
        
        tabbedPane.addChangeListener(e -> {
            int tabIndex = tabbedPane.getSelectedIndex();
            if(tabIndex == tabbedPane.indexOfTab("Recordings"))
            {
                recordingListPanel.update();
            }
            else if(tabIndex == tabbedPane.indexOfTab("Clips"))
            {
                clipListPanel.update();
            }
        });
    }
    
    public void start() throws Exception
    {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        
        statusQueue.setConsumer(statusPanel);
        
        clipQueue.startProcessing();
        uploadQueue.startProcessing();
        
        gameListener.start();
        
        windowCloseHandler.addCloseable(clipQueue);
        windowCloseHandler.addCloseable(uploadQueue);
        windowCloseHandler.addCloseable(gameListener);
        
        mainFrame.addWindowListener(windowCloseHandler);
        
        mainFrame.pack();
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
    }
    
    @Override
    public void showClipView(File videoFile)
    {
        statusQueue.postMessage(new StatusMessage(StatusType.DEBUG, "showing clippingView"));
        clippingPanel.setRecording(videoFile);
        mainLayout.show(mainPanel, "clippingView");
    }
    
    @Override
    public void showUploadView(File videoFile)
    {
        statusQueue.postMessage(new StatusMessage(StatusType.DEBUG, "showing uploadView"));
        uploadPanel.setRecording(videoFile);
        mainLayout.show(mainPanel, "uploadView");
    }
    
    @Override
    public void showLicenseView()
    {
        mainLayout.show(mainPanel, "licenseView");
    }
    
    @Override
    public void showMainView()
    {
        mainLayout.show(mainPanel, "mainView");
    }
}
