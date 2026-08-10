package com.knsn92.noteblock_recorder;

import java.io.ByteArrayOutputStream;
import java.util.*;

/**
 * This Nbs writer is only supported for the NBS format version 5
 */
public class NbsWriter {

    private final ByteArrayOutputStream writer = new ByteArrayOutputStream();

    private String name = "";
    private String author = "";
    private String original_author = "";
    private String description = "";

    private int ticks_len = 0;
    private int layer_count = 0;
    private final Map<Integer, List<Note>> notes = new HashMap<>();
    private final Set<Byte> used_instruments = new HashSet<>();

    public void addNote(Note note, int tick, int layer) {
        var notes_in_tick = notes.get(tick);
        while(notes_in_tick.size() <= layer) notes_in_tick.add(null);
        notes_in_tick.set(layer, note);
        ticks_len = Math.max(ticks_len, tick);
        layer_count = Math.max(layer_count, layer);
        used_instruments.add(note.instrument());
    }

    public void addNote(Note note, int tick) {
        var notes_in_tick = notes.computeIfAbsent(tick, k -> new ArrayList<>());
        notes_in_tick.add(note);
        ticks_len = Math.max(ticks_len, tick);
        layer_count = Math.max(layer_count, notes_in_tick.size());
        used_instruments.add(note.instrument());
    }

    private void write_int_le(int v) {
        writer.write((v >> 0) & 0xFF);
        writer.write((v >> 8) & 0xFF);
        writer.write((v >> 16) & 0xFF);
        writer.write((v >> 24) & 0xFF);
    }

    private void write_short_le(short v) {
        writer.write((v >> 0) & 0xFF);
        writer.write((v >> 8) & 0xFF);
    }

    private void write_byte(byte v) {
        writer.write(v);
    }

    private void write_string(String v) {
        byte[] bytes = v.getBytes();
        write_int_le(bytes.length);
        writer.writeBytes(bytes);
    }

    private void writeHeader() {
        write_short_le((short)0);
        write_byte((byte)5); // NBS format version
        write_byte((byte)16); // number of vanilla instruments
        write_short_le((short) ticks_len); // length in ticks
        write_short_le((short) layer_count); // number of layers
        write_string(name);
        write_string(author);
        write_string(original_author);
        write_string(description);
        write_short_le((short)2000); // tempo * 100
        write_byte((byte)0); // autosave on/off
        write_byte((byte)10); // autosave duration
        write_byte((byte)4); // time signature
        write_int_le(0); // minutes spent
        write_int_le(0); // left clicks
        write_int_le(0); // right clicks
        write_int_le(0); // note blocks added
        write_int_le(0); // note blocks removed
        write_string(""); // midi/schematic file name
        write_byte((byte)0); // loop on/off
        write_byte((byte)0); // loop count
        write_short_le((short)0); // loop start tick
    }

    private void writeNotesAndLayers() {
        var ticks = notes.keySet().toArray(Integer[]::new);
        Arrays.sort(ticks);

        int tick = -1;
        for(int t : ticks) {
            write_short_le((short)(t - tick));
            tick = t;
            int layer = -1;
            var notes_in_tick = notes.get(tick);
            for(int l = 0; l < notes_in_tick.size(); l++) {
                Note note = notes_in_tick.get(l);
                if(note == null) continue;
                write_short_le((short)(l - layer));
                layer = l;
                write_byte(note.instrument());
                short pitch = (short)(Math.log(note.pitch()) / Math.log(2) * 1200);
                byte key = (byte)(pitch / 100 + 45);
                pitch = (short)(pitch % 100);
                write_byte(key);
                byte volume = (byte)Math.max(0, Math.min(1.0, note.volume()) * 100);
                write_byte(volume);
                write_byte((byte)100); // panning
                write_short_le(pitch);
            }
            write_short_le((short)0);
        }
        write_short_le((short)0);
        for(int l = 0; l < layer_count; l++) {
            write_string(""); // optional layer name
            write_byte((byte)0); // layer lock on/off
            write_byte((byte)100); // layer volume
            write_byte((byte)100); // layer panning
        }
    }

    public byte[] write() {
        writeHeader();
        writeNotesAndLayers();
        write_byte((byte)0); // Custom instrument count
        return writer.toByteArray();
    }

}
