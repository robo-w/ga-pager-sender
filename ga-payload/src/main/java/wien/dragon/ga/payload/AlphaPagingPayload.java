package wien.dragon.ga.payload;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class AlphaPagingPayload {

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


    private final int targetRic;
    private final DataRate dataRate;
    private final FunctionCode functionCode;
    private final String message;

    public AlphaPagingPayload(final int targetRic, final DataRate dataRate, final FunctionCode functionCode, final String message) {
        if (targetRic < 8 || targetRic > 2097151) {
            throw new IllegalArgumentException("RIC must be in range of '8' and '2097151'. Was: " + targetRic);
        }
        this.targetRic = targetRic;
        this.dataRate = dataRate;
        this.functionCode = functionCode;
        this.message = message;
    }

    public int getTargetRic() {
        return targetRic;
    }

    public DataRate getDataRate() {
        return dataRate;
    }

    public FunctionCode getFunctionCode() {
        return functionCode;
    }

    public String getMessage() {
        return message;
    }

    public String createChecksum() {
        return calculateChecksum("\02" + createMessageFrame());
    }

    public byte[] createSendableFrame() {
        var messageForChecksum = "\02" + createMessageFrame();

        var stringToSend = messageForChecksum + calculateChecksum(messageForChecksum) + "\04";
        return stringToSend.getBytes(StandardCharsets.US_ASCII);
    }

    public String createMessageFrame() {
        return String.format("%07d", targetRic) +
                ALPHABETIC_ENCODING +
                DATA_RATE_MAPPING.get(dataRate) +
                FUNCTION_CODE_MAPPING.get(functionCode) +
                message;
    }

    private static String calculateChecksum(final String messageForChecksum) {
        int checksum = messageForChecksum.chars().sum();
        return String.format("%04X", checksum);
    }
}
