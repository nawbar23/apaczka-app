package com.belamila.backend.webapi;

import com.belamila.model.Package;
import org.json.JSONObject;

/**
 * Created by: Bartosz Nawrot
 * Date: 07.08.2020
 * Description:
 */
public class Order {

    private static final JSONObject ORDER_TEMPLATE = getOrderTemplate();

    public static JSONObject build(Package p) {
        JSONObject json = new JSONObject(ORDER_TEMPLATE, JSONObject.getNames(ORDER_TEMPLATE));
        json.put("service_id", getServiceId(p.getService()));

        JSONObject receiver = new JSONObject();
        receiver.put("country_code", "PL");
        receiver.put("name", p.getReceiver());
        receiver.put("city", p.getCity());
        receiver.put("contact_person", p.getReceiver().split(" ", 2)[0]);
        receiver.put("email", p.getEmail());
        receiver.put("phone", p.getPhone());
        json.getJSONObject("address").put("receiver", receiver);

        if (p.getService().equals("INPOST")) {
            receiver.put("line1", p.getInpostId());
        } else {
            receiver.put("line1", p.getAddress());
            receiver.put("postal_code", p.getZip());
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
        sender.put("name", "F. H. U. OKNA Jerzy Kosim");
        sender.put("line1", "Wizjonerów 2/23");
        sender.put("postal_code", "31-356");
        sender.put("city", "Kraków");
        sender.put("contact_person", "BELAMILA");
        sender.put("email", "sklep@belamila.pl");
        sender.put("phone", "+48889127157");
        address.put("sender", sender);
        template.put("address", address);

        JSONObject notification = new JSONObject();
        notification.put("new", getNotificationStructure(0, 0, 0));
        notification.put("sent", getNotificationStructure(1, 1, 1));
        notification.put("exception", getNotificationStructure(0, 0, 1));
        notification.put("delivered", getNotificationStructure(1, 1, 0));
        template.put("notification", notification);

        JSONObject pickup = new JSONObject();
        pickup.put("type", "SELF");
        template.put("pickup", pickup);

        JSONObject shipment = new JSONObject();
        shipment.put("dimension1", 20);
        shipment.put("dimension2", 15);
        shipment.put("dimension3", 5);
        shipment.put("weight", 0.5);
        shipment.put("shipment_type_code", "PACZKA");
        template.put("shipment", shipment);

        template.put("comment", "Say hello to your new BELAMILA!");
        template.put("content", "BELAMILA.PL");

        JSONObject option = new JSONObject();
        //template.put("option", option);

        JSONObject cod = new JSONObject();
        //template.put("cod", cod);

        //template.put("shipment_value", 0);

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
