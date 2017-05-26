/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.probes;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import maspack.interpolation.NumericList;
import maspack.interpolation.NumericListKnot;
import maspack.matrix.ImproperStateException;
import maspack.matrix.VectorNd;
import maspack.properties.NumericConverter;
import maspack.properties.Property;
import maspack.properties.PropertyList;
import maspack.util.*;
import artisynth.core.modelbase.*;

import artisynth.core.util.*;

public class MonitorOutputProbe extends NumericProbeBase 
   implements CopyableComponent {
   private boolean myShowTime;
   private static boolean defaultShowTime = true;

   private boolean myShowHeader;
   private static boolean defaultShowHeader = true;

   public static PropertyList myProps =
      new PropertyList (MonitorOutputProbe.class, NumericProbeBase.class);

   static {
      myProps.add (
         "showTime * *", "show time explicitly in output file",
         defaultShowTime);

      myProps.add (
         "showHeader * *", "show header explicitly in output file",
         defaultShowHeader);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   protected void setDefaultValues() {
      super.setDefaultValues();
      myShowTime = defaultShowTime;
      myShowHeader = defaultShowHeader;
   }

   public boolean getShowTime() {
      return myShowTime;
   }

   public void setShowTime (boolean enable) {
      myShowTime = enable;
   }

   public boolean getShowHeader() {
      return myShowHeader;
   }

   public void setShowHeader (boolean enable) {
      myShowHeader = enable;
   }

   public MonitorOutputProbe() {
      setDefaultValues();
      myPlotTraceManager = new PlotTraceManager ("output");
   }

   public MonitorOutputProbe (int vsize, double interval) {
      this (vsize, null, 0, 1, interval);
   }

   public MonitorOutputProbe (
      int vsize, String fileName,
      double startTime, double stopTime, double interval) {

      this();
      setVsize (vsize);
      setAttachedFileName (fileName);
      setUpdateInterval (interval);
      setStartTime (startTime);
      setStopTime (stopTime);
   }

   /**
    * Writes the start and stop times, scale value, and data for this probe to a
    * PrintWriter, using the format described for {@link
    * artisynth.core.probes.NumericInputProbe#read(File,boolean)
    * NumericInputProbe.read(File)}. The format used for producing floating
    * point numbers can be controlled using a printf-style format string,
    * details of which are described in {@link maspack.util.NumberFormat
    * NumberFormat}.
    * 
    * @param pw
    * writer which accepts the output
    * @param fmtStr
    * printf-style format string (if set to null then "%g" will be assumed,
    * which will produce full precision output).
    * @param showTime
    * if true, then time values are written explicitly. Otherwise, an implicit
    * step size corresponding to the value returned by {@link #getUpdateInterval
    * getUpdateInterval} will be specified.
    * @throws IOException
    * if an I/O error occurs.
    */
   public void write (PrintWriter pw, String fmtStr, boolean showTime)
      throws IOException {
      pw.println (getStartTime() + " " + getStopTime() + " " + myScale);
      pw.print (myInterpolation.getOrder()+" "+myNumericList.getVectorSize());
      if (showTime) {
         pw.println (" explicit");
      }
      else {
         pw.println (" " + getUpdateInterval());
      }
      writeData (pw, fmtStr, showTime);
   }

   public void setAttachedFileName (String fileName, String fmtStr) {
      setAttachedFileName (fileName);
      setFormat (fmtStr);
   }

   public void setAttachedFileName (
      String fileName, String fmtStr, boolean showTime, boolean showHeader) {
      setAttachedFileName (fileName);
      setFormat (fmtStr);
      setShowTime (showTime);
      setShowHeader (showHeader);
   }

   /**
    * When called (perhaps by the Artsynth timeline), causes information about
    * this probe to be written to its attached file.
    * 
    * @see #write
    */
   public void save() throws IOException {
      File file = getAttachedFile();
      if (file != null && !file.isDirectory ()) {
         try {
            if (isAttachedFileRelative()) {
               file.getParentFile().mkdirs();
            }
            PrintWriter pw =
               new PrintWriter (new BufferedWriter (new FileWriter (file)));
            System.out.println ("saving output probe to " + file.getName());
            if (myShowHeader) {
               write (pw, myFormatStr, myShowTime);
            }
            else {
               writeData (pw, myFormatStr, myShowTime);
            }
            pw.close();
         }
         catch (Exception e) {
            System.out.println ("Error writing file " + file.getName());
            e.printStackTrace();
         }
      }
   }

   public void writeData (PrintWriter pw, String fmtStr, boolean showTime) {
      NumberFormat timeFmt = null;
      if (showTime) {
         if (getUpdateInterval() < 1e-5) {
            timeFmt = new NumberFormat ("%12.9f");
         }
         else {
            timeFmt = new NumberFormat ("%9.6f");
         }
      }
      NumberFormat fmt = new NumberFormat (fmtStr);
      Iterator<NumericListKnot> it = myNumericList.iterator();
      while (it.hasNext()) {
         NumericListKnot knot = it.next();
         if (showTime) {
            pw.print (timeFmt.format (knot.t) + " ");
         }
         pw.println (knot.v.toString (fmt));
      }
   }

   protected void getData (VectorNd vec0, double t, double trel) {
      vec0.setZero();
   }

   protected Object[] getPropsOrDimens () {
      Object[] propsOrDimens = new Object[1];
      propsOrDimens[0] = myVsize;
      return propsOrDimens;
   }

   public void apply (double t) {
      // XXX don't we want to apply scaling here too?
      double trel = (t-getStartTime())/myScale;

      NumericListKnot knot = new NumericListKnot (myVsize);
      getData (knot.v, t, trel);
      knot.t = trel;
      myNumericList.add (knot);
      myNumericList.clearAfter (knot);
   }

   public Object clone() throws CloneNotSupportedException {
      MonitorOutputProbe probe = (MonitorOutputProbe)super.clone();
      //probe.myNumericList.clear();
      return probe;
   }

   public NumericList getOutput() {
      return myNumericList;
   }

   private PlotTraceInfo[] tmpTraceInfos = null;

   public boolean scanItem (
      ReaderTokenizer rtok, Deque<ScanToken> tokens) throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "vsize")) {
         int vsize = rtok.scanInteger();
         setVsize (vsize);
         return true;
      }
      else if (scanAttributeName (rtok, "plotTraceInfo")) {
         tmpTraceInfos = scanPlotTraceInfo (rtok);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      tmpTraceInfos = null;
      super.scan (rtok, ref);
   }

   public void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      super.writeItems (pw, fmt, ancestor);
      
      pw.println ("vsize=" + myVsize);
      maybeWritePlotTraceInfo (pw);
   }

   public void setVsize (int vsize) {
      setVsize (vsize, null);
   }

   public void setVsize (int vsize, PlotTraceInfo[] traceInfos) {

      myVsize = vsize;
      myNumericList = new NumericList (myVsize);

      if (traceInfos != null) {
         myPlotTraceManager.rebuild (getPropsOrDimens(), traceInfos);
      }
      else {
         myPlotTraceManager.rebuild (getPropsOrDimens());
      }
      if (myLegend != null) {
         myLegend.rebuild();
      }
   }
   
   /**
    * {@inheritDoc}
    */
   public boolean isDuplicatable() {
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public boolean getCopyReferences (
      List<ModelComponent> refs, ModelComponent ancestor) {
      return true;
   }

   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      MonitorOutputProbe probe;
      try {
         probe = (MonitorOutputProbe)clone();
      }
      catch (CloneNotSupportedException e) {
         throw new InternalErrorException ("Cannot clone MonitorOutputProbe");
      }
      double duration = getStopTime()-getStartTime();
      probe.setStartTime (probe.getStartTime()+duration);
      probe.setStopTime (probe.getStopTime()+duration);
      return probe;
   }

}