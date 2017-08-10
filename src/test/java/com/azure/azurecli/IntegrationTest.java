package com.azure.azurecli;

import com.microsoft.azure.util.AzureCredentials;
import org.junit.Before;

/*
To execute the integration tests you need to set the credentials env variables (the ones that don't have a default) and run mvn failsafe:integration-test
*/
public class IntegrationTest {

    protected static class TestEnvironment {
        public final String subscriptionId;
        public final String clientId;
        public final String clientSecret;
        public final String oauth2TokenEndpoint;
        public final String serviceManagementURL;
        public final String authenticationEndpoint;
        public final String resourceManagerEndpoint;
        public final String graphEndpoint;
        protected TestEnvironment testEnv = null;
        protected CredentialsCache credentialsCache = null;
        protected ShellExecuter shellExecuter = null;

        TestEnvironment() {
            subscriptionId = TestEnvironment.loadFromEnv("AZURE_CLI_TEST_SUBSCRIPTION_ID");
            clientId = TestEnvironment.loadFromEnv("AZURE_CLI_TEST_CLIENT_ID");
            clientSecret = TestEnvironment.loadFromEnv("AZURE_CLI_TEST_CLIENT_SECRET");
            oauth2TokenEndpoint = TestEnvironment.loadFromEnv("AZURE_CLI_TEST_TOKEN_ENDPOINT");
            serviceManagementURL = TestEnvironment.loadFromEnv("AZURE_CLI_TEST_AZURE_URL");
            authenticationEndpoint = TestEnvironment.loadFromEnv("AZURE_CLI_TEST_AZURE_AUTH_URL");
            resourceManagerEndpoint = TestEnvironment.loadFromEnv("AZURE_CLI_TEST_AZURE_RESOURCE_URL");
            graphEndpoint = TestEnvironment.loadFromEnv("AZURE_CLI_TEST_AZURE_GRAPH_URL");

        }

        private static String loadFromEnv(String name) {
            return TestEnvironment.loadFromEnv(name, "");
        }

        private static String loadFromEnv(String name, String defaultValue) {
            final String value = System.getenv(name);
            if (value == null || value.isEmpty()) {
                return defaultValue;
            } else {
                return value;
            }
        }

        @Before
        public void setUp() {
            testEnv = new TestEnvironment();
            AzureCredentials.ServicePrincipal servicePrincipal = new AzureCredentials.ServicePrincipal(
                    testEnv.subscriptionId,
                    testEnv.clientId,
                    testEnv.clientSecret,
                    testEnv.oauth2TokenEndpoint,
                    testEnv.serviceManagementURL,
                    testEnv.authenticationEndpoint,
                    testEnv.resourceManagerEndpoint,
                    testEnv.graphEndpoint);

            credentialsCache = new CredentialsCache(servicePrincipal);
            shellExecuter = new ShellExecuter();
        }


    }
}
