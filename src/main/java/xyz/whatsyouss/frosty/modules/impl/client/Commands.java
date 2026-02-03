package xyz.whatsyouss.frosty.modules.impl.client;

import xyz.whatsyouss.frosty.modules.Module;

public class Commands extends Module {
    public Commands() {
        super("Commands", category.Client);
    }

    @Override
    public String getDesc() {
        return "Client commands (.help)";
    }
}
