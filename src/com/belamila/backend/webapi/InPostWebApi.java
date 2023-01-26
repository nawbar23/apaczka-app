package com.belamila.backend.webapi;

import com.belamila.model.Package;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Created by: Bartosz Nawrot
 * Date: 21.08.2020
 * Description:
 */
public class InPostWebApi {

    private static final Logger logger = LoggerFactory.getLogger(InPostWebApi.class);

    private final OkHttpClient client = new OkHttpClient();

    public JSONObject findInPost(Package pack) throws RuntimeException, IOException {
        String addressKey = (pack.getAddress() + ", " + pack.getCity() + ", " + pack.getZip())
                .replace(" ", "+");

        Request googleRequest = new Request.Builder()
                .method("GET", null)
                .url("https://maps.googleapis.com/maps/api/geocode/json?address=" + addressKey
                        + "&key=" + GoogleKeys.KEY)
                .build();

        Response googleResponse = client.newCall(googleRequest).execute();
        logger.info("Geo localizing ID: {}, response: {}", pack.getId(), googleResponse);

        JSONObject location = new JSONObject(googleResponse.body().string())
                .getJSONArray("results")
                .getJSONObject(0)
                .getJSONObject("geometry")
                .getJSONObject("location");
        logger.info("Location: {}", location);

        String point = location.getDouble("lat") + "," + location.getDouble("lng");
        Request request = new Request.Builder()
                .method("GET", null)
                .url("https://api-shipx-pl.easypack24.net/v1/points?relative_point=" + point
                        + "&sort_by=distance_to_relative_point&fields=name,distance,status")
                .addHeader("Authorization", InPostKeys.APP_TOKEN)
                .build();

        Response response = client.newCall(request).execute();
        logger.info("Finding parcel for ID: {}, response: {}", pack.getId(), response);

        JSONObject result = new JSONObject(response.body().string())
                .getJSONArray("items").getJSONObject(0);
        logger.info("Parcel for ID: {} found: {}", pack.getId(), result);

        return result;
    }

    public void verifyInPostId(String inPostId) throws RuntimeException, IOException {
        Request request = new Request.Builder()
                .method("GET", null)
                .url("https://api-shipx-pl.easypack24.net/v1/points/" + inPostId)
                .addHeader("Authorization", InPostKeys.APP_TOKEN)
                .build();

        logger.info("Validating InPostId: {}", request);

        Response response = client.newCall(request).execute();
        JSONObject responseJson = new JSONObject(response.body().string());

        logger.info("Response: {}", response);
        logger.info("Body: {}", responseJson);

        if (!response.isSuccessful() || !responseJson.get("status").equals("Operating")) {
            logger.warn("Request:{} failed:{}", request, response);
            throw new RuntimeException("InPostId: " + inPostId + " validation failed");
        }
    }
}
