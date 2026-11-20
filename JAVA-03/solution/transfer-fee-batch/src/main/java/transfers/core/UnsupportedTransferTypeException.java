package transfers.core;

/** A transfer type the app does not offer, such as {@code intl}. The rest of the batch continues. */
public class UnsupportedTransferTypeException extends RuntimeException {
    private final String transferId;
    private final String type;

    public UnsupportedTransferTypeException(String transferId, String type) {
        super("Unsupported transfer type: " + type + " (transfer " + transferId + ")");
        this.transferId = transferId;
        this.type = type;
    }

    public String transferId() { return transferId; }
    public String type() { return type; }
}
