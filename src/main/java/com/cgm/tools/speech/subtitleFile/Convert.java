package com.cgm.tools.speech.subtitleFile;

import java.io.*;


public class Convert {

    private Convert() {
    }

    /**
     * @param inputFile
     * @param inputFormat
     * @param outputFormat
     * @param outputFile
     * @throws Exception
     */
    public static void convert(String inputFile, String inputFormat, String outputFormat, String outputFile) throws Exception {

        TimedTextObject tto;
        TimedTextFileFormat ttff;
        OutputStream output;

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
            throw new Exception("Unrecognized input format: " + inputFormat + " only [SRT,STL,SCC,XML,ASS] are possible");
        }

        File file = new File(inputFile);
        InputStream is = new FileInputStream(file);
        tto = ttff.parseFile(file.getName(), is);

        if ("SRT".equalsIgnoreCase(outputFormat)) {
            IOClass.writeFileTxt(outputFile, tto.toSRT());
        } else if ("STL".equalsIgnoreCase(outputFormat)) {
            output = new BufferedOutputStream(new FileOutputStream(outputFile));
            output.write(tto.toSTL());
            output.close();
        } else if ("SCC".equalsIgnoreCase(outputFormat)) {
            IOClass.writeFileTxt(outputFile, tto.toSCC());
        } else if ("XML".equalsIgnoreCase(outputFormat)) {
            IOClass.writeFileTxt(outputFile, tto.toTTML());
        } else if ("ASS".equalsIgnoreCase(outputFormat)) {
            IOClass.writeFileTxt(outputFile, tto.toASS());
        }
        else {
            throw new Exception("Unrecognized input format: " + outputFormat + " only [SRT,STL,SCC,XML,ASS] are possible");
        }
        // normal test use
    }

    public static String[] toStringArray(String inputFile, String inputFormat) throws IOException, FatalParsingException {
        TimedTextObject tto;
        TimedTextFileFormat ttff;
        OutputStream output;

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
        InputStream is = new FileInputStream(file);
        tto = ttff.parseFile(file.getName(), is);
        return tto.toTXT();
    }
}
