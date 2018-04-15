/*
 * NTRU Cryptography Reference Source Code
 *
 * Copyright (C) 2009-2016  Security Innovation (SI)
 *
 * SI has dedicated the work to the public domain by waiving all of its rights
 * to the work worldwide under copyright law, including all related and
 * neighboring rights, to the extent allowed by law.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * You can copy, modify, distribute and perform the work, even for commercial
 * purposes, all without asking permission. You should have received a copy of
 * the creative commons license (CC0 1.0 universal) along with this program.
 * See the license file for more information. 
 */
package kr.motd.maven.sphinx;

import java.util.Arrays;

/**
 * This class implements the SHA-256 message digest algorithm.
 */
final class Sha256 {

    /**
     * Constructor.
     */
    Sha256() {
        reset();
    }

    /**
     * Get the length of the hash output, in bytes.
     * For SHA-256, this will always return 32.
     */
    int getDigestLen() {
        return HASH_LEN;
    }

    /**
     * Get the size of the input block for the core hash algorithm, in bytes.
     * For SHA-256, this will always return 64.
     */
    int getBlockLen() {
        return BLOCK_LEN;
    }

    /**
     * Reinitialize the digest operation, discarding any internal state.
     */
    void reset() {
        Arrays.fill(buf, (byte) 0);
        bufOff = 0;
        byteCount = 0;

        // initial values
        state[0] = 0x6a09e667;
        state[1] = 0xbb67ae85;
        state[2] = 0x3c6ef372;
        state[3] = 0xa54ff53a;
        state[4] = 0x510e527f;
        state[5] = 0x9b05688c;
        state[6] = 0x1f83d9ab;
        state[7] = 0x5be0cd19;
    }

    /**
     * Updates the message digest with new data.
     *
     * @param data      the data to be added.
     * @param offset    the start of the data in the array.
     * @param length    the number of bytes of data to add.
     */
    void update(byte[] data, int offset, int length) {
        // Sanity check inputs
        if (data == null) {
            throw new NullPointerException("Input data buffer is null");
        }
        if (offset + length > data.length) {
            throw new IllegalArgumentException(
                    "reading " + length + " bytes of input starting at offset " +
                    offset + " will overrun end of input buffer (" + data.length +
                    " bytes long)");
        }
        if (offset < 0) {
            throw new IllegalArgumentException(
                    "input offset is negative (" + offset + ')');
        }
        if (length < 0) {
            throw new IllegalArgumentException(
                    "input length is negative (" + offset + ')');
        }

        byteCount += length;

        // Process any full blocks we get by combining cached input
        // with the new input
        while (bufOff + length >= buf.length) {
            int todo = buf.length - bufOff;
            System.arraycopy(data, offset, buf, bufOff, todo);
            transform(buf);
            length -= todo;
            offset += todo;
            bufOff = 0;
        }

        // Copy any extra data into the cached input buffer.
        System.arraycopy(data, offset, buf, bufOff, length);
        bufOff += length;
    }

    /**
     * Completes the digest calculation and returns the result in the
     * supplied array. The output will be {@code getDigestLen()}
     * bytes long. The object is reinitialized (see {@code reset()}).
     */
    void finishDigest(byte[] out, int outOffset) {
        // Sanity check inputs
        if (out == null) {
            throw new NullPointerException("output array is null");
        }
        if (outOffset + getDigestLen() > out.length) {
            throw new IllegalArgumentException(
                    "writing " + getDigestLen() +
                    " bytes of input starting at offset " + outOffset +
                    " will overrun end of output buffer (" + out.length +
                    " bytes long)");
        }
        if (outOffset < 0) {
            throw new IllegalArgumentException(
                    "output offset is negative (" + outOffset + ')');
        }

        // Add the "end of input" marker
        buf[bufOff++] = (byte) 0x80;

        // If the current block is not large enough to hold the
        // 8-byte long bitcount, pad the current block with 0s,
        // process it, and start a new block.
        if (bufOff + 8 > buf.length) {
            Arrays.fill(buf, bufOff, getBlockLen(), (byte) 0);
            transform(buf);
            bufOff = 0;
        }

        // Pad the final block with 0's, then the bitcount of all of the input
        Arrays.fill(buf, bufOff, buf.length - 8, (byte) 0);
        long bitCount = byteCount * 8;
        for (int i = 0; i < 8; i++) {
            buf[buf.length - i - 1] = (byte) bitCount;
            bitCount >>>= 8;
        }

        // Process the final block
        transform(buf);

        // Copy the result to the output buffer.
        int2byte(state, 0, out, outOffset, state.length);

        // Reset the object
        reset();
    }

