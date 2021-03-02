package com.belamila.backend.webapi;

import com.belamila.model.Package;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by: Bartosz Nawrot
 * Date: 07.08.2020
 * Description:
 */
public class Order {

    public static JSONObject build(Package p) {
        JSONObject json = getOrderTemplate();
        json.put("service_id", getServiceId(p.getService()));
        json.put("content", "BELAMILA.PL (" + p.getId() + ")");

        JSONObject receiver = new JSONObject();
        receiver.put("country_code", "PL");
        receiver.put("name", p.getReceiver());
        receiver.put("city", p.getCity());
        receiver.put("contact_person", p.getReceiver().split(" ", 2)[0]);
        receiver.put("email", p.getEmail());
        receiver.put("phone", p.getPhone());

        if (p.getService().equals("INPOST")) {
            receiver.put("foreign_address_id", p.getInPostId());
        } else {
            receiver.put("line1", p.getAddress());
            receiver.put("postal_code", p.getZip());
        }
        json.getJSONObject("address").put("receiver", receiver);

        if (p.getAmount() > 0.0) {
            json.getJSONObject("cod").put("amount", p.getAmount() * 100.0);
        }

        return json;
    }

    private static int getServiceId(String service) {
        switch (service) {
            case "DPD Classic": return 21;
            case "INPOST": return 41;
            default:
                throw new RuntimeException("Unexpected service type string");
        }
    }

    private static JSONObject getOrderTemplate() {
        JSONObject template = new JSONObject();

        JSONObject address = new JSONObject();
        JSONObject sender = new JSONObject();
        sender.put("country_code", "PL");
        sender.put("name", "BELAMILA");
        sender.put("line1", "Wizjonerów 2/23");
        sender.put("postal_code", "31-356");
        sender.put("city", "Kraków");
        sender.put("contact_person", "Mila");
        sender.put("email", "sklep@belamila.pl");
        sender.put("phone", "+48889127157");
        sender.put("foreign_address_id", "KRA28N");
        address.put("sender", sender);
        template.put("address", address);

        JSONObject notification = new JSONObject();
        notification.put("new", getNotificationStructure(0, 0, 0));
        notification.put("sent", getNotificationStructure(1, 1, 0));
        notification.put("exception", getNotificationStructure(0, 0, 1));
        notification.put("delivered", getNotificationStructure(1, 1, 0));
        template.put("notification", notification);

        JSONObject pickup = new JSONObject();
        pickup.put("type", "SELF");
        template.put("pickup", pickup);

        JSONArray shipment = new JSONArray();
        JSONObject shipElement = new JSONObject();
        shipElement.put("dimension1", 20);
        shipElement.put("dimension2", 15);
        shipElement.put("dimension3", 5);
        shipElement.put("weight", 0.5);
        shipElement.put("shipment_type_code", "PACZKA");
        shipment.put(shipElement);
        template.put("shipment", shipment);

        JSONObject cod = new JSONObject();
        cod.put("bankaccount", "31105016181000002324507819");
        template.put("cod", cod);

        template.put("comment", "Say hello to your BELAMILA!");

        return template;
    }

    private static JSONObject getNotificationStructure(int receMail, int receSms, int sendMail) {
        JSONObject result = new JSONObject();
        result.put("isReceiverEmail", receMail);
        result.put("isReceiverSms", receSms);
        result.put("isSenderEmail", sendMail);
        return result;
    }
}
