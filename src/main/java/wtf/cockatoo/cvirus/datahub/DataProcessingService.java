package wtf.cockatoo.cvirus.datahub;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import wtf.cockatoo.cvirus.google.GoogleSheetService;


@ApplicationScoped
public class DataProcessingService {
    private static final Logger LOG = Logger.getLogger(DataProcessingService.class);

    private static final String TEMP_FILE_NAME = "c19_tmp_data";
    private static final String FILE_EXTENSION = ".csv";

    @Inject
    DataTransformService dataTransformService;
    @Inject
    GoogleSheetService sheetService;


    public boolean start(final String awsReqId, final String data) {
        boolean done = false;
        File tmpDataFile = null;

        // -- Don't print the "Data" as it has whole bunch of data (the entire CSV file).
        LOG.infof("Process data ~ Begin");
        try {
            tmpDataFile = saveIntoTempFile(data);
            final List<DataRow> transformedData = dataTransformService.start(tmpDataFile);

            LOG.infof("No. of records = %d", transformedData.size());

            sheetService.init();
            sheetService.clearData();
            sheetService.writeData(formatIntoGoogleSheetValue(transformedData));
            done = true;
            LOG.infof("Process data ~ End");
        }
        catch (final Exception e) {
            LOG.errorf(e, "Err while processing data: %s", e.getMessage());
        }
        finally {
            if (tmpDataFile != null) {
                LOG.infof("Process data ~ Cleaning temp file");

                tmpDataFile.deleteOnExit();
            }
        }

        return done;
    }


    private File saveIntoTempFile(final String data) throws Exception {

        final File temp = File.createTempFile(TEMP_FILE_NAME, FILE_EXTENSION);
        final BufferedWriter writer = new BufferedWriter(new FileWriter(temp));

        writer.write(data);
        writer.close();

        return temp;
    }


    private List<List<Object>> formatIntoGoogleSheetValue(final List<DataRow> transformedData) {
        final List<List<Object>> sheetValues = new ArrayList<>();

        for (final DataRow row : transformedData) {
            sheetValues.add(row.toArrayList());
        }

        return sheetValues;
    }
}
