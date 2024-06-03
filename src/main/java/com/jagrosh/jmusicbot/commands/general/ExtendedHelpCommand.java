package com.jagrosh.jmusicbot.commands.general;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.JMusicBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;

public class ExtendedHelpCommand {
    public static void execute(CommandEvent event) {
        StringBuilder builder = new StringBuilder("**" + event.getSelfUser().getName() + "** commands:\n");
        Command.Category category = null;
        Iterator<Command> var8 = JMusicBot.client.getCommands().iterator();

        while(true) {
            Command command;
            do {
                do {
                    if (!var8.hasNext()) {
                        User owner = event.getJDA().getUserById(JMusicBot.client.getOwnerId());
                        if (owner != null) {
                            builder.append("\n\nFor additional help, contact **").append(owner.getName()).append("**#").append(owner.getDiscriminator());
                            if (JMusicBot.client.getServerInvite() != null) {
                                builder.append(" or join ").append(JMusicBot.client.getServerInvite());
                            }
                        }
                        final ArrayList<String> strings = CommandEvent.splitMessage(builder.toString());
                        final boolean[] alreadyReplied = {false};
                        for (String s : strings) {
                            event.replyInDm(s, (unused) -> {
                                if(alreadyReplied[0]) return;
                                if (event.isFromType(ChannelType.TEXT)) {
                                    event.reactSuccess();
                                    alreadyReplied[0] = true;
                                }

                            }, (t) -> {
                                if(alreadyReplied[0]) return;
                                event.replyWarning("Help cannot be sent because you are blocking Direct Messages.");
                                alreadyReplied[0] = true;
                            });
                        }
                        return;
                    }

                    command = var8.next();
                } while(command.isHidden());
            } while(command.isOwnerCommand() && !event.isOwner());

            if (!Objects.equals(category, command.getCategory())) {
                category = command.getCategory();
                builder.append("\n\n  __").append(category == null ? "No Category" : category.getName()).append("__:\n");
            }


            builder.append("\n`")
                    .append(JMusicBot.client.getTextualPrefix())
                    .append(JMusicBot.client.getPrefix() == null ? " " : "")
                    .append(command.getName()).append(command.getArguments() == null ? "`" : " " + command.getArguments() + "`").append(" - ").append(command.getHelp());
        }
    }
}
