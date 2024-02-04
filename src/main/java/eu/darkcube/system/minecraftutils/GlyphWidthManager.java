package eu.darkcube.system.minecraftutils;


import eu.darkcube.system.minecraftutils.util.GlyphsData;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public final class GlyphWidthManager {

    private final List<Integer> spaceCodepoints = new ArrayList<>();
    private final List<Float> spaceWidthsList = new ArrayList<>();
    private final GlyphsData glyphsData = new GlyphsData();

    /**
     * Returns the nearest codepoint for the given width.
     */
    public int nearestCodepoint(float width) {
        return spaceCodepoints.get(nearestCodepointIndex(width));
    }

    /**
     * Same as {@link #spacesForWidth(float)} but returns codepoints.
     *
     * @return the codepoints that best correspond the width
     * @see #spacesForWidth(float)
     * @see #nearestCodepoint(float)
     */
    public int[] spaceCodepointsForWidth(final float width) {
        List<Integer> codepoints = new ArrayList<>();
        float anySignRemaining = width;
        do {
            int index = nearestCodepointIndex(anySignRemaining);
            float anySignDist = anySignRemaining - spaceWidthsList.get(index);
            if (Math.abs(anySignDist) < Math.abs(anySignRemaining)) {
                // Progress!!!
                anySignRemaining = anySignDist;
                codepoints.add(spaceCodepoints.get(index));
            } else {
                break;
            }
        } while (true);
        int[] array = new int[codepoints.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = codepoints.get(i);
        }
        return array;
    }

    /**
     * Generates a string of spaces that correspond with the given width best. <br>
     * This means this method makes best efforts to generate a string representing the width, but the result is likely not perfect.
     * The accuracy of the text depends on the space-widths this {@link GlyphWidthManager} has to work with.
     *
     * @return a text of spaces that best correspond the width
     * @see #spaceCodepointsForWidth(float)
     */
    public String spacesForWidth(final float width) {
        int[] codepoints = spaceCodepointsForWidth(width);
        return new String(codepoints, 0, codepoints.length);
    }

    /**
     * Calculates the width of the text
     *
     * @return the width of the text
     * @see #width(int)
     */
    public float width(String text) {
        int[] codepoints = text.codePoints().toArray();
        float width = 0;
        for (int codepoint : codepoints) width += width(codepoint);
        return width;
    }

    /**
     * @return if this {@link GlyphWidthManager} has the given codepoint registered
     */
    public boolean has(int codepoint) {
        return glyphsData.getBitmapWidths().containsKey(codepoint) || glyphsData.getSpaceWidths().containsKey(codepoint);
    }

    /**
     * @return the width of the codepoint, 0 if no codepoint was found (or if the width is 0)
     * @see #has(int)
     */
    public float width(int codepoint) {
        if (glyphsData.getSpaceWidths().containsKey(codepoint)) return glyphsData.getSpaceWidths().get(codepoint);
        if (glyphsData.getBitmapWidths().containsKey(codepoint)) return glyphsData.getBitmapWidths().get(codepoint);
        return 0;
    }

    /**
     * Loads data from the given {@link Path}
     */
    public void loadGlyphData(Path path) throws IOException {
        loadGlyphData(Files.newInputStream(path));
    }

    /**
     * Loads data from the given path on the current {@link ClassLoader}.<br>
     *
     * @see #loadGlyphDataFromClassLoader(ClassLoader, String)
     */
    public void loadGlyphDataFromClassLoader(String path) throws IOException {
        loadGlyphDataFromClassLoader(Thread.currentThread().getContextClassLoader(), path);
    }

    /**
     * Loads data from the given path on the current {@link ClassLoader}.<br>
     * Uses {@link ClassLoader#getResourceAsStream(String)}
     *
     * @see #loadGlyphData(InputStream)
     */
    public void loadGlyphDataFromClassLoader(ClassLoader classLoader, String path) throws IOException {
        loadGlyphData(classLoader.getResourceAsStream(path));
    }

    /**
     * Loads data from the given {@link InputStream}
     */
    public void loadGlyphData(InputStream in) throws IOException {
        spaceWidthsList.clear();
        spaceCodepoints.clear();
        glyphsData.load(in);
        in.close();
        putAllSpaces(glyphsData.getSpaceWidths());
        putAllBitmaps(glyphsData.getBitmapWidths());
    }

    private void putAllBitmaps(Map<Integer, Float> widths) {
        // nothing to do
    }

    private void putAllSpaces(Map<Integer, Float> widths) {
        for (var entry : widths.entrySet()) {
            spaceCodepoints.add(entry.getKey());
        }
        // Sort the spaceCodepoints by the width size corresponding to it
        Arrays.quickSort(0, spaceCodepoints.size(), (k1, k2) -> Float.compare(glyphsData.getSpaceWidths().get(spaceCodepoints.get(k1)), glyphsData.getSpaceWidths().get(spaceCodepoints.get(k2))), (a, b) -> {
            int tmp = spaceCodepoints.get(a);
            spaceCodepoints.set(a, spaceCodepoints.get(b));
            spaceCodepoints.set(b, tmp);
        });
        for (Integer spaceCodepoint : spaceCodepoints) {
            spaceWidthsList.add(glyphsData.getSpaceWidths().get(spaceCodepoint));
        }
    }

    private int nearestCodepointIndex(float width) {
        float midVal;
        int from = 0;
        int to = spaceWidthsList.size() - 1;
        if (to == -1) throw new IllegalStateException("No Space Glyph Data Loaded");
        while (from <= to) {
            final int mid = (from + to) >>> 1;
            midVal = spaceWidthsList.get(mid);
            final int cmp = Float.compare(midVal, width);
            if (cmp < 0) from = mid + 1;
            else if (cmp > 0) to = mid - 1;
            else return mid;
        }
        if (from == 0) return from;
        if (from == spaceWidthsList.size()) return from - 1;
        float prev = spaceWidthsList.get(from - 1);
        float d1 = Math.abs(prev - width);
        float d2 = Math.abs(spaceWidthsList.get(from) - width);
        if (d1 < d2) return from - 1;
        return from;
    }

    public GlyphsData glyphsData() {
        return glyphsData;
    }
}