package com.radium.client.utils;
// radium client

import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.security.SecureRandom;
import java.util.Arrays;

public class EncryptedString implements AutoCloseable, CharSequence {
    private static final SecureRandom random = new SecureRandom();
    private final char[] key;
    private final char[] value;
    private final int length;
    private boolean closed = false;

    public EncryptedString(String string) {
        System.out.println("New ES: "+ string);
        if (string == null) {
            throw new IllegalArgumentException("Input string cannot be null");
        }
        this.length = string.length();
        this.key = generateRandomKey(Math.min(this.length, 128));
        this.value = new char[this.length];
        string.getChars(0, this.length, this.value, 0);
        applyXorEncryption(this.value, this.key, 0, this.length);
    }

    private EncryptedString(char[] original, char[] originalKey) {
        System.out.println("New ES: "+ original + " | " + originalKey);
        this.length = original.length;
        this.value = Arrays.copyOf(original, original.length);
        this.key = Arrays.copyOf(originalKey, originalKey.length);
    }

    public static EncryptedString of(String s) {
        return new EncryptedString(s);
    }

    private static char[] generateRandomKey(int n) {
        char[] array = new char[n];
        for (int i = 0; i < n; ++i) {
            array[i] = (char) random.nextInt(65536);
        }
        return array;
    }

    private static void applyXorEncryption(char[] array, char[] array2, int n, int n2) {
        for (int i = 0; i < n2; ++i) {
            array[n + i] ^= array2[i % array2.length];
        }
    }

    public int length() {
        checkNotClosed();
        return this.length;
    }

    public char charAt(int n) {
        checkNotClosed();
        if (n >= 0 && n < this.length) {
            System.out.println((char)(this.value[n] ^ this.key[n % this.key.length])); // ADDED
            return (char) (this.value[n] ^ this.key[n % this.key.length]);
        }
        throw new IndexOutOfBoundsException("Index: " + n + ", Length: " + this.length);
    }

    @NotNull
    public CharSequence subSequence(int n, int n2) {
        checkNotClosed();
        if (n < 0 || n2 > this.length || n > n2) {
            throw new IndexOutOfBoundsException("Invalid range: " + n + " to " + n2);
        }
        int len = n2 - n;
        char[] val = new char[len];
        char[] k = new char[len];
        for (int i = 0; i < len; i++) {
            val[i] = this.value[n + i];
            k[i] = this.key[(n + i) % this.key.length];
        }
        return new EncryptedString(val, k);
    }

    @NotNull
    public String toString() {
        if (closed) return "CLOSED";
        char[] array = new char[this.length];
        for (int i = 0; i < this.length; ++i) {
            array[i] = this.charAt(i);
        }
        String s = new String(array);
        Arrays.fill(array, '\u0000');
        System.out.println(s); // ADDED
        return s;
    }

    public Text toText() {
        return Text.of(this.toString());
    }

    public void close() {
        if (!this.closed) {
            Arrays.fill(this.value, '\u0000');
            Arrays.fill(this.key, '\u0000');
            this.closed = true;
        }
    }

    private void checkNotClosed() {
        if (this.closed) {
            throw new IllegalStateException("EncryptedString is closed");
        }
    }
}

