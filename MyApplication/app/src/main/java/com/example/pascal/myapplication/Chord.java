package com.example.pascal.myapplication;

import android.text.TextUtils;
import junit.framework.AssertionFailedError;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Created by pascal on 03.10.16.
 */
public class Chord {

    public enum EnharmonicPolicy{ Sharp, Natural, Flat }
    public enum MinorPolicy { LowerCase, FollowingM}
    public static final MinorPolicy MINOR_POLICY = MinorPolicy.LowerCase;
    public static final EnharmonicPolicy ENHARMONIC_POLICY = EnharmonicPolicy.Natural;
    public static final String FLAT = "\u266D";
    public static final String SHARP = "\u266F";

    private static HashMap<String, Line> lineCache = new HashMap<>();
    private static HashMap<String, Chord> chordCache = new HashMap<>();

    private String textBefore;
    private String textAfter;
    private int base;
    private String attachment;
    private boolean isValid;
    private boolean isMinor;

    static int hits = 0;
    static int misses = 0;

    static public Chord chordFromString(String text) {

        if (!chordCache.containsKey(text)) {
            chordCache.put(text, new Chord(text));
            misses++;
        }
        else {
            hits++;
        }

        System.out.println("Hits: " + hits + ", misses: " + misses + ", ratio: " + (hits / ((double) hits + misses)));

        return chordCache.get(text);
    }

    private Chord(String text) {
        isValid = parse(text);
    }

    @org.jetbrains.annotations.Contract(pure = true)
    static private String shift(EnharmonicPolicy naturalPolicy, int base) {

        EnharmonicPolicy enharmonicPolicy = ENHARMONIC_POLICY;
        if (enharmonicPolicy == EnharmonicPolicy.Natural) {
            enharmonicPolicy = naturalPolicy;
        }
        if (enharmonicPolicy == EnharmonicPolicy.Natural) {
            throw new AssertionFailedError();
        }
        switch (enharmonicPolicy) {
            case Flat:
                return baseString(base + 1) + FLAT;
            case Sharp:
                return baseString(base - 1) + SHARP;
            default:
                throw new AssertionFailedError();
        }
    }

    static public String baseString(int base) {
        switch (base) {
            case 0:
                return "C";
            case 1:
                return shift(EnharmonicPolicy.Sharp, base);
            case 2:
                return "D";
            case 3:
                return shift(EnharmonicPolicy.Flat, base);
            case 4:
                return "E";
            case 5:
                return "F";
            case 6:
                return shift(EnharmonicPolicy.Sharp, base);
            case 7:
                return "G";
            case 8:
                return shift(EnharmonicPolicy.Flat, base);
            case 9:
                return "A";
            case 10:
                return shift(EnharmonicPolicy.Flat, base);
            case 11:
                return "B";
            default:
                throw new AssertionFailedError();
        }
    }

    public String baseString() {
        return baseString(base);
    }

    public String toString() {
        return textBefore + key() + attachment + textAfter;
    }

    public String key() {
        if (isValid) {
            String base = baseString();
            if (isMinor) {
                if (MINOR_POLICY == MinorPolicy.LowerCase) {
                    return base.toLowerCase();
                } else {
                    return base + "m";
                }
            } else {
                return base;
            }
        } else {
            return "";
        }
    }

    boolean isValid() {
        return isValid;
    }

    void transpose(int t) {
        base += t;
        base %= 12;
        base += 12;
        base %= 12;
    }

    private int parseBase(Character c) {
        c = Character.toUpperCase(c);
        switch (c) {
            case 'C':
                return 0;
            case 'D':
                return 2;
            case 'E':
                return 4;
            case 'F':
                return 5;
            case 'G':
                return 7;
            case 'A':
                return 9;
            case 'B':
                return 11;
            default:
                return -1;
        }
    }

