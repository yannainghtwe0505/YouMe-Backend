package com.example.dating.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Helpers for discovery_settings / lifestyle JSON maps on profiles. */
public final class DiscoveryJson {
	private DiscoveryJson() {
	}

	public static Map<String, Object> mapOrEmpty(Map<String, Object> m) {
		return m != null ? m : Map.of();
	}

	public static Map<String, Object> nestedMap(Map<String, Object> parent, String key) {
		Object o = parent.get(key);
		if (o instanceof Map<?, ?> raw) {
			Map<String, Object> out = new HashMap<>();
			for (var e : raw.entrySet())
				out.put(String.valueOf(e.getKey()), e.getValue());
			return out;
		}
		return Map.of();
	}

	public static boolean truthy(Object o) {
		if (o instanceof Boolean b)
			return b;
		if (o instanceof Number n)
			return n.intValue() != 0;
		if (o instanceof String s)
			return "true".equalsIgnoreCase(s) || "1".equals(s);
		return false;
	}

	public static int intValue(Object o, int defaultVal) {
		if (o instanceof Number n)
			return n.intValue();
		if (o instanceof String s) {
			try {
				return Integer.parseInt(s.trim());
			} catch (@SuppressWarnings("unused") NumberFormatException e) {
				return defaultVal;
			}
		}
		return defaultVal;
	}

	public static String str(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}

	@SuppressWarnings("unchecked")
	public static List<String> stringList(Object o) {
		if (o == null)
			return List.of();
		if (o instanceof List<?> list) {
			List<String> out = new ArrayList<>();
			for (Object x : list)
				out.add(String.valueOf(x).trim());
			return out.stream().filter(s -> !s.isEmpty()).toList();
		}
		if (o instanceof Collection<?> col) {
			return col.stream().map(String::valueOf).map(String::trim).filter(s -> !s.isEmpty()).toList();
		}
		String s = String.valueOf(o).trim();
		return s.isEmpty() ? List.of() : List.of(s);
	}

	/** Normalize gender for interested-in matching. */
	public static String normGender(String gender) {
		if (gender == null)
			return "";
		String s = gender.trim().toLowerCase(Locale.ROOT);
		if (s.isEmpty())
			return "";
		if (s.equals("m") || s.equals("man") || s.equals("men") || s.equals("male"))
			return "men";
		if (s.equals("f") || s.equals("woman") || s.equals("women") || s.equals("female"))
			return "women";
		if (s.contains("non") || s.contains("nb") || "beyond binary".equals(s))
			return "nonbinary";
		return s;
	}

	public static boolean gendersOverlap(List<String> interestedInNorm, String candidateNorm) {
		if (interestedInNorm == null || interestedInNorm.isEmpty())
			return true;
		if (candidateNorm.isEmpty())
			return false;
		return interestedInNorm.stream().anyMatch(g -> Objects.equals(g, candidateNorm));
	}

	public static boolean langsOverlap(List<String> want, List<String> have) {
		if (want == null || want.isEmpty())
			return true;
		if (have == null || have.isEmpty())
			return false;
		for (String w : want) {
			for (String h : have) {
				if (w.equalsIgnoreCase(h))
					return true;
			}
		}
		return false;
	}

	public static boolean stringPrefMatch(String viewerWants, String candidateHas) {
		if (viewerWants == null || viewerWants.isEmpty())
			return true;
		if (candidateHas == null || candidateHas.isEmpty())
			return false;
		return viewerWants.equalsIgnoreCase(candidateHas);
	}

	public static boolean interestContains(List<String> candidateInterests, String mustContain) {
		if (mustContain == null || mustContain.isEmpty())
			return true;
		if (candidateInterests == null || candidateInterests.isEmpty())
			return false;
		String m = mustContain.trim().toLowerCase(Locale.ROOT);
		return candidateInterests.stream()
				.filter(Objects::nonNull)
				.anyMatch(t -> t.trim().toLowerCase(Locale.ROOT).equals(m));
	}
}
