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
package com.jagrosh.jmusicbot.commands.music;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jdautilities.menu.OrderedMenu;
import com.jagrosh.jdautilities.menu.Paginator;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.audio.RequestMetadata;
import com.jagrosh.jmusicbot.commands.MusicCommand;
import com.jagrosh.jmusicbot.settings.QueueType;
import com.jagrosh.jmusicbot.settings.RepeatMode;
import com.jagrosh.jmusicbot.settings.Settings;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import com.jagrosh.jmusicbot.utils.TimeUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 *
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class HistoryCmd extends MusicCommand
{
    private final OrderedMenu.Builder builder;
    private final String searchingEmoji;

    public HistoryCmd(Bot bot)
    {
        super(bot);
        this.name = "history";
        this.help = "shows previous tracks";
        this.searchingEmoji = bot.getConfig().getSearching();
        this.aliases = bot.getConfig().getAliases(this.name);
        this.beListening = true;
        this.bePlaying = false;
        this.botPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.options = Collections.singletonList(
                new OptionData(OptionType.STRING, "track", "Entry from the history to play")
                        .setRequired(true).setAutoComplete(true)
        );
        builder = new OrderedMenu.Builder()
            .allowTextInput(true)
            .useNumbers()
            .useCancelButton(false)
            .setEventWaiter(bot.getWaiter())
            .setTimeout(1, TimeUnit.MINUTES);
    }

    @Override
    public void doCommand(CommandEvent event)
    {
        AudioHandler ah = (AudioHandler)event.getGuild().getAudioManager().getSendingHandler();
        final HashMap<Long, AudioTrack> history = ah.getTrackHistory().entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));;
        if(history.isEmpty()){
            event.reply(event.getClient().getWarning()+" No history available.");
            return;
        }
        final ArrayList<AudioTrack> tracks = new ArrayList<>();
        final Iterator<Long> historyIterator = history.keySet().iterator();
        builder.setColor(event.getSelfMember().getColor())
                .setText(FormatUtil.filter(event.getClient().getSuccess()+" Track History:"))
                .setChoices(new String[0])
                .setSelection((msg,i) ->
                {
                    AudioTrack track = tracks.get(i-1);
                    if(bot.getConfig().isTooLong(track))
                    {
                        event.replyWarning("This track (**"+track.getInfo().title+"**) is longer than the allowed maximum: `"
                                + TimeUtil.formatTime(track.getDuration())+"` > `"+bot.getConfig().getMaxTime()+"`");
                        return;
                    }
                    AudioHandler handler = (AudioHandler)event.getGuild().getAudioManager().getSendingHandler();
                    int pos = handler.addTrack(new QueuedTrack(track.makeClone(), RequestMetadata.fromResultHandler(track, event)))+1;
                    event.replySuccess("Added **" + FormatUtil.filter(track.getInfo().title)
                            + "** (`" + TimeUtil.formatTime(track.getDuration()) + "`) " + (pos==0 ? "to begin playing"
                            : " to the queue at position "+pos));
                })
                .setCancel((msg) -> {})
                .setUsers(event.getAuthor())
        ;

        while(historyIterator.hasNext())
        {
            final Long time = historyIterator.next();
            AudioTrack track = history.get(time);
            tracks.add(track);
            builder.addChoices("<t:"+time+":R> `["+ TimeUtil.formatTime(track.getDuration())+"]` [**"+track.getInfo().title+"**]("+track.getInfo().uri+")");
        }
        event.reply(searchingEmoji+"Loading history...", m->builder.build().display(m));
    }

    @Override
    public void doCommand(SlashCommandEvent event)
    {
        AudioHandler ah = (AudioHandler)event.getGuild().getAudioManager().getSendingHandler();
        final HashMap<Long, AudioTrack> history = ah.getTrackHistory().entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));;
        if(!event.hasOption("track")){
            event.reply(event.getClient().getWarning()+" No history available.").queue();
            return;
        }
        final CompletableFuture<InteractionHook> reply = event.deferReply().submit();
        AudioTrack track = history.get(event.getOption("track"));
        if(bot.getConfig().isTooLong(track))
                    {
                        reply.thenAccept((m)->m.editOriginal("This track (**"+track.getInfo().title+"**) is longer than the allowed maximum: `"
                                + TimeUtil.formatTime(track.getDuration())+"` > `"+bot.getConfig().getMaxTime()+"`").queue());
                        return;
                    }
        AudioHandler handler = (AudioHandler)event.getGuild().getAudioManager().getSendingHandler();
        int pos = handler.addTrack(new QueuedTrack(track.makeClone(), RequestMetadata.fromResultHandler(track, event, null)))+1;
        reply.thenAccept((m)->m.editOriginal("Added **" + FormatUtil.filter(track.getInfo().title)
                            + "** (`" + TimeUtil.formatTime(track.getDuration()) + "`) " + (pos==0 ? "to begin playing"
                            : " to the queue at position "+pos)).queue());

    }

    @Override
    public void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
        AudioHandler ah = (AudioHandler)event.getGuild().getAudioManager().getSendingHandler();
        final HashMap<Long, AudioTrack> history = ah.getTrackHistory().entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));;


        final ArrayList<Command.Choice> tracks = new ArrayList<>();
        for (Long time : history.keySet()) {
            AudioTrack track = history.get(time);
            tracks.add(new Command.Choice(track.getInfo().title, time));
            builder.addChoices("[" + TimeUtil.formatTime(track.getDuration()) + "] " + track.getInfo().title);
        }

    }
}
