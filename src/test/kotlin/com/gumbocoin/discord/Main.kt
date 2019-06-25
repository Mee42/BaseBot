package com.gumbocoin.discord

import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.io.File

fun main() {
    val client = start(File("key.txt"))


    config {
        prefix = ">>"
        admin {
            id = 293853365891235841
        }
        help = NormalHelpPrinter()
        permission { c, command ->
            c.cache("Permission request denied")
            c.cache("You used a command that needs level ${command.permission}")
            c.cache("You only have permission level ${c.permissionLevel}")
            c.flush().last().then()
        }
    }

    collection {
        discordClient = client
        command {
            name = "ping"
            desc = "Time a request to discord"
            permission = PermissionLevel.LOCAL_ADMIN
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
        command {
            name = "perms"
            desc = "Query your permission level"
            execute { c ->
                c.sendMessage(c.permissionLevel.toString())
            }
        }
    }.subscribe()

    client.login().block()
}