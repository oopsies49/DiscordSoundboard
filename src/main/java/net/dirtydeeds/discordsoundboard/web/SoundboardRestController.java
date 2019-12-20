package net.dirtydeeds.discordsoundboard.web;

import net.dirtydeeds.discordsoundboard.SoundPlaybackException;
import net.dirtydeeds.discordsoundboard.beans.PlayEvent;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.beans.SoundFilePlayEventCount;
import net.dirtydeeds.discordsoundboard.beans.User;
import net.dirtydeeds.discordsoundboard.repository.PlayEventRepository;
import net.dirtydeeds.discordsoundboard.repository.SoundFileRepository;
import net.dirtydeeds.discordsoundboard.service.SoundPlayerImpl;
import net.dirtydeeds.discordsoundboard.util.SortIgnoreCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * REST Controller.
 *
 * @author dfurrer.
 */
@RestController
@RequestMapping("/soundsApi")
@SuppressWarnings("unused")
public class SoundboardRestController {
    
    private SoundPlayerImpl soundPlayer;
    private PlayEventRepository playEventRepository;
    private SoundFileRepository soundFileRepository;

    @SuppressWarnings("unused") //Damn spring and it's need for empty constructors
    public SoundboardRestController() {
    }

    @Inject
    public SoundboardRestController(final SoundPlayerImpl soundPlayer, final PlayEventRepository playEventRepository, final SoundFileRepository soundFileRepository) {
        this.soundPlayer = soundPlayer;
        this.playEventRepository = playEventRepository;
        this.soundFileRepository = soundFileRepository;
    }

    @RequestMapping(value = "/availableSounds", method = RequestMethod.GET)
    @Deprecated
    public List<SoundFile> getSoundFileList() {
        Map<String, SoundFile> soundMap = soundPlayer.getAvailableSoundFiles();
        return soundMap.entrySet().stream()
                .map(Map.Entry::getValue)
                .sorted(new SortIgnoreCase())
                .collect(Collectors.toCollection(LinkedList::new));
    }
    
    @RequestMapping(value = "/soundCategories", method = RequestMethod.GET)
    @Deprecated
    public Set<String> getSoundCategories() {
        Map<String, SoundFile> soundMap = soundPlayer.getAvailableSoundFiles();
        return soundMap.entrySet().stream()
                .map(entry -> entry.getValue().getCategory())
                .collect(Collectors.toSet());
    }

    @RequestMapping(value = "/users", method = RequestMethod.GET)
    public List<User> getUsers() {
        return soundPlayer.getUsers();
    }
    
    @RequestMapping(value = "/playFile", method = RequestMethod.POST)
    public HttpStatus playSoundFile(@RequestParam String soundFileId, @RequestParam String username) {
        soundPlayer.playFileForUser(soundFileId, username);
        return HttpStatus.OK;
    }

    @RequestMapping(value = "/playUrl", method = RequestMethod.POST)
    public HttpStatus playSoundUrl(@RequestParam String url, @RequestParam String username) {
            soundPlayer.playUrlForUser(url, username);
            return HttpStatus.OK;
    }
    
    @RequestMapping(value = "/playRandom", method = RequestMethod.POST)
    @Deprecated
    public HttpStatus playRandomSoundFile(@RequestParam String username) {
        try {
            soundPlayer.playRandomSoundFile(username, null);
        } catch (SoundPlaybackException e) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.OK;
    }

    @RequestMapping(value = "/sounds", method = RequestMethod.GET)
    public List<SoundFile> getSoundFileListNew() {
        Map<String, SoundFile> soundMap = soundPlayer.getAvailableSoundFiles();
        return soundMap.entrySet().stream().map(Map.Entry::getValue)
                .sorted(new SortIgnoreCase())
                .collect(Collectors.toCollection(LinkedList::new));
    }

    @RequestMapping(value = "/playEvents", method = RequestMethod.GET)
    public List<PlayEvent> getPlayEvents() {
        Iterable<PlayEvent> all = playEventRepository.findAll();
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(all.iterator(), Spliterator.SORTED), false)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @RequestMapping(value = "/soundPlays", method = RequestMethod.GET)
    public List<SoundFilePlayEventCount> getSoundFilePlayEventCount() {
        Collection<SoundFilePlayEventCount> all = soundFileRepository.getSoundFilePlayEventCountDesc();
        return new ArrayList<>(all);
    }


    @RequestMapping(value = "/sounds", method = RequestMethod.POST)
    public HttpStatus soundCommand(@RequestParam String username, @RequestParam String command) {
        switch (command) {
            case "random":
                try {
                    soundPlayer.playRandomSoundFile(username, null);
                } catch (SoundPlaybackException e) {
                    return HttpStatus.INTERNAL_SERVER_ERROR;
                }
                return HttpStatus.OK;
            default:
                return HttpStatus.NOT_IMPLEMENTED;
        }
    }

    @RequestMapping(value = "/sounds/category", method = RequestMethod.GET)
    public Set<String> getSoundCategoriesNew() {
        Map<String, SoundFile> soundMap = soundPlayer.getAvailableSoundFiles();
        return soundMap.entrySet().stream()
                .map(entry -> entry.getValue().getCategory())
                .collect(Collectors.toSet());
    }

    @RequestMapping(value = "/stop", method = RequestMethod.POST)
    public HttpStatus stopPlayback(@RequestParam String username) {
        soundPlayer.stop(username);
        return HttpStatus.OK;
    }
    
    @RequestMapping(value = "/volume", method = RequestMethod.POST)
    public HttpStatus setVolume(@RequestParam Integer volume, @RequestParam String username) {
        soundPlayer.setSoundPlayerVolume(volume, username);
        return HttpStatus.OK;
    }
    
    @RequestMapping(value = "/volume", method = RequestMethod.GET) 
    public float getVolume() {
        return soundPlayer.getSoundPlayerVolume();
    }

    @RequestMapping(value = "/updateSoundList", method = RequestMethod.GET)
    public HttpStatus updateSoundList() {
        soundPlayer.getFileList();
        return HttpStatus.OK;
    }
}
