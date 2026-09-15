package com.knsn92.noteblock_recorder.mixin;

import com.knsn92.noteblock_recorder.NoteBlockRecorder;
import com.knsn92.noteblock_recorder.NoteBlockRecorderMod;
import com.knsn92.noteblock_recorder.Note;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {

    @Shadow
    protected abstract float calculatePitch(final SoundInstance instance);

    @Shadow
    protected abstract float calculateVolume(final SoundInstance instance);

    @Inject(
            method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;",
            at = @At("TAIL")
    )
    void playSoundNoteBlock(SoundInstance instance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        NoteBlockRecorder recorder = NoteBlockRecorderMod.getInstance().recorder;
        if(!recorder.isRecording()) return;
        Identifier identifier = instance.getIdentifier();
        float pitch = calculatePitch(instance);
        float volume = calculateVolume(instance);
        Note note = Note.of(identifier, pitch, volume, recorder.getCurrentRecorderCustomInstruments());
        if(note == null) return;
        recorder.addNote(note);
    }

}
