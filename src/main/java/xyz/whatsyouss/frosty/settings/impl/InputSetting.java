package xyz.whatsyouss.frosty.settings.impl;

import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.events.impl.SettingUpdateEvent;
import xyz.whatsyouss.frosty.settings.Setting;

public class InputSetting extends Setting {
    private String value, cnName;
    private final int maxLength;
    private String placeholder;

    public InputSetting(String name, int maxLength, String Value) {
        super(name);
        this.maxLength = maxLength;
        this.value = Value;
        this.placeholder = "";
    }

    public InputSetting(String name, int maxLength, String Value, String placeholder) {
        super(name);
        this.maxLength = maxLength;
        this.value = Value;
        this.placeholder = placeholder;
    }

    public InputSetting(String name, String cnName, int maxLength, String Value) {
        super(name);
        this.cnName = cnName;
        this.maxLength = maxLength;
        this.value = Value;
        this.placeholder = "";
    }

    public InputSetting(String name, String cnName, int maxLength, String Value, String placeholder) {
        super(name);
        this.cnName = cnName;
        this.maxLength = maxLength;
        this.value = Value;
        this.placeholder = placeholder;
    }

    public String getName() {
        return this.name;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        if (value.length() <= maxLength) {
            this.value = value;
        } else {
            this.value = value.substring(0, maxLength);
        }
        Frosty.EVENT_BUS.post(new SettingUpdateEvent());
    }

    public int getMaxLength() {
        return this.maxLength;
    }

    public String getPlaceholder() {
        return this.placeholder;
    }
}