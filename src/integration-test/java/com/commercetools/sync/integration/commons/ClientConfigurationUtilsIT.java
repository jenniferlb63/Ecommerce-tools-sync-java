package com.commercetools.sync.integration.commons;

import com.commercetools.sync.commons.utils.ClientConfigurationUtils;
import com.commercetools.sync.integration.commons.utils.SphereClientUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClientConfigurationUtilsIT {

    @Test
    void createClient_WithSameConfig_ReturnsSameClient() {
        assertThat(ClientConfigurationUtils.createClient(SphereClientUtils.CTP_SOURCE_CLIENT_CONFIG))
            .isEqualTo(ClientConfigurationUtils.createClient(SphereClientUtils.CTP_SOURCE_CLIENT_CONFIG));
    }

    @Test
    void createClient_WithDifferentConfig_ReturnDifferentClient() {
        assertThat(ClientConfigurationUtils.createClient(SphereClientUtils.CTP_SOURCE_CLIENT_CONFIG))
            .isNotEqualTo(ClientConfigurationUtils.createClient(SphereClientUtils.CTP_TARGET_CLIENT_CONFIG));
    }
}
