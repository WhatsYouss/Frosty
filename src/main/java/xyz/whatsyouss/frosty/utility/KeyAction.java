package xyz.whatsyouss.frosty.utility;


public enum KeyAction {
    Press,
    Repeat,
    Release;

    public static KeyAction get(int action) {
        return switch (action) {
            case 1 -> Press;
            case 0 -> Release;
            default -> Repeat;
        };
    }
}
