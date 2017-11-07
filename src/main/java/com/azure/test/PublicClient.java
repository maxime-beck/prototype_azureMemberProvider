package com.azure.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.naming.ServiceUnavailableException;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;
import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.membership.MemberImpl;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;


public class PublicClient {

    /**
     * Fill in these constants with your personal data
     */
    private final static String SUBSCRIPTION_ID = "<subscription id>";
    private final static String TENANT_ID = "<tenant id>";
    private final static String RESOURCEGROUPNAME = "<resource group name>";
    private final static String CLIENT_ID = "<client id>";
    private final static String PASSWORD = "<service principal password>";

    private final static String AUTHORITY = "https://login.windows.net/" + TENANT_ID;

    private static AuthenticationResult result;

    public static void main(String args[]) throws Exception {

        result = getAccessTokenFromUserCredentials();
        System.out.println("Access Token - " + result.getAccessToken());

        List<? extends Member> members = getMembers();

        System.out.println("\nMember list inside \"" + RESOURCEGROUPNAME + "\" :");
        if(members.size() == 0)
            System.out.println("None.");

        for(Member member : members) {
            System.out.println("> " + member.getName());
        }
    }

    private static AuthenticationResult getAccessTokenFromUserCredentials() throws Exception {
        AuthenticationContext context = null;
        AuthenticationResult result = null;
        ExecutorService service = null;
        try {
            service = Executors.newFixedThreadPool(1);
            context = new AuthenticationContext(AUTHORITY, false, service);
            ClientCredential credential = new ClientCredential(CLIENT_ID,PASSWORD);
            Future<AuthenticationResult> future = context.acquireToken("https://management.azure.com/", credential, null);
            result = future.get();
        } finally {
            service.shutdown();
        }
        if (result == null) {
            throw new ServiceUnavailableException("authentication result was null");
        }
        return result;
    }

    private static List<? extends Member> getMembers() throws Exception {

        List<MemberImpl> members = new ArrayList<>();

        HttpClient client = new DefaultHttpClient();

        HttpGet request = new HttpGet("https://management.azure.com/subscriptions/" + SUBSCRIPTION_ID +
                "/resourceGroups/" + RESOURCEGROUPNAME +
                "/providers/Microsoft.Web/sites?api-version=2016-08-01");
        request.addHeader("Authorization","Bearer " + result.getAccessToken());
        HttpResponse response = client.execute(request);

        /* Print JSON response */
        /*
        BufferedReader rd = new BufferedReader (new InputStreamReader(response.getEntity().getContent()));
        String line = "";
        while ((line = rd.readLine()) != null)
        {
            System.out.println(line);
        }
        */

        InputStreamReader streamReader = new InputStreamReader(response.getEntity().getContent());
        JSONObject json = new JSONObject(new JSONTokener(streamReader));

        JSONArray items = json.getJSONArray("value");

        int port = 80; // We'll get this from properties
        for (int i = 0; i < items.length(); i++) {
            String id;
            String phase;
            String host;
            String ips;
            String name;
            Instant creationTime;

            try {
                JSONObject item = items.getJSONObject(i);
                id = item.getString("id");
                name = item.getString("name");
                ips = item.getJSONObject("properties").getString("outboundIpAddresses");
                host = item.getJSONObject("properties").getJSONArray("hostNames").getString(0);

                /*
                phase = status.getString("phase");

                // Ignore shutdown pods
                if (!phase.equals("Running"))
                    continue;

                ip = status.getString("podIP");

                // Get name & start time
                JSONObject metadata = item.getJSONObject("metadata");
                name = metadata.getString("name");
                String timestamp = metadata.getString("creationTimestamp");
                creationTime = Instant.parse(timestamp);
                */
            } catch (JSONException e) {
                continue;
            }

            // We found ourselves, ignore
            /*
            if (name.equals(hostName))
                continue;


            // id = md5(hostname)
            byte[] id = md5.digest(name.getBytes());
            long aliveTime = Duration.between(creationTime, startTime).getSeconds() * 1000; // aliveTime is in ms
            */
            MemberImpl member = null;
            try {
                member = new MemberImpl(host, port, 0);
            } catch (IOException e) {
                // Shouldn't happen:
                // an exception is thrown if hostname can't be resolved to IP, but we already provide an IP
                continue;
            }

            //member.setUniqueId(id);
            members.add(member);
        }

        return members;
    }
}
