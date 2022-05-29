/*
 * Copyright 2020 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */

package com.adobe.pdfservices.operation.samples.extractpdf;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;

import com.adobe.pdfservices.operation.ExecutionContext;
import com.adobe.pdfservices.operation.auth.Credentials;
import com.adobe.pdfservices.operation.exception.SdkException;
import com.adobe.pdfservices.operation.exception.ServiceApiException;
import com.adobe.pdfservices.operation.exception.ServiceUsageException;
import com.adobe.pdfservices.operation.io.FileRef;
import com.adobe.pdfservices.operation.pdfops.ExtractPDFOperation;
import com.adobe.pdfservices.operation.pdfops.options.extractpdf.ExtractPDFOptions;
import com.adobe.pdfservices.operation.pdfops.options.extractpdf.ExtractElementType;
import com.adobe.pdfservices.operation.pdfops.options.extractpdf.ExtractRenditionsElementType;
import com.adobe.pdfservices.operation.pdfops.options.extractpdf.TableStructureType;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.slf4j.LoggerFactory;
import java.util.Calendar;
import org.apache.pdfbox.pdmodel.PDDocument;
/**
 * This sample illustrates how to extract Text, Table Elements Information from PDF along with renditions of Table elements.
 * It also exports the table renditions in a CSV / XLSX format.
 * Refer to README.md for instructions on how to run the samples & understand output zip file.
 */

public class ExtractTextTableInfoWithTableStructureFromPdf {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ExtractTextTableInfoWithTableStructureFromPdf.class);
    //PDFs you want to extract
    static String dir = "C:\\Users\\Jason\\OneDrive\\Documents\\EXTRACT\\temp";

    static File logFile = new File("output/log.csv");
    static File errorLogFile = new File ("output/error_log.csv");
    static File additionalInfoFile = new File ("output/additional_info.csv");

    static DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    static ExecutionContext executionContext;

    static PrintWriter log;
    static PrintWriter errorLog;
    static PrintWriter additionalLog;

    static int extractCount = 0;
    public static void main(String[] args) throws IOException {

        //dir = args[0];
        File[] files = new File(dir).listFiles();
        // Initial setup, create credentials instance.
        Credentials credentials = Credentials.serviceAccountCredentialsBuilder()
                .fromFile("pdfservices-api-credentials.json")
                .build();

        // Create an ExecutionContext using credentials.
        executionContext = ExecutionContext.create(credentials);
        log  = new PrintWriter(logFile);
        errorLog = new PrintWriter(errorLogFile);
        additionalLog = new PrintWriter(additionalInfoFile);

        iterateFiles(files, new ArrayList<>());
        System.out.println("Total PDFs extracted:" + extractCount);
        errorLog.close();
        log.close();
        additionalLog.close();

    }
    public static void iterateFiles(File[] files, ArrayList<String> path) {
        for (File file : files) {
            if (file.isDirectory()) {
                path.add(file.getName());
                iterateFiles(file.listFiles(), path);
                path.remove(path.size()-1);
            } else {
                extractPDF(file, path);
            }
        }
    }
    public static void extractPDF(File file, ArrayList<String> path){
        Calendar cal = Calendar.getInstance();
        String logOutput = "";
        double startTime = 0;
        String fileNameWithOutExt = FilenameUtils.removeExtension(file.getName());
        if(FilenameUtils.getExtension(file.getName()).equals("pdf")){
            try {
                PDDocument doc = Loader.loadPDF(file);
                PDDocumentInformation info = doc.getDocumentInformation();

                if(doc.getNumberOfPages() > 1) {
                    extractCount++;
                    startTime = ((double) System.currentTimeMillis()) / 1000;

                    ExtractPDFOperation extractPDFOperation = ExtractPDFOperation.createNew();

                    logOutput += dateFormat.format(cal.getTime()) + "," + fileNameWithOutExt + "," + file.length() + " bytes,";

                    // Provide an input FileRef for the operation
                    FileRef source = FileRef.createFromLocalFile(dir + "/" + String.join("/", path) + "/" + file.getName());
                    extractPDFOperation.setInputFile(source);

                    // Build ExtractPDF options and set them into the operation
                    ExtractPDFOptions extractPDFOptions = ExtractPDFOptions.extractPdfOptionsBuilder()
                            .addElementsToExtract(Arrays.asList(ExtractElementType.TEXT, ExtractElementType.TABLES))
                            .addElementToExtractRenditions(ExtractRenditionsElementType.TABLES)
                            .addTableStructureFormat(TableStructureType.XLSX)
                            .build();
                    extractPDFOperation.setOptions(extractPDFOptions);

                    // Execute the operation
                    FileRef result = extractPDFOperation.execute(executionContext);

                    // Save the result at the specified location
                    String s_path = "output/" + String.join("/", path) + "/";
                    File outputDir = new File(s_path);
                    if (!outputDir.exists()) {
                        outputDir.mkdirs();
                    }
                    result.saveAs("output/" + String.join("/", path) + "/" + fileNameWithOutExt + ".zip");
                    logOutput += String.valueOf(((double) (System.currentTimeMillis()) / 1000) - startTime);
                    additionalLog.write(doc.getNumberOfPages() + "," + formatLog(info.getTitle()) + "," + formatLog(info.getAuthor()) + "," + formatLog(info.getSubject()) + "," + formatLog(info.getKeywords()) + "\n");
                }
                doc.close();

            } catch (ServiceApiException | IOException | SdkException | ServiceUsageException e) {
                LOGGER.error("Exception encountered while executing operation", e);
                logOutput += "Error";
                errorLog.write(fileNameWithOutExt + "," + e + "\n");
            }
            logOutput += "\n";
            log.write(logOutput);
        }
    }
    public static String formatLog(String input){
        if(input == null){
            return "None";
        }
        else{
            return input.replaceAll(",", " ");
        }
    }
}
