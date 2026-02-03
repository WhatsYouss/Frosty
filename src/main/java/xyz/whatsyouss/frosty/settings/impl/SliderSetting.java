package xyz.whatsyouss.frosty.settings.impl;

import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.events.impl.SettingUpdateEvent;
import xyz.whatsyouss.frosty.settings.Setting;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SliderSetting extends Setting {
    private double value;
    private double valueMin, valueMax;
    private final double min;
    private final double max;
    private final double intervals;
    private boolean isRange;
    private String suffix = "";

    public SliderSetting(String name, double defaultValue, double min, double max, double intervals) {
        super(name);
        this.value = defaultValue;
        this.min = min;
        this.max = max;
        this.intervals = intervals;
        this.isRange = false;
    }

    public SliderSetting(String name, String suffix, double defaultValue, double min, double max, double intervals) {
        super(name);
        this.suffix = suffix;
        this.value = defaultValue;
        this.min = min;
        this.max = max;
        this.intervals = intervals;
        this.isRange = false;
    }

    public SliderSetting(String name, double valueMin, double valueMax, double min, double max, double intervals) {
        super(name);
        this.valueMin = valueMin;
        this.valueMax = valueMax;
        this.min = min;
        this.max = max;
        this.intervals = intervals;
        this.isRange = true;
    }

    public SliderSetting(String name, String suffix, double valueMin, double valueMax, double min, double max, double intervals) {
        super(name);
        this.suffix = suffix;
        this.valueMin = valueMin;
        this.valueMax = valueMax;
        this.min = min;
        this.max = max;
        this.intervals = intervals;
        this.isRange = true;
    }

    public double getInput() {
        return roundToInterval(this.value, 2);
    }

    public void setInput(double input) {
        this.value = Math.max(min, Math.min(max, input));
        Frosty.EVENT_BUS.post(new SettingUpdateEvent());
    }

    public double getInputMin() {
        return roundToInterval(this.valueMin, 2);
    }

    public void setInputMin(double input) {
        this.valueMin = Math.max(min, Math.min(valueMax, input));
        Frosty.EVENT_BUS.post(new SettingUpdateEvent());
    }

    public double getInputMax() {
        return roundToInterval(this.valueMax, 2);
    }

    public void setInputMax(double input) {
        this.valueMax = Math.max(valueMin, Math.min(max, input));
        Frosty.EVENT_BUS.post(new SettingUpdateEvent());
    }

    public static double roundToInterval(double v, int p) {
        if (p < 0) {
            return 0.0D;
        } else {
            BigDecimal bd = new BigDecimal(v);
            bd = bd.setScale(p, RoundingMode.HALF_UP);
            return bd.doubleValue();
        }
    }

    public double getIntervals() {
        return this.intervals;
    }

    public double getMin() {
        return this.min;
    }

    public double getMax() {
        return this.max;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public boolean isRange() {
        return this.isRange;
    }
}