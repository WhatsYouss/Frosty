package xyz.whatsyouss.frosty.settings.impl;

import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.events.impl.SettingUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.impl.client.UI;
import xyz.whatsyouss.frosty.settings.Setting;

public class SelectSetting extends Setting {
    private String name, cnName;
    private String[] options, cnOptions;
    private double defaultValue;

    public SelectSetting(String name, int defaultValue, String[] options) {
        super(name);
        this.name = name;
        this.options = options;
        this.defaultValue = defaultValue;
    }

    public SelectSetting(String name, String cnName, int defaultValue, String[] options, String[] cnOptions) {
        super(name);
        this.name = name;
        this.cnName = cnName;
        this.options = options;
        this.cnOptions = cnOptions;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return this.name;
    }

    public String getTransName() {
        if (this.cnName != null && !this.cnName.isEmpty() && UI.lang.getValue() == 1) {
            return this.cnName;
        }
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

    public String[] getTransOptions() {
        if (this.cnName != null && this.cnOptions != null && this.cnOptions.length > 0 && UI.lang.getValue() == 1) {
            return this.cnOptions;
        }
        return this.options;
    }

    public String getTransOption() {
        if (options == null && cnOptions == null || defaultValue < 0 || defaultValue >= options.length) {
            return null;
        }
        if (this.cnName != null && this.cnOptions != null && this.cnOptions.length > 0 && UI.lang.getValue() == 1) {
            return cnOptions[(int) defaultValue];
        }
        return options[(int) defaultValue];
    }
}
