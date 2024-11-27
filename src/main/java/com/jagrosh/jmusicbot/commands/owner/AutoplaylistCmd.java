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
package com.jagrosh.jmusicbot.commands.owner;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.commands.OwnerCommand;
import com.jagrosh.jmusicbot.settings.Settings;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Collections;

/**
 *
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class AutoplaylistCmd extends OwnerCommand
{
    private final Bot bot;
    
    public AutoplaylistCmd(Bot bot)
    {
        this.bot = bot;
        this.guildOnly = true;
        this.name = "autoplaylist";
        this.arguments = "<name|NONE>";
        this.options = Collections.singletonList(
                new OptionData(OptionType.STRING, "name", "Name of the playlist, or \"NONE\"")
                        .setRequired(true)
        );
        this.help = "sets the default playlist for the server";
        this.aliases = bot.getConfig().getAliases(this.name);
    }

    @Override
    public void execute(SlashCommandEvent event)
    {
        if(!event.hasOption("name"))
        {
            event.reply(event.getClient().getError()+" Please include a playlist name or NONE").setEphemeral(true).queue();
            return;
        }
        if(event.getOption("name").getAsString().equalsIgnoreCase("none"))
        {
            Settings settings = event.getClient().getSettingsFor(event.getGuild());
            settings.setDefaultPlaylist(null);
            event.reply(event.getClient().getSuccess()+" Cleared the default playlist for **"+event.getGuild().getName()+"**").setEphemeral(true).queue();
            return;
        }
        String pname = event.getOption("name").getAsString().replaceAll("\\s+", "_");
        if(bot.getPlaylistLoader().getPlaylist(pname)==null)
        {
            event.reply(event.getClient().getError()+" Could not find `"+pname+".txt`!").setEphemeral(true).queue();
        }
        else
        {
            Settings settings = event.getClient().getSettingsFor(event.getGuild());
            settings.setDefaultPlaylist(pname);
            event.reply(event.getClient().getSuccess()+" The default playlist for **"+event.getGuild().getName()+"** is now `"+pname+"`").setEphemeral(true).queue();
        }
    }
    @Override
    public void execute(CommandEvent event)
    {
        if(event.getArgs().isEmpty())
        {
            event.reply(event.getClient().getError()+" Please include a playlist name or NONE");
            return;
        }
        if(event.getArgs().equalsIgnoreCase("none"))
        {
            Settings settings = event.getClient().getSettingsFor(event.getGuild());
            settings.setDefaultPlaylist(null);
            event.reply(event.getClient().getSuccess()+" Cleared the default playlist for **"+event.getGuild().getName()+"**");
            return;
        }
        String pname = event.getArgs().replaceAll("\\s+", "_");
        if(bot.getPlaylistLoader().getPlaylist(pname)==null)
        {
            event.reply(event.getClient().getError()+" Could not find `"+pname+".txt`!");
        }
        else
        {
            Settings settings = event.getClient().getSettingsFor(event.getGuild());
            settings.setDefaultPlaylist(pname);
            event.reply(event.getClient().getSuccess()+" The default playlist for **"+event.getGuild().getName()+"** is now `"+pname+"`");
        }
    }
}
