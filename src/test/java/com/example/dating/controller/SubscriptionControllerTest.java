package com.example.dating.controller;

import static com.example.dating.support.TestFixtures.userId;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.dating.model.subscription.SubscriptionPlan;
import com.example.dating.service.SubscriptionService;
import com.example.dating.service.UserSubscriptionService;
import com.example.dating.support.AbstractWebMvcSliceTest;

@WebMvcTest(controllers = SubscriptionController.class)
class SubscriptionControllerTest extends AbstractWebMvcSliceTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private SubscriptionService subscriptionService;

	@MockBean
	private UserSubscriptionService userSubscriptionService;

	@Test
	void plans_publicEndpoint_returnsCatalog() throws Exception {
		when(subscriptionService.getPlansCatalog()).thenReturn(Map.of("currency", "jpy", "billingCycle", "monthly"));
		mockMvc.perform(get("/subscription/plans"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.currency").value("jpy"));
	}

	@Test
	void current_returnsBillingView() throws Exception {
		Map<String, Object> view = Map.of("tier", "PLUS");
		when(subscriptionService.currentPlan(8L)).thenReturn(view);
		mockMvc.perform(get("/subscription/current").with(userId(8L)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tier").value("PLUS"));
	}

	@Test
	void webCheckoutSession_invokesService() throws Exception {
		Map<String, Object> session = Map.of("demoUpgradeAvailable", true);
		when(subscriptionService.createPaymentSession(2L, SubscriptionPlan.PLUS)).thenReturn(session);
		mockMvc.perform(post("/subscription/web/checkout-session").with(userId(2L))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"targetPlan\":\"PLUS\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.demoUpgradeAvailable").value(true));
		verify(subscriptionService).createPaymentSession(2L, SubscriptionPlan.PLUS);
	}
}
