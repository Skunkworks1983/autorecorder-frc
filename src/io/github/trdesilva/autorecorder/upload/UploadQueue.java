/*
 * Copyright (c) 2022 Thomas DeSilva.
 * Distributed under GPLv3.
 */

package io.github.trdesilva.autorecorder.upload;

import com.google.inject.Inject;
import io.github.trdesilva.autorecorder.ui.status.StatusMessage;
import io.github.trdesilva.autorecorder.ui.status.StatusQueue;
import io.github.trdesilva.autorecorder.ui.status.StatusType;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

public class UploadQueue implements AutoCloseable
{
    private final Uploader uploader;
    private final StatusQueue status;
    
    private final ConcurrentLinkedQueue<UploadJob> jobs;
    private final Semaphore semaphore;
    private Thread uploadingThread;
    
    @Inject
    public UploadQueue(Uploader uploader, StatusQueue status)
    {
        this.uploader = uploader;
        this.status = status;
        
        jobs = new ConcurrentLinkedQueue<>();
        semaphore = new Semaphore(0);
    }
    
    public synchronized void enqueue(UploadJob clipJob)
    {
        jobs.offer(clipJob);
        semaphore.release(1);
    }
    
    public void startProcessing()
    {
        if(uploader != null && (uploadingThread == null || !uploadingThread.isAlive()))
        {
            uploadingThread = new Thread(() -> {
                while(true)
                {
                    try
                    {
                        semaphore.acquire();
                    }
                    catch(InterruptedException e)
                    {
                        return;
                    }
                    UploadJob job = jobs.poll();
                    try
                    {
                        if(uploader.getValidator().validate(job))
                        {
                            status.postMessage(
                                    new StatusMessage(StatusType.INFO, "Starting upload of " + job.getVideoTitle()));
                            String url = uploader.upload(job);
                            status.postMessage(
                                    new StatusMessage(StatusType.SUCCESS, job.getVideoTitle() + " uploaded", url));
                        }
                    }
                    catch(Exception e)
                    {
                        status.postMessage(
                                new StatusMessage(StatusType.FAILURE, "Failed to upload clip: " + job.getClipName()));
                    }
                }
            });
            uploadingThread.setName("Uploading thread");
            uploadingThread.start();
        }
    }
    
    public void stopProcessing()
    {
        if(uploadingThread != null && uploadingThread.isAlive())
        {
            uploadingThread.interrupt();
        }
    }
    
    @Override
    public void close() throws Exception
    {
        stopProcessing();
    }
}
