package com.gumbocoin.discord

import discord4j.core.DiscordClient
import reactor.core.publisher.Mono


object CommandCollection {

    private val commands = mutableMapOf<DiscordClient,MutableList<Command>>()
    fun addCommands(client :DiscordClient, commands :List<Command>) : Mono<Void> {
        if(!this.commands.contains(key = client)){
            this.commands[client] = mutableListOf()
        }
        this.commands[client]!!.addAll(commands)
        return client.applyCommands(commands)
    }

    fun commands(client :DiscordClient):List<Command>{
        return commands[client] ?: emptyList()
    }
}