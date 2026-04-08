package com.example.dating.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;

import com.example.dating.config.AiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AiCoachService {
	private final OpenAiClient openAi;
	private final AiProperties aiProperties;
	private final ObjectMapper objectMapper;

	public AiCoachService(OpenAiClient openAi, AiProperties aiProperties, ObjectMapper objectMapper) {
		this.openAi = openAi;
		this.aiProperties = aiProperties;
		this.objectMapper = objectMapper;
	}

	public String generateMatchGreeting(String nameA, String nameB, String bioA, String bioB) {
		String sys = """
				You write one short, warm first message for a dating app chat that just opened between two people who matched.
				No hashtags, no quotes, no "As an AI". Max 2 sentences. Fun and easy to reply to.""";
		String user = "Names: " + nameA + " and " + nameB + ".\n"
				+ (trimOrEmpty(bioA).isEmpty() ? "" : "About " + nameA + ": " + trimOrEmpty(bioA) + "\n")
				+ (trimOrEmpty(bioB).isEmpty() ? "" : "About " + nameB + ": " + trimOrEmpty(bioB) + "\n")
				+ "Write ONE greeting line they both see (not from either person).";
		try {
			String ai = openAi.chatCompletion(sys, user);
			if (ai != null && !ai.isBlank())
				return sanitizeOneLine(ai);
		} catch (Exception ignored) {
		}
		return templateGreeting(nameA, nameB);
	}

	public List<String> replyIdeas(String peerName, List<String> lastMessagesFromPeer, String tone) {
		if (tone == null || tone.isBlank())
			tone = "warm and playful";
		String sys = "You help with short reply ideas for a dating chat. Return JSON only: {\"ideas\":[\"idea1\",\"idea2\",\"idea3\"]}. "
				+ "Each idea max 18 words. Tone: " + tone + ".";
		StringBuilder ctx = new StringBuilder();
		ctx.append("Their name: ").append(peerName == null ? "Match" : peerName).append("\nRecent lines from them:\n");
		for (String line : lastMessagesFromPeer) {
			if (line != null && !line.isBlank())
				ctx.append("- ").append(line.trim()).append("\n");
		}
		try {
			String raw = openAi.chatCompletion(sys, ctx.toString());
			if (raw != null) {
				var node = objectMapper.readTree(raw.replaceAll("^[^{]*", ""));
				var arr = node.path("ideas");
				if (arr.isArray()) {
					List<String> out = new ArrayList<>();
					for (var el : arr)
						if (el.isTextual() && !el.asText().isBlank())
							out.add(sanitizeOneLine(el.asText()));
					if (out.size() >= 2)
						return out.subList(0, Math.min(3, out.size()));
				}
			}
		} catch (Exception ignored) {
		}
		return templateReplies(peerName);
	}

	public AiMeta meta() {
		return new AiMeta(aiProperties.isEnabled() && aiProperties.hasApiKey());
	}

	public record AiMeta(boolean llmConfigured) {
	}

	private static String templateGreeting(String nameA, String nameB) {
		String a = nameA == null || nameA.isBlank() ? "You two" : nameA;
		String b = nameB == null || nameB.isBlank() ? "both" : nameB;
		String[] lines = {
				"You matched - hi " + a + " and " + b + "! What is each of you most excited about this week?",
				"New match energy. " + a + " and " + b + ": if you could grab coffee anywhere tomorrow, where?",
				"Hey " + a + " and " + b + "! One fun fact each - go.",
		};
		return lines[ThreadLocalRandom.current().nextInt(lines.length)];
	}

	private static List<String> templateReplies(String peerName) {
		String n = peerName == null || peerName.isBlank() ? "they" : peerName;
		return List.of(
				"That made me smile - tell me more about that?",
				"I'm curious what you'd want to do on a lazy Sunday.",
				"Sounds fun - have you always been into that, " + n + "?");
	}

	private static String trimOrEmpty(String s) {
		return s == null ? "" : s.trim();
	}

	private static String sanitizeOneLine(String s) {
		String t = s.replace('"', ' ').replace('\n', ' ').trim();
		if (t.length() > 500)
			t = t.substring(0, 500);
		return t;
	}
}
