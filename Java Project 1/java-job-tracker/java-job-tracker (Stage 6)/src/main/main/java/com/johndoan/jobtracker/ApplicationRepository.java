package com.johndoan.jobtracker;

import java.util.List;

public interface ApplicationRepository {

    JobApplication add(JobApplication app);

    boolean updateStatus(int id, ApplicationStatus newStatus);

    boolean deleteById(int id);

    List<JobApplication> findAll();

    List<JobApplication> findByStatus(ApplicationStatus status);

    List<JobApplication> search(SearchFilter filter);

    void replaceAll(List<JobApplication> apps);
}
