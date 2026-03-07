package com.babjo.deliverycommerce;

import com.babjo.deliverycommerce.domain.product.service.AiDescriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DeliverycommerceApplicationTests {

    @MockBean
    private AiDescriptionService aiDescriptionService;

    @Test
    void contextLoads() {
    }

}
