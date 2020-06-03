package net.dirtydeeds.discordsoundboard.service;

import net.dirtydeeds.discordsoundboard.DiscordSoundboardProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Observable;
import java.util.Observer;

@Service
public class SoundWatchService implements Observer {

    private SoundPlayerImpl soundPlayer;

    @Autowired
    public SoundWatchService(DiscordSoundboardProperties appProperties, SoundFolderWatch soundFolderWatch, SoundPlayerImpl soundPlayer) {
        this.soundPlayer = soundPlayer;

        soundFolderWatch.addObserver(this);
        soundFolderWatch.watchDirectoryPath(appProperties.getSoundsDirectory());
    }

    @Override
    public void update(Observable o, Object arg) {
        soundPlayer.getFileList();
    }
}
