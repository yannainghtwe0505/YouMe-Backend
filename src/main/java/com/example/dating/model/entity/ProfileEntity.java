package com.example.dating.model.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "profiles")
public class ProfileEntity {
	@Id
	private Long userId;
	@Column(length = 100)
	private String displayName;
	@Column(length = 2000)
	private String bio;
	@Column(length = 20)
	private String gender;
	private LocalDate birthday;
    @JdbcTypeCode(SqlTypes.JSON)           //  tell Hibernate this is JSON
    @Column(columnDefinition = "jsonb")    //  keep jsonb in Postgres
    private List<String> interests;        //  use a real JSON-friendly type
	private Double latitude;
	private Double longitude;
	private Integer minAge;
	private Integer maxAge;
	private Integer distanceKm;
	@Column(length = 255)
	private String city;
	@Column(length = 200)
	private String education;
	@Column(length = 200)
	private String occupation;
	@Column(length = 500)
	private String hobbies;
	@Column(name = "photo_url", length = 1024)
	private String photoUrl;
	@Column(name = "is_premium", nullable = false)
	@JsonProperty("isPremium")
	private boolean premium = false;

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getBio() {
		return bio;
	}

	public void setBio(String bio) {
		this.bio = bio;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public LocalDate getBirthday() {
		return birthday;
	}

	public void setBirthday(LocalDate birthday) {
		this.birthday = birthday;
	}

	public List<String> getInterests() {
	    return interests;
	}

	public void setInterests(List<String> interests) {
	    this.interests = interests;
	}
	
	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public Integer getMinAge() {
		return minAge;
	}

	public void setMinAge(Integer minAge) {
		this.minAge = minAge;
	}

	public Integer getMaxAge() {
		return maxAge;
	}

	public void setMaxAge(Integer maxAge) {
		this.maxAge = maxAge;
	}

	public Integer getDistanceKm() {
		return distanceKm;
	}

	public void setDistanceKm(Integer distanceKm) {
		this.distanceKm = distanceKm;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getEducation() {
		return education;
	}

	public void setEducation(String education) {
		this.education = education;
	}

	public String getOccupation() {
		return occupation;
	}

	public void setOccupation(String occupation) {
		this.occupation = occupation;
	}

	public String getHobbies() {
		return hobbies;
	}

	public void setHobbies(String hobbies) {
		this.hobbies = hobbies;
	}

	public String getPhotoUrl() {
		return photoUrl;
	}

	public void setPhotoUrl(String photoUrl) {
		this.photoUrl = photoUrl;
	}

	public boolean isPremium() {
		return premium;
	}

	public void setPremium(boolean premium) {
		this.premium = premium;
	}
}
