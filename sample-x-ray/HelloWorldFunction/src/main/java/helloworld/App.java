package helloworld;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.AWSKMSException;
import com.amazonaws.services.kms.model.KeyListEntry;
import com.amazonaws.services.kms.model.ListKeysRequest;
import com.amazonaws.services.kms.model.ListKeysResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.xray.proxies.apache.http.HttpClientBuilder;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.lambda.powertools.tracing.Tracing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static software.amazon.lambda.powertools.tracing.CaptureMode.DISABLED;
import static software.amazon.lambda.powertools.tracing.CaptureMode.RESPONSE_AND_ERROR;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    Logger log = LogManager.getLogger(App.class);

    @Tracing(captureMode = DISABLED)
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        var headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        this.getS3Buckets();

        this.listKmsKeys();

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(headers);
        try {


            final String pageContents = this.getPageContents("https://checkip.amazonaws.com");
            String output = String.format("{ \"message\": \"hello world\", \"location\": \"%s\" }", pageContents);

            return response
                    .withStatusCode(200)
                    .withBody(output);
        } catch (IOException e) {
            return response
                    .withBody("{}")
                    .withStatusCode(500);
        }


    }
    @Tracing(captureMode = RESPONSE_AND_ERROR)
    private String getPageContents(String address) throws IOException {

//      Use aws implemented HttpClientBuilder
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet httpGet = new HttpGet(address);
        CloseableHttpResponse response = httpClient.execute(httpGet);
        try {
            HttpEntity entity = response.getEntity();
            InputStream inputStream = entity.getContent();
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        } finally {
            response.close();
        }
    }

//    https://docs.aws.amazon.com/lambda/latest/dg/services-xray.html， OverHead
    @Tracing(captureMode = DISABLED)
    private void getS3Buckets(){
        final AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.AP_SOUTHEAST_1).build();
        List<Bucket> buckets = s3.listBuckets();
        System.out.println("Your Amazon S3 buckets are:");
        for (Bucket b : buckets) {
            System.out.println("* " + b.getName());
        }
    }


    @Tracing(captureMode = DISABLED)
    private void listKmsKeys(){
        try {

            final AWSKMS kmsClient = AWSKMSClientBuilder.standard().withRegion(Regions.AP_SOUTHEAST_1).build();
            ListKeysRequest listKeysRequest = new ListKeysRequest().withLimit(10);
            ListKeysResult result = kmsClient.listKeys(listKeysRequest);
            for (KeyListEntry entry: result.getKeys()){
                System.out.println("key arn: " + entry.getKeyArn() + " kms keyid: " + entry.getKeyId());
            }
        } catch (AWSKMSException e){
            System.out.println(e.getErrorMessage());
        }
    }
}