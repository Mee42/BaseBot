package com.gumbocoin.discord

import discord4j.core.DiscordClient
import discord4j.core.DiscordClientBuilder
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.util.Permission
import discord4j.core.`object`.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono
import reactor.util.function.Tuples
import java.awt.Color
import java.io.File
import java.util.*


fun start(token :String, block :DiscordClientBuilder.() -> Unit = {}): DiscordClient{
    return DiscordClientBuilder(token).apply(block).build()
}
fun start(token : File, block :DiscordClientBuilder.() -> Unit = {}): DiscordClient {
    return start(token.readText(Charsets.UTF_8),block)
}


interface PrefixProducer{
    fun getPrefixForGuild(guild :Snowflake):String
    fun getDefaultPrefix():String
    fun getPrefixForGuild(guild : Optional<Snowflake>):String{
        return guild
            .map { getPrefixForGuild(it) }
            .orElseGet { getDefaultPrefix() }
    }
}

object Config{
    lateinit var isMasterAdmin: AdminManager
    lateinit var prefix: PrefixProducer
    lateinit var defaultHelp: HelpCommand
    lateinit var badPermissionHandler: BadPermissionHandler
}

interface BadPermissionHandler{
    fun run(context: Context,command: Command):Mono<Void>
}

interface HelpCommand{
    fun getFor(event :MessageCreateEvent):Command
}

@FunctionalInterface
interface AdminManager{
    fun isMasterAdmin(user :User):Mono<Boolean>
}

const val HELP_OVERRIDE = "!!!help"


fun DiscordClient.applyCommands(commands :List<Command>):Mono<Void>{
    return this.eventDispatcher
        .on(MessageCreateEvent::class.java)
        .filter { it.message.content.isPresent }
        .map { Tuples.of(it,Config.prefix.getPrefixForGuild(it.guildId)) }
        .filter { tuple -> tuple.t1.message.content.get().startsWith(tuple.t2) || tuple.t1.message.content.get() == HELP_OVERRIDE }
        .map { (event,prefix) ->

            val command = commands.firstOrNull { command ->
                command.names.any { alias -> event.message.content.get().startsWith(prefix + alias) }
            } ?: event.message.content.map<String?> { it }.orElse(null)?.run {
                if(this == HELP_OVERRIDE || this == "${prefix}help") Config.defaultHelp.getFor(event) else null
            }
            Tuples.of(event,prefix,Optional.ofNullable(command))

        }
        .filter { it.t3.isPresent }
        .map { Tuples.of(it.t1,it.t2,it.t3.get()) }
        .map { (event,prefix,command) ->
            Tuples.of(event,prefix,command, getPermissionLevelForUser(event.member))
        }
        .flatMap { tuple ->
            tuple.t4.map { four -> Tuples.of(tuple.t1,tuple.t2,tuple.t3,four) } }
        .map { (event,prefix,command,permission) ->
            Tuples.of(Context(
                prefix = prefix,
                permissionLevel = permission,
                event = event,
                nameUsed = command.names.first { alias -> event.message.content.get().startsWith(prefix + alias) }
            ),command)
        }.flatMap {(context,command) ->
            (if (command.permission > context.permissionLevel) {
                Config.badPermissionHandler.run(context, command)
            } else {
                command.run(context)
            }).onErrorResume { e ->
                e.printStackTrace()

                context.event.message.channel.flatMap { channel ->
                    channel.createEmbed { spec ->
                        spec.setColor(Color.RED)
                        spec.setTitle("Internal error : ${e::class.java.simpleName}")
                        spec.setDescription(e.message ?: "")
                    }
                }.then()
            }
        }
        .then()

}




fun getPermissionLevelForUser(memberOp: Optional<Member>):Mono<PermissionLevel> {

    return memberOp.map {member ->
        member.isAdmin()
        .map { Tuples.of(it, Config.isMasterAdmin.isMasterAdmin(member)) }
        .flatMap { it.t2.map { two -> Tuples.of(it.t1, two) } }
        .map { (isAdmin, isMasterAdmin) ->
            when {
                isMasterAdmin -> PermissionLevel.MASTER_ADMIN
                isAdmin -> PermissionLevel.LOCAL_ADMIN
                else -> PermissionLevel.ANY
            }
        }
    }.orElse(Mono.just(PermissionLevel.ANY))
}

fun Member.isAdmin(): Mono<Boolean> = this.basePermissions.map { it.contains(Permission.ADMINISTRATOR) }



abstract class Command(
    val desc :String,
    val permission :PermissionLevel,
    val names :List<String>){
    abstract fun run(context: Context):Mono<Void>
}




class PermissionLevel private constructor(val name :String) {


    operator fun compareTo(other :PermissionLevel):Int{
        if(this === other)
            return 0
        return when {
            this == ANY -> -1
            this == LOCAL_ADMIN && other == MASTER_ADMIN -> -1
            this == LOCAL_ADMIN -> 1
            this == MASTER_ADMIN -> 1
            else -> error("fuck")
        }
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PermissionLevel

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String {
        return "PermissionLevel('$name')"
    }

    companion object {
        val LOCAL_ADMIN = PermissionLevel("LOCAL_ADMIN")
        val MASTER_ADMIN = PermissionLevel("MASTER_ADMIN")
        val ANY = PermissionLevel("ANY")
    }

}