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
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.commands.AdminCommand;
import com.jagrosh.jmusicbot.settings.Settings;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class SetsponsorblockCmd extends AdminCommand {
    public SetsponsorblockCmd(Bot bot) {
        this.name = "setsponsorblock";
        this.help = "sets sponsorblock settings for this server";
        this.arguments = "<category|category,category|off>";
        this.aliases = bot.getConfig().getAliases(this.name);
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
            for (String str : event.getArgs().split(",")) {
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
