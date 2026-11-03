package com.restauranthub;

import com.restauranthub.dto.DashboardMetricsDto;
import com.restauranthub.service.BranchService;
import com.restauranthub.service.ReportingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BranchService branchService;

    @MockBean
    private ReportingService reportingService;

    @Test
    @DisplayName("Security: Public branch search endpoint should be accessible without credentials")
    void testPublicEndpointAccessible() throws Exception {
        when(branchService.searchBranches(null)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/branches"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Security: Protected order placement endpoint should reject unauthenticated requests with 401")
    void testOrderPlacementRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderType\": \"TAKEAWAY\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "customer@example.com", authorities = {"ROLE_CUSTOMER"})
    @DisplayName("Security: Customers must NOT access admin reporting endpoint (HTTP 403 Forbidden)")
    void testCustomerCannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@restauranthub.com", authorities = {"ROLE_ADMIN"})
    @DisplayName("Security: System Admin should successfully access admin reporting dashboard")
    void testAdminCanAccessAdminEndpoint() throws Exception {
        DashboardMetricsDto dummy = DashboardMetricsDto.builder()
                .totalReservations(10)
                .totalOrders(25)
                .totalRevenueRecognized(new BigDecimal("1500.00"))
                .lowStockAlerts(Collections.emptyList())
                .build();
        when(reportingService.getDashboardMetrics()).thenReturn(dummy);

        mockMvc.perform(get("/api/v1/admin/reports/dashboard"))
                .andExpect(status().isOk());
    }
}
