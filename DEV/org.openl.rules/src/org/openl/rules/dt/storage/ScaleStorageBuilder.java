package org.openl.rules.dt.storage;

import org.openl.rules.dt.DTScale.RowScale;

public class ScaleStorageBuilder implements IStorageBuilder {

    private final RowScale scale;
    private final StorageBuilder sb;

    ScaleStorageBuilder(RowScale scale, StorageBuilder sb) {
        super();
        this.scale = scale;
        this.sb = sb;
    }

    @Override
    public void writeObject(Object loadedValue, int index) {
        sb.writeObject(loadedValue, getStorageIndex(index));
    }

    private int getStorageIndex(int index) {
        return scale.getActualIndex(index);
    }

    @Override
    public int size() {
        return sb.size() * scale.getMultiplier();
    }

    @Override
    public IStorage optimizeAndBuild() {
        IStorage storage = sb.optimizeAndBuild();

        return new ScaledStorage(scale, storage, sb.getInfo());
    }
}
