package net.dirtydeeds.discordsoundboard.beans;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Date;

/**
 * Class that represents a sound file.
 *
 * @author dfurrer.
 */
@SuppressWarnings("unused")
@Entity
public class SoundFile {

    @Id
    private String soundFileId;

    @Column
    private String soundFileLocation;

    @Column
    private String category;

    @Column
    private Date lastModified;

    protected SoundFile() {
    }

    public SoundFile(String soundFileId, String soundFileLocation, String category, Date lastModified) {
        this.soundFileId = soundFileId;
        this.soundFileLocation = soundFileLocation;
        this.category = category;
        this.lastModified = lastModified;
    }

    public String getSoundFileId() {
        return soundFileId;
    }

    public String getSoundFileLocation() {
        return soundFileLocation;
    }

    public String getCategory() {
        return category;
    }

    public Date getLastModified() {
        return lastModified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SoundFile soundFile = (SoundFile) o;

        return soundFileId.equals(soundFile.soundFileId);

    }

    @Override
    public int hashCode() {
        return soundFileId.hashCode();
    }
}
