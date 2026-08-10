package com.knsn92.noteblock_recorder;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class Command {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext buildContext) {

        var nbsrecorder = literal("nbsrecorder");
        nbsrecorder.then(
                literal("start").then(argument("song_name", StringArgumentType.greedyString())
                        .executes(Command::startRecording)
                ));
        nbsrecorder.then(literal("cancel").executes(Command::cancelRecording));
        nbsrecorder.then(literal("finish").executes(Command::finishRecording));
        var sounds = literal("sounds");
        sounds.then(literal("add")
                .then(argument("sound", IdentifierArgument.id())
                        .suggests(SuggestionProviders.cast(SuggestionProviders.AVAILABLE_SOUNDS))
                        .executes(_ -> SINGLE_SUCCESS)
                )
        );
        sounds.then(literal("remove")
                .then(argument("sound", IdentifierArgument.id())
                        .suggests(SuggestionProviders.cast(SuggestionProviders.AVAILABLE_SOUNDS))
                        .executes(_ -> SINGLE_SUCCESS)
                )
        );
        sounds.then(literal("clear").executes(_ -> SINGLE_SUCCESS));
        sounds.then(literal("list").executes(_ -> SINGLE_SUCCESS));
        nbsrecorder.then(sounds);
        dispatcher.register(nbsrecorder);
    }

    private static int startRecording(CommandContext<FabricClientCommandSource> ctx) {
        var recorder =  NoteBlockRecorderMod.getInstance().recorder;
        if(recorder.isRecording()) {
            ctx.getSource().sendError(Component.literal("Already recording a song! Finish the current recording before starting a new one."));
            return 0;
        }
        String songName = ctx.getArgument("song_name", String.class);
        recorder.startRecording(songName);
        ctx.getSource().sendFeedback(Component.literal("Started recording song: " + songName));
        return SINGLE_SUCCESS;
    }

    private static int cancelRecording(CommandContext<FabricClientCommandSource> ctx) {
        var recorder = NoteBlockRecorderMod.getInstance().recorder;
        if(!recorder.isRecording()) {
            ctx.getSource().sendError(Component.literal("Not currently recording a song! Start a recording before trying to cancel one."));
            return 0;
        }
        recorder.cancelRecording();
        ctx.getSource().sendFeedback(Component.literal("Cancelled current recording!"));
        return SINGLE_SUCCESS;
    }

    private static int finishRecording(CommandContext<FabricClientCommandSource> ctx) {
        var recorder = NoteBlockRecorderMod.getInstance().recorder;
        if(!recorder.isRecording()) {
            ctx.getSource().sendError(Component.literal("Not currently recording a song! Start a recording before trying to finish one."));
            return 0;
        }
        String songName = recorder.getSongName();
        String songFileName = songName.endsWith(".nbs") ? songName : songName + ".nbs";
        byte[] nbsData = recorder.finishRecording();
        Path saveDir = FabricLoader.getInstance().getGameDir().resolve("recorded_nbs_songs");
        try {
            Files.createDirectories(saveDir);
            Path outputPath = saveDir.resolve(songFileName);
            Files.write(outputPath, nbsData);
            ctx.getSource().sendFeedback(Component.literal("Finished recording song '" + songName + "'. Saving to the game directory"));
        } catch (IOException e) {
            ctx.getSource().sendError(Component.literal("Failed to save the recorded song '" + songName + "': " + e.getMessage()));
            return 0;
        }
        return SINGLE_SUCCESS;
    }
}