    private static final Pattern IGNORE_BEFORE_PATTERN = Pattern.compile(
            "^("
                    + TextUtils.join("|",
                    new String[]{
                            Pattern.quote("("),
                            Pattern.quote("["),
                            Pattern.quote("{"),
                            Pattern.quote("<")
                    })
                    + ")*");
    private static final Pattern IGNORE_AFTER_PATTERN = Pattern.compile(
            "("
                    + TextUtils.join("|",
                    new String[]{
                            Pattern.quote(")"),
                            Pattern.quote("]"),
                            Pattern.quote("}"),
                            Pattern.quote(">"),
                            "\\(\\w*\\)", "\\[\\w*\\]"
                    })
                    + ")*$");

    private static final Pattern CHORD_EXTENSION_PATTERN = Pattern.compile(
            "^(maj|min|2|4|5|7th|maj7|min7|sus4|sus2|sus|°|dim|dim7|aug|6|min6|"
                    + "9|min9|maj9|11|min11|maj11|13|min13|maj13|add9|maj7th|7|b5|\\+|_)*$"
    );

    private boolean parse(String text) {
        if (text.isEmpty()) {
            return false;
        }

        Matcher beforeMatcher = IGNORE_BEFORE_PATTERN.matcher(text);
        if (beforeMatcher.find()) {
            textBefore = beforeMatcher.group();
        }
        Matcher afterMatcher = IGNORE_AFTER_PATTERN.matcher(text);
        if (afterMatcher.find()) {
            textAfter = afterMatcher.group();
        }
        if (textBefore.length() < text.length()) {
            text = text.substring(textBefore.length());
        } else {
            text = "";
        }

        final int length = text.length() - textAfter.length();
        if (length >= 0) {
            text = text.substring(0, length);
        } else {
            text = "";
        }

        if (text.isEmpty()) {
            return false;
        }

        final Character baseCharacter = text.charAt(0);
        base = parseBase(baseCharacter);
        if (base < 0) {
            return false;
        }
        text = text.substring(1);

        if (!text.startsWith("sus") && text.startsWith("s")) {
            text = text.substring(1);
            base -= 1;
        } else if (text.startsWith("es")) {
            text = text.substring(2);
            base -= 1;
        } else if (text.startsWith("b")) {
            text = text.substring(1);
            base -= 1;
        } else if (text.startsWith(FLAT)) {
            text = text.substring(FLAT.length());
            base -= 1;
        } else if (text.startsWith("is")) {
            text = text.substring(2);
            base += 1;
        } else if (text.startsWith("#")) {
            text = text.substring(1);
            base += 1;
        } else if (text.startsWith(SHARP)) {
            text = text.substring(SHARP.length());
            base += 1;
        }

        transpose(0);

        if (Character.isLowerCase(baseCharacter)) {
            isMinor = true;
        } else if (!text.startsWith("min") && !text.startsWith("maj") && !text.startsWith("m")) {
            isMinor = true;
            text =  text.substring(1);
        } else {
            isMinor = false;
        }

        attachment = text;
        Matcher attachmentMatcher = CHORD_EXTENSION_PATTERN.matcher(attachment);
        if (attachmentMatcher.matches() && attachmentMatcher.group() == attachment) {
            return true;
        }
        else {
            return false;
        }

    }

    static public class Line {
        private Line() {
        }
        public boolean isChordLine;
        public String[] tokens;
    }

    private static final Pattern WORD_PATTERN = Pattern.compile("^[a-zA-Z'].*");
    public static Line parseLine(String line) {

        if (!lineCache.containsKey(line)) {
            Line result = new Line();
            result.tokens = line.split("\\||,|-|/|`|\\*|'|\\s", -1);

            int numWords = 0;
            int numChords = 0;
            for (String token : result.tokens) {
                Chord chord = new Chord(token);
                if (chord.isValid()) {
                    numChords++;
                } else {
                    String text = Normalizer.normalize(token, Normalizer.Form.NFD);
                    Matcher matcher = WORD_PATTERN.matcher(text);
                    if (matcher.matches()) {
                        numWords++;
                    }
                }
            }
            result.isChordLine = (numChords > numWords);
            lineCache.put(line, result);
        }
        return lineCache.get(line);
    }



}
