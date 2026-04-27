package is.pig.minecraft.build.api;

/**
 * Pure Java vector interface for 3D coordinates.
 */
public interface IVector3 {
    double x();
    double y();
    double z();
    
    IVector3 add(double x, double y, double z);
    IVector3 add(IVector3 other);
    IVector3 multiply(double factor);
}
