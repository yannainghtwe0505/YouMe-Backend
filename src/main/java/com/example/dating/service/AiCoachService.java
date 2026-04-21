package com.example.dating.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;

import com.example.dating.config.AiProperties;
import com.example.dating.dto.ProfileTipView;
import com.example.dating.dto.ReplyIdeaView;
import com.example.dating.model.entity.ProfileEntity;
import com.example.dating.model.subscription.SubscriptionPlan;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AiCoachService {
	private static final Set<String> SUGGESTION_TYPES = Set.of(
			"emotional_support", "advice", "casual", "flirty", "question", "suggestion");

	private final ChatLlmClient llmClient;
	private final AiProperties aiProperties;
	private final ObjectMapper objectMapper;

	public AiCoachService(ChatLlmClient llmClient, AiProperties aiProperties, ObjectMapper objectMapper) {
		this.llmClient = llmClient;
		this.aiProperties = aiProperties;
		this.objectMapper = objectMapper;
	}

	private static String preamble(SubscriptionPlan plan) {
		return """
				You are YouMe AI, a dating assistant.
				USER PLAN: %s
				INSTRUCTIONS:
				- Adapt response quality to this plan (FREE = short/generic; PLUS = moderate personalization; GOLD = deeper behavioral insight).
				- Never claim to know private facts you were not given.
				- Encourage respectful, authentic connection.
				""".formatted(plan.name());
	}

	public record MatchGreetingOutcome(String text, boolean fromLlm) {
	}

	public MatchGreetingOutcome generateMatchGreetingOutcome(String nameA, String nameB, String bioA, String bioB,
			SubscriptionPlan plan) {
		String depth = switch (plan) {
			case FREE -> "One warm line only. Generic icebreaker; avoid deep personalization.";
			case PLUS -> "One or two short sentences; light nod to bios if present.";
			case GOLD -> "Two sentences max; subtly reflect compatible vibes from bios if present.";
		};
		int tokens = switch (plan) {
			case FREE -> 80;
			case PLUS -> 120;
			case GOLD -> 160;
		};
		double temp = switch (plan) {
			case FREE -> 0.55;
			case PLUS -> 0.75;
			case GOLD -> 0.88;
		};
		String sys = preamble(plan) + "\nTASK: Opening line for a new mutual match chat visible to both.\n" + depth
				+ "\nNo hashtags, no quotes, no 'As an AI'.";
		String user = "Names: " + nameA + " and " + nameB + ".\n"
				+ (trimOrEmpty(bioA).isEmpty() ? "" : "About " + nameA + ": " + trimOrEmpty(bioA) + "\n")
				+ (trimOrEmpty(bioB).isEmpty() ? "" : "About " + nameB + ": " + trimOrEmpty(bioB) + "\n")
				+ "Write ONE greeting line they both see (not from either person).";
		try {
			String ai = llmClient.chatCompletion(sys, user, tokens, temp);
			if (ai != null && !ai.isBlank())
				return new MatchGreetingOutcome(sanitizeOneLine(ai), true);
		} catch (Exception ignored) {
		}
		return new MatchGreetingOutcome(templateGreeting(nameA, nameB), false);
	}

	public String generateMatchGreeting(String nameA, String nameB, String bioA, String bioB, SubscriptionPlan plan) {
		return generateMatchGreetingOutcome(nameA, nameB, bioA, bioB, plan).text();
	}

	public List<ReplyIdeaView> replyIdeas(String peerName, List<String> lastMessagesFromPeer, String tone,
			SubscriptionPlan plan) {
		if (tone == null || tone.isBlank())
			tone = switch (plan) {
				case FREE -> "simple and friendly";
				case PLUS -> "warm and playful";
				case GOLD -> "calibrated to increase thoughtful replies";
			};
		String strategy = switch (plan) {
			case FREE -> "Short icebreaker-style lines only.";
			case PLUS -> "Context-aware using recent lines; optional light humor.";
			case GOLD ->
					"You may vary subtext for thoughtful replies; keep every line respectful and consent-aware.";
		};
		int tokens = switch (plan) {
			case FREE -> 220;
			case PLUS -> 380;
			case GOLD -> 560;
		};
		double temp = switch (plan) {
			case FREE -> 0.5;
			case PLUS -> 0.78;
			case GOLD -> 0.9;
		};
		int maxIdeas = switch (plan) {
			case FREE -> 2;
			case PLUS -> 3;
			case GOLD -> 3;
		};
		String jsonShape = """
				Return JSON only: {"ideas":[{"text":"max 18 words","type":"casual"}, ...]}.
				Include exactly %d items. Each type must be one of: emotional_support, advice, casual, flirty, question.
				Use flirty only when it fits a mutual, comfortable dating tone (never explicit or crude).
				Vary types across items. Tone hint: %s.""".formatted(maxIdeas, tone);
		String sys = preamble(plan) + "\nTASK: Short reply suggestions for a dating chat.\n" + strategy + "\n"
				+ jsonShape;
		StringBuilder ctx = new StringBuilder();
		ctx.append("Their name: ").append(peerName == null ? "Match" : peerName).append("\nRecent lines from them:\n");
		for (String line : lastMessagesFromPeer) {
			if (line != null && !line.isBlank())
				ctx.append("- ").append(line.trim()).append("\n");
		}
		try {
			String raw = llmClient.chatCompletion(sys, ctx.toString(), tokens, temp);
			if (raw != null) {
				var node = objectMapper.readTree(raw.replaceAll("^[^{]*", ""));
				var arr = node.path("ideas");
				if (arr.isArray()) {
					List<ReplyIdeaView> parsed = parseReplyIdeasArray(arr, maxIdeas);
					if (!parsed.isEmpty())
						return parsed;
				}
			}
		} catch (Exception ignored) {
		}
		return templateReplyIdeas(peerName, plan);
	}

	private List<ReplyIdeaView> parseReplyIdeasArray(JsonNode arr, int maxIdeas) {
		List<ReplyIdeaView> out = new ArrayList<>();
		for (JsonNode el : arr) {
			if (out.size() >= maxIdeas)
				break;
			if (el.isTextual()) {
				String t = sanitizeOneLine(el.asText());
				if (!t.isBlank())
					out.add(new ReplyIdeaView(t, "suggestion"));
			} else if (el.isObject()) {
				String text = sanitizeOneLine(el.path("text").asText(""));
				if (text.isBlank())
					continue;
				String typ = normalizeSuggestionType(el.path("type").asText(""));
				out.add(new ReplyIdeaView(text, typ));
			}
		}
		return out;
	}

	private static String normalizeSuggestionType(String raw) {
		if (raw == null || raw.isBlank())
			return "suggestion";
		String s = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
		if (!SUGGESTION_TYPES.contains(s))
			return "suggestion";
		return s;
	}

	private static List<ReplyIdeaView> templateReplyIdeas(String peerName, SubscriptionPlan plan) {
		String n = peerName == null || peerName.isBlank() ? "they" : peerName;
		List<ReplyIdeaView> all = List.of(
				new ReplyIdeaView("That made me smile - tell me more about that?", "question"),
				new ReplyIdeaView("I'm curious what you'd want to do on a lazy Sunday.", "casual"),
				new ReplyIdeaView("Sounds fun - have you always been into that, " + n + "?", "advice"));
		int max = plan == SubscriptionPlan.FREE ? 2 : 3;
		return all.subList(0, Math.min(max, all.size()));
	}

	public List<ProfileTipView> profileImprovementTips(String localeHint, ProfileEntity p, int photoCount,
			SubscriptionPlan plan) {
		String low = localeHint == null ? "" : localeHint.toLowerCase();
		boolean japanese = low.startsWith("ja");
		boolean burmese = low.startsWith("my");
		String lang = japanese
				? "Respond in Japanese. All title and detail strings must be in Japanese."
				: burmese
						? "Respond in Burmese (Myanmar). All title and detail strings must be in Burmese (Myanmar script)."
						: "Respond in English.";
		String depth = switch (plan) {
			case FREE -> "Give 2-3 concise tips only. Basic suggestions.";
			case PLUS -> "Give 4-5 tips; include interest-tag ideas and one note on photo variety.";
			case GOLD ->
				"Give 5-6 tips covering bio, interests, tone, and photo engagement; mention match improvement angle without inventing metrics.";
		};
		int maxTips = switch (plan) {
			case FREE -> 3;
			case PLUS -> 5;
			case GOLD -> 6;
		};
		int tokens = switch (plan) {
			case FREE -> 380;
			case PLUS -> 520;
			case GOLD -> 720;
		};
		double temp = switch (plan) {
			case FREE -> 0.55;
			case PLUS -> 0.75;
			case GOLD -> 0.85;
		};
		String sys = preamble(plan) + "\n" + lang + "\nTASK: Dating profile coach.\n" + depth
				+ "\nReturn JSON only: {\"tips\":[{\"title\":\"max 6 words\",\"detail\":\"max 2 short sentences\"}]}."
				+ " No markdown, no hashtags.";
		StringBuilder ctx = new StringBuilder();
		ctx.append("Photos uploaded: ").append(photoCount).append(".\n");
		ctx.append("Nickname: ").append(trimOrEmpty(p.getDisplayName())).append("\n");
		ctx.append("Gender: ").append(trimOrEmpty(p.getGender())).append("\n");
		ctx.append("City: ").append(trimOrEmpty(p.getCity())).append("\n");
		ctx.append("Bio: ").append(trimOrEmpty(p.getBio())).append("\n");
		ctx.append("Education: ").append(trimOrEmpty(p.getEducation())).append("\n");
		ctx.append("Work: ").append(trimOrEmpty(p.getOccupation())).append("\n");
		ctx.append("Hobbies line: ").append(trimOrEmpty(p.getHobbies())).append("\n");
		if (p.getInterests() != null && !p.getInterests().isEmpty())
			ctx.append("Interest tags: ").append(String.join(", ", p.getInterests())).append("\n");
		if (p.getLifestyle() != null && !p.getLifestyle().isEmpty())
			ctx.append("Lifestyle (self-reported keys): ").append(p.getLifestyle().keySet()).append("\n");
		try {
			String raw = llmClient.chatCompletion(sys, ctx.toString(), tokens, temp);
			if (raw != null) {
				var node = objectMapper.readTree(raw.replaceAll("^[^{]*", ""));
				var arr = node.path("tips");
				if (arr.isArray()) {
					List<ProfileTipView> out = new ArrayList<>();
					for (var el : arr) {
						String t = el.path("title").asText("");
						String d = el.path("detail").asText("");
						if (!t.isBlank() && !d.isBlank())
							out.add(new ProfileTipView(sanitizeOneLine(t), sanitizeOneLine(d)));
					}
					if (out.size() >= 1)
						return out.subList(0, Math.min(maxTips, out.size()));
				}
			}
		} catch (Exception ignored) {
		}
		return templateProfileTips(japanese, burmese);
	}

	/**
	 * Match insight deck — depth scales by plan (controller enforces MATCH_INSIGHT quota).
	 */
	public Map<String, Object> matchCompatibilityInsight(ProfileEntity viewer, ProfileEntity peer, SubscriptionPlan plan) {
		Map<String, Object> fallback = templateInsight(viewer, peer, plan);
		if (plan == SubscriptionPlan.FREE) {
			fallback.put("depth", "minimal");
			return fallback;
		}
		if (!aiProperties.isEnabled() || !aiProperties.isLlmConfigured())
			return fallback;
		String vName = trimOrEmpty(viewer.getDisplayName());
		String pName = trimOrEmpty(peer.getDisplayName());
		String task = plan == SubscriptionPlan.PLUS ? """
				Return JSON only: {"summary":"one line","bullets":["reason1","reason2","reason3"],"chatTip":"one short tip"}.
				Base reasons only on provided fields; no invented private facts."""
				: """
						Return JSON only: {"summary":"one line","bullets":["b1","b2","b3","b4"],"personalityFit":"short",
						"communicationFit":"short","interestOverlap":"short","predictedChatEase":"low|medium|high","reviveTip":"how to revive a quiet chat"}.
						Stay evidence-based from provided text only.""";
		String sys = preamble(plan) + "\nTASK: Explain why this match could work.\n" + task;
		StringBuilder user = new StringBuilder();
		user.append("Viewer nickname: ").append(vName).append("\nViewer bio: ").append(trimOrEmpty(viewer.getBio()))
				.append("\nViewer interests: ");
		user.append(viewer.getInterests() == null ? "" : String.join(", ", viewer.getInterests()));
		user.append("\nPeer nickname: ").append(pName).append("\nPeer bio: ").append(trimOrEmpty(peer.getBio()))
				.append("\nPeer interests: ");
		user.append(peer.getInterests() == null ? "" : String.join(", ", peer.getInterests()));
		int tokens = plan == SubscriptionPlan.GOLD ? 900 : 480;
		double temp = plan == SubscriptionPlan.GOLD ? 0.82 : 0.7;
		try {
			String raw = llmClient.chatCompletion(sys, user.toString(), tokens, temp);
			if (raw != null) {
				var node = objectMapper.readTree(raw.replaceAll("^[^{]*", ""));
				Map<String, Object> out = new HashMap<>();
				out.put("summary", node.path("summary").asText(fallback.get("summary").toString()));
				out.put("bullets", jsonStringList(node.path("bullets"), 6));
				out.put("chatTip", node.path("chatTip").asText(""));
				if (plan == SubscriptionPlan.GOLD) {
					out.put("personalityFit", node.path("personalityFit").asText(""));
					out.put("communicationFit", node.path("communicationFit").asText(""));
					out.put("interestOverlap", node.path("interestOverlap").asText(""));
					out.put("predictedChatEase", node.path("predictedChatEase").asText(""));
					out.put("reviveTip", node.path("reviveTip").asText(""));
				}
				out.put("plan", plan.name());
				out.put("source", "llm");
				return out;
			}
		} catch (Exception ignored) {
		}
		fallback.put("plan", plan.name());
		return fallback;
	}

	private static List<String> jsonStringList(com.fasterxml.jackson.databind.JsonNode arr, int max) {
		List<String> out = new ArrayList<>();
		if (!arr.isArray())
			return out;
		for (var el : arr) {
			if (el.isTextual() && !el.asText().isBlank()) {
				out.add(sanitizeOneLine(el.asText()));
				if (out.size() >= max)
					break;
			}
		}
		return out;
	}

	private static Map<String, Object> templateInsight(ProfileEntity viewer, ProfileEntity peer, SubscriptionPlan plan) {
		Map<String, Object> m = new HashMap<>();
		m.put("summary", "You both show up as open to meeting someone new — start with something light you both enjoy.");
		m.put("bullets", List.of(
				"Shared app context: you're both active here now.",
				"Try a specific question about a hobby or place they mention.",
				"Keep early messages short and easy to answer."));
		m.put("chatTip", "Reference one detail from their bio so it feels personal.");
		m.put("source", "template");
		m.put("plan", plan.name());
		if (plan == SubscriptionPlan.GOLD) {
			m.put("personalityFit", "Not enough structured personality data — stay curious and ask values-based questions.");
			m.put("communicationFit", "Match their message length and emoji level.");
			m.put("interestOverlap",
					viewer.getInterests() != null && peer.getInterests() != null
							? "Compare interests you both listed in-app."
							: "Ask what they're into lately.");
			m.put("predictedChatEase", "medium");
			m.put("reviveTip", "Send a low-pressure check-in with a new photo or plan suggestion.");
		}
		return m;
	}

	public AiMeta meta() {
		return new AiMeta(aiProperties.isLlmConfigured());
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

	private static List<ProfileTipView> templateProfileTips(boolean japanese, boolean burmese) {
		if (japanese) {
			return List.of(
					new ProfileTipView("写真を増やす", "自然な笑顔の顔アップが信頼感につながります。"),
					new ProfileTipView("自己紹介を具体化", "趣味や休日の過ごし方を一文ずつ具体的に書くと会話が始めやすいです。"),
					new ProfileTipView("希望を一言", "どんな関係を望むか短く書くとマッチの質が上がります。"),
					new ProfileTipView("ネガティブは避ける", "文句や条件の羅列より、前向きな表現に整えましょう。"));
		}
		if (burmese) {
			return List.of(
					new ProfileTipView("ဓာတ်ပုံပိုထည့်ပါ", "ရင်သပ်စရာ ပြုံးပုံရှိမှ လူတွေ ယုံကြည်လွယ်ပါသည်။"),
					new ProfileTipView("မိတ်ဆက်ကို ရှင်းပါ", "ဝါသနာနှင့် အားလပ်ရက်ကို တိုတောင်းစွာ ဖော်ပြပါ။"),
					new ProfileTipView("လိုချင်သည်ကို ပြောပါ", "ဆက်ဆံရေးမျိုး တိုတောင်းဖော်ပြပါ။"),
					new ProfileTipView("အပြုသဘောဆန်ပါ", "မကြိုက်သည်များထက် ကြိုက်သည်များကို ဦးစားပေးရေးသားပါ။"));
		}
		return List.of(
				new ProfileTipView("Add clear photos", "Several friendly face-forward shots help people feel comfortable reaching out."),
				new ProfileTipView("Make your bio concrete", "Swap vague lines for specific hobbies or weekend routines."),
				new ProfileTipView("Say what you want", "One line about the kind of connection you want sets expectations."),
				new ProfileTipView("Keep tone positive", "Lead with what you enjoy rather than a long list of dislikes."));
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
