package com.example.demo;

import com.example.demo.connection.HttpConnection;
import com.example.demo.service.Service;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1")
public class Controller {
    private static final String BASE_URL = "https://www.workato.com/";

    @GetMapping("/auth")
    public ResponseEntity<String> auth() throws IOException, JSONException {
        String endpoint = "api/users/me";
        HttpConnection connection = new HttpConnection(BASE_URL + endpoint, HttpMethod.GET.name(), null);
        JSONObject response = connection.getResponse();
        return ResponseEntity.ok(response.toString());
    }

    @GetMapping("/customers")
    public ResponseEntity<String> getCustomers() throws IOException, JSONException {
        String endpoint = "api/managed_users";
        HttpConnection connection = new HttpConnection(BASE_URL + endpoint, HttpMethod.GET.name(), null);
        JSONObject response = connection.getResponse();
        return ResponseEntity.ok(response.toString());
    }

    @GetMapping("customer")
    public ResponseEntity<String> createCustomer(@RequestParam(name = "site") String siteId, @RequestParam(name = "email") String email, @RequestParam(name = "manifest_id") String manifestID, @RequestParam(name = "api_key") String apiKey, @RequestParam(name = "folder") String folder) throws Exception {
//        if (StringUtils.isEmpty(siteId)) {
//            siteId = "0";
//        }
//        if (StringUtils.isEmpty(email)) {
//            email = "0@chargebee.com";
//        }
//
//        if (StringUtils.isEmpty(manifestID)) {
//            manifestID = "0";
//        }
//        if (StringUtils.isEmpty(apiKey)) {
//            apiKey = "0";
//        }
//        if (StringUtils.isEmpty(folder)) {
//            folder = "mailchimp";
//        }
        return ResponseEntity.ok(new Service().createOrGetCustomer(siteId, email, manifestID, apiKey,folder));
    }

    @GetMapping("deleterecipe")
    public ResponseEntity<String> deleteRecipe(@RequestParam(name = "customerid") Integer customerid, @RequestParam(name = "folderid") Integer folderId) throws Exception {
        return ResponseEntity.ok(new Service().disconnect(customerid, folderId));
    }
}


