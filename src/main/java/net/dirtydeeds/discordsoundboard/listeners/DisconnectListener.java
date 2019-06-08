package net.dirtydeeds.discordsoundboard.listeners;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

public class DisconnectListener extends ListenerAdapter {

    private static final Log LOG = LogFactory.getLog("DisconnectListener");

    public DisconnectListener() {
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        VoiceChannel channel = event.getChannelLeft();
        if (channel == null) {
            return;
        }

        List<Member> members = channel.getMembers();
        Guild guild = channel.getGuild();
        if (members.size() == 1 && members.get(0).equals(guild.getSelfMember())) {
            LOG.info("Bot is the last user left in channel, disconnecting");
            guild.getAudioManager().closeAudioConnection();
        }
    }
}
