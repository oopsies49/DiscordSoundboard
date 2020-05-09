package net.dirtydeeds.discordsoundboard.repository;

import net.dirtydeeds.discordsoundboard.beans.Tag;
import org.springframework.data.repository.CrudRepository;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface TagRepository extends CrudRepository<Tag, UUID> {

    Tag findOneByNameIgnoreCase(String name);
    Collection<Tag> findByNameIgnoreCaseIn(Set<String> name);
}
