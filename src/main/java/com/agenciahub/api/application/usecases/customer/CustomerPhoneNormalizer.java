package com.agenciahub.api.application.usecases.customer;

public final class CustomerPhoneNormalizer {

    private CustomerPhoneNormalizer() {}

    public static String normalize(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.replaceAll("\\D", "");
    }
}
