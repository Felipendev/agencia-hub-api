package com.agenciahub.api.integration.http;

import com.agenciahub.api.support.AbstractIntegrationTest;
import com.agenciahub.api.support.IntegrationHttpSupport;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.agenciahub.api.support.IntegrationHttpSupport.unauthenticated;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LGPD-01: verifica portabilidade de dados (art. 18, V) via GET /profile/data-export.
 */
class DataExportIntegrationTest extends AbstractIntegrationTest {

    private static final String URL = IntegrationHttpSupport.API_PREFIX + "/profile/data-export";

    @Test
    void dataExport_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(unauthenticated());
    }

    @Test
    void dataExport_ownerAuth_returns200WithZip() throws Exception {
        byte[] body = mockMvc.perform(http.authorized(mockMvc, get(URL)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/zip"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"data-export.zip\""))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertThat(body).isNotEmpty();
        Set<String> entries = zipEntryNames(body);
        assertThat(entries).contains("account.json", "agency.json", "customers.json",
                "quotations.json", "financial_entries.json", "submissions.json", "consents.json");
    }

    @Test
    void dataExport_accountJson_doesNotContainPasswordHash() throws Exception {
        byte[] body = mockMvc.perform(http.authorized(mockMvc, get(URL)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        String accountJson = readZipEntry(body, "account.json");
        assertThat(accountJson).doesNotContain("passwordHash");
        assertThat(accountJson).doesNotContain("passwordChangedAt");
        assertThat(accountJson).doesNotContain("mustChangePassword");
    }

    @Test
    void dataExport_accountJson_containsExpectedFields() throws Exception {
        byte[] body = mockMvc.perform(http.authorized(mockMvc, get(URL)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        String accountJson = readZipEntry(body, "account.json");
        assertThat(accountJson).contains("\"id\"", "\"name\"", "\"email\"", "\"accountKind\"");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Set<String> zipEntryNames(byte[] zipBytes) throws Exception {
        Set<String> names = new HashSet<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                names.add(entry.getName());
                zis.closeEntry();
            }
        }
        return names;
    }

    private static String readZipEntry(byte[] zipBytes, String entryName) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entryName.equals(entry.getName())) {
                    return new String(zis.readAllBytes());
                }
                zis.closeEntry();
            }
        }
        throw new IllegalStateException("Entry not found: " + entryName);
    }
}
