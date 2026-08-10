package com.knsn92.noteblock_recorder;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class NoteBlockRecorderMod implements ClientModInitializer {

    private static NoteBlockRecorderMod instance;

    public static NoteBlockRecorderMod getInstance() {
        return instance;
    }

    public final NoteBlockRecorder recorder = new NoteBlockRecorder();

    @Override
    public void onInitializeClient() {
        instance = this;
        ClientTickEvents.START_CLIENT_TICK.register(_ -> recorder.forwardTick());
        ClientCommandRegistrationCallback.EVENT.register(Command::register);
    }
}