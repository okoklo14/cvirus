package wtf.cockatoo.cvirus;

import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.logging.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import wtf.cockatoo.cvirus.datahub.DataExtractionService;
import wtf.cockatoo.cvirus.datahub.DataProcessingService;

/**
 *
 * PAT: The RequestHandler Input & Output parameter types can be custom defined objects instead of APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent
 *
 */
@Named("c19auto")
public class Covid19Automation implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger LOG = Logger.getLogger(Covid19Automation.class);

    @Inject
    DataExtractionService extractionService;
    @Inject
    DataProcessingService processingService;

    @Override
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        final String requestId = "#" + context.getAwsRequestId() + ":";

        try {
            final String data = extractionService.start(requestId);

            if (data != null) {
                if (processingService.start(requestId, data)) {
                    LOG.infof("Job completed. GoogleSheet should be reflected. ReqID = %s", requestId);
                }
                else {
                    LOG.fatalf("Job failed. ReqID = %s", requestId);
                }
            }
            else {
                LOG.fatal("Successful URL invocation with unexpected Null data returned.");
            }
        }
        catch (final Exception e) {
            LOG.errorf(e, "Err while calling services: %s", e.getMessage());
        }
        return null;
    }
}
