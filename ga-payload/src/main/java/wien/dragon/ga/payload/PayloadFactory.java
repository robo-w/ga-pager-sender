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

    private static final String ALPHABETIC_ENCODING = "A";
    private static final Map<DataRate, Character> DATA_RATE_MAPPING = Map.of(
            DataRate.BPS_512, '5',
            DataRate.BPS_1200, '1',
            DataRate.BPS_2400, '2'
    );
    private static final Map<FunctionCode, Character> FUNCTION_CODE_MAPPING = Map.of(
            FunctionCode.DEFAULT, '0',
            FunctionCode.CODE_1, '1',
            FunctionCode.CODE_2, '2',
            FunctionCode.CODE_3, '3',
            FunctionCode.CODE_4, '4'
    );

    public static byte[] createAlphaPagingPayload(final int targetRic, final DataRate dataRate, final FunctionCode functionCode, final String message) {
        StringBuilder buffer = new StringBuilder("\02");

        if (targetRic < 8 || targetRic > 2097151) {
            throw new IllegalArgumentException("RIC must be in range of '8' and '2097151'. Was: " + targetRic);
        }

        buffer.append(String.format("%07d", targetRic));
        buffer.append(ALPHABETIC_ENCODING);
        buffer.append(DATA_RATE_MAPPING.get(dataRate));
        buffer.append(FUNCTION_CODE_MAPPING.get(functionCode));
        buffer.append(message);
        String messageForChecksum = buffer.toString();
        buffer.append(createChecksum(messageForChecksum));
        buffer.append("\04");

        var stringToSend = buffer.toString();
        LOG.debug("Created raw message for RIC '{}' with data rate '{}' and function code '{}':\n  {}", targetRic, dataRate, functionCode, stringToSend);
        return stringToSend.getBytes(StandardCharsets.US_ASCII);
    }

    private static String createChecksum(final String messageForChecksum) {
        int checksum = messageForChecksum.chars().sum();
        return String.format("%04X", checksum);
    }
}
