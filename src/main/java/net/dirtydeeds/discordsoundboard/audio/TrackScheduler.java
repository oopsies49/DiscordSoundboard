package net.dirtydeeds.discordsoundboard.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.core.entities.Guild;

import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author dave_f
 */
public class TrackScheduler extends AudioEventAdapter {

    private final AudioPlayer player;
    private final Deque<AudioInfo> dequeue;

    TrackScheduler(AudioPlayer player) {
        this.player = player;
        this.dequeue = new LinkedBlockingDeque<>();
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
            dequeue.add(info);
        }
    }

    public void playNow(AudioTrack track, Guild guild) {
        if (!player.startTrack(track, false)) {
            AudioInfo info = new AudioInfo(track, guild);
            dequeue.addFirst(info);
        }
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason == AudioTrackEndReason.REPLACED) {
            return;
        }

        AudioInfo audioInfo = dequeue.poll();
        if (audioInfo == null) {
            return;
        }
        AudioTrack nextTrack = audioInfo.getTrack();
        this.player.playTrack(nextTrack);
    }

    public void stop() {
        dequeue.clear();
        player.stopTrack();
    }
}
