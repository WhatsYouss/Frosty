package xyz.whatsyouss.frosty.settings;


import java.util.function.BooleanSupplier;

public abstract class Setting {

    public String name;
    public boolean visible = true;
    private BooleanSupplier visibilityCondition;

    public Setting(String name) {
        this.name = name;
    }


    public void setVisibilityCondition(BooleanSupplier condition) {
        this.visibilityCondition = condition;
    }

    public boolean isVisible() {
        if (visibilityCondition != null) {
            return visibilityCondition.getAsBoolean();
        }
        return visible;
    }

    public String getName() {
        return this.name;
    }
}
