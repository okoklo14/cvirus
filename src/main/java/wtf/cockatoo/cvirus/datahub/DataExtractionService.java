package wtf.cockatoo.cvirus.datahub;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class DataExtractionService {
    private static final Logger LOG = Logger.getLogger(DataExtractionService.class);

    @ConfigProperty(name = "data.source.url")
    String dataSourceUrl;


    public String start(final String awsReqId) throws Exception {
        LOG.infof("%s extracting data from %s", awsReqId, dataSourceUrl);

        final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(20))
                .build();
        final HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(dataSourceUrl)).headers("cache-control", "no-cache", "pragma", "no-cache").build();
        final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());


        switch (response.statusCode()) {
            case 200:
                return response.body();

            default:
                throw new InternalError(String.format("%s HTTP Res: %d, Body Res: %s", awsReqId, response.statusCode(), response.body()));
        }
    }
}
