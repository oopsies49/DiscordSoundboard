package net.dirtydeeds.discordsoundboard.beans;

import java.util.Date;

public interface SoundFilePlayEventCount {
    String getSoundFileId();
    String getSoundFileLocation();
    String getCategory();
    Date getLastModified();
    int getPlayEventCount();
}