    // SHA-256 block routines
    //......................................................................

    /** Size (in bytes) of this hash */
    private static final int HASH_LEN = 32;

    /** Size (in bytes) of one process block */
    private static final int BLOCK_LEN = 64;

    /** 64 byte input buffer */
    private final byte[] buf = new byte[64];

    /** index of first empty element in buf */
    private int bufOff;

    /** Total number of bytes hashed so far. */
    private long byteCount;

    /** 8 32-bit words (interim result) */
    private final int[] state = new int[8];

    /**
     * A temporary buffer used by the transform() routine 
     * It is allocated once per object to reduce the cost of allocation.
     */
    private final int[] bufInts = new int[BLOCK_LEN / 4];
    private final int[] w = new int[64];

    /**
     * Convert an array of bytes into an array of 32-bit integers.
     * Each output integer uses 4 input bytes. The number of input
     * bytes must be a multiple of 4.
     */
    private static void byte2int(
            byte[] src, int srcOffset,
            int[] dst, int dstOffset,
            int numInts) {
        while (numInts-- > 0) {
            // Big endian
            dst[dstOffset++] = src[srcOffset++] << 24 |
                               (src[srcOffset++] & 0xFF) << 16 |
                               (src[srcOffset++] & 0xFF) << 8 |
                               src[srcOffset++] & 0xFF;
        }
    }

    /**
     * Convert an integer array into an byte array using the
     * big-endian encoing of the integers.
     */
    private static void int2byte(
            int[] src, int srcOffset,
            byte[] dst, int dstOffset,
            int numInts) {
        int end = numInts + srcOffset;
        for (int i = srcOffset, j = dstOffset; i < end; i++) {
            int d = src[srcOffset + i];
            dst[j++] = (byte) (d >>> 24);
            dst[j++] = (byte) (d >>> 16);
            dst[j++] = (byte) (d >>> 8);
            dst[j++] = (byte) d;
        }
    }

    private static int RR(int a, int n) {
        return a >>> n | a << 32 - n;
    }

    private static int S0(int a) {
        return RR(a, 2) ^ RR(a, 13) ^ RR(a, 22);
    }

    private static int S1(int a) {
        return RR(a, 6) ^ RR(a, 11) ^ RR(a, 25);
    }

    private static int s0(int a) {
        return RR(a, 7) ^ RR(a, 18) ^ a >>> 3;
    }

    private static int s1(int a) {
        return RR(a, 17) ^ RR(a, 19) ^ a >>> 10;
    }

