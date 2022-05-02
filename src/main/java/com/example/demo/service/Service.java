package com.example.demo.service;

import com.example.demo.connection.HttpConnection;
import com.example.demo.dbUtil.DataCache;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Service {
    private static final String BASE_URL = "https://www.workato.com/";

    public static List createOrGetCustomer(String siteID, String email, Object manifestID, String apiKey) throws JSONException, IOException, InterruptedException {
        JSONObject response1 = new JSONObject();
        JSONObject response2 = new JSONObject();
        JSONObject authObject = new JSONObject();
        String fwCustomerName = siteID + "-wf";
        authObject.put("type", "workato_auth");
        if (true) {
            String endpoint = "api/managed_users";
            JSONObject integJSON = new JSONObject();
            integJSON.put("name", siteID);
            integJSON.put("notification_email", email);
            integJSON.put("time_zone", "Central Time (US & Canada)");
            integJSON.put("auth_settings", authObject);

            JSONObject workflowJSON = new JSONObject();
            workflowJSON.put("name", fwCustomerName);
            workflowJSON.put("notification_email", email);
            workflowJSON.put("time_zone", "Central Time (US & Canada)");
            workflowJSON.put("auth_settings", authObject);
            workflowJSON.put("whitelisted_apps", Collections.singletonList("Chargebee"));
            HttpConnection connection1 = new HttpConnection(BASE_URL + endpoint, HttpMethod.POST.name(), integJSON.toString());
            response1 = connection1.getResponse();
            JSONObject res1 = new JSONObject((String) response1.get(HttpConnection.HTTP_RESPONSE));
            if ((boolean) response1.get(HttpConnection.SUCCESS)) {
                DataCache.SITE_TO_ID_MAP.put(res1.get("name"), res1.get("id"));
            }
//            HttpConnection connection2 = new HttpConnection(BASE_URL + endpoint, HttpMethod.POST.name(), workflowJSON.toString());
//            response2 = connection2.getResponse();
//            JSONObject res2 = new JSONObject((String) response2.get(HttpConnection.HTTP_RESPONSE));
//            if ((boolean) response2.get(HttpConnection.SUCCESS)) {
//                DataCache.SITE_TO_ID_MAP.put(res2.get("name"), res2.get("id"));
//            }
//            System.out.println("CUSTOMER ID: "+res1.get("id")+"FOR CUSTOMER "+res1.get("name")+"   "+"CUSTOMER_WORKFLOW_ID : "+res2.get("id")+"FOR CUSTOMER "+res2.get("name"));
            Object folderID = createAndGetFolderID("mailchimp", res1.get("id"));
            System.out.println("CUSTOMER ID: " + res1.get("id") + "  FOLDER ID: " + folderID + " for folder mailchimp");
            callImportRecipe(manifestID, res1.get("id"), folderID);

            JSONObject r = createUpdateConnection(res1.get("id"), siteID, apiKey, email);
        } else {
            String endpoint = "api/managed_users/";
            HttpConnection connection1 = new HttpConnection(BASE_URL + endpoint + DataCache.SITE_TO_ID_MAP.get(siteID), HttpMethod.GET.name(), null);
            response1 = connection1.getResponse();
            HttpConnection connection2 = new HttpConnection(BASE_URL + endpoint + DataCache.SITE_TO_ID_MAP.get(fwCustomerName), HttpMethod.GET.name(), null);
            response2 = connection2.getResponse();

        }

        return Arrays.asList(response1, response2);
    }

    private static Object createAndGetFolderID(String folderName, Object id) throws JSONException, IOException {
        String endpoint = "/api/managed_users/" + id + "/folders";
        JSONObject folderJSON = new JSONObject();
        folderJSON.put("name", folderName);
        JSONObject response1 = new HttpConnection(BASE_URL + endpoint, HttpMethod.POST.name(), folderJSON.toString()).getResponse();
        JSONObject res = new JSONObject((String) response1.get(HttpConnection.HTTP_RESPONSE));
        return res.get("id");
    }

    public static JSONObject callImportRecipe(Object manifestID, Object customerID, Object folderID) throws JSONException, IOException {
        JSONObject custFolderJSON = new JSONObject();
        custFolderJSON.put("customer_id", customerID);
        custFolderJSON.put("folder_id", folderID);
        JSONObject wrapperJSON = new JSONObject();
        wrapperJSON.put("manifest_id", manifestID);
        wrapperJSON.put("customers", custFolderJSON);
        return new HttpConnection("https://www.workato.com/webhooks/rest/09424938-2a16-4966-9fa2-39e4c366bac8/import_customer_recipes", HttpMethod.POST.name(), wrapperJSON.toString()).getResponse();
    }

    public static JSONObject createUpdateConnection(Object managedCustomer, String subDomain, String apiKey, String email) throws IOException, JSONException, InterruptedException {
        String endpoint = "https://www.workato.com/api/managed_users/" + managedCustomer + "/connections";
        JSONObject connectionObject = getObject(endpoint);
        if (connectionObject == null) return null;
        String connectionID = connectionObject.optString("id");
        String name = connectionObject.optString("name");
        String provider = connectionObject.optString("provider");
        JSONObject requestJSON = new JSONObject();
        requestJSON.put("name", name);
        requestJSON.put("provider", provider);
        JSONObject input = new JSONObject();
        input.put("subdomain", subDomain);
        input.put("oauth", false);
        input.put("email", email);
        input.put("api_key", apiKey);
        requestJSON.put("input", input);
        JSONObject responce = new HttpConnection(endpoint + "/" + connectionID, HttpMethod.PUT.name(), requestJSON.toString()).getResponse();
        return new JSONObject((String) responce.get(HttpConnection.HTTP_RESPONSE));
    }

    private static JSONObject getObject(String endpoint) throws IOException, InterruptedException, JSONException {
        int k = 10;
        while (k-- > 0) {
            JSONObject response1 = new HttpConnection(endpoint, HttpMethod.GET.name(), null).getResponse();
            JSONObject responce = new JSONObject((String) response1.get(HttpConnection.HTTP_RESPONSE));
            JSONArray jsonArray = (JSONArray) responce.get("result");
            if (jsonArray.length() > 0)
                return (jsonArray).optJSONObject(0);
            Thread.sleep(2000);
        }
        return null;
    }
}