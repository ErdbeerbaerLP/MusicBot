/*
 * Copyright 2022 John Grosh <john.a.grosh@gmail.com>.
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
package com.jagrosh.jmusicbot.commands.admin;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.commands.AdminCommand;
import com.jagrosh.jmusicbot.settings.QueueType;
import com.jagrosh.jmusicbot.settings.Settings;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author Wolfgang Schwendtbauer
 */
public class QueueTypeCmd extends AdminCommand
{
    public QueueTypeCmd(Bot bot)
    {
        super();
        this.name = "queuetype";
        this.help = "changes the queue type";
        this.arguments = "[" + String.join("|", QueueType.getNames()) + "]";
        final ArrayList<Command.Choice> queueTypes = new ArrayList<>();
        for (QueueType c : QueueType.values()) {
            queueTypes.add(new Command.Choice(c.getUserFriendlyName().toLowerCase(), c.name()));
        }
        this.options = Collections.singletonList(
                new OptionData(OptionType.STRING, "queue-type", "Queue Type to use")
                        .setRequired(true)
        );
        this.aliases = bot.getConfig().getAliases(this.name);
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        QueueType value;
        Settings settings = event.getClient().getSettingsFor(event.getGuild());

        if (!event.hasOption("queue-type"))
        {
            QueueType currentType = settings.getQueueType();
            event.reply(currentType.getEmoji() + " Current queue type is: `" + currentType.getUserFriendlyName() + "`.").setEphemeral(true).queue();
            return;
        }

        try
        {
            value = QueueType.valueOf(event.getOption("queue-type").getAsString());
        }
        catch (IllegalArgumentException e)
        {
            event.reply("Invalid queue type. Valid types are: [" + String.join("|", QueueType.getNames()) + "]").setEphemeral(true).queue();
            return;
        }

        if (settings.getQueueType() != value)
        {
            settings.setQueueType(value);

            AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
            if (handler != null)
                handler.setQueueType(value);
        }

        event.reply(value.getEmoji() + " Queue type was set to `" + value.getUserFriendlyName() + "`.").setEphemeral(true).queue();
    }

    @Override
    protected void execute(CommandEvent event)
    {
        String args = event.getArgs();
        QueueType value;
        Settings settings = event.getClient().getSettingsFor(event.getGuild());

        if (args.isEmpty())
        {
            QueueType currentType = settings.getQueueType();
            event.reply(currentType.getEmoji() + " Current queue type is: `" + currentType.getUserFriendlyName() + "`.");
            return;
        }

        try
        {
            value = QueueType.valueOf(args.toUpperCase());
        }
        catch (IllegalArgumentException e)
        {
            event.replyError("Invalid queue type. Valid types are: [" + String.join("|", QueueType.getNames()) + "]");
            return;
        }

        if (settings.getQueueType() != value)
        {
            settings.setQueueType(value);

            AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
            if (handler != null)
                handler.setQueueType(value);
        }

        event.reply(value.getEmoji() + " Queue type was set to `" + value.getUserFriendlyName() + "`.");
    }
}
