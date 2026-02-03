package xyz.whatsyouss.frosty.utility;

import java.awt.*;

public enum Theme {
    Rainbow(null, null), // 0
    Cherry(new Color(255, 200, 200), new Color(243, 58, 106)), // 1
    Cotton_candy(new Color(99, 249, 255), new Color(255, 104, 204)), // 2
    Flare(new Color(231, 39, 24), new Color(245, 173, 49)), // 3
    Flower(new Color(215, 166, 231), new Color(211, 90, 232)), // 4
    Gold(new Color(255, 215, 0), new Color(240, 159, 0)), // 5
    Grayscale(new Color(240, 240, 240), new Color(110, 110, 110)), // 6
    Royal(new Color(125, 204, 241), new Color(30, 71, 170)), // 7
    Sky(new Color(160, 230, 225), new Color(15, 190, 220)), // 8
    Vine(new Color(17, 192, 45), new Color(201, 234, 198)); // 9

    public final Color firstGradient;
    public final Color secondGradient;

    public static Color[] descriptor = new Color[]{new Color(95, 235, 255), new Color(68, 102, 250)};
    public static Color[] hiddenBind = new Color[]{new Color(245, 33, 33), new Color(229, 21, 98)};

    Theme(Color firstGradient, Color secondGradient) {
        this.firstGradient = firstGradient;
        this.secondGradient = secondGradient;
    }

    public String getName() {
        return this.name().replace("_", " ");
    }

    public static String[] getAllThemeNames() {
        Theme[] themes = values();
        String[] names = new String[themes.length];
        for (int i = 0; i < themes.length; i++) {
            names[i] = themes[i].getName();
        }
        return names;
    }

    public static Theme getTheme(int index) {
        return values()[index];
    }

    public static int getGradient(int index, double delay) {
        if (index > 0) {
            Theme theme = values()[index];
            if (theme.firstGradient != null && theme.secondGradient != null) {
                return convert(theme.firstGradient, theme.secondGradient,
                        (Math.sin(System.currentTimeMillis() / 1.0E8 * 400000.0 + delay * 0.55) + 1.0) * 0.5).getRGB();
            }
        }
        else if (index == 0) { // Rainbow
            return getChroma(2, (long) delay);
        }
        return -1;
    }

    public static int getChroma(long speed, long delay) {
        float hue = (float) ((System.currentTimeMillis() + delay) % (15000L / speed)) / (15000.0F / (float) speed);
        return Color.getHSBColor(hue, 1.0F, 1.0F).getRGB();
    }

    public static Color convert(Color c1, Color c2, double ratio) {
        double inv = 1.0 - ratio;
        return new Color(
                (int)(c1.getRed() * ratio + c2.getRed() * inv),
                (int)(c1.getGreen() * ratio + c2.getGreen() * inv),
                (int)(c1.getBlue() * ratio + c2.getBlue() * inv)
        );
    }

    public Color getFirstGradient() {
        return this.firstGradient;
    }

    public Color getSecondGradient() {
        return this.secondGradient;
    }

    public static String[] getThemeNames() {
        return new String[]{"Rainbow", "Cherry", "Cotton candy", "Flare",
                "Flower", "Gold", "Grayscale", "Royal", "Sky", "Vine"};
    }

    public int getAnimatedColor(double offset, int alpha) {
        if (this == Rainbow) {
            float hue = (float) ((System.currentTimeMillis() * 0.0002 + offset) % 1.0);
            return Color.HSBtoRGB(hue, 0.8f, 1.0f) | (alpha << 24);
        }

        double progress = (System.currentTimeMillis() * 0.0005 + offset) % 1.0;
        progress = (Math.sin(progress * Math.PI * 2) + 1) / 2;

        int r = (int)(firstGradient.getRed() * (1-progress) + secondGradient.getRed() * progress);
        int g = (int)(firstGradient.getGreen() * (1-progress) + secondGradient.getGreen() * progress);
        int b = (int)(firstGradient.getBlue() * (1-progress) + secondGradient.getBlue() * progress);

        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }

    public int getAnimatedColor(double offset, int alpha, double speed) {
        if (this == Rainbow) {
            float hue = (float) ((Math.sin(System.currentTimeMillis() * speed + offset * Math.PI) + 1) / 2.0);
            return Color.HSBtoRGB(hue, 0.8f, 1.0f) | (alpha << 24);
        }

        double progress = (Math.sin(System.currentTimeMillis() * speed + offset * Math.PI) + 1) / 2.0;
        int r = (int)(firstGradient.getRed() * (1-progress) + secondGradient.getRed() * progress);
        int g = (int)(firstGradient.getGreen() * (1-progress) + secondGradient.getGreen() * progress);
        int b = (int)(firstGradient.getBlue() * (1-progress) + secondGradient.getBlue() * progress);

        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }
}