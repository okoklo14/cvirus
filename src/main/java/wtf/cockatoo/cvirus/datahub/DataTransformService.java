package wtf.cockatoo.cvirus.datahub;

import java.io.File;
import java.io.FileReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.jboss.logging.Logger;

@ApplicationScoped
public class DataTransformService {
    private static final Logger LOG = Logger.getLogger(DataTransformService.class);

    private static final String COUNTRY_NAME_GLOBAL = "Global";
    private static final String[] HEADERS = { "Date", "Country", "Confirmed", "Recovered", "Deaths" };

    private final DateTimeFormatter DATA_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private int lyConfirmed;
    private int lyRecovered;
    private int lyDeaths;
    private int tyConfirmed;
    private int tyRecovered;
    private int tyDeaths;


    public List<DataRow> start(final File dataFile) throws Exception {

        final List<CSVRecord> records = CSVFormat.DEFAULT.withHeader(HEADERS).withSkipHeaderRecord().parse(new FileReader(dataFile)).getRecords();
        final List<DataRow> finalData = new ArrayList<>();

        if (!records.isEmpty()) {
            final int totalLines = records.size() - 1;

            final String dateEarliest = getDate(records.get(0));
            final String dateLatest = getDate(records.get(totalLines));
            final long totalDays = ChronoUnit.DAYS.between(convertDate(dateEarliest), convertDate(dateLatest));


            /**
             * Raw data is sorted (ASC by date) and grouped by country.
             * Calculate the daily delta changes, begin from the last line (i) of data (latest date) of each group.
             * Work its way up (earliest date) recursively to process each line within the same country group.
             *
             * Once the recursion is done, continue the process at the last line of data (i) for another group.
             */
            for (int i = totalLines; i >= 0; i -= (totalDays + 1)) {
                final CSVRecord currentData = records.get(i);

                // Ensure we always start from the last line of data (latest date) in a group
                if (getDate(currentData).equals(dateLatest)) {
                    calculateDeltaChanges(finalData, records, currentData, totalDays, i, 1);
                }
            }


            // The final data should have one line (the header) more than the total line
            if (finalData.size() - totalLines != 1) {
                finalData.clear();
                LOG.fatalf("Biz logic broken (due to unexpected data file changes). Source data V.S. transfromed data lines NOT Matched! %d vs %d.", totalLines, finalData.size());
            }
            else {
                /**
                 * TWO custom made rows, for a Data Studio chart to do comparison.
                 * The date value will be hard-coded to 1st of JAN for both this and last year.
                 */
                final int currentYear = LocalDate.now().getYear();

                final DataRow currentYearRow = new DataRow(currentYear + "-01-01", COUNTRY_NAME_GLOBAL, tyConfirmed, tyRecovered, tyDeaths);
                finalData.add(0, currentYearRow);

                final DataRow prevYearRow = new DataRow((currentYear - 1) + "-01-01", COUNTRY_NAME_GLOBAL, lyConfirmed, lyRecovered, lyDeaths);
                finalData.add(0, prevYearRow);
            }
        }
        else {
            LOG.fatalf("Unable to read CSV data from tmp file @ path = %s.", dataFile.getAbsolutePath());
        }

        return finalData;
    }


    /**
     * Recursively process the same country's data
     *
     * @param dataRows        Collect the output (enhanced) data
     * @param rawData         The raw data from file
     * @param currentData     Begin from the last line of data of a country group. Recursively move one line up within the same group.
     * @param totalDays       The total number of days in data file
     * @param lastLineOfGroup The last line number of each country group (188 groups)
     * @param lineCounter     The current line number, increment by 1 for each recursion. When this reached the total number of days, we will exit the recursion.
     */
    private void calculateDeltaChanges(
            final List<DataRow> dataRows,
            final List<CSVRecord> rawData,
            final CSVRecord currentData,
            final long totalDays,
            final int lastLineOfGroup,
            int lineCounter) {

        final DataRow processedRow = new DataRow(getDate(currentData), getCountry(currentData), getConfirmed(currentData), getRecovered(currentData), getDeaths(currentData));

        if (lineCounter <= totalDays) {
            final CSVRecord previousData = rawData.get(lastLineOfGroup - lineCounter);
            final LocalDate previousDate = convertDate(getDate(previousData));
            final LocalDate currentDate = convertDate(getDate(currentData));


            // Confirm previous record really is one day before current record and they are still within the same country group
            if ((currentDate.minusDays(1).isEqual(previousDate)) && getCountry(currentData).equalsIgnoreCase(getCountry(previousData))) {

                // Calculate daily new cases
                processedRow.colNew = processedRow.colConfirmed - getConfirmed(previousData);

                // Calculate TODAY new cases, and new death toll
                if (lineCounter == 1) {
                    processedRow.colTodayConfirm = processedRow.colNew;
                    processedRow.colTodayDeath = processedRow.colDeaths - getDeaths(previousData);
                }

                dataRows.add(processedRow);
                accumulateGlobalSum(lineCounter, processedRow);

                // Recursion
                calculateDeltaChanges(dataRows, rawData, previousData, totalDays, lastLineOfGroup, ++lineCounter);
            }
            else {
                LOG.fatalf(
                        "RAW data is not organized in an expected order. Stucked At: CSV Row = %d, with Country = %s and Date = %s. CSV Prev Row = %d, with Prev Country = %s and Date = %s",
                        currentData.getRecordNumber(),
                        getCountry(currentData),
                        getDate(currentData),
                        previousData.getRecordNumber(),
                        getCountry(previousData),
                        getDate(previousData));

                // TODO send alert notification and quit immediately.
                // Breaking the recursion due to unexpected data.
                return;
            }
        }
        else {
            // This is the first day record, no more previous data to compare. New case should be the same as confirmed case.
            processedRow.colNew = processedRow.colConfirmed;
            dataRows.add(processedRow);
            // End of recursion
        }
    }


    /**
     * Sum up the value for the "latest day" and "one day before latest day" to make a new set of data for global.
     *
     * Due to the limitation in Google Data Studio to showcase today V.s. yesterday data,
     * the "one day before latest day" will be used to fake as last year data; while "latest day" will be used to fake this year data for comparison.
     *
     * @param day Smallest number indicate the latest date. E.g: 1 is today, 2 is yesterday.
     */
    private void accumulateGlobalSum(final int day, final DataRow currentBo) {

        switch (day) {
            // Latest day (aka fake this year) data
            case 1:
                tyConfirmed += currentBo.colConfirmed;
                tyRecovered += currentBo.colRecovered;
                tyDeaths += currentBo.colDeaths;
                break;

                // One day before latest day (aka fake last year) data
            case 2:
                lyConfirmed += currentBo.colConfirmed;
                lyRecovered += currentBo.colRecovered;
                lyDeaths += currentBo.colDeaths;
                break;

            default:
                return;
        }
    }


    private String getDate(final CSVRecord record) {
        return record.get(HEADERS[0]);
    }


    private String getCountry(final CSVRecord record) {
        return record.get(HEADERS[1]);
    }


    private int getConfirmed(final CSVRecord record) {
        return Integer.parseInt(record.get(HEADERS[2]));
    }


    private int getRecovered(final CSVRecord record) {
        return Integer.parseInt(record.get(HEADERS[3]));
    }


    private int getDeaths(final CSVRecord record) {
        return Integer.parseInt(record.get(HEADERS[4]));
    }


    private LocalDate convertDate(final String date) {
        return LocalDate.parse(date, DATA_DATETIME_FORMAT);
    }
}
