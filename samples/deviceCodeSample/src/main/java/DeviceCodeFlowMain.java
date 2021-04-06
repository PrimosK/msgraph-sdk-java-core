import com.azure.identity.DeviceCodeCredential;
import com.azure.identity.DeviceCodeCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.content.BatchRequestContent;
import com.microsoft.graph.content.BatchResponseContent;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.MessageCollectionPage;

import okhttp3.*;

import java.util.Arrays;
import java.util.List;


public class DeviceCodeFlowMain {

    //Replace CLIENT_ID with your own client id from an app that is configured according to the requirements below
    //for requirements visit:
    //https://github.com/Azure/azure-sdk-for-java/wiki/Set-up-Your-Environment-for-Authentication#enable-applications-for-device-code-flow
    private final static String CLIENT_ID = "6755f70d-fac6-4b8b-a460-1510aaa95d1e";
    private final static String TENANT_ID = "bd4c6c31-c49c-4ab6-a0aa-742e07c20232";

    //Set the scopes for your ms-graph request
    private final static List<String> SCOPES = Arrays.asList("Mail.Read", "User.Read");

    public static void main(String[] args) throws Exception {

        // initilizing the client
        final DeviceCodeCredential deviceCodeCred = new DeviceCodeCredentialBuilder()
                .clientId(CLIENT_ID)
                .tenantId(TENANT_ID)
                .challengeConsumer(challenge -> System.out.println(challenge.getMessage()))
                .build();

        final TokenCredentialAuthProvider tokenCredAuthProvider = new TokenCredentialAuthProvider(SCOPES, deviceCodeCred);

        GraphServiceClient<Request> graphClient = GraphServiceClient
                                                    .builder()
                                                    .authenticationProvider(tokenCredAuthProvider)
                                                    .buildClient();
        // async API
        System.out.println("Getting me (async)");
        graphClient.me().buildRequest().getAsync().thenAccept(u -> {
            System.out.println("Hello " + u.displayName + "(async)");
        }).get();

        // sync API
        final User me = graphClient.me().buildRequest().get();

        System.out.println("Hello " + me.displayName + "(sync)");


        // OffsetDateTime + fluent api for OData query string parameters
        final MessageCollectionPage messagesPage = graphClient.me().messages()
                                        .buildRequest()
                                        .top(2)
                                        .select("subject,receivedDateTime")
                                        .orderBy("receivedDateTime desc")
                                        .count()
                                        .get();
        System.out.println("got " + messagesPage.getCount() + " messages");
        final List<Message> messages = messagesPage.getCurrentPage();
        System.out.println("last message received at "+ messages.get(0).receivedDateTime.plusHours(1).toLocalDate().toString());


        // batching
        final BatchRequestContent batchRequestContent = new BatchRequestContent();
        final String meGetId = batchRequestContent
                        .addBatchRequestStep(graphClient
                                              .me()
                                              .buildRequest());
        final String messgesGetId = batchRequestContent.addBatchRequestStep(graphClient.me().messages()
        .buildRequest()
        .top(2));
        final BatchResponseContent batchResponseContent = graphClient.batch().buildRequest().post(batchRequestContent);

        final User user = batchResponseContent.getResponseById(meGetId).getDeserializedBody(User.class);
        System.out.println("Hello " + user.displayName + " (batch)");
    }
}
