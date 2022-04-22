package org.apache.fineract.utils;

import org.apache.fineract.exception.CsvWriterException;
import org.apache.fineract.exception.WriteToCsvException;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.List;

/**
 * custom csv writer for writing list of POJOs into printer of HttpServletResponse
 * @param <T> generic type representing the data type of POJO
 */
public class CsvWriter<T> {

    interface Callback<T> {
        T call() throws Exception;
    }

    private final ICsvBeanWriter iCsvBeanWriter;
    private final String[] csvHeader;
    private final List<T> data;
    private final String[] nameMapping;

    private CsvWriter(PrintWriter printWriter, String[] csvHeader, List<T> data, String[] nameMapping) {
        this.iCsvBeanWriter = new CsvBeanWriter(printWriter, CsvPreference.STANDARD_PREFERENCE);
        this.csvHeader = csvHeader;
        this.data = data;
        this.nameMapping = nameMapping;
    }

    /**
     * writes csv data into printer
     * @throws WriteToCsvException @see [writeCsvHeaders], [writeData] and [closeStream]
     */
    public void write() throws WriteToCsvException {
        writeCsvHeaders(csvHeader);
        writeData(nameMapping, data);
        closeStream();
    }

    /**
     * writes the headers into csv
     * @param csvHeader the headers to be written in csv of type string array
     * @throws WriteToCsvException @see [performErrorProneTask]
     */
    private void writeCsvHeaders(String[] csvHeader) throws WriteToCsvException {
        performErrorProneTask(
                new WriteToCsvException(WriteToCsvException.ErrorCode.CSV_WRITE_HEADER, "Unable to write csv headers"),
                () -> {
                    iCsvBeanWriter.writeHeader(csvHeader);
                    return null;
                });
    }

    /**
     * writes the data/ rows in csv
     * @param nameMapping string array of the fields to be populated from POJO
     * @param objects list of the objects to be translated into rows in csv
     * @throws WriteToCsvException @see [performErrorProneTask]
     */
    private void writeData(String[] nameMapping, List<T> objects) throws WriteToCsvException {
        for(T data: objects) {
            performErrorProneTask(
                    new WriteToCsvException(WriteToCsvException.ErrorCode.CSV_WRITE_DATA, "Unable to write csv headers"),
                    () -> {
                        iCsvBeanWriter.write(data, nameMapping);
                        return null;
                    });
        }
    }

    /**
     * Closes the existing [ICsvBeanWriter] stream
     * @throws WriteToCsvException @see [performErrorProneTask]
     */
    private void closeStream() throws WriteToCsvException {
        performErrorProneTask(
                new WriteToCsvException(WriteToCsvException.ErrorCode.CSV_STREAM, "Unable to close/flush stream"),
                () -> {
                    iCsvBeanWriter.close();
                    return null;
                });
    }

    /**
     * Use this function to perform the task which are prone to errors along with custom [WriteToCsvException]
     * instance.
     * @param exception an instance of [WriteToCsvException], which is to be thrown on error occurred
     * @param callback the task which needs to be performed under try block
     * @param <T> generic return type of the successful callback call
     * @return the result of the successful callback of type [T] or throws an error passed
     * @throws WriteToCsvException if callbacks throws and exception
     */
    public static  <T> T performErrorProneTask(WriteToCsvException exception, Callback<T> callback) throws WriteToCsvException {
        try {
            return callback.call();
        } catch (Exception e) {
            exception.setStackTrace(e.getStackTrace());
            exception.setDeveloperMessage(e.getLocalizedMessage());
            throw exception;
        }
    }

    public static class Builder<T> {
        private PrintWriter printWriter;
        private String[] csvHeader;
        private List<T> data;
        private String[] nameMapping;

        public Builder<T> setPrintWriter(HttpServletResponse response) throws WriteToCsvException {
            this.printWriter = performErrorProneTask(
                    new WriteToCsvException(
                            WriteToCsvException.ErrorCode.CSV_GET_WRITER,
                            "Unable to create csv bean writer instance"),
                    response::getWriter) ;
            return this;
        }

        public Builder<T> setHeader(String[] header) {
            this.csvHeader = header;
            return this;
        }

        public Builder<T> setData(List<T> data) {
            this.data = data;
            return this;
        }

        public Builder<T> setData(List<T> data, String[] nameMapping) {
            this.data = data;
            setNameMapping(nameMapping);
            return this;
        }

        public Builder<T> setNameMapping(String[] nameMapping) {
            this.nameMapping = nameMapping;
            return this;
        }

        public CsvWriter<T> build() throws CsvWriterException {
            if(printWriter == null) {
                throw new CsvWriterException(WriteToCsvException.ErrorCode.CSV_BUILDER,"Print writer can't be null");
            }
            if(csvHeader == null) {
                throw new CsvWriterException(WriteToCsvException.ErrorCode.CSV_BUILDER,"Csv header can't be null");
            }
            if(data == null) {
                throw new CsvWriterException(WriteToCsvException.ErrorCode.CSV_BUILDER,"Data can't be null");
            }
            if(nameMapping == null) {
                throw new CsvWriterException(WriteToCsvException.ErrorCode.CSV_BUILDER, "Name mapping can't be null");
            }
            return new CsvWriter<>(printWriter, csvHeader, data, nameMapping);
        }

    }
}
