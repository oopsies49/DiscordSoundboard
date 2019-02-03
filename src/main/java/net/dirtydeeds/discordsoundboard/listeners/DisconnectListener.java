package net.dirtydeeds.discordsoundboard.listeners;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DisconnectListener extends ListenerAdapter {

    private static final Log LOG = LogFactory.getLog("DisconnectListener");

    public DisconnectListener() {
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        VoiceChannel channel = event.getChannelLeft();
        if (channel != null && channel.getMembers().size() == 1) {
            Guild guild = channel.getGuild();
            LOG.info("Bot is the last user left in channel, disconnecting");
            guild.getAudioManager().closeAudioConnection();
        }
    }
}
