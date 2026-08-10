package com.knsn92.noteblock_recorder;

import com.google.common.collect.*;
import org.jspecify.annotations.NonNull;

public class NoteBlockRecorder {

    private String songName;
    private NbsWriter nbsWriter;

    private boolean isRecording = false;
    private int tick = 0;

    public NoteBlockRecorder() {}

    public boolean isRecording() {
        return isRecording;
    }

    public void startRecording(String songName) {
        this.songName = songName;
        this.isRecording = true;
        this.tick = 0;
        this.nbsWriter = new NbsWriter();
    }

    public void forwardTick() {
        if(isRecording) tick++;
    }

    public void addNote(@NonNull Note note) {
        if(!isRecording) return;
        nbsWriter.addNote(note, tick);
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
