package in.evpulse.stations;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StationController.class)
class StationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StationRepository repository;

    @MockitoBean
    private OpenChargeMapService openChargeMapService;

    @Test
    void defaultsToHyderabadAndReturnsSearchMetadata() throws Exception {
        when(repository.search(eq("Hyderabad"), isNull(), isNull(), isNull(), isNull(), eq(20), eq(0)))
                .thenReturn(List.of());
        when(repository.count(eq("Hyderabad"), isNull(), isNull(), isNull(), isNull())).thenReturn(0L);
        when(openChargeMapService.discoverHyderabad(isNull(), isNull(), isNull(), isNull(), eq(20)))
                .thenReturn(List.of());
        when(openChargeMapService.isConfigured()).thenReturn(false);

        mockMvc.perform(get("/api/v1/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Hyderabad"))
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.limit").value(20))
                .andExpect(jsonPath("$.offers").isArray())
                .andExpect(jsonPath("$.dataQualityNote").value(org.hamcrest.Matchers.containsString("OPENCHARGEMAP_API_KEY")));
    }

    @Test
    void passesSearchFiltersToRepository() throws Exception {
        when(repository.search(eq("Hyderabad"), eq("Gachibowli"), eq("CCS2"),
                eq(new BigDecimal("50")), eq(new BigDecimal("20")), eq(10), eq(5)))
                .thenReturn(List.of());
        when(repository.count(eq("Hyderabad"), eq("Gachibowli"), eq("CCS2"),
                eq(new BigDecimal("50")), eq(new BigDecimal("20")))).thenReturn(0L);

        mockMvc.perform(get("/api/v1/stations")
                        .param("q", "Gachibowli")
                        .param("connectorType", "CCS2")
                        .param("minPowerKw", "50")
                        .param("maxPricePerKwh", "20")
                        .param("limit", "10")
                        .param("offset", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Hyderabad"))
                .andExpect(jsonPath("$.limit").value(10))
                .andExpect(jsonPath("$.offset").value(5));
    }
}
