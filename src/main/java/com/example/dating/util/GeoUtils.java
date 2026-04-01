package com.example.dating.util;

public final class GeoUtils {
	private GeoUtils() {
	}

	private static final double EARTH_KM = 6371.0;

	/** Great-circle distance in kilometers (WGS84 sphere approximation). */
	public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
		double r1 = Math.toRadians(lat1);
		double r2 = Math.toRadians(lat2);
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(r1) * Math.cos(r2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return EARTH_KM * c;
	}
}
