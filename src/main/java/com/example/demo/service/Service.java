package com.example.demo.service;

import com.example.demo.connection.HttpConnection;
import com.example.demo.dbUtil.DataCache;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static java.lang.System.out;

public class Service {
    private static final String BASE_URL = "https://www.workato.com/api/managed_users/";
    private static JSONObject cache = null;
    public String createOrGetCustomer(String siteID, String email, String manifestID, String apiKey, String folder) throws Exception {
        try {
            JSONObject authObject = new JSONObject();
            authObject.put("type", "workato_auth");
            if (!DataCache.SITE_TO_ID_MAP.containsKey(siteID)) {
                out.println("Going to create Customer in workato");
                JSONObject integJSON = new JSONObject();
                integJSON.put("name", siteID);
                integJSON.put("notification_email", email);
                integJSON.put("time_zone", "Kolkata");
                integJSON.put("auth_settings", authObject);
//          workflowJSON.put("whitelisted_apps", Collections.singletonList("Chargebee"));
                HttpConnection connection = new HttpConnection(BASE_URL, HttpMethod.POST.name(), integJSON.toString());
                JSONObject response = connection.getResponse();
                pushAndStartRecipes(response, manifestID, siteID, apiKey, email, true, folder);
            } else {
                HttpConnection connection = new HttpConnection(BASE_URL + DataCache.SITE_TO_ID_MAP.get(siteID), HttpMethod.GET.name(), null);
                JSONObject response = connection.getResponse();
                pushAndStartRecipes(response, manifestID, siteID, apiKey, email, false, folder);
            }
            return "Successfully connected";
        } catch (Exception e) {
            e.printStackTrace();
            return "Connection failed";
        }
    }

    public void pushAndStartRecipes(JSONObject response, String manifestID, String siteID, String apiKey, String email, boolean isNew, String project) throws Exception {
        Callable c2 = () -> {
            JSONObject responseObj = new JSONObject((String) response.get(HttpConnection.HTTP_RESPONSE));
            Integer customerID = (Integer) responseObj.get("id");
            out.println("Customer created..... Customer ID :: " + customerID);
            if (isNew && (boolean) response.get(HttpConnection.SUCCESS)) {
                DataCache.SITE_TO_ID_MAP.put(responseObj.get("name"), customerID);
            }
            out.println("Going to create folder for " + project);
            Integer folderID1 = createAndGetFolderID(project, customerID);
            out.println("Folder for " + project + " is created and created Folder ID  :: " + folderID1);
            out.println("Going to import project manifest into customer project folder");
            callImportRecipe(manifestID, customerID, folderID1);
            out.println("Successfully invoked import manifest API");
            out.println("Going to update Chargebee connection");
            createUpdateConnection(customerID, siteID, apiKey, email, folderID1);
            out.println("Chargebee connection Authenticated successfully....");
            out.println("Going to start the recipe in customer project folder....");
            boolean isThereErrors = changeRecipeState(customerID, folderID1, true);
            if (isThereErrors) {
                out.println("Error in starting all the recipe in project folder....");
            } else {
                out.println("All the recipe in customer project folder is started....");
            }
            return null;
        };
        c2.call();
    }

    public Integer createAndGetFolderID(String folderName, Integer id) throws JSONException, IOException {
        JSONObject folderJSON = new JSONObject();
        folderJSON.put("name", folderName);
        JSONObject response = new HttpConnection(BASE_URL + id + "/folders", HttpMethod.POST.name(), folderJSON.toString()).getResponse();
        JSONObject res = new JSONObject((String) response.get(HttpConnection.HTTP_RESPONSE));
        return (Integer) res.get("id");
    }

    public void callImportRecipe(String manifestID, Integer customerID, Integer folderID) throws JSONException, IOException {
        JSONObject custFolderJSON = new JSONObject();
        custFolderJSON.put("customer_id", customerID);
        custFolderJSON.put("folder_id", folderID);
        JSONObject wrapperJSON = new JSONObject();
        wrapperJSON.put("manifest_id", manifestID);
        wrapperJSON.put("customers", custFolderJSON);
        new HttpConnection("https://www.workato.com/webhooks/rest/09424938-2a16-4966-9fa2-39e4c366bac8/import_customer_recipes", HttpMethod.POST.name(), wrapperJSON.toString()).getResponse();
    }

