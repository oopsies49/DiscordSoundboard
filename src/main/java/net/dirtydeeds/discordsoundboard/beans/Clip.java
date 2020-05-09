package net.dirtydeeds.discordsoundboard.beans;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

/**
 * Class that represents a sound file.
 *
 * @author dfurrer.
 */
@SuppressWarnings("unused")
@Entity
public class Clip {

    @Id
    private String clipId;

    @Column
    private String clipLocation;

    @Column
    private String category;

    @Column
    private Date lastModified;

    @ManyToMany(mappedBy = "clips")
    private Set<Tag> tags;

    protected Clip() {
    }

    public Clip(String soundFileId, String soundFileLocation, String category, Date lastModified) {
        this.clipId = soundFileId;
        this.clipLocation = soundFileLocation;
        this.category = category;
        this.lastModified = lastModified;
    }

    public String getClipId() {
        return clipId;
    }

    public String getClipLocation() {
        return clipLocation;
    }

    public String getCategory() {
        return category;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Clip clip = (Clip) o;

        return clipId.equals(clip.clipId);

    }

    @Override
    public int hashCode() {
        return clipId.hashCode();
    }
}
