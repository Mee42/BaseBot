package com.gumbocoin.discord

import com.google.common.base.Splitter
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import java.lang.NumberFormatException

class Context(
    val prefix: String,
    val permissionLevel: PermissionLevel,
    val event : MessageCreateEvent,
    val nameUsed : String){

    //helper functions
    fun sendMessage(message :String): Mono<Message> {
        return event.message.channel.flatMap { it.createMessage(message) }
    }

    private var cache = ""
    fun cache(message :String){
        cache += "\n$message"
    }
    fun cacheWithNoNewline(message: String){
        cache += message
    }
    fun flush(): Flux<Message> {
        val cacheCache = cache
        cache = ""
        return event
            .message
            .channel
            .flatMapMany { channel ->
                val split = Splitter.fixedLength(Message.MAX_CONTENT_LENGTH)
                    .split(cacheCache)
                split.toFlux()
                    .filter { it.isNotBlank() }
                    .flatMap { channel.createMessage(it) }
            }
    }



    fun getArguments():String{
        return event.message.content.get()
            .replaceFirst(prefix,"")
            .replaceFirst(nameUsed,"")
            .trim()
    }

    fun getUserAsFirstArgument(): Snowflake? {
        return getArguments()
            .split(" ")
            .firstOrNull()
            ?.let { getUser(it) }
    }

    fun getUser(identifier :String): Snowflake? {
        val id = identifier
            .filter { !"<@!#>".contains("" + it) }
        return try { Snowflake.of(id) }catch(e :NumberFormatException) { null }

    }

}