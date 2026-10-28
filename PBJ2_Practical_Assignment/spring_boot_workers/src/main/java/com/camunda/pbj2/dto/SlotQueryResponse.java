package com.camunda.pbj2.dto;

public class SlotQueryResponse {
    private String department;
    private Boolean slotsAvailable;
    private String slotId;
    private String slotTime;
    private Integer availableCount;

    public SlotQueryResponse() {}

    public SlotQueryResponse(String department, Boolean slotsAvailable, String slotId, String slotTime, Integer availableCount) {
        this.department = department;
        this.slotsAvailable = slotsAvailable;
        this.slotId = slotId;
        this.slotTime = slotTime;
        this.availableCount = availableCount;
    }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public Boolean getSlotsAvailable() { return slotsAvailable; }
    public void setSlotsAvailable(Boolean slotsAvailable) { this.slotsAvailable = slotsAvailable; }

    public String getSlotId() { return slotId; }
    public void setSlotId(String slotId) { this.slotId = slotId; }

    public String getSlotTime() { return slotTime; }
    public void setSlotTime(String slotTime) { this.slotTime = slotTime; }

    public Integer getAvailableCount() { return availableCount; }
    public void setAvailableCount(Integer availableCount) { this.availableCount = availableCount; }
}
