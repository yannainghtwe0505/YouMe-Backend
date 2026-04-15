package com.example.dating.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RegistrationProfilePatch {
	public String onboardingStep;
	/** Optional account email during phone-based onboarding (normalized server-side). */
	public String email;
	public String gender;
	public String birthday;
	public String city;
	public String displayName;
	public List<String> interests;
	public String referralSource;
	public Map<String, Object> lifestyle;
	public Boolean acceptTos;
	public Boolean acceptPrivacy;
	public Boolean acceptCommunity;
}
