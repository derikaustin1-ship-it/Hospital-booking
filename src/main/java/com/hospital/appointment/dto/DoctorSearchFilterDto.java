package com.hospital.appointment.dto;

public class DoctorSearchFilterDto {

    private String query;
    private String specialization;
    private String sortBy; // name, experience, fee

    public DoctorSearchFilterDto() {
    }

    public DoctorSearchFilterDto(String query, String specialization, String sortBy) {
        this.query = query;
        this.specialization = specialization;
        this.sortBy = sortBy;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }
}
