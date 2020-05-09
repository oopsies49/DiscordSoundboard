package net.dirtydeeds.discordsoundboard.repository;

import net.dirtydeeds.discordsoundboard.beans.Clip;
import net.dirtydeeds.discordsoundboard.beans.SoundFilePlayEventCount;
import org.apache.commons.io.FilenameUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author dfurrer.
 */
public interface ClipRepository extends CrudRepository<Clip, String> {
    Clip findOneBySoundFileIdIgnoreCase(String name);
    Page<Clip> findAll(Pageable pageable);

    default Clip findRandom() {
        long count = count();
        int random = (int) (Math.random() * count);

        Page<Clip> page = findAll(PageRequest.of(random, 1));

        if (page.hasContent()) {
            return page.getContent().get(0);
        }
        return null;
    }

    @Query(value = "select s.soundFileLocation as soundFileLocation from SoundFile s")
    Collection<String> getSoundFileLocations();

    default Set<String> getSoundFileNames() {
        Collection<String> soundFileLocations = getSoundFileLocations();
        return soundFileLocations.stream()
                .map(FilenameUtils::getBaseName)
                .collect(Collectors.toSet());
    }

    @Query(value = "select s.id as soundFileId, s.soundFileLocation as soundFileLocation, s.category as category, s.lastModified as lastModified, count(p.id) as playEventCount from SoundFile s join PlayEvent p on p.filename = s.id group by s.id, s.soundFileLocation, s.category, s.lastModified order by playEventCount desc")
    List<SoundFilePlayEventCount> getSoundFilePlayEventCountDesc();

    @Query(value = "select s.id as soundFileId, s.soundFileLocation as soundFileLocation, s.category as category, s.lastModified as lastModified, count(p.id) as playEventCount from SoundFile s join PlayEvent p on p.filename = s.id group by s.id, s.soundFileLocation, s.category, s.lastModified order by playEventCount")
    List<SoundFilePlayEventCount> getSoundFilePlayEventCountAsc();
}
