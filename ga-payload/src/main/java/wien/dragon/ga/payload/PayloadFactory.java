/*
 * Copyright (c) 2020. Robert Wittek (robo-w on GitHub)
 *
 * This software may be modified and distributed under the terms of the MIT license.  See the LICENSE file for details.
 */

package wien.dragon.ga.payload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class PayloadFactory {
    private static final Logger LOG = LoggerFactory.getLogger(PayloadFactory.class);

    @Deprecated
    public static byte[] createAlphaPagingPayload(final int targetRic, final DataRate dataRate, final FunctionCode functionCode, final String message) {
        return new AlphaPagingPayload(targetRic, dataRate, functionCode, message).createSendableFrame();
    }
}
