/*
 * Copyright (c) 2020. Robert Wittek (robo-w on GitHub)
 *
 * This software may be modified and distributed under the terms of the MIT license.  See the LICENSE file for details.
 */

package wien.dragon.ga.payload;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class PayloadFactoryTest {
    @Test
    void createAlphaPagingPayload_1200bps() {
        var message = "Test Message";

        var payload = PayloadFactory.createAlphaPagingPayload(12345, DataRate.BPS_1200, FunctionCode.CODE_1, message);

        var expectedPayload = "\0020012345A11" + message + "0689\004";
        assertThat(payload, equalTo(expectedPayload.getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    void createAlphaPagingPayload_512bps() {
        var message = "Test Message";

        var payload = PayloadFactory.createAlphaPagingPayload(12345, DataRate.BPS_512, FunctionCode.CODE_1, message);

        var expectedPayload = "\0020012345A51" + message + "068D\004";
        assertThat(payload, equalTo(expectedPayload.getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    void createAlphaPagingPayload_2400bps() {
        var message = "Test Message";

        var payload = PayloadFactory.createAlphaPagingPayload(12345, DataRate.BPS_2400, FunctionCode.CODE_1, message);

        var expectedPayload = "\0020012345A21" + message + "068A\004";
        assertThat(payload, equalTo(expectedPayload.getBytes(StandardCharsets.US_ASCII)));
    }
}
