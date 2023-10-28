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
import java.util.Objects;

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
        listener.onProgressUpdated("Pobieram zamówienia...");

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
        listener.onProgressUpdated(" wyszło tego " + ordersJson.length() + " sztuk\n");

        List<Package> packages = new LinkedList<>();
        for (Object po : ordersJson) {
            JSONObject p = (JSONObject) po;
            log.info("Order: {}", p);
            String service = parseService(p);
            if (Objects.isNull(service)) {
                continue;
            }
            Package pack = Package.builder()
                    .id(String.valueOf(p.getInt("id")))
                    .service(service)
                    .build();
            parseAddress(pack, p);
            parseAmount(pack, p);
            if (pack.isInPost()) {
                parseInpostId(pack, p);
            }
            packages.add(pack);
        };

        listener.onProgressUpdated("Ale tylko " + packages.size() + " do wysyłki\n");
        return packages;
    }

    private String parseService(JSONObject pack) throws RuntimeException {
        JSONObject shippingLines = pack
                .getJSONArray("shipping_lines")
                .getJSONObject(0);
        String deliveryMethod = shippingLines.getString("method_title");
        if (deliveryMethod.contains("Kurier DPD")
                || deliveryMethod.contains("Darmowa dostawa: Kurier DPD")
                || deliveryMethod.contains("Kurier DPD pobranie")) {
            return "DPD Classic";
        } else if (deliveryMethod.contains("InPost Paczkomat")
                || deliveryMethod.contains("Darmowa dostawa: InPost Paczkomat")) {
            return "INPOST";
        } else {
            log.warn("Unrecognized delivery method: {}", deliveryMethod);
            return null;
        }
    }

    private void parseAddress(Package pack, JSONObject p) {
        JSONObject shipping = p.getJSONObject("shipping");
        JSONObject billing = p.getJSONObject("billing");
        pack.setReceiver(shipping.getString("first_name") + " " + shipping.getString("last_name"));
        pack.setAddress(shipping.getString("address_1"));
        pack.setCity(shipping.getString("city"));
        pack.setZip(shipping.getString("postcode"));
        String shippingEmail = shipping.has("email") ? shipping.getString("email") : null;
        if (Objects.nonNull(shippingEmail) && shippingEmail.length() > 0) {
            pack.setEmail(shippingEmail);
        } else {
            pack.setEmail(billing.getString("email"));
        }
        String shippingPhone = shipping.has("phone") ? shipping.getString("phone") : null;
        if (Objects.nonNull(shippingPhone) && shippingPhone.length() > 0) {
            pack.setPhone(shippingPhone);
        } else {
            pack.setPhone(billing.getString("phone"));
        }
    }

    private void parseAmount(Package pack, JSONObject p) {
        JSONObject shippingLines = p
                .getJSONArray("shipping_lines")
                .getJSONObject(0);
        String deliveryMethod = shippingLines.getString("method_title");
        String value = p.getString("total");
        double amount = -1.0;
        if (deliveryMethod.contains("Kurier DPD pobranie")) {
            try {
                amount = Double.parseDouble(value);
            } catch (NumberFormatException ignored) { }
            if (amount < 0.0) {
                log.warn("Wrong value: {} for delivery method: {}", value, deliveryMethod);
                throw new RuntimeException("Brak kwoty do paczki pobraniowej!");
            }
        }
        pack.setAmount(amount);
    }

    private void parseInpostId(Package pack, JSONObject p) {
        JSONArray metaList = p.getJSONArray("meta_data");
        for (Object metaO : metaList) {
            JSONObject meta = (JSONObject) metaO;
            if (meta.getString("key").equals("apaczka_delivery_point")) {
                pack.setInPostId(meta.getJSONObject("value").getString("apm_access_point_id"));
            }
        }
        log.warn("Could not find InpostId meta data");
    }
}
