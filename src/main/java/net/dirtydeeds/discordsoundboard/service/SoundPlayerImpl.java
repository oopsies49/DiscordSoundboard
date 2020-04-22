package net.dirtydeeds.discordsoundboard.service;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dirtydeeds.discordsoundboard.DiscordSoundboardProperties;
import net.dirtydeeds.discordsoundboard.audio.GuildMusicManager;
import net.dirtydeeds.discordsoundboard.SoundPlaybackException;
import net.dirtydeeds.discordsoundboard.beans.PlayEvent;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.beans.SoundFilePlayEventCount;
import net.dirtydeeds.discordsoundboard.beans.User;
import net.dirtydeeds.discordsoundboard.listeners.ChatSoundBoardListener;
import net.dirtydeeds.discordsoundboard.listeners.DisconnectListener;
import net.dirtydeeds.discordsoundboard.repository.PlayEventRepository;
import net.dirtydeeds.discordsoundboard.repository.SoundFileRepository;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.audio.hooks.ConnectionStatus;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.managers.AudioManager;
import net.dv8tion.jda.core.requests.RequestFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author dfurrer.
 * <p>
 * This class handles moving into channels and playing sounds. Also, it loads the available sound files
 * and the configuration properties.
 */
@Service
public class SoundPlayerImpl {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    private final Map<String, GuildMusicManager> musicManagers;
    private DiscordSoundboardProperties appProperties;
    private JDA bot;
    private float playerVolume = (float) .75;
    private AudioPlayerManager playerManager;
    private String soundFileDir;
    private List<String> allowedUsers;
    private List<String> allowedUserIds;
    private List<String> bannedUsers;
    private List<String> bannedUserIds;
    private SoundFileRepository soundFileRepository;
    private final PlayEventRepository playEventRepository;
    private final SoundPlayerRateLimiter rateLimiter;

    @Autowired
    public SoundPlayerImpl(DiscordSoundboardProperties discordSoundboardProperties, SoundFileRepository soundFileRepository, PlayEventRepository playEventRepository) {
        this.appProperties = discordSoundboardProperties;
        this.soundFileRepository = soundFileRepository;
        this.playEventRepository = playEventRepository;
        this.musicManagers = new HashMap<>();

        initializeDiscordBot();
        getFileList();

        this.playerManager = new DefaultAudioPlayerManager();
        this.playerManager.registerSourceManager(new LocalAudioSourceManager());
        this.playerManager.registerSourceManager(new YoutubeAudioSourceManager());
        this.playerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        this.playerManager.registerSourceManager(new BandcampAudioSourceManager());
        this.playerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
        this.playerManager.registerSourceManager(new VimeoAudioSourceManager());
        this.playerManager.registerSourceManager(new HttpAudioSourceManager());
        this.playerManager.registerSourceManager(new BeamAudioSourceManager());

        this.rateLimiter = new SoundPlayerRateLimiter(appProperties);
    }

    private GuildMusicManager getGuildAudioPlayer(Guild guild) {
        String guildId = guild.getId();
        GuildMusicManager mng = musicManagers.get(guildId);
        if (mng == null) {
            synchronized (musicManagers) {
                mng = musicManagers.computeIfAbsent(guildId, k -> new GuildMusicManager(playerManager));
            }
        }
        return mng;
    }

    /**
     * Gets a Map of the loaded sound files.
     *
     * @return Map of sound files that have been loaded.
     */
    public Map<String, SoundFile> getAvailableSoundFiles() {
        Map<String, SoundFile> returnFiles = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (SoundFile soundFile : soundFileRepository.findAll()) {
            returnFiles.put(soundFile.getSoundFileId(), soundFile);
        }
        return returnFiles;
    }

    /**
     * Sets volume of the player.
     *
     * @param volume - The volume value to set.
     */
    public void setSoundPlayerVolume(int volume, String username) {
        playerVolume = (float) volume / 100;
        Guild guild = getUsersGuild(username);
        GuildMusicManager gmm;
        if (guild != null) {
            gmm = getGuildAudioPlayer(guild);
            gmm.player.setVolume(volume);
        }
    }

    /**
     * Returns the current volume
     *
     * @return float representing the current volume.
     */
    public float getSoundPlayerVolume() {
        return playerVolume;
    }

