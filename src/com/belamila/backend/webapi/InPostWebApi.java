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

    public String suggestInPost(Package pack) throws RuntimeException, IOException {
        String addressKey = (pack.getAddress() + ", " + pack.getCity() + ", " + pack.getZip())
                .replace(" ", "+");
        logger.info("Id: {}, address to be geo localized: {}", pack.getId(), addressKey);

        // TODO geo localize

        // TODO find parcel by lat and lon

        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {
        }

        return null;
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
