package eu.darkcube.system.minecraftutils.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public class GlyphsData {

    private final Map<Integer, Float> bitmapWidths = new HashMap<>();
    private final Map<Integer, Float> spaceWidths = new HashMap<>();

    public void load(InputStream inputStream) throws IOException {
        bitmapWidths.clear();
        spaceWidths.clear();
        BufferedInputStream bin = new BufferedInputStream(inputStream);

        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        loadBuffer(buffer, bin);
        int spaceWidthCount = buffer.getInt(0);
        int bitmapWidthCount = buffer.getInt(4);
        Map<Integer, Float> spaceWidths = loadWidths(spaceWidthCount, buffer, bin);
        Map<Integer, Float> bitmapWidths = loadWidths(bitmapWidthCount, buffer, bin);
        this.spaceWidths.putAll(spaceWidths);
        this.bitmapWidths.putAll(bitmapWidths);
    }

    private Map<Integer, Float> loadWidths(int count, ByteBuffer buffer, BufferedInputStream in) throws IOException {
        Map<Integer, Float> widths = new HashMap<>();
        for (int i = 0; i < count; i++) {
            loadBuffer(buffer, in);
            int codepoint = buffer.getInt(0);
            float width = buffer.getFloat(4);
            widths.put(codepoint, width);
        }
        return widths;
    }

    private void loadBuffer(ByteBuffer buffer, BufferedInputStream in) throws IOException {
        int read = in.read(buffer.array(), 0, 8);
        if (read != 8) throw new IllegalArgumentException("Corrupt glyph data");
    }

    public void save(OutputStream outputStream) throws IOException {
        // reusable buffer for converting integers to bytes
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);

        buffer.putInt(0, spaceWidths.size());
        buffer.putInt(4, bitmapWidths.size());
        outputStream.write(buffer.array());
        save(outputStream, buffer, spaceWidths);
        save(outputStream, buffer, bitmapWidths);
    }

    private void save(OutputStream outputStream, ByteBuffer buffer, Map<Integer, Float> widths) throws IOException {
        for (int codepoint : widths.keySet()) {
            float width = widths.get(codepoint);
            buffer.putInt(0, codepoint).putFloat(4, width);
            outputStream.write(buffer.array());
        }
    }

    public Map<Integer, Float> bitmapWidths() {
        return bitmapWidths;
    }

    public Map<Integer, Float> spaceWidths() {
        return spaceWidths;
    }

    public Map<Integer, Float> getBitmapWidths() {
        return bitmapWidths;
    }

    public Map<Integer, Float> getSpaceWidths() {
        return spaceWidths;
    }
}
