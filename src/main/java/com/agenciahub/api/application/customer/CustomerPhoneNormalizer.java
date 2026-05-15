package com.agenciahub.api.application.customer;

final class CustomerPhoneNormalizer {

    private CustomerPhoneNormalizer() {}

    static String normalize(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.replaceAll("\\D", "");
    }
}