    public void playRandomSoundFile(String requestingUser, MessageReceivedEvent event) throws SoundPlaybackException {
        try {
            SoundFile randomValue = soundFileRepository.findRandom();

            LOG.info("Attempting to play random file: " + randomValue.getSoundFileId() + ", requested by : " + requestingUser);
            try {
                if (event != null) {
                    playFileForEvent(randomValue.getSoundFileId(), event);
                } else {
                    playFileForUser(randomValue.getSoundFileId(), requestingUser);
                }
            } catch (Exception e) {
                LOG.error("Could not play random file: " + randomValue.getSoundFileId());
            }
        } catch (Exception e) {
            throw new SoundPlaybackException("Problem playing random file.");
        }
    }

    /**
     * Joins the channel of the user provided and then plays a file.
     *
     * @param fileName - The name of the file to play.
     * @param userName - The name of the user to lookup what VoiceChannel they are in.
     */
    public void playFileForUser(String fileName, String userName) {
        if (userName == null || userName.isEmpty()) {
            userName = appProperties.getUsernameToJoinChannel();
        }

        if (rateLimiter.userIsRateLimited(userName)) {
            return;
        }

        playEventRepository.save(new PlayEvent(userName, fileName));

        try {
            Guild guild = getUsersGuild(userName);
            joinUsersCurrentChannel(userName);

            SoundFile fileToPlay = getSoundFileById(fileName);
            if (fileToPlay != null) {
                File soundFile = new File(fileToPlay.getSoundFileLocation());
                playFile(soundFile, guild);
            } else {
                throw new SoundPlaybackException("Could not find sound file that was requested.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playUrlForUser(String url, String userName) {
        if (userName == null || userName.isEmpty()) {
            userName = appProperties.getUsernameToJoinChannel();
        }

        if (rateLimiter.userIsRateLimited(userName)) {
            return;
        }

        try {
            Guild guild = getUsersGuild(userName);
            joinUsersCurrentChannel(userName);

            playFile(url, guild, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Plays the fileName requested.
     *
     * @param fileName - The name of the file to play.
     * @param event    -  The event that triggered the sound playing request. The event is used to find the channel to play
     *                 the sound back in.
     * @throws Exception Throws exception if it couldn't find the file requested or can't join the voice channel
     */
    private void playFileForEvent(String fileName, MessageReceivedEvent event) throws Exception {
        playFileForEvent(fileName, event, 1);
    }

    /**
     * Plays the fileName requested.
     *
     * @param fileName     - The name of the file to play.
     * @param event        -  The event that triggered the sound playing request. The event is used to find the channel to play
     *                     the sound back in.
     * @param repeatNumber - the number of times to repeat the sound file
     * @throws Exception Throws exception if it couldn't find the file requested or can't join the voice channel
     */
    public void playFileForEvent(String fileName, MessageReceivedEvent event, int repeatNumber) throws Exception {
        SoundFile fileToPlay = getSoundFileById(fileName);
        if (event != null) {
            // TODO move this to ChatSoundBoardListener
            if (rateLimiter.userIsRateLimited(event.getAuthor().getName())) {
                return;
            }

            playEventRepository.save(new PlayEvent(event.getAuthor().getName(), fileName));

            Guild guild = event.getGuild();
            if (guild == null) {
                guild = getUsersGuild(event.getAuthor().getName());
            }
            if (guild != null) {
                if (fileToPlay != null) {
                    try {
                        moveToUserIdsChannel(event, guild);
                    } catch (SoundPlaybackException e) {
                        sendPrivateMessage(event, e.getLocalizedMessage());
                    }
                    File soundFile = new File(fileToPlay.getSoundFileLocation());
                    playFile(soundFile.getAbsolutePath(), guild, repeatNumber);
                } else {
                    sendPrivateMessage(event, "Could not find sound to play. Requested sound: " + fileName + ".");
                }
            } else {
                sendPrivateMessage(event, "I can not find a voice channel you are connected to.");
                LOG.warn("no guild to play to.");
            }
        }
    }


    /**
     * This doesn't play anything, but since all of these actions are currently in the soundplayer service,
     * we keep this code here. The Bot will switch channels and stop.
     *
     * @param event The MessageReceivedEvent
     * @throws Exception exception
     */
    public void playNothingForEvent(MessageReceivedEvent event) throws Exception {
        if (event != null) {
            Guild guild = event.getGuild();
            if (guild == null) {
                guild = getUsersGuild(event.getAuthor().getName());
            }
            if (guild != null) {
                try {
                    moveToUserIdsChannel(event, guild);
                } catch (SoundPlaybackException e) {
                    sendPrivateMessage(event, e.getLocalizedMessage());
                }
            } else {
                sendPrivateMessage(event, "I can not find a voice channel you are connected to.");
                LOG.warn("no guild to join.");
            }
        }
    }

    /**
     * Plays the fileName requested for a voice channel disconnect.
     *
     * @param fileName - The name of the file to play.
     * @param event    -  The even that triggered the sound playing request. The event is used to find the channel to play
     *                 the sound back in.
     */
    public void playFileForDisconnect(String fileName, GuildVoiceLeaveEvent event) {
        if (event == null) return;
        try {
            moveToChannel(event.getChannelLeft(), event.getGuild());
            LOG.info("Playing file for disconnect of user: " + fileName);
            try {
                playFile(fileName, event.getGuild());
            } catch (SoundPlaybackException e) {
                LOG.info("Could not find any sound to play for disconnect of user: " + fileName);
            }
        } catch (SoundPlaybackException e) {
            LOG.debug(e.toString());
        }
    }

    /**
     * Stops sound playback and returns true or false depending on if playback was stopped.
     *
     * @return boolean representing whether playback was stopped.
     */
    public boolean stop(String requestingUser) {
        Guild guild = getUsersGuild(requestingUser);
        GuildMusicManager mng;
        if (guild != null) {
            mng = getGuildAudioPlayer(guild);
            AudioPlayer player = mng.player;
            player.stopTrack();
            return true;
        }
        return false;
    }

    /**
     * Get a list of users
     *
     * @return List of soundboard users.
     */
    public List<net.dirtydeeds.discordsoundboard.beans.User> getUsers() {
        String userNameToSelect = appProperties.getUsernameToJoinChannel();
        List<User> users = new ArrayList<>();
        for (Guild guild : bot.getGuilds()) {
            for (Member member : guild.getMembers()) {
                boolean selected = false;
                String username = member.getUser().getName();
                if (userNameToSelect.equals(username)) {
                    selected = true;
                }
                users.add(new net.dirtydeeds.discordsoundboard.beans.User(member.getUser().getId(), username, member.getOnlineStatus().name(), selected));
            }
        }
        Comparator<User> c = Comparator.comparing(User::getUsernameLowerCase);
        users.sort(c);
        return users;
    }

    public boolean isUserAllowed(String username, String userId) {
        if ((allowedUsers == null || allowedUsers.isEmpty()) && (allowedUserIds == null || allowedUserIds.isEmpty())) {
            return true;
        } else {
            return (allowedUsers != null && !allowedUsers.isEmpty() && allowedUsers.contains(username)) ||
                    (allowedUserIds != null && !allowedUserIds.isEmpty() && allowedUserIds.contains(userId));
        }
    }

    public boolean isUserBanned(String username, String userId) {
        return (bannedUsers != null && !bannedUsers.isEmpty() && bannedUsers.contains(username)) ||
                (bannedUserIds != null && !bannedUserIds.isEmpty() && bannedUserIds.contains(userId));
    }

    /**
     * Get the path the application is using for sound files.
     *
     * @return String representation of the sound file path.
     */
    public String getSoundsPath() {
        return soundFileDir;
    }

    public void sendPrivateMessage(MessageReceivedEvent event, String message) throws InterruptedException, java.util.concurrent.ExecutionException, java.util.concurrent.TimeoutException {
        RequestFuture<PrivateChannel> privateChannel = event.getAuthor().openPrivateChannel().submit();
        PrivateChannel channel = privateChannel.get(5, TimeUnit.SECONDS);
        channel.sendMessage(message).submit();
    }

    private SoundFile getSoundFileById(String soundFileId) {
        return soundFileRepository.findOneBySoundFileIdIgnoreCase(soundFileId);
    }

    /**
     * Delete a sound file from the repository and the filesystem.
     * @param soundFileId
     * @return true if the sound file was successfully deleted, false if the sound file was not found or there was an error
     */
    public boolean deleteSoundFileById(String soundFileId) {
        SoundFile soundFile = soundFileRepository.findOneBySoundFileIdIgnoreCase(soundFileId);
        if (soundFile == null) {
            return false;
        }
        soundFileRepository.delete(soundFile);
        try {
            Path path = Paths.get(soundFile.getSoundFileLocation());
            LOG.info("Deleting file={}", path);
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            LOG.error("Could not delete sound file", e);
            return false;
        }
    }


    /**
     * Find the "author" of the event and join the voice channel they are in.
     *
     * @param event - The event
     * @throws Exception Throws exception if the bot couldn't join the users channel.
     */
    private void moveToUserIdsChannel(MessageReceivedEvent event, Guild guild) throws Exception {
        VoiceChannel channel = findUsersChannel(event, guild);

        if (channel == null) {
            sendPrivateMessage(event, "Hello @" + event.getAuthor().getName() + "! I can not find you in any Voice Channel. Are you sure you are connected to voice?.");
            LOG.warn("Problem moving to requested users channel. Maybe user, " + event.getAuthor().getName() + " is not connected to Voice?");
        } else {
            moveToChannel(channel, guild);
        }
    }

    /**
     * Moves to the specified voice channel.
     *
     * @param channel - The channel specified.
     */
    private void moveToChannel(VoiceChannel channel, Guild guild) throws SoundPlaybackException {
        GuildMusicManager mng = getGuildAudioPlayer(guild);

        AudioManager audioManager = guild.getAudioManager();
        audioManager.setSendingHandler(mng.sendHandler);

        LOG.info("Connection Status during move to channel: " + audioManager.getConnectionStatus().toString());
        if (!audioManager.isAttemptingToConnect() || !audioManager.getConnectionStatus().equals(ConnectionStatus.CONNECTED)) {
            try {
                guild.getAudioManager().openAudioConnection(channel);
            } catch (PermissionException e) {
                if (e.getPermission() == Permission.VOICE_CONNECT) {
                    throw new SoundPlaybackException("The bot does not have permission to speak in the requested channel: " + channel.getName() + ".");
                }
            }

            int i = 0;
            int waitTime = 100;
            int maxIterations = 80;
            //Wait for the audio connection to be ready before proceeding.
            synchronized (this) {
                while (!audioManager.isConnected()) {
                    try {
                        wait(waitTime);
                        i++;
                        if (i >= maxIterations) {
                            break; //break out if after some time if it doesn't get a connection;
                        }
                    } catch (InterruptedException e) {
                        LOG.warn("Waiting for audio connection was interrupted.");
                    }
                }
            }
        }
    }

    /**
     * Finds a users voice channel based on event and what guild to look in.
     *
     * @param event - The event that triggered this search. This is used to get th events author.
     * @param guild - The guild (discord server) to look in for the author.
     * @return The VoiceChannel if one is found. Otherwise return null.
     */
    private VoiceChannel findUsersChannel(MessageReceivedEvent event, Guild guild) {
        VoiceChannel channel = null;

        outerloop:
        for (VoiceChannel channel1 : guild.getVoiceChannels()) {
            for (Member user : channel1.getMembers()) {
                if (user.getUser().getId().equals(event.getAuthor().getId())) {
                    channel = channel1;
                    break outerloop;
                }
            }
        }

        return channel;
    }

    /**
     * Join the users current channel.
     */
    private void joinUsersCurrentChannel(String userName) throws SoundPlaybackException {
        for (Guild guild : bot.getGuilds()) {
            for (VoiceChannel channel : guild.getVoiceChannels()) {
                for (Member user : channel.getMembers()) {
                    if (user.getUser().getName().equalsIgnoreCase(userName)) {
                        try {
                            moveToChannel(channel, guild);
                        } catch (SoundPlaybackException e) {
                            LOG.error(e.toString());
                            throw e;
                        }
                    }
                }
            }
        }
    }

    /**
     * Looks through all the guilds the bot has access to and returns the Guild the requested user is connected to.
     *
     * @param userName - The username to look for.
     * @return The voice channel the user is connected to. If user is not connected to a voice channel will return null.
     */
    private Guild getUsersGuild(String userName) {
        for (Guild guild : bot.getGuilds()) {
            for (VoiceChannel channel : guild.getVoiceChannels()) {
                for (Member user : channel.getMembers()) {
                    if (user.getUser().getName().equalsIgnoreCase(userName)) {
                        return guild;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Play file name requested. Will first try to load the file from the map of available sounds.
     *
     * @param fileName - fileName to play.
     * @param guild    - The guild (discord server) the playback is going to happen in.
     */
    private void playFile(String fileName, Guild guild) throws SoundPlaybackException {
        SoundFile fileToPlay = getSoundFileById(fileName);
        if (fileToPlay != null) {
            File soundFile = new File(fileToPlay.getSoundFileLocation());
            playFile(soundFile, guild);
        } else {
            throw new SoundPlaybackException("Could not find sound file that was requested.");
        }
    }

    /**
     * Play the provided File object
     *
     * @param audioFile - The File object to play.
     * @param guild     - The guild (discord server) the playback is going to happen in.
     */
    private void playFile(File audioFile, Guild guild) {
        playFile(audioFile.getAbsolutePath(), guild, 1);
    }

    /**
     * Play the provided File object
     *
     * @param audioFile    - The File object to play.
     * @param guild        - The guild (discord server) the playback is going to happen in.
     * @param repeatNumber - The number of times to repeat the audio file.
     */
    @Async
    private void playFile(String audioFile, Guild guild, int repeatNumber) {
        if (guild == null) {
            LOG.error("Guild is null. Have you added your bot to a guild? https://discordapp.com/developers/docs/topics/oauth2");
        } else {
            LOG.info("Attempting to play file " + audioFile);
            GuildMusicManager mng = getGuildAudioPlayer(guild);
            playerManager.loadItemOrdered(mng, audioFile, new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack track) {
                    if (repeatNumber <= 1) {
                        mng.scheduler.playNow(track, guild);
                    } else {
                        for (int i = 0; i <= repeatNumber - 1; i++) {
                            if (i == 0) {
                                mng.scheduler.queue(track, guild);
                            } else {
                                LOG.info("Queuing additional play of track.");
                                mng.scheduler.queue(track.makeClone(), guild);
                            }
                        }
                    }
                }

                @Override
                public void playlistLoaded(AudioPlaylist playlist) {
                    AudioTrack firstTrack = playlist.getSelectedTrack();

                    if (firstTrack == null) {
                        return;
                    }

                    mng.scheduler.playNow(firstTrack, guild);
                }

                @Override
                public void noMatches() {
                    // Notify the user that we've got nothing
                    LOG.debug("Could not find file");
                }

                @Override
                public void loadFailed(FriendlyException throwable) {
                    // Notify the user that everything exploded
                    LOG.error(throwable.getMessage());
                }
            });
        }
    }

    /**
     * This method loads the files. This checks if you are running from a .jar file and loads from the /sounds dir relative
     * to the jar file. If not it assumes you are running from code and loads relative to your resource dir.
     */
    public void getFileList() {
        try {

            soundFileDir = appProperties.getSoundsDirectory();

            if (soundFileDir == null || soundFileDir.isEmpty()) {
                soundFileDir = System.getProperty("user.dir") + "/sounds";
            }
            LOG.info("Loading from " + soundFileDir);
            Path soundFilePath = Paths.get(soundFileDir);

            if (!soundFilePath.toFile().exists()) {
                System.out.println("creating directory: " + soundFilePath.toFile().toString());
                boolean result = false;

                try {
                    result = soundFilePath.toFile().mkdir();
                } catch (SecurityException se) {
                    LOG.error("Could not create directory: " + soundFilePath.toFile().toString());
                }
                if (result) {
                    LOG.info("DIR: " + soundFilePath.toFile().toString() + " created.");
                }
            }

            Files.walk(soundFilePath).filter(p -> Files.isReadable(p) && !Files.isDirectory(p) && !Files.isSymbolicLink(p))
                    .forEach(filePath -> {
                        String fileName = filePath.getFileName().toString();
                        try {
                            fileName = fileName.substring(fileName.indexOf("/") + 1);
                            fileName = fileName.substring(0, fileName.indexOf("."));
                            LOG.info(fileName);
                            File file = filePath.toFile();
                            String parent = file.getParentFile().getName();
                            SoundFile soundFile = new SoundFile(fileName, filePath.toString(), parent, new Date(file.lastModified()));
                            SoundFile existing = soundFileRepository.findOneBySoundFileIdIgnoreCase(fileName);
                            if (existing != null) {
                                soundFileRepository.delete(existing);
                            }
                            soundFileRepository.save(soundFile);
                        } catch (Exception e) {
                            LOG.error(e.toString());
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            LOG.error(e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Logs the discord bot in and adds the ChatSoundBoardListener if the user configured it to be used
     */
    private void initializeDiscordBot() {
        try {
            if (bot != null) {
                bot.shutdown();
            }

            String botToken = appProperties.getBotToken();
            bot = new JDABuilder(AccountType.BOT)
                    .setAudioEnabled(true)
                    .setAutoReconnect(true)
                    .setToken(botToken)
                    .build()
                    .awaitReady();

            if (appProperties.isRespondToChatCommands()) {
                ChatSoundBoardListener chatListener = new ChatSoundBoardListener(this, appProperties, playEventRepository, soundFileRepository);
                this.addBotListener(chatListener);

                if (appProperties.isLeaveWhenLastUserInChannel()) {
                    DisconnectListener disconnectListener = new DisconnectListener();
                    this.addBotListener(disconnectListener);
                }
            }

            allowedUsers = appProperties.getAllowedUserIds();
            allowedUserIds = appProperties.getAllowedUserIds();
            bannedUsers = appProperties.getBannedUserIds();
            bannedUserIds = appProperties.getBannedUserIds();

            Game game = Game.of(Game.GameType.DEFAULT,"Type " + appProperties.getCommandCharacter() + "help for a list of commands.");
            bot.getPresence().setGame(game);

            try {
                File avatarFile = new File(System.getProperty("user.dir") + "/avatar.jpg");
                Icon icon = Icon.from(avatarFile);
                bot.getSelfUser().getManager().setAvatar(icon).queue();
            } catch (IllegalArgumentException e) {
                LOG.warn("Could not find avatar file " + System.getProperty("user.dir") + "/avatar.jpg");
            }
        } catch (IllegalArgumentException e) {
            LOG.warn("Something was configured incorrectly.", e);
        } catch (LoginException e) {
            LOG.warn("The provided bot token was incorrect. Please provide valid details.", e);
        } catch (InterruptedException e) {
            LOG.error("Login Interrupted.", e);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    @SuppressWarnings("unused")
    public void cleanUp() {
        System.out.println("SoundPlayer is shutting down. Cleaning up.");
        playerManager.shutdown();
        bot.shutdown();
    }

    /**
     * Sets listeners
     *
     * @param listener - The listener object to set.
     */
    private void addBotListener(Object listener) {
        bot.addEventListener(listener);
    }

    public void playTopSoundFile(String requestingUser, MessageReceivedEvent event, int number) throws SoundPlaybackException {
        try {
            List<SoundFilePlayEventCount> soundFilePlayEventCounts = soundFileRepository.getSoundFilePlayEventCountDesc();
            playRandom(requestingUser, event, number, soundFilePlayEventCounts);
        } catch (Exception e) {
            LOG.error("Problem playing top file.", e);
            throw new SoundPlaybackException("Problem playing top file.");
        }
    }

    public void playBottomSoundFile(String requestingUser, MessageReceivedEvent event, int number) throws SoundPlaybackException {
        try {
            List<SoundFilePlayEventCount> soundFilePlayEventCounts = soundFileRepository.getSoundFilePlayEventCountAsc();
            playRandom(requestingUser, event, number, soundFilePlayEventCounts);
        } catch (Exception e) {
            LOG.error("Problem playing top file.", e);
            throw new SoundPlaybackException("Problem playing top file.");
        }
    }

    private void playRandom(String requestingUser, MessageReceivedEvent event, int number, List<SoundFilePlayEventCount> soundFilePlayEventCounts) {
        SoundFilePlayEventCount randomValue;
        Random random = new SecureRandom();
        if (soundFilePlayEventCounts.size() == 0) {
            return;
        } else if (soundFilePlayEventCounts.size() == 1) {
            randomValue = soundFilePlayEventCounts.get(0);
        } else if (soundFilePlayEventCounts.size() < number) {
            int rand = random.nextInt(soundFilePlayEventCounts.size());
            randomValue = soundFilePlayEventCounts.get(rand);
        } else {
            int rand = random.nextInt(number);
            randomValue = soundFilePlayEventCounts.get(rand);
        }

        LOG.info("Attempting to play top file: " + randomValue.getSoundFileId() + ", requested by : " + requestingUser);
        try {
            if (event != null) {
                playFileForEvent(randomValue.getSoundFileId(), event);
            } else {
                playFileForUser(randomValue.getSoundFileId(), requestingUser);
            }
        } catch (Exception e) {
            LOG.error("Could not play top file: " + randomValue.getSoundFileId());
        }
    }
}
