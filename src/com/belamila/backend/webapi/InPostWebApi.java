package com.belamila.backend.webapi;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by: Bartosz Nawrot
 * Date: 21.08.2020
 * Description:
 */
public class InPostWebApi {

    private static final Logger logger = LoggerFactory.getLogger(InPostWebApi.class);

    private final OkHttpClient client = new OkHttpClient();

    public void verifyInPostId(String inPostId) throws RuntimeException, IOException {
        Request request = new Request.Builder()
                .method("GET", null)
                .url("https://api-shipx-pl.easypack24.net/v1/points/" + inPostId)
                .addHeader("Authorization", InPostKeys.APP_TOKEN)
                .build();

        logger.info("Validating InPostId:{}", request);

        Response response = client.newCall(request).execute();
        JSONObject responseJson = new JSONObject(response.body().string());

        if (!response.isSuccessful()) {
            logger.warn("Request:{} failed:{}", request, response);
            throw new RuntimeException("InPostId: " + inPostId + " validation failed: "
                    + responseJson.getString("message"));
        }
    }
}
