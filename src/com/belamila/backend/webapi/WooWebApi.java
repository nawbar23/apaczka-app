package com.belamila.backend.webapi;

import com.belamila.model.Package;
import com.belamila.ui.ProgressListener;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

@Slf4j
public class WooWebApi {

    private static final Integer ORDERS_PER_PAGE = 50;

    private static final String STATUSES = "processing";

    private static final String API_URL = "https://www.belamila.pl/wp-json/wc/v3/orders"
            + "?consumer_key=" + WooKeys.CONSUMER_KEY
            + "&consumer_secret=" + WooKeys.CONSUMER_SECRET
            + "&status=" + STATUSES
            + "&per_page=" + ORDERS_PER_PAGE;

    private final OkHttpClient client = new OkHttpClient();

    private final ProgressListener listener;

    public WooWebApi(ProgressListener listener) {
        this.listener = listener;
    }


    public List<Package> fetchOrders() throws IOException {
        listener.onProgressUpdated("Pobieram zamówienia...\n");

        Request request = new Request.Builder()
                .url(API_URL)
                .get()
                .build();
        Response response = client.newCall(request).execute();
        String responseString = response.body().string();
        response.body().close();

        String responsePrint = responseString.substring(0, Math.min(responseString.length(), 2000));
        log.info("Call: {}{}", responsePrint,
                responseString.length() != responsePrint.length() ? " <<< and many more characters... >>>" : "");

        if (!response.isSuccessful()) {
            JSONObject responseJson = new JSONObject(responseString);
            throw new RuntimeException(responseJson.getString("message"));
        }

        JSONArray ordersJson = new JSONArray(responseString);
        listener.onProgressUpdated("Wczytałem " + ordersJson.length() + " zamówień\n");

        List<Package> result = new LinkedList<>();
        ordersJson.forEach(o -> {
            log.info("Order: {}", o);
        });

        return result;
    }
}
