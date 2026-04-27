package is.pig.minecraft.build.api;

/**
 * Pure Java record implementation of IVector3.
 */
public record Vector3(double x, double y, double z) implements IVector3 {
    
    @Override
    public IVector3 add(double x, double y, double z) {
        return new Vector3(this.x + x, this.y + y, this.z + z);
    }

    @Override
    public IVector3 add(IVector3 other) {
        return new Vector3(this.x + other.x(), this.y + other.y(), this.z + other.z());
    }

    @Override
    public IVector3 multiply(double factor) {
        return new Vector3(this.x * factor, this.y * factor, this.z * factor);
    }
}
