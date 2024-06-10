package com.jagrosh.jmusicbot.audio;

import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.settings.Settings;
import com.jagrosh.jmusicbot.utils.SponsorblockUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import de.erdbeerbaerlp.jsponsorblock.Segment;
import net.dv8tion.jda.api.entities.Guild;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class SponsorblockHandler {
    private final Bot bot;
    private final ArrayList<Long> guilds;
    private static final HashMap<String, Segment[]> segmentCache = new HashMap<>();

    public SponsorblockHandler(Bot bot) {
        this.bot = bot;
        this.guilds = new ArrayList<>();
    }

    public void addGuild(long g) {
        guilds.add(g);
    }

    public void removeGuild(long guild) {
        guilds.remove(guild);
    }

    public static void addSegment(AudioTrack track, Segment[] segments) {
        segmentCache.put(track.getInfo().uri, segments);
    }

    public static void removeSegment(AudioTrack track) {
        segmentCache.remove(track.getInfo().uri);
    }

    public static boolean containsSegment(AudioTrack track) {
        return segmentCache.containsKey(track.getInfo().uri);
    }

    public void init() {
        bot.getThreadpool().scheduleWithFixedDelay(this::checkAll, 0, 500, TimeUnit.MILLISECONDS);
    }

    private void checkAll() {
        for (final long guildId : guilds) {
            final Settings s = bot.getSettingsManager().getSettings(guildId);
            if (s.getCategories().length > 0) {
                final Guild guild = bot.getJDA().getGuildById(guildId);
                if(guild == null) continue;
                final AudioHandler handler = (AudioHandler) guild.getAudioManager().getSendingHandler();
                if (handler != null && handler.isMusicPlaying(bot.getJDA())) {
                    if(handler.getPlayer() == null) continue;
                    if(handler.getPlayer().getPlayingTrack() == null) continue;
                    final long curPos = handler.getPlayer().getPlayingTrack().getPosition();
                    if(!segmentCache.containsKey(handler.getPlayer().getPlayingTrack().getInfo().uri)) continue;
                    final Segment[] segs = SponsorblockUtil.filterSegments(segmentCache.get(handler.getPlayer().getPlayingTrack().getInfo().uri), s.getCategories());
                    for (final Segment seg : segs) {
                        if (seg.getSegmentStart() * 1000 <= curPos && seg.getSegmentEnd() * 1000 >= curPos) {
                            handler.getPlayer().getPlayingTrack().setPosition((long) (seg.getSegmentEnd() * 1000));
                        }
                    }
                }
            }
        }
    }

}
