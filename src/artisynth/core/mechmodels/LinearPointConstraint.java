/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.Arrays;
import java.util.List;

import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
import maspack.matrix.Matrix3x3Block;
import maspack.matrix.Point3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.VectorNd;
import maspack.matrix.VectorNi;
import maspack.properties.PropertyList;
import maspack.render.PointLineRenderProps;
import maspack.render.Renderer;

/**
 * 
 * Constrain a linear combination of points to sum to a value:
 *   sum( w_i p_i, i=0..N ) = pos
 *   
 *   Useful for "attaching" an arbitrary position inside one finite element
 *   to another arbitrary position inside a different element, or to a
 *   specific point in space
 *
 */
public class LinearPointConstraint extends ConstrainerBase {

   public static double DEFAULT_COMPLIANCE = 0;
   public static double DEFAULT_DAMPING = 0;
   
   Point[] myPoints;
   double[] myWgts;
   Point3d myTarget;
   Matrix3x3Block[] myBlks;
   double[] myLam;
   double myCompliance;
   double myDamping;
   

   static PropertyList myProps = new PropertyList (LinearPointConstraint.class, ConstrainerBase.class);
   static {
      myProps.add ("renderProps", "render props", new PointLineRenderProps ());
   }
   
   @Override
   public PropertyList getAllPropertyInfo () {
      return myProps;
   }
   
   /**
    * General constructor.  Make sure to call {@link #setPoints(Point[], double[])}.
    */
   public LinearPointConstraint() {
      myDamping = DEFAULT_DAMPING;
      myCompliance = DEFAULT_COMPLIANCE;
   }

   /**
    * General constructor
    * @param pnts list of points to constrain
    * @param wgts list of weights
    */
   public LinearPointConstraint(Point[] pnts, double[] wgts) {
      this(pnts, wgts, Point3d.ZERO);
   }
   
   /**
    * General constructor
    * @param pnts list of points to constrain
    * @param wgts list of weights
    * @param target target sum
    */
   public LinearPointConstraint(Point[] pnts, double[] wgts, Point3d target) {
      this();
      setPoints(pnts, wgts);
      setTarget (target);
   }

   /**
    * General constructor
    * @param pnts list of points to constrain
    * @param wgts list of weights
    * @param target target sum
    */
   public LinearPointConstraint(Point[] pnts, VectorNd wgts, Point3d target) {
      double[] dwgts = new double[wgts.size ()];
      wgts.get (dwgts);
      setPoints(pnts, dwgts);
      setTarget (target);
   }
   
   /**
    * Initializes the constraint with a set of points and weights.  All
    * {@code Point} objects should be unique.
    * @param pnts list of points to constrain
    * @param wgts set of weights
    */
   public void setPoints(Point[] pnts, double[] wgts) {
      myTarget = new Point3d(0, 0, 0);
      myPoints = Arrays.copyOf(pnts, pnts.length);
      myLam = new double[3];    // 3 constraints (x, y, z)
      myWgts = Arrays.copyOf(wgts, wgts.length);
      myBlks = new Matrix3x3Block[myPoints.length];
      for (int i=0; i<myPoints.length; i++) {
         myBlks[i] = new Matrix3x3Block();
         myBlks[i].m00 = myWgts[i];
         myBlks[i].m11 = myWgts[i];
         myBlks[i].m22 = myWgts[i];
      }
   }
   
   /**
    * Sets a target sum of positions
    * @param pos target position
    */
   public void setTarget(Point3d pos) {
      myTarget.set (pos);
   }

   /**
    * @return the set of points involved in the constraint
    */
   public Point[] getPoints() {
      return myPoints;
   }

   /**
    * @return the linear constraint weights
    */
   public double[] getWeights() {
      return myWgts;
   }
   
   public Point3d getTarget() {
      return myTarget;
   }
   
   public double getCompliance() {
      return myCompliance;
   }
   
   public void setCompliance(double c) {
      myCompliance = c;
   }
   
   public double getDamping() {
      return myDamping;
   }
   
   public void setDamping(double d) {
      myDamping = d;
   }

