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
package com.jagrosh.jmusicbot.commands.admin;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.commands.AdminCommand;
import com.jagrosh.jmusicbot.settings.Settings;
import de.erdbeerbaerlp.jsponsorblock.Category;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class SetsponsorblockCmd extends AdminCommand {
    public SetsponsorblockCmd(Bot bot) {
        this.name = "setsponsorblock";
        this.help = "sets sponsorblock settings for this server";
        this.arguments = "<category|category,category|off>";
        this.options = Collections.singletonList(
                new OptionData(OptionType.STRING, "categories", "Sponsorblock categories to enable")
                        .setRequired(true).setAutoComplete(true)
        );
        this.aliases = bot.getConfig().getAliases(this.name);
    }

    @Override
    public void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
        final ArrayList<net.dv8tion.jda.api.interactions.commands.Command.Choice> choices = new ArrayList<>();
        final String input = event.getFocusedOption().getValue();
        final String[] split = input.split(",");
        if(split.length == 1) choices.add(new Command.Choice("off", "off"));
        String prefix;
        if (input.endsWith(",")) {
            // If the input ends with a comma, return it as is.
            prefix = input;
        } else if (input.contains(",")) {
            // If the input does not end with a comma but contains commas,
            // find the last complete segment before the last comma.
            int lastCommaIndex = input.lastIndexOf(",");
            prefix = input.substring(0, lastCommaIndex + 1); // Include up to the last comma.
        } else {
            // If the input has no commas, return an empty string.
            prefix = "";
        }
        final String val = event.getFocusedOption().getValue().endsWith(",")?"":split[split.length-1];
        int count = 0;
        for (de.erdbeerbaerlp.jsponsorblock.Category cat : de.erdbeerbaerlp.jsponsorblock.Category.values()) {
            String name = cat.name();
            if (cat.name().toLowerCase().contains(val.toLowerCase())) {
                choices.add(new net.dv8tion.jda.api.interactions.commands.Command.Choice(prefix+name, prefix+name));
                count++;
                if (count >= 20) break;
            }
        }
        event.replyChoices(choices).queue();

    }

    @Override
    protected void execute(SlashCommandEvent event) {
        if (!event.hasOption("categories")) {
            event.reply(event.getClient().getError() + " Please include a category or off\nAvailable categories are: `" + Arrays.toString(de.erdbeerbaerlp.jsponsorblock.Category.values()) + "`").setEphemeral(true).queue();
            return;
        }
        Settings s = event.getClient().getSettingsFor(event.getGuild());
        if (event.getOption("categories").getAsString().equalsIgnoreCase("off")) {
            s.setCategories(new de.erdbeerbaerlp.jsponsorblock.Category[0]);
            event.reply(event.getClient().getSuccess() + " Sponsorblock has now been disabled!").setEphemeral(true).queue();
        } else {
            final ArrayList<de.erdbeerbaerlp.jsponsorblock.Category> cats = new ArrayList<>();
            for (String str : event.getOption("categories").getAsString().replace(" ", "").toUpperCase().split(",")) {
                try {
                    cats.add(de.erdbeerbaerlp.jsponsorblock.Category.valueOf(str));
                } catch (IllegalArgumentException e) {
                    event.reply(event.getClient().getError() + " The provided values are invalid!\nAvailable categories are: `" + Arrays.toString(de.erdbeerbaerlp.jsponsorblock.Category.values()) + "`").setEphemeral(true).queue();
                    return;
                }
            }
            s.setCategories(cats.toArray(new de.erdbeerbaerlp.jsponsorblock.Category[0]));
            event.reply(event.getClient().getSuccess() + " Sponsorblock has now been enabled!").setEphemeral(true).queue();

        }
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getArgs().isEmpty()) {
            event.reply(event.getClient().getError() + " Please include a category or off\nAvailable categories are: `" + Arrays.toString(de.erdbeerbaerlp.jsponsorblock.Category.values()) + "`");
            return;
        }
        Settings s = event.getClient().getSettingsFor(event.getGuild());
        if (event.getArgs().equalsIgnoreCase("off")) {
            s.setCategories(new de.erdbeerbaerlp.jsponsorblock.Category[0]);
            event.reply(event.getClient().getSuccess() + " Sponsorblock has now been disabled!");
        } else {
            final ArrayList<de.erdbeerbaerlp.jsponsorblock.Category> cats = new ArrayList<>();
            for (String str : event.getArgs().replace(" ", "").toUpperCase().split(",")) {
                try {
                    cats.add(de.erdbeerbaerlp.jsponsorblock.Category.valueOf(str));
                } catch (IllegalArgumentException e) {
                    event.reply(event.getClient().getError() + " The provided values are invalid!\nAvailable categories are: `" + Arrays.toString(de.erdbeerbaerlp.jsponsorblock.Category.values()) + "`");
                    return;
                }
            }
            s.setCategories(cats.toArray(new de.erdbeerbaerlp.jsponsorblock.Category[0]));
            event.reply(event.getClient().getSuccess() + " Sponsorblock has now been enabled!");

        }
    }

}
