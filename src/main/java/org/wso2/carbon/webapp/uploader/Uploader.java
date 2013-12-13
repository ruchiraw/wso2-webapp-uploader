package org.wso2.carbon.webapp.uploader;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.transport.http.HTTPConstants;
import org.wso2.carbon.authenticator.stub.AuthenticationAdminStub;
import org.wso2.carbon.webapp.mgt.stub.WebappAdminStub;
import org.wso2.carbon.webapp.mgt.stub.types.carbon.WebappUploadData;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Uploader {

    private static String serverUrl = "https://localhost:9443/services/";
    private static String username = "admin";
    private static String password = "admin";

    private static String webappPath = "src/main/resources/example.war";
    private static String webappName = "example.war";
    private static String webappVersion = "default";

    public static void main(String[] args) throws Exception {
        WebappAdminStub stub = null;
        try {
            String cookie = authenticate(username, password);
            if (cookie == null) {
                System.err.println("Error authenticating user : " + username);
                return;
            }
            final FileInputStream inputStream = new FileInputStream(webappPath);
            stub = new WebappAdminStub(getConfigurationContext(), serverUrl + "WebappAdmin");
            ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
            option.setProperty(Constants.Configuration.ENABLE_MTOM, Constants.VALUE_TRUE);

            //build webapp data
            WebappUploadData data = getUploadData(inputStream, webappName, webappVersion);
            //upload the war file
            stub.uploadWebapp(new WebappUploadData[]{data});
            System.out.println("Successfully uploaded the webapp");
        } finally {
            if (stub != null) {
                try {
                    stub.cleanup();
                } catch (Exception ignore) {

                }
            }
        }
    }

    private static WebappUploadData getUploadData(final InputStream in, String name, String version)
            throws Exception {
        WebappUploadData data = new WebappUploadData();
        data.setFileName(name);
        data.setDataHandler(new DataHandler(new DataSource() {
            @Override
            public InputStream getInputStream() throws IOException {
                return in;
            }

            @Override
            public OutputStream getOutputStream() throws IOException {
                throw new UnsupportedOperationException("Not implemented");
            }

            @Override
            public String getContentType() {
                return "*/*";
            }

            @Override
            public String getName() {
                return "DataSource";
            }
        }));
        data.setVersion(version);
        return data;
    }

    private static String authenticate(String username, String password) throws Exception {
        AuthenticationAdminStub stub = null;
        try {
            stub = new AuthenticationAdminStub(getConfigurationContext(), serverUrl + "AuthenticationAdmin");
            if (!stub.login(username, password, null)) {
                return null;
            }
            return (String) stub._getServiceClient().getLastOperationContext().getServiceContext()
                    .getProperty(HTTPConstants.COOKIE_STRING);
        } finally {
            if (stub != null) {
                try {
                    stub.cleanup();
                } catch (Exception ignore) {

                }
            }
        }
    }

    private static ConfigurationContext getConfigurationContext() throws AxisFault {
        return ConfigurationContextFactory.createConfigurationContextFromFileSystem("src/main/resources/axis2_client.xml");
    }
}
