package aekeylegacy.api.storage;

import aekeylegacy.api.stacks.AEKey;

@FunctionalInterface
public interface AEKeyFilter {
    static AEKeyFilter none() {
        return NoOpKeyFilter.INSTANCE;
    }

    boolean matches(AEKey what);
}