    private void transform(
            byte[] block) {
        byte2int(buf, 0, bufInts, 0, BLOCK_LEN / 4);

        int A, B, C, D, E, F, G, H;

        /* init A - H */

        A = state[0];
        B = state[1];
        C = state[2];
        D = state[3];
        E = state[4];
        F = state[5];
        G = state[6];
        H = state[7];

        /* rounds 0 - 15 */

        H += S1(E) + (E & (F ^ G) ^ G) + 0x428A2F98 + bufInts[0];
        D += H;
        H += S0(A) + (A & B | C & (A | B));
        G += S1(D) + (D & (E ^ F) ^ F) + 0x71374491 + bufInts[1];
        C += G;
        G += S0(H) + (H & A | B & (H | A));
        F += S1(C) + (C & (D ^ E) ^ E) + 0xB5C0FBCF + bufInts[2];
        B += F;
        F += S0(G) + (G & H | A & (G | H));
        E += S1(B) + (B & (C ^ D) ^ D) + 0xE9B5DBA5 + bufInts[3];
        A += E;
        E += S0(F) + (F & G | H & (F | G));
        D += S1(A) + (A & (B ^ C) ^ C) + 0x3956C25B + bufInts[4];
        H += D;
        D += S0(E) + (E & F | G & (E | F));
        C += S1(H) + (H & (A ^ B) ^ B) + 0x59F111F1 + bufInts[5];
        G += C;
        C += S0(D) + (D & E | F & (D | E));
        B += S1(G) + (G & (H ^ A) ^ A) + 0x923F82A4 + bufInts[6];
        F += B;
        B += S0(C) + (C & D | E & (C | D));
        A += S1(F) + (F & (G ^ H) ^ H) + 0xAB1C5ED5 + bufInts[7];
        E += A;
        A += S0(B) + (B & C | D & (B | C));
        H += S1(E) + (E & (F ^ G) ^ G) + 0xD807AA98 + bufInts[8];
        D += H;
        H += S0(A) + (A & B | C & (A | B));
        G += S1(D) + (D & (E ^ F) ^ F) + 0x12835B01 + bufInts[9];
        C += G;
        G += S0(H) + (H & A | B & (H | A));
        F += S1(C) + (C & (D ^ E) ^ E) + 0x243185BE + bufInts[10];
        B += F;
        F += S0(G) + (G & H | A & (G | H));
        E += S1(B) + (B & (C ^ D) ^ D) + 0x550C7DC3 + bufInts[11];
        A += E;
        E += S0(F) + (F & G | H & (F | G));
        D += S1(A) + (A & (B ^ C) ^ C) + 0x72BE5D74 + bufInts[12];
        H += D;
        D += S0(E) + (E & F | G & (E | F));
        C += S1(H) + (H & (A ^ B) ^ B) + 0x80DEB1FE + bufInts[13];
        G += C;
        C += S0(D) + (D & E | F & (D | E));
        B += S1(G) + (G & (H ^ A) ^ A) + 0x9BDC06A7 + bufInts[14];
        F += B;
        B += S0(C) + (C & D | E & (C | D));
        A += S1(F) + (F & (G ^ H) ^ H) + 0xC19BF174 + bufInts[15];
        E += A;
        A += S0(B) + (B & C | D & (B | C));

        /* rounds 16 - 63 */

        w[0] = bufInts[0] + s0(bufInts[1]) + bufInts[9] + s1(bufInts[14]);
        H += S1(E) + (E & (F ^ G) ^ G) + 0xE49B69C1 + w[0];
        D += H;
        H += S0(A) + (A & B | C & (A | B));
        w[1] = bufInts[1] + s0(bufInts[2]) + bufInts[10] + s1(bufInts[15]);
        G += S1(D) + (D & (E ^ F) ^ F) + 0xEFBE4786 + w[1];
        C += G;
        G += S0(H) + (H & A | B & (H | A));
        w[2] = bufInts[2] + s0(bufInts[3]) + bufInts[11] + s1(w[0]);
        F += S1(C) + (C & (D ^ E) ^ E) + 0x0FC19DC6 + w[2];
        B += F;
        F += S0(G) + (G & H | A & (G | H));
        w[3] = bufInts[3] + s0(bufInts[4]) + bufInts[12] + s1(w[1]);
        E += S1(B) + (B & (C ^ D) ^ D) + 0x240CA1CC + w[3];
        A += E;
        E += S0(F) + (F & G | H & (F | G));
        w[4] = bufInts[4] + s0(bufInts[5]) + bufInts[13] + s1(w[2]);
        D += S1(A) + (A & (B ^ C) ^ C) + 0x2DE92C6F + w[4];
        H += D;
        D += S0(E) + (E & F | G & (E | F));
        w[5] = bufInts[5] + s0(bufInts[6]) + bufInts[14] + s1(w[3]);
        C += S1(H) + (H & (A ^ B) ^ B) + 0x4A7484AA + w[5];
        G += C;
        C += S0(D) + (D & E | F & (D | E));
        w[6] = bufInts[6] + s0(bufInts[7]) + bufInts[15] + s1(w[4]);
        B += S1(G) + (G & (H ^ A) ^ A) + 0x5CB0A9DC + w[6];
        F += B;
        B += S0(C) + (C & D | E & (C | D));
        w[7] = bufInts[7] + s0(bufInts[8]) + w[0] + s1(w[5]);
        A += S1(F) + (F & (G ^ H) ^ H) + 0x76F988DA + w[7];
        E += A;
        A += S0(B) + (B & C | D & (B | C));
        w[8] = bufInts[8] + s0(bufInts[9]) + w[1] + s1(w[6]);
        H += S1(E) + (E & (F ^ G) ^ G) + 0x983E5152 + w[8];
        D += H;
        H += S0(A) + (A & B | C & (A | B));
        w[9] = bufInts[9] + s0(bufInts[10]) + w[2] + s1(w[7]);
        G += S1(D) + (D & (E ^ F) ^ F) + 0xA831C66D + w[9];
        C += G;
        G += S0(H) + (H & A | B & (H | A));
        w[10] = bufInts[10] + s0(bufInts[11]) + w[3] + s1(w[8]);
        F += S1(C) + (C & (D ^ E) ^ E) + 0xB00327C8 + w[10];
        B += F;
        F += S0(G) + (G & H | A & (G | H));
        w[11] = bufInts[11] + s0(bufInts[12]) + w[4] + s1(w[9]);
        E += S1(B) + (B & (C ^ D) ^ D) + 0xBF597FC7 + w[11];
        A += E;
        E += S0(F) + (F & G | H & (F | G));
        w[12] = bufInts[12] + s0(bufInts[13]) + w[5] + s1(w[10]);
        D += S1(A) + (A & (B ^ C) ^ C) + 0xC6E00BF3 + w[12];
        H += D;
        D += S0(E) + (E & F | G & (E | F));
        w[13] = bufInts[13] + s0(bufInts[14]) + w[6] + s1(w[11]);
        C += S1(H) + (H & (A ^ B) ^ B) + 0xD5A79147 + w[13];
        G += C;
        C += S0(D) + (D & E | F & (D | E));
        w[14] = bufInts[14] + s0(bufInts[15]) + w[7] + s1(w[12]);
        B += S1(G) + (G & (H ^ A) ^ A) + 0x06CA6351 + w[14];
        F += B;
        B += S0(C) + (C & D | E & (C | D));
        w[15] = bufInts[15] + s0(w[0]) + w[8] + s1(w[13]);
        A += S1(F) + (F & (G ^ H) ^ H) + 0x14292967 + w[15];
        E += A;
        A += S0(B) + (B & C | D & (B | C));
        w[0] += s0(w[1]) + w[9] + s1(w[14]);
        H += S1(E) + (E & (F ^ G) ^ G) + 0x27B70A85 + w[0];
        D += H;
        H += S0(A) + (A & B | C & (A | B));
        w[1] += s0(w[2]) + w[10] + s1(w[15]);
        G += S1(D) + (D & (E ^ F) ^ F) + 0x2E1B2138 + w[1];
        C += G;
        G += S0(H) + (H & A | B & (H | A));
        w[2] += s0(w[3]) + w[11] + s1(w[0]);
        F += S1(C) + (C & (D ^ E) ^ E) + 0x4D2C6DFC + w[2];
        B += F;
        F += S0(G) + (G & H | A & (G | H));
        w[3] += s0(w[4]) + w[12] + s1(w[1]);
        E += S1(B) + (B & (C ^ D) ^ D) + 0x53380D13 + w[3];
        A += E;
        E += S0(F) + (F & G | H & (F | G));
        w[4] += s0(w[5]) + w[13] + s1(w[2]);
        D += S1(A) + (A & (B ^ C) ^ C) + 0x650A7354 + w[4];
        H += D;
        D += S0(E) + (E & F | G & (E | F));
        w[5] += s0(w[6]) + w[14] + s1(w[3]);
        C += S1(H) + (H & (A ^ B) ^ B) + 0x766A0ABB + w[5];
        G += C;
        C += S0(D) + (D & E | F & (D | E));
        w[6] += s0(w[7]) + w[15] + s1(w[4]);
        B += S1(G) + (G & (H ^ A) ^ A) + 0x81C2C92E + w[6];
        F += B;
        B += S0(C) + (C & D | E & (C | D));
        w[7] += s0(w[8]) + w[0] + s1(w[5]);
        A += S1(F) + (F & (G ^ H) ^ H) + 0x92722C85 + w[7];
        E += A;
        A += S0(B) + (B & C | D & (B | C));
        w[8] += s0(w[9]) + w[1] + s1(w[6]);
        H += S1(E) + (E & (F ^ G) ^ G) + 0xA2BFE8A1 + w[8];
        D += H;
        H += S0(A) + (A & B | C & (A | B));
        w[9] += s0(w[10]) + w[2] + s1(w[7]);
        G += S1(D) + (D & (E ^ F) ^ F) + 0xA81A664B + w[9];
        C += G;
        G += S0(H) + (H & A | B & (H | A));
        w[10] += s0(w[11]) + w[3] + s1(w[8]);
        F += S1(C) + (C & (D ^ E) ^ E) + 0xC24B8B70 + w[10];
        B += F;
        F += S0(G) + (G & H | A & (G | H));
        w[11] += s0(w[12]) + w[4] + s1(w[9]);
        E += S1(B) + (B & (C ^ D) ^ D) + 0xC76C51A3 + w[11];
        A += E;
        E += S0(F) + (F & G | H & (F | G));
        w[12] += s0(w[13]) + w[5] + s1(w[10]);
        D += S1(A) + (A & (B ^ C) ^ C) + 0xD192E819 + w[12];
        H += D;
        D += S0(E) + (E & F | G & (E | F));
        w[13] += s0(w[14]) + w[6] + s1(w[11]);
        C += S1(H) + (H & (A ^ B) ^ B) + 0xD6990624 + w[13];
        G += C;
        C += S0(D) + (D & E | F & (D | E));
        w[14] += s0(w[15]) + w[7] + s1(w[12]);
        B += S1(G) + (G & (H ^ A) ^ A) + 0xF40E3585 + w[14];
        F += B;
        B += S0(C) + (C & D | E & (C | D));
        w[15] += s0(w[0]) + w[8] + s1(w[13]);
        A += S1(F) + (F & (G ^ H) ^ H) + 0x106AA070 + w[15];
        E += A;
        A += S0(B) + (B & C | D & (B | C));
        w[0] += s0(w[1]) + w[9] + s1(w[14]);
        H += S1(E) + (E & (F ^ G) ^ G) + 0x19A4C116 + w[0];
        D += H;
        H += S0(A) + (A & B | C & (A | B));
        w[1] += s0(w[2]) + w[10] + s1(w[15]);
        G += S1(D) + (D & (E ^ F) ^ F) + 0x1E376C08 + w[1];
        C += G;
        G += S0(H) + (H & A | B & (H | A));
        w[2] += s0(w[3]) + w[11] + s1(w[0]);
        F += S1(C) + (C & (D ^ E) ^ E) + 0x2748774C + w[2];
        B += F;
        F += S0(G) + (G & H | A & (G | H));
        w[3] += s0(w[4]) + w[12] + s1(w[1]);
        E += S1(B) + (B & (C ^ D) ^ D) + 0x34B0BCB5 + w[3];
        A += E;
        E += S0(F) + (F & G | H & (F | G));
        w[4] += s0(w[5]) + w[13] + s1(w[2]);
        D += S1(A) + (A & (B ^ C) ^ C) + 0x391C0CB3 + w[4];
        H += D;
        D += S0(E) + (E & F | G & (E | F));
        w[5] += s0(w[6]) + w[14] + s1(w[3]);
        C += S1(H) + (H & (A ^ B) ^ B) + 0x4ED8AA4A + w[5];
        G += C;
        C += S0(D) + (D & E | F & (D | E));
        w[6] += s0(w[7]) + w[15] + s1(w[4]);
        B += S1(G) + (G & (H ^ A) ^ A) + 0x5B9CCA4F + w[6];
        F += B;
        B += S0(C) + (C & D | E & (C | D));
        w[7] += s0(w[8]) + w[0] + s1(w[5]);
        A += S1(F) + (F & (G ^ H) ^ H) + 0x682E6FF3 + w[7];
        E += A;
        A += S0(B) + (B & C | D & (B | C));
        w[8] += s0(w[9]) + w[1] + s1(w[6]);
        H += S1(E) + (E & (F ^ G) ^ G) + 0x748F82EE + w[8];
        D += H;
        H += S0(A) + (A & B | C & (A | B));
        w[9] += s0(w[10]) + w[2] + s1(w[7]);
        G += S1(D) + (D & (E ^ F) ^ F) + 0x78A5636F + w[9];
        C += G;
        G += S0(H) + (H & A | B & (H | A));
        w[10] += s0(w[11]) + w[3] + s1(w[8]);
        F += S1(C) + (C & (D ^ E) ^ E) + 0x84C87814 + w[10];
        B += F;
        F += S0(G) + (G & H | A & (G | H));
        w[11] += s0(w[12]) + w[4] + s1(w[9]);
        E += S1(B) + (B & (C ^ D) ^ D) + 0x8CC70208 + w[11];
        A += E;
        E += S0(F) + (F & G | H & (F | G));
        w[12] += s0(w[13]) + w[5] + s1(w[10]);
        D += S1(A) + (A & (B ^ C) ^ C) + 0x90BEFFFA + w[12];
        H += D;
        D += S0(E) + (E & F | G & (E | F));
        w[13] += s0(w[14]) + w[6] + s1(w[11]);
        C += S1(H) + (H & (A ^ B) ^ B) + 0xA4506CEB + w[13];
        G += C;
        C += S0(D) + (D & E | F & (D | E));
        w[14] += s0(w[15]) + w[7] + s1(w[12]);
        B += S1(G) + (G & (H ^ A) ^ A) + 0xBEF9A3F7 + w[14];
        F += B;
        B += S0(C) + (C & D | E & (C | D));
        w[15] += s0(w[0]) + w[8] + s1(w[13]);
        A += S1(F) + (F & (G ^ H) ^ H) + 0xC67178F2 + w[15];
        E += A;
        A += S0(B) + (B & C | D & (B | C));

        /* update H0 - H7 */

        state[0] += A;
        state[1] += B;
        state[2] += C;
        state[3] += D;
        state[4] += E;
        state[5] += F;
        state[6] += G;
        state[7] += H;

        /* clear temp variables */

        Arrays.fill(bufInts, 0);
        Arrays.fill(w, 0);
    }
}
