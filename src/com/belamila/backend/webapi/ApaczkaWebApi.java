package com.belamila.backend.webapi;

import com.belamila.model.Package;
import com.belamila.ui.ProgressListener;
import okhttp3.*;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Decoder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Created by: Bartosz Nawrot
 * Date: 21.07.2020
 * Description:
 */
public class ApaczkaWebApi {

    private static final Logger logger = LoggerFactory.getLogger(ApaczkaWebApi.class);

    private static final String API_URL = "https://www.apaczka.pl/api/v2/";
    private static final String APP_ID = ApaczkaKeys.APP_ID;
    private static final String APP_SECRET = ApaczkaKeys.APP_SECRET;

    private final InPostWebApi inPostWebApi = new InPostWebApi();
    private final OkHttpClient client = new OkHttpClient();

    private final ProgressListener listener;

    public ApaczkaWebApi(ProgressListener listener) {
        this.listener = listener;
    }

    public void issueOrdersAndDownloadCards(List<Package> packages, File file) throws Exception {
        for (Package p : packages) {
            if (p.getService().equals("INPOST")) {
                inPostWebApi.verifyInPostId(p.getInPostId());
            }
            valuateOrder(p);
        }

        int i = 1;
        for (Package p : packages) {
            JSONObject send = sendOrder(p);
            JSONObject waybill = downloadWaybill(
                    send.getJSONObject("order").getInt("id"));
            safeWaybill(waybill.getString("waybill"), p, file);

            StringBuilder progress = new StringBuilder();
            progress.append(i++).append('/').append(packages.size()).append(" - ");
            progress.append(p.getId()).append(" ");
            progress.append(p.getReceiver()).append(" ");
            progress.append(p.getServiceName());
            progress.append(send.getJSONObject("order").getString("waybill_number"));
            progress.append('\n');
            listener.onProgressUpdated(progress.toString());
        }
        listener.onProgressUpdated("Issued " + packages.size() + " packages with WebApi :)");
    }

    public void safeWaybill(String pdfBase64, Package p, File input) throws Exception {
        String fileName;
        fileName = p.getServiceName();
        fileName += p.getId() + " - " + p.getReceiver() + ".pdf";

        File file = new File(input.getParent() + "\\" + fileName);
        FileOutputStream fop = new FileOutputStream(file);
        fop.write(new BASE64Decoder().decodeBuffer(pdfBase64));
        fop.flush();
        fop.close();
    }

    public JSONObject downloadWaybill(int orderId) throws Exception {
        return request("waybill/" + orderId + "/", "{}");
    }

    public JSONObject valuateOrder(Package p) throws Exception {
        JSONObject json = new JSONObject();
        json.put("order", Order.build(p));
        return request("order_valuation/", json.toString());
    }

    public JSONObject sendOrder(Package p) throws Exception {
        JSONObject json = new JSONObject();
        json.put("order", Order.build(p));
        return request("order_send/", json.toString());
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

        String responsePrint = responseString.substring(0, Math.min(responseString.length(), 2000));
        logger.info("Call: {}({}): {}{}", endpoint, data, responsePrint,
                responseString.length() != responsePrint.length() ? " <<< and many more characters... >>>" : "");

        JSONObject responseJson = new JSONObject(responseString);
        if (responseJson.getInt("status") != 200) {
            logger.warn("app_id:{}, signature:{}, expires:{}, request:{}, response:{}",
                    APP_ID, signature, expires, data, responseString);
            throw new RuntimeException(responseJson.getString("message"));
        }
        return responseJson.getJSONObject("response");
    }

    private String getExpires() {
        return String.valueOf((System.currentTimeMillis() + 600000)/1000);
    }

    private String stringToSign(String endpoint, String data, String expires) {
        return String.format("%s:%s:%s:%s", APP_ID, endpoint, data, expires);
    }

    private String getSignature(String data) throws Exception {
        String algorithm = "HmacSHA256";
        Mac sha256 = Mac.getInstance(algorithm);
        SecretKeySpec secret_key = new SecretKeySpec(APP_SECRET.getBytes(StandardCharsets.UTF_8), algorithm);
        sha256.init(secret_key);
        return Hex.encodeHexString(sha256.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }
}
