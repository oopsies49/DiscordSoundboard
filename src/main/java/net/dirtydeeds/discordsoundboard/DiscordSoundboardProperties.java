package net.dirtydeeds.discordsoundboard;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@ConfigurationProperties()
@Validated
public class DiscordSoundboardProperties {

    @NotNull
    private String botToken;
    private String commandCharacter = "?";
    private boolean respondToChatCommands = true;
    private boolean respondToDm = true;
    private String leaveSuffix = "_leave";
    @Min(0)
    @Max(1994)
    private int messageSizeLimit = 1994;

    @NotEmpty
    private String soundsDirectory = "sounds";
    private List<String> allowedUserIds = Collections.emptyList();
    private List<String> bannedUserIds = Collections.emptyList();
    private Set<String> unlimitedUserIds = Collections.emptySet();
    private boolean leaveWhenLastUserInChannel = true;
    @Min(1)
    private int repeatLimit = 3;

    @Min(0)
    private int rateLimitRestrictDuration;

    @NotNull
    public String getBotToken() {
        return botToken;
    }

    public void setBotToken(@NotNull String botToken) {
        this.botToken = botToken;
    }

    public String getCommandCharacter() {
        return commandCharacter;
    }

    public void setCommandCharacter(String commandCharacter) {
        this.commandCharacter = commandCharacter;
    }

    public boolean isRespondToChatCommands() {
        return respondToChatCommands;
    }

    public void setRespondToChatCommands(boolean respondToChatCommands) {
        this.respondToChatCommands = respondToChatCommands;
    }

    public boolean isRespondToDm() {
        return respondToDm;
    }

    public void setRespondToDm(boolean respondToDm) {
        this.respondToDm = respondToDm;
    }

    public String getLeaveSuffix() {
        return leaveSuffix;
    }

    public void setLeaveSuffix(String leaveSuffix) {
        this.leaveSuffix = leaveSuffix;
    }

    public int getMessageSizeLimit() {
        return messageSizeLimit;
    }

    public void setMessageSizeLimit(int messageSizeLimit) {
        this.messageSizeLimit = messageSizeLimit;
    }

    public String getSoundsDirectory() {
        return soundsDirectory;
    }

    public void setSoundsDirectory(String soundsDirectory) {
        this.soundsDirectory = soundsDirectory;
    }

    public List<String> getAllowedUserIds() {
        return allowedUserIds;
    }

    public void setAllowedUserIds(List<String> allowedUserIds) {
        this.allowedUserIds = allowedUserIds;
    }

    public List<String> getBannedUserIds() {
        return bannedUserIds;
    }

    public void setBannedUserIds(List<String> bannedUserIds) {
        this.bannedUserIds = bannedUserIds;
    }

    public int getRateLimitRestrictDuration() {
        return rateLimitRestrictDuration;
    }

    public void setRateLimitRestrictDuration(int rateLimitRestrictDuration) {
        this.rateLimitRestrictDuration = rateLimitRestrictDuration;
    }

    public Set<String> getUnlimitedUserIds() {
        return unlimitedUserIds;
    }

    public void setUnlimitedUserIds(Set<String> unlimitedUserIds) {
        this.unlimitedUserIds = unlimitedUserIds;
    }

    public boolean isLeaveWhenLastUserInChannel() {
        return leaveWhenLastUserInChannel;
    }

    public void setLeaveWhenLastUserInChannel(boolean leaveWhenLastUserInChannel) {
        this.leaveWhenLastUserInChannel = leaveWhenLastUserInChannel;
    }

    public int getRepeatLimit() {
        return repeatLimit;
    }

    public void setRepeatLimit(int repeatLimit) {
        this.repeatLimit = repeatLimit;
    }
}
