package com.example.dating.util;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Tokyo-only launch: 23 special wards + major cities (shi). Expand via DB later.
 */
public final class TokyoMunicipalities {
	private static final Set<String> NAMES = new LinkedHashSet<>(List.of(
			"Chiyoda-ku", "Chuo-ku", "Minato-ku", "Shinjuku-ku", "Bunkyo-ku", "Taito-ku", "Sumida-ku",
			"Koto-ku", "Shinagawa-ku", "Meguro-ku", "Ota-ku", "Setagaya-ku", "Shibuya-ku", "Nakano-ku",
			"Suginami-ku", "Toshima-ku", "Kita-ku", "Arakawa-ku", "Itabashi-ku", "Nerima-ku", "Adachi-ku",
			"Katsushika-ku", "Edogawa-ku",
			"Hachioji-shi", "Tachikawa-shi", "Musashino-shi", "Mitaka-shi", "Ome-shi", "Fuchu-shi",
			"Akishima-shi", "Chofu-shi", "Machida-shi", "Koganei-shi", "Kodaira-shi", "Hino-shi",
			"Higashimurayama-shi", "Kokubunji-shi", "Kunitachi-shi", "Fussa-shi", "Komae-shi", "Higashiyamato-shi",
			"Kiyose-shi", "Higashikurume-shi", "Musashimurayama-shi", "Tama-shi", "Inagi-shi",
			"Hamura-shi", "Akiruno-shi", "Hinode-machi", "Hinohara-mura", "Okutama-machi",
			"Nishitokyo-shi"));

	private TokyoMunicipalities() {
	}

	public static Set<String> all() {
		return Collections.unmodifiableSet(NAMES);
	}

	public static boolean isAllowed(String city) {
		if (city == null)
			return false;
		return NAMES.contains(city.trim());
	}
}
