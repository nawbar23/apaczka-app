package com.belamila.backend.webapi;

import com.belamila.model.Package;
import okhttp3.*;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Created by: Bartosz Nawrot
 * Date: 21.07.2020
 * Description:
 */
public class ApaczkaWebApi {

    private static final Logger logger = LoggerFactory.getLogger(ApaczkaWebApi.class);

    private final String API_URL = "https://www.apaczka.pl/api/v2/";

    private final String APP_ID = "1254552_5f17ff51d9ec51.40863007";
    private final String APP_SECRET = "85e5m4fk75t6mb3pcy68n7yjwt53v983";

    private final OkHttpClient client = new OkHttpClient();

    public void issueOrdersAndDownloadCards(List<Package> packages, File file) throws Exception {
        for (Package p : packages) {
            valuateOrder(p);
        }

        // TODO issue orders

        // TODO download waybill and safe in file directory with name "Package.receiver Package.service.pdf"
    }

    public JSONObject valuateOrder(Package p) throws Exception {
        JSONObject json = new JSONObject();
        json.put("order", Order.build(p));
        return request("order_valuation/", json.toString());
    }

    public JSONObject listOrders(int page, int limit) throws Exception {
        JSONObject json = new JSONObject();
        json.put("page", page);
        json.put("limit", limit);
        return request("orders/", json.toString());
    }

    private JSONObject request(String endpoint, String data) throws Exception {
        String expires = getExpires();
        String sing = stringToSign(endpoint, data, expires);
        String signature = getSignature(sing);

        logger.info("String: {}", sing);
        logger.info("Signature: {}", signature);

        RequestBody formBody = new FormBody.Builder()
                .add("app_id", APP_ID)
                .add("signature", signature)
                .add("expires", expires)
                .add("request", data)
                .build();
        Request request = new Request.Builder()
                .url(API_URL + endpoint)
                .post(formBody)
                .build();
        Response response = client.newCall(request).execute();
        String responseString = response.body().string();
        response.body().close();

        logger.info("Call: {}({}): {}", endpoint, data, responseString);

        JSONObject responseJson = new JSONObject(responseString);
        if (responseJson.getInt("status") != 200) {
            throw new RuntimeException(responseJson.getString("message"));
        }
        return responseJson.getJSONObject("response");
    }

    private String getExpires() {
        return String.valueOf((System.currentTimeMillis() + 1800000)/1000);
    }

    private String stringToSign(String endpoint, String data, String expires) {
        return String.format("%s:%s:%s:%s", APP_ID, endpoint, data, expires);
    }

    private String getSignature(String data) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(APP_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        return Hex.encodeHexString(sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }
}
