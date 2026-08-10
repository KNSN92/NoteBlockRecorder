package com.knsn92.noteblock_recorder.mixin;

import com.knsn92.noteblock_recorder.NoteBlockRecorder;
import com.knsn92.noteblock_recorder.NoteBlockRecorderMod;
import com.knsn92.noteblock_recorder.Note;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public class SoundEngineMixin {

    @Inject(
            method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;",
            at = @At("RETURN")
    )
    void playSoundNoteBlock(SoundInstance instance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        NoteBlockRecorder recorder = NoteBlockRecorderMod.getInstance().recorder;
        if(!recorder.isRecording()) return;
        Note note = Note.fromSound(instance);
        if(note == null) return;
        recorder.addNote(note);
    }

}
