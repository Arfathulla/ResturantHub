package com.restauranthub.integration.zomato;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.zomato")
public class ZomatoProperties {

    /**
     * Whether Zomato partner integration is active.
     */
    private boolean enabled = true;

    /**
     * Zomato Partner Merchant API Key passed in headers (x-zomato-api-key / user-key).
     */
    private String apiKey = "zom_partner_live_8f3a9e2d1c7b5a04e6";

    /**
     * Zomato Partner Restaurant ID registered with Zomato Merchant Portal.
     */
    private String restaurantId = "zom_res_50892";

    /**
     * Zomato Partner API Base URL.
     */
    private String apiUrl = "https://api.zomato.com/v1/partner";

    /**
     * Automatically accept incoming/synced orders.
     */
    private boolean autoAccept = true;

    /**
     * If true or when external endpoint is unreachable, runs high-fidelity partner simulation.
     */
    private boolean simulateNetworkSync = true;

    /**
     * Helper to display a securely masked version of the Zomato API key in logs and UI.
     */
    public String getMaskedApiKey() {
        if (apiKey == null || apiKey.length() < 10) {
            return "zom_***";
        }
        return apiKey.substring(0, 12) + "..." + apiKey.substring(apiKey.length() - 4);
    }
}
