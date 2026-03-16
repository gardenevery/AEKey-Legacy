package aekeylegacy;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import aekeylegacy.aekeylegacy.Tags;

public class AELog {
    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    public static void warn(String message, Object... params) {
        LOGGER.warn(message, params);
    }

    public static void error(String message, Object... params) {
        LOGGER.error(message, params);
    }

    public static void debug(String message, Object... params) {
        LOGGER.debug(message, params);
    }
}
