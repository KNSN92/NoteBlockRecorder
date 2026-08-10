package com.knsn92.noteblock_recorder;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

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
                        .executes(Command::addSound)
                )
        );
        sounds.then(literal("remove")
                .then(argument("sound", IdentifierArgument.id())
                        .suggests(SuggestionProviders.cast(SuggestionProviders.AVAILABLE_SOUNDS))
                        .executes(Command::removeSound)
                )
        );
        sounds.then(literal("clear").executes(Command::clearSounds));
        sounds.then(literal("list").executes(Command::listSounds));
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
        var customInstruments = recorder.getCurrentRecorderCustomInstruments();
        byte[] nbsData = recorder.finishRecording();
        Path saveDir = FabricLoader.getInstance().getGameDir().resolve("recorded_nbs_songs/"+songName);
        try {
            Files.createDirectories(saveDir);
            Path outputPath = saveDir.resolve(songFileName);
            Files.write(outputPath, nbsData);
            for (var ci : customInstruments) {
                Path instrumentPath = saveDir.resolve(ci.file());
                Files.createDirectories(instrumentPath.getParent());
                var sounds = Minecraft.getInstance().getSoundManager().getSoundEvent(ci.identifier());
                Identifier soundLocation = Objects.requireNonNull(sounds).getSound(RandomSource.create()).getLocation();
                var optionalSoundResource = Minecraft.getInstance().getResourceManager().getResource(Sound.SOUND_LISTER.idToFile(soundLocation));
                if (optionalSoundResource.isPresent()) {
                    var soundResource = optionalSoundResource.get();
                    var soundInputStream = soundResource.open();
                    Files.copy(soundInputStream, instrumentPath, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    ctx.getSource().sendError(Component.literal("Failed to find sound resource for custom instrument " + ci.name() + " (" + ci.identifier() + "). The instrument will not be saved."));
                }
            }
            ctx.getSource().sendFeedback(Component.literal("Finished recording song '" + songName + "'. Saving to the game directory"));
        } catch (IOException e) {
            ctx.getSource().sendError(Component.literal("Failed to save the recorded song '" + songName + "': " + e.getMessage()));
            return 0;
        }
        return SINGLE_SUCCESS;
    }

    private static int addSound(CommandContext<FabricClientCommandSource> ctx) {
        Identifier soundId = ctx.getArgument("sound", Identifier.class);
        var recorder = NoteBlockRecorderMod.getInstance().recorder;
        if(recorder.hasCustomInstrument(soundId)) {
            ctx.getSource().sendError(Component.literal("Sound " + soundId + " is already added as a custom instrument."));
            return 0;
        }
        if(Note.NOTE_SOUND_TO_ID.containsKey(soundId)) {
            ctx.getSource().sendError(Component.literal("Sound " + soundId + " is a vanilla instrument and cannot be added as a custom instrument."));
            return 0;
        }
        boolean success = recorder.addCustomInstrument(soundId, soundId.toString());
        if(success) {
            ctx.getSource().sendFeedback(Component.literal("Added sound " + soundId + " as a custom instrument." + (recorder.isRecording() ? " It will no longer be available in the next recording." : "")));
            return SINGLE_SUCCESS;
        } else {
            ctx.getSource().sendError(Component.literal("Failed to add sound " + soundId + " as a custom instrument. Maximum number of instruments reached."));
            return 0;
        }
    }

    private static int removeSound(CommandContext<FabricClientCommandSource> ctx) {
        Identifier soundId = ctx.getArgument("sound", Identifier.class);
        var recorder = NoteBlockRecorderMod.getInstance().recorder;
        if (!recorder.hasCustomInstrument(soundId)) {
            ctx.getSource().sendError(Component.literal("Sound " + soundId + " is not contains in the custom instruments."));
            return 0;
        }
        boolean success = recorder.removeCustomInstrument(soundId);
        if (success) {
            ctx.getSource().sendFeedback(Component.literal("Removed sound " + soundId + " from custom instruments." + (recorder.isRecording() ? " It will no longer be available in the next recording." : "")));
            return SINGLE_SUCCESS;
        } else {
            ctx.getSource().sendError(Component.literal("Failed to remove sound " + soundId + " from custom instruments."));
            return 0;
        }
    }

    private static int clearSounds(CommandContext<FabricClientCommandSource> ctx) {
        var recorder = NoteBlockRecorderMod.getInstance().recorder;
        recorder.clearCustomInstruments();
        ctx.getSource().sendFeedback(Component.literal("Cleared all custom instruments." + (recorder.isRecording() ? " They will no longer be available in the next recording." : "")));
        return SINGLE_SUCCESS;
    }

    private static int listSounds(CommandContext<FabricClientCommandSource> ctx) {
        var recorder = NoteBlockRecorderMod.getInstance().recorder;
        var customInstruments = recorder.getCustomInstruments();
        if (customInstruments.length == 0) {
            ctx.getSource().sendFeedback(Component.literal("No custom instruments added."));
        } else {
            StringBuilder sb = new StringBuilder("Custom instruments:\n");
            for (var ci : customInstruments) {
                sb.append("- ").append(ci.name()).append(" (").append(ci.file()).append(")\n");
            }
            sb.deleteCharAt(sb.length() - 1); // Remove the last newline
            ctx.getSource().sendFeedback(Component.literal(sb.toString()));
        }
        return SINGLE_SUCCESS;
    }
}
