package com.gumbocoin.discord.commands

import com.gumbocoin.discord.CommandBuilder
import com.gumbocoin.discord.PermissionLevel


internal fun command(block :CommandBuilder.() -> Unit) = block

val ping = command {
    name = "ping"
    desc = "Time a request to discord"
    execute { c ->
        c.sendMessage("Pinging")
            .flatMap { first ->
                first.edit {
                    val time = -c.event.message.timestamp.toEpochMilli() + first.timestamp.toEpochMilli()
                    it.setContent("Pong! Time:${time}ms")
                }
            }
    }
}

val ban = command {
    name = "ban"
    desc = "Ban a user"
    permission = PermissionLevel.LOCAL_ADMIN
    execute {c ->
        val user = c.getUserAsFirstArgument() ?: return@execute c.sendMessage("Can't find that user")
        c.event.guild
            .flatMap { it.getMemberById(user) }
            .flatMap { it.ban { spec ->
                spec.reason = c.getArguments().split(" ").getOrNull(2) ?: ""
            } }
            .flatMap { c.sendMessage("Banned ${user.asString()}") }
    }
}

val pardon = command {
    name = "pardon"
    name = "unban"
    permission = PermissionLevel.LOCAL_ADMIN
    execute { c ->
        val user = c.getUserAsFirstArgument() ?: return@execute c.sendMessage("Can't find that user")
        c.event.guild
            .flatMap { it.unban(user) }
            .flatMap { c.sendMessage("Pardoned ${user.asString()}") }
    }
}