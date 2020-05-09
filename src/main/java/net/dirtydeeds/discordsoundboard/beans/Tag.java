package net.dirtydeeds.discordsoundboard.beans;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column
    private String name;
    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(
            name = "clip_tag",
            joinColumns = {@JoinColumn(name = "clip_id")},
            inverseJoinColumns = {@JoinColumn(name = "tag_id")})

    private Set<Clip> clips = new HashSet<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Clip> getClips() {
        return clips;
    }

    public void setClips(Set<Clip> clips) {
        this.clips = clips;
    }
}
