package wtf.cockatoo.cvirus.google;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;


@ApplicationScoped
public class GoogleSheetService {
    private static final Logger LOG = Logger.getLogger(GoogleSheetService.class);

    // https://developers.google.com/sheets/api/reference/rest/v4/ValueInputOption
    private static final String SHEET_VAL_INPUT_OPTION_RAW = "RAW";
    private static final String SHEET_RANGE_START = "data!A2";
    private static final String SHEET_RANGE_ALL = "data!A2:H";
    
    private Sheets sheets;
    
    @ConfigProperty(name = "data.spreadsheet.id")
    String spreadsheetId;
    
    /**
     * OAuth2.0: Service Account (server to server, accessing own data)
     * https://github.com/googleapis/google-auth-library-java
     */
    public void init() throws Exception {

        final InputStream jsonFileStream = GoogleSheetService.class.getResourceAsStream("/c19data-service-account.json");
        final GoogleCredentials credentials = GoogleCredentials.fromStream(jsonFileStream).createScoped(Arrays.asList(SheetsScopes.SPREADSHEETS));

        // -- Code without customise timeout value
        // sheets = new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), new HttpCredentialsAdapter(credentials)).build();

        // -- Code with customise timeout value
        sheets = new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), setHttpTimeout(new HttpCredentialsAdapter(credentials)))
                .build();
    }


    public void clearData() throws Exception {

        final ClearValuesRequest requestBody = new ClearValuesRequest();
        final Sheets.Spreadsheets.Values.Clear request = sheets.spreadsheets().values().clear(spreadsheetId, SHEET_RANGE_ALL, requestBody);

        // Might have read time out issue with default (20 seconds) setting
        request.execute();
    }


    public void writeData(final List<List<Object>> values) throws Exception {

        final ValueRange requestBody = new ValueRange().setValues(values);
        final Sheets.Spreadsheets.Values.Update request = sheets.spreadsheets().values().update(spreadsheetId, SHEET_RANGE_START, requestBody)
                .setValueInputOption(SHEET_VAL_INPUT_OPTION_RAW);
        final UpdateValuesResponse response = request.execute();

        LOG.infof("%d cells inserted.", response.getUpdatedCells());
    }


    /**
     * Default read time out is 20 seconds
     * Solution: https://developers.google.com/api-client-library/java/google-api-java-client/errors
     */
    private HttpRequestInitializer setHttpTimeout(final HttpRequestInitializer requestInitializer) {
        return new HttpRequestInitializer() {
            @Override
            public void initialize(final HttpRequest httpRequest) throws IOException {
                requestInitializer.initialize(httpRequest);
                httpRequest.setConnectTimeout(1 * 60000); // 1 minutes connect timeout
                httpRequest.setReadTimeout(1 * 60000); // 1 minutes read timeout
            }
        };
    }
}
