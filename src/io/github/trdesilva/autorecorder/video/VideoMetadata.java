/*
 * Copyright (c) 2022 Thomas DeSilva.
 * Distributed under GPLv3.
 */

package io.github.trdesilva.autorecorder.video;

import org.joda.time.DateTime;

public class VideoMetadata
{
    private DateTime creationDate = new DateTime(0);
    private long duration = -1;
    private String resolution = "N/A";
    
    public DateTime getCreationDate()
    {
        return creationDate;
    }
    
    public void setCreationDate(DateTime creationDate)
    {
        this.creationDate = creationDate;
    }
    
    public long getDuration()
    {
        return duration;
    }
    
    public void setDuration(long duration)
    {
        this.duration = duration;
    }
    
    public String getResolution()
    {
        return resolution;
    }
    
    public void setResolution(String resolution)
    {
        this.resolution = resolution;
    }
}
