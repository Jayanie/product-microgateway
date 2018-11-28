import ballerina/io;
import ballerina/log;
import ballerina/http;

string throttleEndpointUrl = getConfigValue(THROTTLE_CONF_INSTANCE_ID,THROTTLE_ENDPOINT_URL,"https://localhost:9443/endpoints");

public function publishThrottleEventToTrafficManager(RequestStreamDTO throttleEvent) {
    endpoint http:Client throttleEndpoint {
        url: throttleEndpointUrl
    };
    json sendEvent = {
        event: {
            metaData: {},
            correlationData: {},
            payloadData: {
                messageID: throttleEvent.messageID,
                appKey: throttleEvent.appKey,
                appTier: throttleEvent.appTier,
                apiKey: throttleEvent.apiKey,
                apiTier: throttleEvent.apiTier,
                subscriptionKey: throttleEvent.subscriptionKey,
                subscriptionTier: throttleEvent.subscriptionTier,
                resourceKey: throttleEvent.resourceKey,
                resourceTier: throttleEvent.resourceTier,
                userId: throttleEvent.userId,
                apiContext: throttleEvent.apiContext,
                apiVersion: throttleEvent.apiVersion,
                appTenant: throttleEvent.appTenant,
                apiTenant: throttleEvent.apiTenant,
                appId: throttleEvent.appId,
                apiName: throttleEvent.apiName,
                properties: throttleEvent.properties
            }
        }
    };
    http:Request clientRequest = new;
    io:println("sending throttle event to traffic manager");
    io:println(sendEvent);
    clientRequest.setPayload(sendEvent);
    io:println("success1");

    var response = throttleEndpoint->post("/throttleEventReceiver", clientRequest);

    match response {
        http:Response resp => {
            log:printInfo("\nStatus Code: " + resp.statusCode);
        }
        error err => {
            log:printError(err.message, err = err);
        }
    }
}



