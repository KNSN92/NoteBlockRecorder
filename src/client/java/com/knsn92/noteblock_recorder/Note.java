package com.knsn92.noteblock_recorder;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;

public record Note(byte instrument, float pitch, float volume) {
    public static final ImmutableMap<Identifier, Byte> NOTE_SOUND_TO_ID = new ImmutableMap.Builder<Identifier, Byte>()
            .put(Identifier.parse("minecraft:block.note_block.harp"),           (byte)0)
            .put(Identifier.parse("minecraft:block.note_block.basedrum"),       (byte)1)
            .put(Identifier.parse("minecraft:block.note_block.snare"),          (byte)3)
            .put(Identifier.parse("minecraft:block.note_block.hat"),            (byte)4)
            .put(Identifier.parse("minecraft:block.note_block.bass"),           (byte)2)
            .put(Identifier.parse("minecraft:block.note_block.flute"),          (byte)6)
            .put(Identifier.parse("minecraft:block.note_block.bell"),           (byte)7)
            .put(Identifier.parse("minecraft:block.note_block.guitar"),         (byte)5)
            .put(Identifier.parse("minecraft:block.note_block.chime"),          (byte)8)
            .put(Identifier.parse("minecraft:block.note_block.xylophone"),      (byte)9)
            .put(Identifier.parse("minecraft:block.note_block.iron_xylophone"), (byte)10)
            .put(Identifier.parse("minecraft:block.note_block.cow_bell"),       (byte)11)
            .put(Identifier.parse("minecraft:block.note_block.didgeridoo"),     (byte)12)
            .put(Identifier.parse("minecraft:block.note_block.bit"),            (byte)13)
            .put(Identifier.parse("minecraft:block.note_block.banjo"),          (byte)14)
            .put(Identifier.parse("minecraft:block.note_block.pling"),          (byte)15)
            .build();

    public static Note fromSound(SoundInstance sound, NbsWriter.CustomInstrument[] custom_instruments) {
        if(sound == null) return null;
        var identifier = sound.getIdentifier();
        var instrument = NOTE_SOUND_TO_ID.get(identifier);
        if(instrument == null) {
            for(int i = 0; i < custom_instruments.length; i++) {
                if(i + NOTE_SOUND_TO_ID.size() >= 256) return null;
                if(custom_instruments[i].identifier().equals(identifier)) {
                    instrument = (byte)(i + NOTE_SOUND_TO_ID.size());
                }
            }
            if(instrument == null) return null;
        }
        float pitch;
        float volume;
        try {
            pitch = sound.getPitch();
            volume = sound.getVolume();
        } catch(NullPointerException e) {
            return null;
        }
        return new Note(instrument, pitch, volume);
    }

}