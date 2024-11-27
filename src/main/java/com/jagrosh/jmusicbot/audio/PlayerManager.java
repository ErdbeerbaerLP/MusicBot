/*
 * Copyright 2018 John Grosh <john.a.grosh@gmail.com>.
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

import com.dunctebot.sourcemanagers.DuncteBotSources;
import com.github.topi314.lavasrc.spotify.SpotifySourceManager;
import com.jagrosh.jmusicbot.Bot;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerRegistry;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.getyarn.GetyarnAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.nico.NicoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.clients.*;
import net.dv8tion.jda.api.entities.Guild;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class PlayerManager extends DefaultAudioPlayerManager {
    private final Bot bot;

    public PlayerManager(Bot bot) {
        this.bot = bot;
    }

    File refreshTokenFile = new File("./.ytRefreshToken");

    public void init() {
        TransformativeAudioSourceManager.createTransforms(bot.getConfig().getTransforms()).forEach(t -> registerSourceManager(t));

        YoutubeAudioSourceManager yt = new YoutubeAudioSourceManager(true, new Web(), new WebEmbedded(), new TvHtml5Embedded(), new Ios(), new Music());
        yt.setPlaylistPageCount(bot.getConfig().getMaxYTPlaylistPages());
        if (bot.getConfig().isOAUTHEnabled())
            if (!refreshTokenFile.exists()) {
                try {
                    refreshTokenFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                yt.useOauth2(null, false);

                while (yt.getOauth2RefreshToken() == null) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                try {
                    Files.writeString(refreshTokenFile.toPath(), yt.getOauth2RefreshToken());
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else {
                try {
                    yt.useOauth2(Files.readString(refreshTokenFile.toPath()), true);
                    while (yt.getOauth2RefreshToken() == null) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                    try {
                        Files.writeString(refreshTokenFile.toPath(), yt.getOauth2RefreshToken());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    yt.useOauth2(null, false);
                }
            }

        registerSourceManager(yt);

        registerSourceManager(SoundCloudAudioSourceManager.createDefault());

        registerSourceManager(new BandcampAudioSourceManager());

        registerSourceManager(new VimeoAudioSourceManager());

        registerSourceManager(new TwitchStreamAudioSourceManager());

        registerSourceManager(new BeamAudioSourceManager());

        registerSourceManager(new GetyarnAudioSourceManager());

        registerSourceManager(new NicoAudioSourceManager());

        registerSourceManager(new HttpAudioSourceManager(MediaContainerRegistry.DEFAULT_REGISTRY));

        if (bot.getConfig().

                isSpotifyEnabled()) {
            final SpotifySourceManager spotify = new SpotifySourceManager(new String[]{"ytsearch:%ISRC%", "ytsearch:%QUERY% music", "ytsearch:%QUERY% song", "ytsearch:%QUERY%"}, bot.getConfig().getSpotifyClientID(), bot.getConfig().getSpotifyClientSecret(), "DE", this);
            spotify.setPlaylistPageLimit(25);
            spotify.setAlbumPageLimit(25);
            registerSourceManager(spotify);
        }

        AudioSourceManagers.registerLocalSource(this);

        DuncteBotSources.registerAll(this, "en-US");
    }

    public Bot getBot() {
        return bot;
    }

    public boolean hasHandler(Guild guild) {
        return guild.getAudioManager().getSendingHandler() != null;
    }

    public AudioHandler setUpHandler(Guild guild) {
        AudioHandler handler;
        if (guild.getAudioManager().getSendingHandler() == null) {
            AudioPlayer player = createPlayer();
            player.setVolume(bot.getSettingsManager().getSettings(guild).getVolume());
            handler = new AudioHandler(this, guild, player);
            player.addListener(handler);
            guild.getAudioManager().setSendingHandler(handler);
        } else
            handler = (AudioHandler) guild.getAudioManager().getSendingHandler();
        return handler;
    }
}
