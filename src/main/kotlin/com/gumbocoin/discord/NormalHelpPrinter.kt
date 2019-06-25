package com.gumbocoin.discord

import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono

class NormalHelpPrinter :HelpCommand{
    override fun getFor(event: MessageCreateEvent): Command {

        return object :Command(
            names = listOf("help"),
            desc = "Help Command",
            permission = PermissionLevel.ANY) {
            override fun run(context: Context): Mono<Void> {
                val commands = CommandCollection.commands(event.client)
                    .filter { it.permission <= context.permissionLevel }

                return context.event.message.channel.flatMap { channel ->
                    channel.createEmbed { spec ->
                        commands.forEach { command ->
                            spec.addField(context.prefix + command.names[0],command.desc,false)
                        }
                        spec.setTitle("Help menu")
                    }
                }.then()
            }
        }

    }
}