    public void createUpdateConnection(Object managedCustomer, String subDomain, String apiKey, String email, Object folderID) throws IOException, JSONException, InterruptedException {
        String endpoint = BASE_URL + managedCustomer + "/connections";
        JSONObject connectionObject = getConnectionObject(endpoint + "?folder_id=" + folderID);
        if (connectionObject == null) return;
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
        new JSONObject((String) responce.get(HttpConnection.HTTP_RESPONSE));
    }

    private JSONObject getConnectionObject(String endpoint) throws IOException, InterruptedException, JSONException {
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

    public boolean changeRecipeState(Integer managedUserID, Integer folderId, boolean start) throws IOException, JSONException {
        List<String> recipeIds = getRecipeIdsFromFolder(managedUserID, folderId);
        List<String> errorInActionRecipes = new ArrayList<>();
        for (String recipeId : recipeIds) {
            out.println("Going to " + (start ? "start" : "stop") + " recipe :: " + recipeId);
            if (!startOrStopRecipe(managedUserID, recipeId, start)) {
                errorInActionRecipes.add(recipeId);
                out.println("failed action :::::  " + recipeId);
            } else {
                out.println("success action :::::  " + recipeId);
            }
        }
        return !errorInActionRecipes.isEmpty();
    }

    public boolean startOrStopRecipe(Integer managedUserID, String recipeId, boolean start) throws IOException, JSONException {
        String endpoint = BASE_URL + managedUserID + "/recipes/" + recipeId + (start ? "/start" : "/stop");
        JSONObject response = new HttpConnection(endpoint, HttpMethod.PUT.name(), null).getResponse();
        JSONObject responce = new JSONObject((String) response.get(HttpConnection.HTTP_RESPONSE));
        return responce.optBoolean("success");
    }

    private List<String> getRecipeIdsFromFolder(Integer managedUserID, Integer folderId) throws IOException, JSONException {
        out.println("Going to fetch all the recipes in the project folder :: " + folderId);
        String endpoint = BASE_URL + managedUserID + "/recipes?folder_id=" + folderId;
        JSONObject response = new HttpConnection(endpoint, HttpMethod.GET.name(), null).getResponse();
        JSONObject responce = new JSONObject((String) response.get(HttpConnection.HTTP_RESPONSE));
        JSONArray jsonArray = (JSONArray) responce.get("result");
        List<String> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); ++i) {
            list.add((jsonArray).optJSONObject(i).optString("id"));
        }
        out.println("Number of recipes found in the folder :: " + list.size());
        return list;
    }

    public String stopRecipe(Integer managedUserID, Integer folderID) throws JSONException, IOException {
        boolean isThereErrors = changeRecipeState(managedUserID, folderID, false);
        if (isThereErrors) {
            return "Error in stopping all the recipe in project folder.... ";
        }
        return "All the recipe in customer project folder is stopped....";
    }
    public static JSONObject convertJSON(String jsonString) throws JSONException {
        return new JSONObject(jsonString);
    }
//    private static String configEndpoint = "https://www.workato.com/api/managed_users/604353/recipes/2389626";
//    private static final String configEndpoint = "https://www.workato.com/api/managed_users/604353/recipes/";
    public static JSONObject getProps(String integID,String customerID) throws JSONException, IOException {
        HttpConnection httpConnection1= new HttpConnection(getConfigEndpoint(customerID)+integID,HttpMethod.GET.name(), null);
        JSONObject re1=httpConnection1.getResponse();
        String wraperJSONString = re1.optString(HttpConnection.HTTP_RESPONSE);
        cache = convertJSON(wraperJSONString);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("parameters_schema",cache.getJSONArray("parameters_schema"));
        jsonObject.put("parameters",cache.optJSONObject("parameters"));
        return jsonObject;
    }

    public static void putProps(String integID,String option1,String option2,String customerID) throws JSONException, IOException {
        if (cache == null) {
            out.println("Cache is empty.......");
            return;
        }
        cache.remove("config");
        JSONObject codeJSONObject = convertJSON(cache.getString("code"));
        JSONObject paramJSONObject = codeJSONObject.getJSONObject("param");
        paramJSONObject.put("status", option1);
        paramJSONObject.put("work", option2);
        codeJSONObject.put("param", paramJSONObject);
        cache.put("code", codeJSONObject.toString());
        HttpConnection httpConnection = new HttpConnection(getConfigEndpoint(customerID) + integID, HttpMethod.PUT.name(), cache.toString());
        httpConnection.getResponse();
    }

    public static String getConfigEndpoint(String customerID){
        return "https://www.workato.com/api/managed_users/"+customerID+"/recipes/";
    }
}
