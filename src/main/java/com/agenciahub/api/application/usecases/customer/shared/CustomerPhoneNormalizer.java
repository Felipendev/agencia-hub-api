package com.agenciahub.api.application.usecases.customer.shared;

public final class CustomerPhoneNormalizer {

    private CustomerPhoneNormalizer() {}

    public static String normalize(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.replaceAll("\\D", "");
    }
}
