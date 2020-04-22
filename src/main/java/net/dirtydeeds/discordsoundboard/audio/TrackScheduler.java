package net.dirtydeeds.discordsoundboard.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author dave_f
 */
public class TrackScheduler extends AudioEventAdapter {

    private final AudioPlayer player;
    private final Queue<AudioInfo> queue;

    TrackScheduler(AudioPlayer player) {
        this.player = player;
        this.queue = new LinkedBlockingQueue<>();
    }

    /**
     * Add the next track to queue or play right away if nothing is in the queue.
     *
     * @param track The track to play or add to queue.
     */
    public void queue(AudioTrack track, Guild guild) {
        AudioInfo info = new AudioInfo(track, guild);

        if (player.getPlayingTrack() == null) {
            player.playTrack(track);
        } else {
            queue.add(info);
        }
    }

    public void playNow(AudioTrack track, Guild guild) {
        if (!player.startTrack(track, false)) {
            AudioInfo info = new AudioInfo(track, guild);
            queue.add(info);
        }
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        AudioInfo audioInfo = queue.poll();
        if (audioInfo == null) {
            return;
        }
        Guild guild = audioInfo.getGuild();

        if (!queue.isEmpty()) {
           playNow(queue.element().getTrack(), guild);
        }
    }
}
