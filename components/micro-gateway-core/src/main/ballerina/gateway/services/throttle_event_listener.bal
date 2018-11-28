import ballerina/jms;
import ballerina/log;
import ballerina/io;
import ballerina/config;

string jmsConnectioninitialContextFactory = getConfigValue(THROTTLE_CONF_INSTANCE_ID,
    JMS_CONNECTION_INITIAL_CONTEXT_FACTORY,
    "bmbInitialContextFactory");
string jmsConnectionProviderUrl = getConfigValue(THROTTLE_CONF_INSTANCE_ID, JMS_CONNECTION_PROVIDER_URL,
    "amqp://admin:admin@carbon/carbon?brokerlist='tcp://localhost:5672'");
string jmsConnectionPassword = getConfigValue(THROTTLE_CONF_INSTANCE_ID, JMS_CONNECTION_PASSWORD, "");
string jmsConnectionUsername = getConfigValue(THROTTLE_CONF_INSTANCE_ID, JMS_CONNECTION_USERNAME, "");

// Initialize a JMS connection with the provider.
jms:Connection jmsConnection = new({
        initialContextFactory: jmsConnectioninitialContextFactory,
        providerUrl: jmsConnectionProviderUrl,
        username: jmsConnectionUsername,
        password: jmsConnectionPassword
    });

// Initialize a JMS session on top of the created connection.
jms:Session jmsSession = new(jmsConnection, {
        acknowledgementMode: "AUTO_ACKNOWLEDGE"
    });

// Initialize a Topic subscriber using the created session.
endpoint jms:TopicSubscriber subscriberEndpoint {
    session: jmsSession,
    topicPattern: "throttleData"
};

// Bind the created consumer(subscriber endpoint) to the listener service.
service<jms:Consumer> jmsListener bind subscriberEndpoint {
    onMessage(endpoint subscriber, jms:Message message) {
        match (message.getMapMessageContent()) {
            map m => {
                log:printDebug("ThrottleMessage Received");
                //Throttling decisions made by TM going to throttleDataMap
                if (m.hasKey(THROTTLE_KEY)){
                    string throttleKey = <string>m[THROTTLE_KEY];
                    io:println(throttleKey);

                    boolean throttleState = check <boolean>m[IS_THROTTLED];
                    io:println(throttleState);

                    int expiryTimeStamp = check <int>m[EXPIRY_TIMESTAMP];
                    io:println(expiryTimeStamp);

                    if (throttleState){
                        string s1 = "putThrottleData";
                        io:println(s1);

                        putThrottleData(throttleKey, expiryTimeStamp);
                    } else {
                        string s2 = "removeThrottleData";
                        io:println(s2);

                        removeThrottleData(throttleKey);
                    }
                    //Blocking decisions going to a separate map
                } else if (m.hasKey(BLOCKING_CONDITION_KEY)){
                    string s3 = "putBlockCondition";
                    io:println(s3);

                    putBlockCondition(m);
                }
            }
            error e => log:printError("Error occurred while reading message", err = e);
        }
    }
}

