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
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class LeaveServerCmd extends OwnerCommand {
    private final Bot bot;

    public LeaveServerCmd(Bot bot) {
        this.bot = bot;
        this.name = "leaveserver";
        this.help = "leaves the server specified";
        this.arguments = "<ServerID>";
        this.options = Collections.singletonList(
                new OptionData(OptionType.STRING, "server", "Server to leave")
                        .setRequired(true).setAutoComplete(true)
        );
        this.guildOnly = false;
    }

    @Override
    public void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
        if(event.getUser().getIdLong() != bot.getConfig().getOwnerId()) {
            event.replyChoice("You are not the owner of this bot!", "null").queue();
            return;
        }
        final List<Guild> guilds = bot.getJDA().getGuilds();
        final String val = event.getFocusedOption().getValue();
        final ArrayList<Command.Choice> choices = new ArrayList<>();
        int count = 0;
        for (Guild guild : guilds) {
            final String name = guild.getName();
            if (name.toLowerCase().contains(val.toLowerCase())) {
                choices.add(new Command.Choice(name+ " - "+guild.getId(), guild.getId()));
                count++;
                if (count >= 20) break;
            }
        }
        event.replyChoices(choices).queue();
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        if (event.hasOption("server")) {
            event.reply("Please specify a Server ID").setEphemeral(true).queue();
            return;
        }
        try {
            Long.parseLong(event.getOption("server").getAsString());
        } catch (Exception e) {
            event.reply("Please specify a valid Server ID").setEphemeral(true).queue();
            return;
        }
        final CompletableFuture<InteractionHook> reply = event.deferReply(true).submit();
        final Guild server = event.getJDA().getGuildById(event.getOption("server").getAsString());
        if (server == null) {
            reply.thenAccept((m)->m.editOriginal("Unknown Server").queue());
            return;
        }
        server.leave().complete();
        reply.thenAccept((m)->m.editOriginal("Left the Server \"" + server.getName() + "\"").queue());
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getArgs().isEmpty()) {
            event.replyError("Please specify a Server ID");
            return;
        }
        try {
            Long.parseLong(event.getArgs());
        } catch (Exception e) {
            event.replyError("Please specify a valid Server ID");
            return;
        }
        final Guild server = event.getJDA().getGuildById(event.getArgs());
        if (server == null) {
            event.replyError("Unknown Server");
            return;
        }
        server.leave().complete();
        event.reply("Left the Server \"" + server.getName() + "\"");
    }
}
