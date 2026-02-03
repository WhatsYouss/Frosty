package xyz.whatsyouss.frosty.settings.impl;

import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.events.impl.SettingUpdateEvent;
import xyz.whatsyouss.frosty.settings.Setting;

public class SelectSetting extends Setting {
    private String name;
    private String[] options;
    private double defaultValue;

    public SelectSetting(String name, int defaultValue, String[] options) {
        super(name);
        this.name = name;
        this.options = options;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return this.name;
    }

    public double getValue() {
        return this.defaultValue;
    }

    public void setValue(double newValue) {
        this.defaultValue = newValue;
        Frosty.EVENT_BUS.post(new SettingUpdateEvent());
    }

    public String[] getOptions() {
        return this.options;
    }

    public String getOption() {
        if (options == null || defaultValue < 0 || defaultValue >= options.length) {
            return null;
        }
        return options[(int) defaultValue];
    }
}
