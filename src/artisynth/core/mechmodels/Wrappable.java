package artisynth.core.mechmodels;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;

public interface Wrappable extends PointAttachable {

   /**
    * Computes the point <code>pt</code> on the surface of this Wrappable such
    * that the line segment (pa,pt) is both tangent to the surface and as near
    * as possible to the line defined by the two points pa and p1.
    *
    * <p>To assist with the computation, <code>pt</code> can be assumed to lie
    * fairly close to <code>p1</code>, and its projection onto the line can be
    * assumed to lie between <code>p1</code> and another point p0 defined by
    * <pre>
    * p0 = (1-lam0) pa + lam0 p1
    * </pre>
    * where <code>lam0</code> is a parameter between 0 and 1.
    * 
    * @param pt returns the tangent point
    * @param p0 first point of the line 
    * @param p1 second point of the line
    * @param lam0 parameter defining point p0 as defined above
    * @param sideNrm
    */
   public void surfaceTangent (
      Point3d pt, Point3d p0, Point3d p1, double lam0, Vector3d sideNrm);

   /**
    * Computes the penetration distance of a point <code>p0</code> into this
    * Wrappable, along with the normal <code>nrm</code> that points from
    * <code>p0</code> to its nearest point on the surface. The returned
    * distance should be negative. If <code>p0</code> is not penetrating, the
    * method should return 0 as quickly as it can.
    * 
    * @param nrm returns the normal (should be normalized)
    * @param p0 point to determine penetration for
    * @return returns the penetration distance, or 0 if p0 is
    * not penetrating.
    */
   public double penetrationDistance (Vector3d nrm, Point3d p0);
}