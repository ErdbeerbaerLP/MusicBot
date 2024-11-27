/*
 * Copyright 2017 John Grosh <john.a.grosh@gmail.com>.
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
package com.jagrosh.jmusicbot.commands.owner;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.commands.OwnerCommand;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class SetstatusCmd extends OwnerCommand {
    public SetstatusCmd(Bot bot) {
        this.name = "setstatus";
        this.help = "sets the status the bot displays";
        this.arguments = "<status>";
        final ArrayList<Command.Choice> onlineStatus = new ArrayList<>();
        for (OnlineStatus c : OnlineStatus.values()) {
            onlineStatus.add(new Command.Choice(c.name().toLowerCase(), c.name()));
        }
        final ArrayList<Command.Choice> activityTypes = new ArrayList<>();
        for (Activity.ActivityType c : Activity.ActivityType.values()) {
            activityTypes.add(new Command.Choice(c.name().toLowerCase(), c.name()));
        }
        this.options = List.of(
                new OptionData(OptionType.STRING, "status", "Online status to display")
                        .setRequired(false).addChoices(onlineStatus),
                new OptionData(OptionType.STRING, "type", "Activity type of the status")
                        .setRequired(false).addChoices(activityTypes),
                new OptionData(OptionType.STRING, "text", "Text to display in status")
                        .setRequired(false),
                new OptionData(OptionType.STRING, "stream-url", "Stream url to use for streaming type. Will be ignored otherwise")
                        .setRequired(false)
        );
        this.aliases = bot.getConfig().getAliases(this.name);
        this.guildOnly = false;
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        final CompletableFuture<InteractionHook> s = event.deferReply(true).submit();
        final StringBuilder response = new StringBuilder();
        if (event.hasOption("status"))
            try {
                OnlineStatus status = OnlineStatus.fromKey(event.getOption("status").getAsString());
                if (status == OnlineStatus.UNKNOWN) {
                    response.append("Please include one of the following statuses: `ONLINE`, `IDLE`, `DND`, `INVISIBLE`").append("\n");
                } else {
                    event.getJDA().getPresence().setStatus(status);
                    response.append("Set the online status to `" + status.getKey().toUpperCase() + "`").append("\n");
                }
            } catch (Exception e) {
                response.append(event.getClient().getError() + " The online status could not be set!").append("\n");
            }
        if(event.hasOption("type")) {
            Activity.ActivityType type = Activity.ActivityType.valueOf(event.getOption("type").getAsString());
            event.getJDA().getPresence().setActivity(Activity.of(type, event.hasOption("text") ? event.getOption("text").toString() : event.getJDA().getPresence().getActivity().getName(), event.hasOption("stream-url") ? event.getOption("stream-url").toString() : event.getJDA().getPresence().getActivity().getUrl()));
            response.append("Activity status edited!");
        }else if(event.hasOption("text")){
            event.getJDA().getPresence().setActivity(Activity.of(event.getJDA().getPresence().getActivity().getType(), event.getOption("text").toString(), event.hasOption("stream-url") ? event.getOption("stream-url").toString() : event.getJDA().getPresence().getActivity().getUrl()));
            response.append("Activity status edited!");
        } else if(event.hasOption("stream-url")){
            event.getJDA().getPresence().setActivity(Activity.of(event.getJDA().getPresence().getActivity().getType(), event.getJDA().getPresence().getActivity().getName(), event.hasOption("stream-url") ? event.getOption("stream-url").toString() : event.getJDA().getPresence().getActivity().getUrl()));
            response.append("Activity status edited!");
        }

        s.thenAccept((e)->e.editOriginal(response.toString()).queue());
    }

    @Override
    protected void execute(CommandEvent event) {
        try {
            OnlineStatus status = OnlineStatus.fromKey(event.getArgs());
            if (status == OnlineStatus.UNKNOWN) {
                event.replyError("Please include one of the following statuses: `ONLINE`, `IDLE`, `DND`, `INVISIBLE`");
            } else {
                event.getJDA().getPresence().setStatus(status);
                event.replySuccess("Set the status to `" + status.getKey().toUpperCase() + "`");
            }
        } catch (Exception e) {
            event.reply(event.getClient().getError() + " The status could not be set!");
        }
    }
}
