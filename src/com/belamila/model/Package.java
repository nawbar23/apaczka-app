package com.belamila.model;

import javafx.beans.property.StringProperty;
import lombok.*;

import java.util.List;

/**
 * Created by: Bartosz Nawrot
 * Date: 18.05.2020
 * Description:
 */
@Builder
@Getter
@Setter
@ToString
public class Package {

    public enum InpostStatus {
        UNKNOWN, DONE_VALID, DONE_INVALID
    }

    private String id;
    private String service;

    private String receiver;
    private String address;
    private String zip;
    private String city;
    private String email;
    private String phone;
    private String inPostId;
    private StringProperty inpostStatus;

    private Boolean isCod;
    private Double amount;

    private String shippingLabel;
    private List<Item> items;

    @Data
    @Builder
    public static class Item {

        private String label;
        private Integer quantity;
        private Double total;

        private Boolean isBagRequested;
    }

    public boolean isInPost() {
        return !service.equals("DPD Classic");
    }

    public String getServiceName() {
        if (getService().equals("INPOST")) {
            return  "InPost - ";
        } else {
            return  "DPD - ";
        }
    }
}
