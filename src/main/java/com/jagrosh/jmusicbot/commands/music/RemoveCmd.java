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
package com.jagrosh.jmusicbot.commands.music;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.commands.MusicCommand;
import com.jagrosh.jmusicbot.settings.Settings;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class RemoveCmd extends MusicCommand 
{
    public RemoveCmd(Bot bot)
    {
        super(bot);
        this.name = "remove";
        this.help = "removes a song from the queue";
        this.arguments = "<position|ALL>";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.options = Collections.singletonList(
                new OptionData(OptionType.STRING, "position", "Track to remove from the queue")
                        .setRequired(true).setAutoComplete(true)
        );
        this.beListening = true;
        this.bePlaying = true;
    }

    @Override
    public void doCommand(CommandEvent event) 
    {
        AudioHandler handler = (AudioHandler)event.getGuild().getAudioManager().getSendingHandler();
        if(handler.getQueue().isEmpty())
        {
            event.replyError("There is nothing in the queue!");
            return;
        }
        if(event.getArgs().equalsIgnoreCase("all"))
        {
            int count = handler.getQueue().removeAll(event.getAuthor().getIdLong());
            if(count==0)
                event.replyWarning("You don't have any songs in the queue!");
            else
                event.replySuccess("Successfully removed your "+count+" entries.");
            return;
        }
        int pos;
        try {
            pos = Integer.parseInt(event.getArgs());
        } catch(NumberFormatException e) {
            pos = 0;
        }
        if(pos<1 || pos>handler.getQueue().size())
        {
            event.replyError("Position must be a valid integer between 1 and "+handler.getQueue().size()+"!");
            return;
        }
        Settings settings = event.getClient().getSettingsFor(event.getGuild());
        boolean isDJ = event.getMember().hasPermission(Permission.MANAGE_SERVER);
        if(!isDJ)
            isDJ = event.getMember().getRoles().contains(settings.getRole(event.getGuild()));
        QueuedTrack qt = handler.getQueue().get(pos-1);
        if(qt.getIdentifier()==event.getAuthor().getIdLong())
        {
            handler.getQueue().remove(pos-1);
            event.replySuccess("Removed **"+qt.getTrack().getInfo().title+"** from the queue");
        }
        else if(isDJ)
        {
            handler.getQueue().remove(pos-1);
            User u;
            try {
                u = event.getJDA().getUserById(qt.getIdentifier());
            } catch(Exception e) {
                u = null;
            }
            event.replySuccess("Removed **"+qt.getTrack().getInfo().title
                    +"** from the queue (requested by "+(u==null ? "someone" : "**"+u.getName()+"**")+")");
        }
        else
        {
            event.replyError("You cannot remove **"+qt.getTrack().getInfo().title+"** because you didn't add it!");
        }
    }

    @Override
    public void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
        final AudioHandler h = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        final List<QueuedTrack> queue = h.getQueue().getList();
        final String val = event.getFocusedOption().getValue();
        final ArrayList<Command.Choice> choices = new ArrayList<>();
        int count = 0;
        for (final QueuedTrack track : queue) {
            final String name = track.getTrack().getInfo().title;
            if (name.toLowerCase().contains(val.toLowerCase())) {
                choices.add(new Command.Choice(name, queue.indexOf(track)));
                count++;
                if (count >= 20) break;
            }
        }
        event.replyChoices(choices).queue();
    }

    @Override
    public void doCommand(SlashCommandEvent event)
    {
        AudioHandler handler = (AudioHandler)event.getGuild().getAudioManager().getSendingHandler();
        if(handler.getQueue().isEmpty())
        {
            event.reply("There is nothing in the queue!").queue();
            return;
        }
        if(event.getOption("position").getAsString().equalsIgnoreCase("all"))
        {
            int count = handler.getQueue().removeAll(event.getUser().getIdLong());
            if(count==0)
                event.reply("You don't have any songs in the queue!").queue();
            else
                event.reply("Successfully removed your "+count+" entries.").queue();
            return;
        }
        int pos;
        try {
            pos = Integer.parseInt(event.getOption("position").getAsString());
        } catch(NumberFormatException e) {
            pos = 0;
        }
        if(pos<1 || pos>handler.getQueue().size())
        {
            event.reply("Position must be a valid integer between 1 and "+handler.getQueue().size()+"!").queue();
            return;
        }
        Settings settings = event.getClient().getSettingsFor(event.getGuild());
        boolean isDJ = event.getMember().hasPermission(Permission.MANAGE_SERVER);
        if(!isDJ)
            isDJ = event.getMember().getRoles().contains(settings.getRole(event.getGuild()));
        QueuedTrack qt = handler.getQueue().get(pos-1);
        if(qt.getIdentifier()==event.getUser().getIdLong())
        {
            handler.getQueue().remove(pos-1);
            event.reply("Removed **"+qt.getTrack().getInfo().title+"** from the queue").queue();
        }
        else if(isDJ)
        {
            handler.getQueue().remove(pos-1);
            User u;
            try {
                u = event.getJDA().getUserById(qt.getIdentifier());
            } catch(Exception e) {
                u = null;
            }
            event.reply("Removed **"+qt.getTrack().getInfo().title
                    +"** from the queue (requested by "+(u==null ? "someone" : "**"+u.getName()+"**")+")").queue();
        }
        else
        {
            event.reply("You cannot remove **"+qt.getTrack().getInfo().title+"** because you didn't add it!").queue();
        }
    }
}
