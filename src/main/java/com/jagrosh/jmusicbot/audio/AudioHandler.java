/*
 * Copyright 2016 John Grosh <john.a.grosh@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.jmusicbot.audio;

import com.github.topi314.lavasrc.mirror.MirroringAudioSourceManager;
import com.github.topi314.lavasrc.mirror.MirroringAudioTrack;
import com.github.topi314.lavasrc.spotify.SpotifyAudioTrack;
import com.jagrosh.jmusicbot.playlist.PlaylistLoader.Playlist;
import com.jagrosh.jmusicbot.queue.AbstractQueue;
import com.jagrosh.jmusicbot.settings.QueueType;
import com.jagrosh.jmusicbot.utils.TimeUtil;
import com.jagrosh.jmusicbot.settings.RepeatMode;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.*;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;

import java.time.Instant;
import java.util.*;

import com.jagrosh.jmusicbot.settings.Settings;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import java.nio.ByteBuffer;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class AudioHandler extends AudioEventAdapter implements AudioSendHandler
{
    public final static String PLAY_EMOJI  = "\u25B6"; // ▶
    public final static String PAUSE_EMOJI = "\u23F8"; // ⏸
    public final static String STOP_EMOJI  = "\u23F9"; // ⏹


    private final List<AudioTrack> defaultQueue = new LinkedList<>();
    private final Set<String> votes = new HashSet<>();
    
    private final PlayerManager manager;
    private final AudioPlayer audioPlayer;
    private final long guildId;
    
    private AudioFrame lastFrame;
    private AbstractQueue<QueuedTrack> queue;
    private int retries = 0;

    private final HashMap<Long,AudioTrack> trackHistory = new HashMap<>();

    protected AudioHandler(PlayerManager manager, Guild guild, AudioPlayer player)
    {
        this.manager = manager;
        this.audioPlayer = player;
        this.guildId = guild.getIdLong();

        this.setQueueType(manager.getBot().getSettingsManager().getSettings(guildId).getQueueType());
    }

    public HashMap<Long, AudioTrack> getTrackHistory() {
        return trackHistory;
    }

    public void setQueueType(QueueType type)
    {
        queue = type.createInstance(queue);
    }

    public int addTrackToFront(QueuedTrack qtrack)
    {
        if(audioPlayer.getPlayingTrack()==null)
        {
            audioPlayer.playTrack(qtrack.getTrack());
            return -1;
        }
        else
        {
            queue.addAt(0, qtrack);
            return 0;
        }
    }


    public int addTrack(QueuedTrack qtrack)
    {


        if(audioPlayer.getPlayingTrack()==null)
        {
            audioPlayer.playTrack(qtrack.getTrack());
            return -1;
        }
        else
            return queue.add(qtrack);
    }
    
    public AbstractQueue<QueuedTrack> getQueue()
    {
        return queue;
    }
    
    public void stopAndClear()
    {
        queue.clear();
        defaultQueue.clear();
        audioPlayer.stopTrack();
        //current = null;
    }
    public boolean isMusicPlaying(JDA jda)
    {
        return guild(jda).getSelfMember().getVoiceState().inAudioChannel() && audioPlayer.getPlayingTrack()!=null;
    }
    
    public Set<String> getVotes()
    {
        return votes;
    }
    
    public AudioPlayer getPlayer()
    {
        return audioPlayer;
    }
    
    public RequestMetadata getRequestMetadata()
    {
        if(audioPlayer.getPlayingTrack() == null)
            return RequestMetadata.EMPTY;
        RequestMetadata rm = audioPlayer.getPlayingTrack().getUserData(RequestMetadata.class);
        return rm == null ? RequestMetadata.EMPTY : rm;
    }
    
    public boolean playFromDefault()
    {
        if(!defaultQueue.isEmpty())
        {
            audioPlayer.playTrack(defaultQueue.remove(0));
            return true;
        }
        Settings settings = manager.getBot().getSettingsManager().getSettings(guildId);
        if(settings==null || settings.getDefaultPlaylist()==null)
            return false;
        
        Playlist pl = manager.getBot().getPlaylistLoader().getPlaylist(settings.getDefaultPlaylist());
        if(pl==null || pl.getItems().isEmpty())
            return false;
        pl.loadTracks(manager, (at) -> 
        {
            if(audioPlayer.getPlayingTrack()==null)
                audioPlayer.playTrack(at);
            else
                defaultQueue.add(at);
        }, () -> 
        {
            if(pl.getTracks().isEmpty() && !manager.getBot().getConfig().getStay())
                manager.getBot().closeAudioConnection(guildId);
        });
        return true;
    }
    public String getCurrentTrack(JDA jda) {
        if (this.isMusicPlaying(jda)) {
            final StringBuilder b = new StringBuilder();
            if(this.audioPlayer.getPlayingTrack().getInfo().author != null && !this.audioPlayer.getPlayingTrack().getInfo().author.isEmpty())
                b.append(this.audioPlayer.getPlayingTrack().getInfo().author).append(" - ");
            return b.append(this.audioPlayer.getPlayingTrack().getInfo().title).toString();
        }
        return "No music playing";
    }
    // Audio Events
    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) 
    {
        final RepeatMode repeatMode = manager.getBot().getSettingsManager().getSettings(guildId).getRepeatMode();
        // if the track ended normally, and we're in repeat mode, re-add it to the queue
        if(endReason==AudioTrackEndReason.FINISHED && repeatMode != RepeatMode.OFF)
        {
            final QueuedTrack clone = new QueuedTrack(track.makeClone(), track.getUserData(RequestMetadata.class));
            if(repeatMode == RepeatMode.ALL)
                queue.add(clone);
            else
                queue.addAt(0, clone);
        }
        if(endReason == AudioTrackEndReason.LOAD_FAILED && retries < 3){
            System.err.println("Playing track failed, retrying...");
            retries++;
            final QueuedTrack clone = new QueuedTrack(track.makeClone(), track.getUserData(RequestMetadata.class));
            queue.addAt(0, clone);
        }else{
            retries = 0;
        }
        if(queue.isEmpty())
        {
            if(!playFromDefault())
            {
                manager.getBot().getNowplayingHandler().onTrackUpdate(guildId, track, this);
                if(!manager.getBot().getConfig().getStay())
                    manager.getBot().closeAudioConnection(guildId);
                // unpause, in the case when the player was paused and the track has been skipped.
                // this is to prevent the player being paused next time it's being used.
                player.setPaused(false);
            }
        }
        else
        {
            QueuedTrack qt = queue.pull();
            player.playTrack(qt.getTrack());
        }
        trackHistory.put(Instant.now().getEpochSecond(), track);
        if(trackHistory.size()>9){
            final Long topmost = trackHistory.entrySet().stream().sorted(Map.Entry.comparingByKey()).iterator().next().getKey();
            trackHistory.remove(topmost);
        }
        SponsorblockHandler.removeSegment(track);
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        LoggerFactory.getLogger("AudioHandler").error("Track " + track.getIdentifier() + " has failed to play", exception);
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) 
    {
        votes.clear();
        manager.getBot().getNowplayingHandler().onTrackUpdate(guildId, track, this);
    }

    
    // Formatting
    public MessageCreateData getNowPlaying(JDA jda)
    {
        if(isMusicPlaying(jda))
        {
            Guild guild = guild(jda);
            AudioTrack track = audioPlayer.getPlayingTrack();
            MessageCreateBuilder mb = new MessageCreateBuilder();
            mb.addContent(FormatUtil.filter(manager.getBot().getConfig().getSuccess()+" **Now Playing in "+guild.getSelfMember().getVoiceState().getChannel().getAsMention()+"...**"));
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(guild.getSelfMember().getColor());
            RequestMetadata rm = getRequestMetadata();
            if(rm.getOwner() != 0L)
            {
                User u = guild.getJDA().getUserById(rm.user.id);
                if(u==null)
                    if(rm.user.discrim.equals("0000")) {
                        eb.setAuthor(rm.user.username, null, rm.user.avatar);
                    } else {
                        eb.setAuthor(FormatUtil.formatUsername(rm.user), null, rm.user.avatar);
                    }
                else
                    if(u.getDiscriminator().equals("0000")) {
                        eb.setAuthor(u.getName(), null, u.getEffectiveAvatarUrl());
                    } else {
                        eb.setAuthor(FormatUtil.formatUsername(u), null, u.getEffectiveAvatarUrl());
                    }
            }

            try 
            {
                eb.setTitle(track.getInfo().title, track.getInfo().uri);
            }
            catch(Exception e) 
            {
                eb.setTitle(track.getInfo().title);
            }

            if(track instanceof YoutubeAudioTrack && manager.getBot().getConfig().useNPImages())
            {
                eb.setThumbnail("https://img.youtube.com/vi/"+track.getIdentifier()+"/mqdefault.jpg");
            }
            long spYoutubeLen = 0;
            String spYoutubeUploader = null;
            if(track instanceof SpotifyAudioTrack sAT && manager.getBot().getConfig().useNPImages()){
                eb.setThumbnail(track.getInfo().artworkUrl);
                final AudioItem apply = ((MirroringAudioSourceManager) sAT.getSourceManager()).getResolver().apply(sAT);
                if(apply instanceof BasicAudioPlaylist pl){
                    if(!pl.getTracks().isEmpty() && pl.getTracks().get(0) instanceof dev.lavalink.youtube.track.YoutubeAudioTrack yat){
                        eb.addField("Resolved YouTube URL",yat.getInfo().uri,true);
                        eb.addField("Resolved YouTube Title",yat.getInfo().title,true);
                        spYoutubeLen = yat.getDuration();
                        spYoutubeUploader = yat.getInfo().author;
                    }
                }
            }
            if(track.getDuration() == spYoutubeLen) spYoutubeLen = 0;
            if(TimeUtil.formatTime(track.getDuration()).equals(TimeUtil.formatTime(spYoutubeLen))) spYoutubeLen = 0;
            if(track.getInfo().author.equals(spYoutubeUploader)) spYoutubeUploader = null;
/*
            eb.addField("identifier",track.getIdentifier(),true);
            eb.addField("uri",track.getInfo().uri,true);
            eb.addField("author",track.getInfo().author,true);
            eb.addField("artworkUrl",track.getInfo().artworkUrl,true);
            eb.addField("length",""+track.getInfo().length,true);
            eb.addField("title",track.getInfo().title,true);
            eb.addField("isrc",track.getInfo().isrc,true);
            eb.addField("state",track.getState().name(),true);
            eb.addField("isSeekable",track.isSeekable()?"true":"false",true);
            eb.addField("isStream",track.getInfo().isStream?"true":"false",true);
            eb.addField("Track Class",track.getClass().getCanonicalName(),true);
*/
            if(track.getInfo().author != null && !track.getInfo().author.isEmpty())
                eb.setFooter("Source: " + (spYoutubeUploader==null?track.getInfo().author:(spYoutubeUploader+" ("+track.getInfo().author+")")), null);

            double progress = (double)audioPlayer.getPlayingTrack().getPosition()/(spYoutubeLen!=0?spYoutubeLen:track.getDuration());
            eb.setDescription(getStatusEmoji()
                    + " "+FormatUtil.progressBar(progress)
                    + " `[" + TimeUtil.formatTime(track.getPosition()) + "/" + TimeUtil.formatTime((spYoutubeLen!=0?spYoutubeLen:track.getDuration())) + (spYoutubeLen==0?"":("("+TimeUtil.formatTime(track.getDuration())+")"))+ "]` "
                    + FormatUtil.volumeIcon(audioPlayer.getVolume()));
            
            return mb.setEmbeds(eb.build()).build();
        }
        else return null;
    }
    
    public MessageCreateData getNoMusicPlaying(JDA jda)
    {
        Guild guild = guild(jda);
        return new MessageCreateBuilder()
                .setContent(FormatUtil.filter(manager.getBot().getConfig().getSuccess()+" **Now Playing...**"))
                .setEmbeds(new EmbedBuilder()
                .setTitle("No music playing")
                .setDescription(STOP_EMOJI+" "+FormatUtil.progressBar(-1)+" "+FormatUtil.volumeIcon(audioPlayer.getVolume()))
                .setColor(guild.getSelfMember().getColor())
                .build()).build();
    }

    public String getTopicFormat(JDA jda)
    {
        if(isMusicPlaying(jda))
        {
            long userid = getRequestMetadata().getOwner();
            AudioTrack track = audioPlayer.getPlayingTrack();
            String title = track.getInfo().title;
            if(title==null || title.equals("Unknown Title"))
                title = track.getInfo().uri;
            return "**"+title+"** ["+(userid==0 ? "autoplay" : "<@"+userid+">")+"]"
                    + "\n" + getStatusEmoji() + " "
                    + "[" + FormatUtil.formatTime(track.getDuration()) + "] "
                    + FormatUtil.volumeIcon(audioPlayer.getVolume());
        }
        else return "No music playing " + STOP_EMOJI + " " + FormatUtil.volumeIcon(audioPlayer.getVolume());
    }

    public String getStatusEmoji()
    {
        return audioPlayer.isPaused() ? PAUSE_EMOJI : PLAY_EMOJI;
    }
    
    // Audio Send Handler methods
    /*@Override
    public boolean canProvide() 
    {
        if (lastFrame == null)
            lastFrame = audioPlayer.provide();

        return lastFrame != null;
    }

    @Override
    public byte[] provide20MsAudio() 
    {
        if (lastFrame == null) 
            lastFrame = audioPlayer.provide();

        byte[] data = lastFrame != null ? lastFrame.getData() : null;
        lastFrame = null;

        return data;
    }*/
    
    @Override
    public boolean canProvide() 
    {
        lastFrame = audioPlayer.provide();
        return lastFrame != null;
    }

    @Override
    public ByteBuffer provide20MsAudio() 
    {
        return ByteBuffer.wrap(lastFrame.getData());
    }

    @Override
    public boolean isOpus() 
    {
        return true;
    }
    
    
    // Private methods
    private Guild guild(JDA jda)
    {
        return jda.getGuildById(guildId);
    }
}
