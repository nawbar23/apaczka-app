package com.belamila.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

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

    private String id;
    private String service;

    private String receiver;
    private String address;
    private String zip;
    private String city;
    private String email;
    private String phone;
    private String inPostId;

    private Double amount;

    public String getServiceName() {
        if (getService().equals("INPOST")) {
            return  "InPost - ";
        } else {
            return  "DPD - ";
        }
    }
}
