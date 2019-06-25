package com.gumbocoin.discord

import discord4j.core.DiscordClient
import reactor.core.publisher.Mono

class CommandCollectionBuilder internal constructor(){
    internal fun build():Mono<Void>{
        return CommandCollection.addCommands(discordClient ?: error("You need to specify the client"),commands)
    }
    private val commands = mutableListOf<Command>()

    var discordClient :DiscordClient? = null

    fun command(block :CommandBuilder.() -> Unit){
        commands += CommandBuilder().apply(block).build()
    }
}

class CommandBuilder{
    private val aliases = mutableListOf<String>()
    var desc :String = "No description"

    var permission :PermissionLevel = PermissionLevel.ANY


    private var execute :((Context) -> Mono<Void>)? = null

    fun execute(block :(Context) -> Mono<out Any>){
        execute = { block(it).then() }
    }
    fun <A> executeRaw(block :(Context) -> A){
        execute = { Mono.just(block(it)).then() }
    }

    var alias :String
        get() = ""
        set(value) { aliases.add(value) }
    var name :String
        get() = ""
        set(value) { aliases.add(value) }



    internal fun build():Command{
        if(execute == null)
            error("No executor defined")

        return object : Command(
            desc = desc,
            names = aliases,
            permission = permission
        ){
            override fun run(context: Context): Mono<Void> {
                return execute!!.invoke(context)
            }
        }
    }
}

fun collection(block :CommandCollectionBuilder.() -> Unit):Mono<Void>{
    return CommandCollectionBuilder().apply(block).build()
}