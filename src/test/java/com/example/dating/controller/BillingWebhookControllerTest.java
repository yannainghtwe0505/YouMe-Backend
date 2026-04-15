package com.example.dating.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.dating.service.SubscriptionService;
import com.example.dating.support.AbstractWebMvcSliceTest;

@WebMvcTest(controllers = BillingWebhookController.class)
class BillingWebhookControllerTest extends AbstractWebMvcSliceTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private SubscriptionService subscriptionService;

	@Test
	void stripeWebhook_returnsServiceBody() throws Exception {
		when(subscriptionService.handleStripeWebhook("{}", "sig")).thenReturn("ok");
		mockMvc.perform(post("/webhooks/stripe").contentType(MediaType.APPLICATION_JSON).content("{}")
				.header("Stripe-Signature", "sig"))
				.andExpect(status().isOk())
				.andExpect(content().string("ok"));
	}

	@Test
	void appleWebhook_returnsAccepted() throws Exception {
		mockMvc.perform(post("/webhooks/apple").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("accepted"));
	}
}