   @Override
   public void getBilateralSizes(VectorNi sizes) {
      for (Point p : myPoints) {
         if (p.getSolveIndex() != -1) {
            sizes.append(3);
         }
      }
   }

   @Override
   public int addBilateralConstraints(
      SparseBlockMatrix GT, VectorNd dg, int numb) {
      int idx = 0;
      int bj = GT.numBlockCols();
      for (Point pnt : myPoints) {
         int bi = pnt.getSolveIndex(); 
         if (bi != -1) {
            myBlks[idx].setBlockRow(bi);
            GT.addBlock(bi, bj, myBlks[idx]);
            if (dg != null) {
               dg.set(numb, 0);
            }
            numb++;
         }
         idx++;
      }
      // System.out.println(GT.toString());
      return numb;
   }

   @Override
   public int getBilateralInfo(ConstraintInfo[] ginfo, int idx) {

      Point3d sumPos = new Point3d();
      int nValid = 0;
      for (int i=0; i<myPoints.length; i++) {
         Point pnt = myPoints[i];
         if (pnt.getSolveIndex() > -1) {
            nValid++;
         }
         sumPos.scaledAdd(myWgts[i], pnt.getPosition());
      }
      sumPos.sub (myTarget);

      if (nValid == 0) {
         return idx;
      }

      // x
      ConstraintInfo gi = ginfo[idx++];
      gi.dist = sumPos.x;
      gi.compliance = myCompliance;
      gi.damping = myDamping;
      gi.force = 0;

      // y
      gi = ginfo[idx++];
      gi.dist = sumPos.y;
      gi.compliance = myCompliance;
      gi.damping = myDamping;
      gi.force = 0;

      // z
      gi = ginfo[idx++];
      gi.dist = sumPos.z;
      gi.compliance = myCompliance;
      gi.damping = myDamping;
      gi.force = 0;

      return idx;
   }

   @Override
   public int setBilateralImpulses(VectorNd lam, double h, int idx) {
      for (Point p : myPoints) {
         if (p.getSolveIndex() != -1) {
            myLam[0] = lam.get(idx++);
            myLam[1] = lam.get(idx++);
            myLam[2] = lam.get(idx++);
            return idx;
         }
      }
      return idx;
   }

   @Override
   public int getBilateralImpulses(VectorNd lam, int idx) {
      for (Point p : myPoints) {
         if (p.getSolveIndex() != -1) {
            lam.set(idx++, myLam[0]);
            lam.set(idx++, myLam[1]);
            lam.set(idx++, myLam[2]);
            return idx;
         }
      }
      return idx;
   }

   @Override
   public void zeroImpulses() {
      myLam[0] = 0;
      myLam[1] = 0;
      myLam[2] = 0;
   }

   @Override
   public double updateConstraints(double t, int flags) {
      return 0;
   }

   public void getConstrainedComponents (List<DynamicComponent> list) {
      for (int i=0; i<myPoints.length; i++) {
         list.add (myPoints[i]);
      }
   }
   
   @Override
   public void render(Renderer renderer, int flags) {

      Point3d diff = new Point3d();
      Point3d avgPos = new Point3d();
      for (int i=0; i<myPoints.length; i++) {
         Point pnt = myPoints[i];
         diff.scaledAdd(myWgts[i], pnt.getPosition());
         if (myWgts[i] > 0) {
            avgPos.scaledAdd (myWgts[i], pnt.getPosition ());
         }
      }
      diff.sub (myTarget);
      
      float[] fpnt0 = new float[] {(float)avgPos.x, (float)avgPos.y, (float)avgPos.z};
      float[] fpnt1 = new float[] {(float)(avgPos.x-diff.x), (float)(avgPos.y-diff.y), (float)(avgPos.z-diff.z)};
      
      renderer.drawLine (getRenderProps (), fpnt0, fpnt1, isSelected());
      renderer.drawPoint (getRenderProps(), fpnt0, isSelected());
      renderer.drawPoint (getRenderProps(), fpnt1, isSelected());
      
   }

}
