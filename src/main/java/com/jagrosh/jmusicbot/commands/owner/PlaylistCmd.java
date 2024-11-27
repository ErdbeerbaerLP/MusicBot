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

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.commands.OwnerCommand;
import com.jagrosh.jmusicbot.playlist.PlaylistLoader.Playlist;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class PlaylistCmd extends OwnerCommand {
    private final Bot bot;

    public PlaylistCmd(Bot bot) {
        this.bot = bot;
        this.guildOnly = false;
        this.name = "playlist";
        this.arguments = "<append|delete|make|setdefault>";
        this.help = "playlist management";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.children = new OwnerCommand[]{
                new ListCmd(),
                new AppendlistCmd(),
                new DeletelistCmd(),
                new MakelistCmd(),
                new DefaultlistCmd(bot)
        };
    }

    @Override
    protected void execute(SlashCommandEvent slashCommandEvent) {

    }

    @Override
    public void execute(CommandEvent event) {
        StringBuilder builder = new StringBuilder(event.getClient().getWarning() + " Playlist Management Commands:\n");
        for (Command cmd : this.children)
            builder.append("\n`").append(event.getClient().getPrefix()).append(name).append(" ").append(cmd.getName())
                    .append(" ").append(cmd.getArguments() == null ? "" : cmd.getArguments()).append("` - ").append(cmd.getHelp());
        event.reply(builder.toString());
    }

    public class MakelistCmd extends OwnerCommand {
        public MakelistCmd() {
            this.name = "make";
            this.aliases = new String[]{"create"};
            this.help = "makes a new playlist";
            this.arguments = "<name>";
            this.options = Collections.singletonList(
                    new OptionData(OptionType.STRING, "name", "Name of the playlist")
                            .setRequired(true)
            );
            this.guildOnly = false;
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            if (!event.hasOption("name") || event.getOption("name").getAsString().isEmpty()) {
                event.reply("Please provide a name for the playlist!").setEphemeral(true).queue();
            } else {
                String pname = event.getOption("name").getAsString().replaceAll("\\s+", "_");
                pname = pname.replaceAll("[*?|\\/\":<>]", "");
                if (bot.getPlaylistLoader().getPlaylist(pname) == null) {
                    try {
                        bot.getPlaylistLoader().createPlaylist(pname);
                        event.reply(event.getClient().getSuccess() + " Successfully created playlist `" + pname + "`!").setEphemeral(true).queue();
                    } catch (IOException e) {
                        event.reply(event.getClient().getError() + " I was unable to create the playlist: " + e.getLocalizedMessage()).setEphemeral(true).queue();
                    }
                } else
                    event.reply(event.getClient().getError() + " Playlist `" + pname + "` already exists!").setEphemeral(true).queue();
            }
        }

        @Override
        protected void execute(CommandEvent event) {
            String pname = event.getArgs().replaceAll("\\s+", "_");
            pname = pname.replaceAll("[*?|\\/\":<>]", "");
            if (pname == null || pname.isEmpty()) {
                event.replyError("Please provide a name for the playlist!");
            } else if (bot.getPlaylistLoader().getPlaylist(pname) == null) {
                try {
                    bot.getPlaylistLoader().createPlaylist(pname);
                    event.reply(event.getClient().getSuccess() + " Successfully created playlist `" + pname + "`!");
                } catch (IOException e) {
                    event.reply(event.getClient().getError() + " I was unable to create the playlist: " + e.getLocalizedMessage());
                }
            } else
                event.reply(event.getClient().getError() + " Playlist `" + pname + "` already exists!");
        }
    }

    public class DeletelistCmd extends OwnerCommand {
        public DeletelistCmd() {
            this.name = "delete";
            this.aliases = new String[]{"remove"};
            this.help = "deletes an existing playlist";
            this.arguments = "<name>";
            this.options = Collections.singletonList(
                    new OptionData(OptionType.STRING, "name", "Name of the playlist")
                            .setRequired(true)
            );
            this.guildOnly = false;
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            if (!event.hasOption("name") || event.getOption("name").getAsString().isEmpty()) {
                event.reply("Please provide a name for the playlist!").setEphemeral(true).queue();
            } else {
                String pname = event.getOption("name").getAsString().replaceAll("\\s+", "_");
                pname = pname.replaceAll("[*?|\\/\":<>]", "");
                try {
                    bot.getPlaylistLoader().deletePlaylist(pname);
                    event.reply(event.getClient().getSuccess() + " Successfully deleted playlist `" + pname + "`!");
                } catch (IOException e) {
                    event.reply(event.getClient().getError() + " I was unable to delete the playlist: " + e.getLocalizedMessage());
                }
            }
        }

        @Override
        protected void execute(CommandEvent event) {
            String pname = event.getArgs().replaceAll("\\s+", "_");
            if (bot.getPlaylistLoader().getPlaylist(pname) == null)
                event.reply(event.getClient().getError() + " Playlist `" + pname + "` doesn't exist!");
            else {
                try {
                    bot.getPlaylistLoader().deletePlaylist(pname);
                    event.reply(event.getClient().getSuccess() + " Successfully deleted playlist `" + pname + "`!");
                } catch (IOException e) {
                    event.reply(event.getClient().getError() + " I was unable to delete the playlist: " + e.getLocalizedMessage());
                }
            }
        }
    }

    public class AppendlistCmd extends OwnerCommand {
        public AppendlistCmd() {
            this.name = "append";
            this.aliases = new String[]{"add"};
            this.help = "appends songs to an existing playlist";
            this.arguments = "<name> <URL> | <URL> | ...";
            this.options = List.of(
                    new OptionData(OptionType.STRING, "name", "Name of the playlist")
                            .setRequired(true),
                    new OptionData(OptionType.STRING, "urls", "URL(s) to add. Seperate URLs with | symbols")
                            .setRequired(true)
            );
            this.guildOnly = false;
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            if (!event.hasOption("name") || !event.hasOption("urls")) {
                event.reply(event.getClient().getError() + " Please include a playlist name and URLs to add!").setEphemeral(true).queue();
                return;
            }
            String pname = event.getOption("name").getAsString();
            Playlist playlist = bot.getPlaylistLoader().getPlaylist(pname);
            if (playlist == null)
                event.reply(event.getClient().getError() + " Playlist `" + pname + "` doesn't exist!").setEphemeral(true).queue();
            else {
                final CompletableFuture<InteractionHook> r = event.deferReply(true).submit();
                StringBuilder builder = new StringBuilder();
                playlist.getItems().forEach(item -> builder.append("\r\n").append(item));
                String[] urls = event.getOption("urls").getAsString().split("\\|");
                for (String url : urls) {
                    String u = url.trim();
                    if (u.startsWith("<") && u.endsWith(">"))
                        u = u.substring(1, u.length() - 1);
                    builder.append("\r\n").append(u);
                }
                try {
                    bot.getPlaylistLoader().writePlaylist(pname, builder.toString());
                    r.thenAccept((m)->m.editOriginal(event.getClient().getSuccess() + " Successfully added " + urls.length + " items to playlist `" + pname + "`!").queue());
                } catch (IOException e) {
                    r.thenAccept((m)->m.editOriginal(event.getClient().getError() + " I was unable to append to the playlist: " + e.getLocalizedMessage()).queue());
                }
            }
        }

    @Override
    protected void execute(CommandEvent event) {
        String[] parts = event.getArgs().split("\\s+", 2);
        if (parts.length < 2) {
            event.reply(event.getClient().getError() + " Please include a playlist name and URLs to add!");
            return;
        }
        String pname = parts[0];
        Playlist playlist = bot.getPlaylistLoader().getPlaylist(pname);
        if (playlist == null)
            event.reply(event.getClient().getError() + " Playlist `" + pname + "` doesn't exist!");
        else {
            StringBuilder builder = new StringBuilder();
            playlist.getItems().forEach(item -> builder.append("\r\n").append(item));
            String[] urls = parts[1].split("\\|");
            for (String url : urls) {
                String u = url.trim();
                if (u.startsWith("<") && u.endsWith(">"))
                    u = u.substring(1, u.length() - 1);
                builder.append("\r\n").append(u);
            }
            try {
                bot.getPlaylistLoader().writePlaylist(pname, builder.toString());
                event.reply(event.getClient().getSuccess() + " Successfully added " + urls.length + " items to playlist `" + pname + "`!");
            } catch (IOException e) {
                event.reply(event.getClient().getError() + " I was unable to append to the playlist: " + e.getLocalizedMessage());
            }
        }
    }
}

public class DefaultlistCmd extends AutoplaylistCmd {
    public DefaultlistCmd(Bot bot) {
        super(bot);
        this.name = "setdefault";
        this.aliases = new String[]{"default"};
        this.arguments = "<playlistname|NONE>";
        this.guildOnly = true;
    }
}

public class ListCmd extends OwnerCommand {
    public ListCmd() {
        this.name = "list";
        this.aliases = new String[]{"available", "all"};
        this.help = "lists all available playlists";
        this.guildOnly = true;
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        if (!bot.getPlaylistLoader().folderExists())
            bot.getPlaylistLoader().createFolder();
        if (!bot.getPlaylistLoader().folderExists()) {
            event.reply(event.getClient().getWarning() + " Playlists folder does not exist and could not be created!").setEphemeral(true).queue();
            return;
        }
        List<String> list = bot.getPlaylistLoader().getPlaylistNames();
        if (list == null)
            event.reply(event.getClient().getError() + " Failed to load available playlists!").setEphemeral(true).queue();
        else if (list.isEmpty())
            event.reply(event.getClient().getWarning() + " There are no playlists in the Playlists folder!").setEphemeral(true).queue();
        else {
            StringBuilder builder = new StringBuilder(event.getClient().getSuccess() + " Available playlists:\n");
            list.forEach(str -> builder.append("`").append(str).append("` "));
            event.reply(builder.toString()).setEphemeral(true).queue();
        }
    }
    @Override
    protected void execute(CommandEvent event) {
        if (!bot.getPlaylistLoader().folderExists())
            bot.getPlaylistLoader().createFolder();
        if (!bot.getPlaylistLoader().folderExists()) {
            event.reply(event.getClient().getWarning() + " Playlists folder does not exist and could not be created!");
            return;
        }
        List<String> list = bot.getPlaylistLoader().getPlaylistNames();
        if (list == null)
            event.reply(event.getClient().getError() + " Failed to load available playlists!");
        else if (list.isEmpty())
            event.reply(event.getClient().getWarning() + " There are no playlists in the Playlists folder!");
        else {
            StringBuilder builder = new StringBuilder(event.getClient().getSuccess() + " Available playlists:\n");
            list.forEach(str -> builder.append("`").append(str).append("` "));
            event.reply(builder.toString());
        }
    }
}
}
