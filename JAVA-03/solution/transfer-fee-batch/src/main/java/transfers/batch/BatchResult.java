package transfers.batch;

/** Counts from one run. Refused includes unsupported types and rows that failed. */
public record BatchResult(int read, int priced, int refused) {
}
