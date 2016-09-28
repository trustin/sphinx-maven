package kr.motd.maven.sphinx;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

public final class SphinxUtil {

    /**
     * Replaces the line separators of the generated text files with the platform default line separator.
     */
    public static void convertLineSeparators(File dir) throws IOException {
        if (!dir.isDirectory()) {
            return;
        }

        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                convertLineSeparators(f);
            } else if (isTextFile(f)) {
                convertLineSeparators(f, System.lineSeparator());
            }
        }
    }

    private static void convertLineSeparators(File f, String lineSeparatorStr) throws IOException {
        assert f.length() <= Integer.MAX_VALUE : "text file larger than 2 GiB";

        final byte[] content = new byte[(int) f.length()];
        try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
            for (int i = 0; i < content.length;) {
                final int readBytes = raf.read(content, i, content.length - i);
                if (readBytes < 0) {
                    throw new IOException("file size has been changed during processing: " + f);
                }

                i += readBytes;
            }
        }

        final ByteArrayOutputStream out = new ByteArrayOutputStream(content.length);
        final byte[] lineSeparator = lineSeparatorStr.getBytes("US-ASCII");
        byte lastByte = 0;

        for (final byte b : content) {
            if (b == 0) {
                // Maybe not a text file or UTF-16; give up.
                return;
            }

            if (b == '\n') {
                out.write(lineSeparator);
            } else {
                if (lastByte == '\r') {
                    out.write(lineSeparator);
                }
                if (b != '\r') {
                    out.write(b);
                }
            }
            lastByte = b;
        }

        if (lastByte == '\r') {
            out.write(lineSeparator);
        }

        final byte[] newContent = out.toByteArray();
        if (Arrays.equals(content, newContent)) {
            return;
        }

        try (FileOutputStream fout = new FileOutputStream(f)) {
            fout.write(newContent);
        }
    }

    private static boolean isTextFile(File f) {
        if (!f.isFile()) {
            return false;
        }

        final String name = f.getName();
        final int lastDotIdx = name.lastIndexOf('.');
        if (lastDotIdx < 0) {
            return false;
        }

        final String extension = name.substring(lastDotIdx + 1);
        switch (extension) {
            case "buildinfo":
            case "html":
            case "js":
            case "svg":
            case "txt":
            case "xml":
                return true;
            case "map":
                return name.endsWith(".css.map") || name.endsWith(".js.map");
        }

        return false;
    }

    private SphinxUtil() {}
}
