package boutique;

/** The writer checkout sends successful orders to. Kept so tests can mock it by name. */
public interface ManifestWriter extends RecordWriter<Order> {
}
