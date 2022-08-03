package com.cgm.tools.speech.subtitleFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;


public class Convert {

    private Convert() {
    }

    public static void convert(String inputFile, String inputFormat, String outputFormat, String outputFile)
            throws IOException, FatalParsingException {

        TimedTextObject tto;
        TimedTextFileFormat ttff;

        //this is in case anyone may want to use this as stand alone java executable


        if ("SRT".equalsIgnoreCase(inputFormat)) {
            ttff = new FormatSRT();
        } else if ("STL".equalsIgnoreCase(inputFormat)) {
            ttff = new FormatSTL();
        } else if ("SCC".equalsIgnoreCase(inputFormat)) {
            ttff = new FormatSCC();
        } else if ("XML".equalsIgnoreCase(inputFormat)) {
            ttff = new FormatTTML();
        } else if ("ASS".equalsIgnoreCase(inputFormat)) {
            ttff = new FormatASS();
        } else {
            throw new IllegalArgumentException("Unrecognized input format: " + inputFormat + " only [SRT,STL,SCC,XML,ASS] are possible");
        }

        File file = new File(inputFile);
        InputStream is = Files.newInputStream(file.toPath());
        tto = ttff.parseFile(file.getName(), is);

        if ("SRT".equalsIgnoreCase(outputFormat)) {
            IOClass.writeFileTxt(outputFile, tto.toSRT());
        } else if ("STL".equalsIgnoreCase(outputFormat)) {
            try (OutputStream output = new BufferedOutputStream(Files.newOutputStream(Paths.get(outputFile)))) {
                output.write(tto.toSTL());
            }
        } else if ("SCC".equalsIgnoreCase(outputFormat)) {
            IOClass.writeFileTxt(outputFile, tto.toSCC());
        } else if ("XML".equalsIgnoreCase(outputFormat)) {
            IOClass.writeFileTxt(outputFile, tto.toTTML());
        } else if ("ASS".equalsIgnoreCase(outputFormat)) {
            IOClass.writeFileTxt(outputFile, tto.toASS());
        }
        // normal test use
    }

    public static TimedTextObject toTto(String inputFile, String inputFormat) throws IOException, FatalParsingException {
        TimedTextObject tto;
        TimedTextFileFormat ttfFormat;

        if ("SRT".equalsIgnoreCase(inputFormat)) {
            ttfFormat = new FormatSRT();
        } else if ("STL".equalsIgnoreCase(inputFormat)) {
            ttfFormat = new FormatSTL();
        } else if ("SCC".equalsIgnoreCase(inputFormat)) {
            ttfFormat = new FormatSCC();
        } else if ("XML".equalsIgnoreCase(inputFormat)) {
            ttfFormat = new FormatTTML();
        } else if ("ASS".equalsIgnoreCase(inputFormat)) {
            ttfFormat = new FormatASS();
        } else {
            throw new IllegalArgumentException("Unrecognized input format: " + inputFormat + " only [SRT,STL,SCC,XML,ASS] are possible");
        }

        File file = new File(inputFile);
        InputStream is = Files.newInputStream(file.toPath());
        tto = ttfFormat.parseFile(file.getName(), is);
        return tto;
    }

    public static String[] toStringArray(String inputFile, String inputFormat) throws IOException, FatalParsingException {
        return toTto(inputFile, inputFormat).toTXT();
    }
}
