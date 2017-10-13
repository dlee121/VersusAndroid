package com.vs.bcd.versus.model;



public class SettingObject {

    private String settingName = "";

    public SettingObject(String settingName) {
        this.settingName = settingName;
    }

    public String getSettingName() {
        return settingName;
    }
    public void setSettingName(String settingName) {
        this.settingName = settingName;
    }
    /*
    public int getIconResID() {
        return iconResID;
    }
    public void setIconResID(int iconResID) {
        this.iconResID = iconResID;
    }
    */
    @Override
    public String toString() {
        return this.settingName;
    }
}