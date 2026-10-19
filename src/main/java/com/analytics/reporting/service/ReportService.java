package com.analytics.reporting.service;

import com.analytics.reporting.repo.ReportingJPA;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportingJPA reportingJPA;


    public Object getReport(String appId)
    {
        Map<String,Object> response = new HashMap<>();

        long totalUsers = reportingJPA.countByAppId(appId);
        long uniqueUsers = reportingJPA.countUniqueUsers(appId);
        long mobileUsers = reportingJPA.countByAppIdAndDeviceType(appId,"mobile");
        long desktopUsers = reportingJPA.countByAppIdAndDeviceType(appId,"desktop");

        Map<String,Long> deviceMap = new HashMap<>();
        deviceMap.put("mobile",mobileUsers);
        deviceMap.put("desktop",desktopUsers);
        //contains ["event1":2,"event2":3]
        List<Object[]> eventCounts = reportingJPA.countUsersPerEvent(appId);
        Map<String,Long> eventMap = new HashMap<>();

        for(Object[] event:eventCounts)
            eventMap.put(event[0].toString(),(Long)event[1]);

        response.put("appId",appId);
        response.put("totalUsers",totalUsers);
        response.put("uniqueUsers",uniqueUsers);
        response.put("devices",deviceMap);
        response.put("events",eventMap);
        response.put("mobile",mobileUsers);



        return response;
    }

}
