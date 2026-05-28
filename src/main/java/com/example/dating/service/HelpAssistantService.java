package com.example.dating.service;

import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.example.dating.config.AiProperties;
import com.example.dating.model.entity.ProfileEntity;
import com.example.dating.model.entity.UserEntity;
import com.example.dating.model.subscription.SubscriptionPlan;
import com.example.dating.repository.ProfileRepo;
import com.example.dating.repository.UserRepo;
import com.example.dating.service.SubscriptionPlanService;

@Service
public class HelpAssistantService {
	private static final Set<String> ALLOWED_CONTEXTS = Set.of(
			"like", "super_like", "matching", "safety", "privacy", "subscription", "discover", "general");

	private final OpenAiClient openAi;
	private final AiProperties aiProperties;
	private final UserRepo users;
	private final ProfileRepo profiles;
	private final SubscriptionPlanService subscriptionPlanService;

	public HelpAssistantService(OpenAiClient openAi, AiProperties aiProperties, UserRepo users,
			ProfileRepo profiles, SubscriptionPlanService subscriptionPlanService) {
		this.openAi = openAi;
		this.aiProperties = aiProperties;
		this.users = users;
		this.profiles = profiles;
		this.subscriptionPlanService = subscriptionPlanService;
	}

	public record HelpAnswer(String answer, boolean fromLlm) {
	}

	public HelpAnswer ask(long userId, String question, String contextKey, String localeCode) {
		String q = question == null ? "" : question.trim();
		if (q.isBlank()) {
			return new HelpAnswer(fallback(localeCode, contextKey), false);
		}
		if (q.length() > 500) {
			q = q.substring(0, 500);
		}
		String ctx = normalizeContext(contextKey);
		String locale = normalizeLocale(localeCode);
		UserEntity user = users.findById(userId).orElse(null);
		ProfileEntity profile = profiles.findById(userId).orElse(null);
		SubscriptionPlan plan = profile == null ? SubscriptionPlan.FREE
				: subscriptionPlanService.resolve(profile);

		if (!aiProperties.isEnabled() || !aiProperties.hasApiKey()) {
			return new HelpAnswer(fallback(locale, ctx), false);
		}

		String langInstruction = switch (locale) {
			case "ja" -> "Reply in natural Japanese (です・ます). Be warm and clear for dating-app beginners.";
			case "my" -> "Reply in Burmese if possible; otherwise clear English.";
			default -> "Reply in clear, friendly English for first-time dating app users.";
		};

		String sys = """
				You are YouMe Help, an in-app guide for a dating app (like Tinder/Bumble for Japan-focused users).
				%s
				CONTEXT: %s
				PLAN: %s
				Rules:
				- Max 4 short sentences. Use simple words.
				- Explain Like vs Super Like when relevant: Like = interest; Super Like = stronger signal, stands out.
				- Never invent premium prices or legal claims.
				- Encourage safety: meet in public, report/block if uncomfortable.
				- No hashtags, no "As an AI".
				""".formatted(langInstruction, ctx, plan.name());

		String userMsg = "User question: " + q;
		try {
			String ai = openAi.chatCompletion(sys, userMsg, 220, 0.4);
			if (ai != null && !ai.isBlank()) {
				return new HelpAnswer(ai.trim(), true);
			}
		} catch (Exception ignored) {
			/* template fallback */
		}
		return new HelpAnswer(fallback(locale, ctx), false);
	}

	private static String normalizeContext(String raw) {
		if (raw == null || raw.isBlank()) {
			return "general";
		}
		String k = raw.trim().toLowerCase().replace('-', '_');
		return ALLOWED_CONTEXTS.contains(k) ? k : "general";
	}

	private static String normalizeLocale(String raw) {
		if (raw == null || raw.isBlank()) {
			return "en";
		}
		String lc = raw.trim().toLowerCase(Locale.ROOT);
		if (lc.startsWith("ja")) {
			return "ja";
		}
		if (lc.startsWith("my")) {
			return "my";
		}
		return "en";
	}

	private static String fallback(String locale, String ctx) {
		boolean ja = "ja".equals(locale);
		return switch (ctx) {
			case "super_like" -> ja
					? "スーパーライクは、通常の「いいね」より強い好意のサインです。相手の画面で目立ちやすくなります。気になる相手にだけ使うのがおすすめです。"
					: "Super Like is a stronger signal than a regular Like — it helps you stand out to someone you’re especially interested in.";
			case "like" -> ja
					? "「いいね」は相手に好意を伝えるボタンです。相手もあなたに「いいね」するとマッチしてメッセージが送れます。"
					: "Like shows interest. When you both Like each other, it’s a match and you can message.";
			case "matching" -> ja
					? "お互いに「いいね」するとマッチ成立です。マッチ後はメッセージ画面で会話を始められます。"
					: "A match happens when you both Like each other. Then you can chat in Messages.";
			case "safety" -> ja
					? "不快なことがあればチャットのメニューから通報・ブロックできます。初対面は人の多い場所で会いましょう。"
					: "Use Report or Block from the chat menu if someone makes you uncomfortable. Meet in public for first dates.";
			default -> ja
					? "ヘルプセンターによくある質問があります。Discover では✕でスキップ、♥でいいね、★でスーパーライクです。"
					: "Browse the Help Center for FAQs. On Discover: ✕ pass, ♥ Like, ★ Super Like.";
		};
	}
}
