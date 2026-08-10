package com.knsn92.noteblock_recorder;

import com.google.common.collect.*;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import java.util.*;

public class NoteBlockRecorder {

    private String songName;
    private NbsWriter nbsWriter;

    private boolean isRecording = false;
    private int tick = 0;

    private List<NbsWriter.CustomInstrument> customInstruments = new ArrayList<>();

    public NoteBlockRecorder() {}

    public boolean isRecording() {
        return isRecording;
    }

    public void startRecording(String songName) {
        this.songName = songName;
        this.isRecording = true;
        this.tick = 0;
        var customInstrumentsArray = this.customInstruments.toArray(NbsWriter.CustomInstrument[]::new);
        this.nbsWriter = new NbsWriter(customInstrumentsArray);
    }

    public void forwardTick() {
        if(isRecording) tick++;
    }

    public void addNote(@NonNull Note note) {
        if(!isRecording) return;
        nbsWriter.addNote(note, tick);
    }

    public boolean addCustomInstrument(Identifier soundId, String name) {
        if(Note.NOTE_SOUND_TO_ID.containsKey(soundId)) return false;
        if(customInstruments.size() + Note.NOTE_SOUND_TO_ID.size() >= 256) return false;
        String file = "sounds/" + soundId.getNamespace() + "/" + soundId.getPath() + ".ogg";
        customInstruments.add(new NbsWriter.CustomInstrument(name, file, soundId));
        return true;
    }

    public boolean hasCustomInstrument(Identifier soundId) {
        return customInstruments.stream().anyMatch(ci -> ci.identifier().equals(soundId));
    }

    public boolean removeCustomInstrument(Identifier soundId) {
        return customInstruments.removeIf(ci -> ci.identifier().equals(soundId));
    }

    public void clearCustomInstruments() {
        customInstruments.clear();
    }

    public NbsWriter.CustomInstrument[] getCustomInstruments() {
        return customInstruments.toArray(NbsWriter.CustomInstrument[]::new);
    }

    public NbsWriter.CustomInstrument[] getCurrentRecorderCustomInstruments() {
        if(!isRecording) return null;
        return nbsWriter.custom_instruments;
    }

    public String getSongName() {
        return songName;
    }

    public void cancelRecording() {
        isRecording = false;
        songName = null;
    }

    public byte[] finishRecording() {
        isRecording = false;
        songName = null;
        return nbsWriter.write();
    }

}
