package com.agenciahub.api.validation;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class PhoneValidatorTest {
 @Test void internationalNumbersKeepCountryCode(){for(String phone:new String[]{"+351912345678","+12025550123","+447911123456"}){assertThat(PhoneValidator.isValid(phone)).isTrue();assertThat(PhoneValidator.formatForStorage(phone)).isEqualTo(phone);}assertThat(PhoneValidator.formatForStorage("00351 912345678")).isEqualTo("+351912345678");}
 @Test void legacyBrazilAndInvalidInput(){assertThat(PhoneValidator.formatForStorage("(11) 98765-4321")).isEqualTo("+5511987654321");assertThat(PhoneValidator.isValid("+123")).isFalse();assertThat(PhoneValidator.isValid("telefone1234567890")).isFalse();}
}
