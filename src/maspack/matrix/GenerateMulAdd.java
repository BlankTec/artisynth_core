/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.io.*;
import java.util.*;
import maspack.util.*;

/**
 * Code generator to create MatrixMulAdd.java, MatrixMulTransposeRightAdd.java,
 * and MatrixMulTransposeLeftAdd.java
 */
public class GenerateMulAdd {

   enum Transpose {
      None,
      Left,
      Right
   };

   IndentingPrintWriter pw;
   String myClassName;
   String myFileName;
   String myMethodName;
   Transpose myTranspose;

   private boolean M1Transposed() {
      return myTranspose == Transpose.Left;
   }

   private boolean M2Transposed() {
      return myTranspose == Transpose.Right;
   }

   GenerateMulAdd (
      String className, Transpose transpose) {
      myClassName = className;
      myFileName = className + ".java";
      myTranspose = transpose;
      if (transpose == Transpose.None) {
         myMethodName = "mulAdd";
      }
      else {
         myMethodName = "mulTranspose"+transpose+"Add";
      }
   }

   public void open () throws IOException {
      pw = new IndentingPrintWriter (new FileWriter (myFileName));
      pw.println ("package maspack.matrix;");
      pw.println ("");
      pw.println ("/**");
      pw.println (" * Provides static methods to implement MatrixBlock." +
                  myMethodName + "()");
      pw.println (" * NOTE: This code is machine generated by GenerateMulAdd");
      pw.println (" */");
      pw.println ("public class "+myClassName+" {");
      pw.println ("");
      pw.addIndentation (3);
   }

   protected int numRows (String dimen) {
      return dimen.charAt(0)-'0';
   }

   protected int numCols (String dimen) {
      return dimen.charAt(2)-'0';
   }

   protected String getClassName (String dimen) {
      if (numRows(dimen) == numCols(dimen) && numRows(dimen) > 1) {
         return "Matrix"+numRows(dimen)+"dBase";
      }
      else {
         return "Matrix"+dimen;
      }
   }

   protected String getClassName (int nr, int nc) {
      return getClassName (nr + "x" + nc);
   }

   public void generateMulStatement (
      int i, int j, int nk, int maxk, int newLineIndent) {

      if (nk > maxk) {
         pw.print ("(");
      }
      for (int k=0; k<Math.min (nk, maxk); k++) {
         if (k > 0) {
            pw.print (" + ");
         }
         String Aterm = M1Transposed() ? "A.m"+k+i : "A.m"+i+k;
         String Bterm = M2Transposed() ? "B.m"+j+k : "B.m"+k+j;
         pw.print (Aterm+"*"+Bterm);
      }
      if (nk > maxk) {
         pw.println (" +");
         for (int l=0; l<newLineIndent; l++) {
            pw.print (" ");
         }
         for (int k=maxk; k<nk; k++) {
            if (k > maxk) {
               pw.print (" + ");
            }
            String Aterm = M1Transposed() ? "A.m"+k+i : "A.m"+i+k;
            String Bterm = M2Transposed() ? "B.m"+j+k : "B.m"+k+j;
            pw.print (Aterm+"*"+Bterm);
         }
         pw.println (");");
      }
      else {
         pw.println (";");                  
      }
   }

   public void generateBlock (int nr, int nc, int nk) throws IOException {
      pw.println ("case "+nk+": {");
      pw.addIndentation (3);
      String classR = getClassName (nr, nc);
      String class1, class2;
      if (M1Transposed()) {
         class1 = getClassName (nk, nr);
      }
      else {
         class1 = getClassName (nr, nk);
      }
      if (M2Transposed()) {
         class2 = getClassName (nc, nk);
      }
      else {
         class2 = getClassName (nk, nc);
      }
      pw.println (class1+" A = ("+class1+")M1;");
      pw.println (class2+" B = ("+class2+")M2;");
      if (class1.equals (classR) || class2.equals (classR)) {
         // need to implment using temp variables
         for (int i=0; i<nr; i++) {
            pw.println ("");
            for (int j=0; j<nc; j++) {
               pw.print ("double t"+i+j+" = ");
               generateMulStatement (i, j, nk, 3, 14);
            }
         }
         for (int i=0; i<nr; i++) {
            pw.println ("");
            for (int j=0; j<nc; j++) {
               pw.println ("MR.m"+i+j+" += t"+i+j+";");
            }
         }
      }
      else {
         for (int i=0; i<nr; i++) {
            pw.println ("");
            for (int j=0; j<nc; j++) {
               pw.print ("MR.m"+i+j+" += ");
               generateMulStatement (i, j, nk, 4, 11);
            }
         }
      }
      pw.println ("break;");
      pw.addIndentation (-3);      
      pw.println ("}");
   }

   public void writeMethod (String dimen) throws IOException {
      int nr = numRows(dimen);
      int nc = numCols(dimen);
      pw.println ("public static void "+myMethodName+dimen+
                  " ("+getClassName(dimen)+" MR, Matrix M1, Matrix M2) {");
      pw.addIndentation (3);
      String M1rowSize = M1Transposed() ? "M1.colSize()" : "M1.rowSize()";
      String M1colSize = M1Transposed() ? "M1.rowSize()" : "M1.colSize()";
      String M2rowSize = M2Transposed() ? "M2.colSize()" : "M2.rowSize()";
      String M2colSize = M2Transposed() ? "M2.rowSize()" : "M2.colSize()";
      pw.println ("if ("+M1rowSize+" != "+nr+" ||");
      pw.println ("    "+M2colSize+" != "+nc+" ||");
      pw.println ("    "+M1colSize+" != "+M2rowSize+") {");

      pw.println ("   throw new ImproperSizeException (");
      pw.println ("      \"matrix sizes \"+M1.getSize()+\" and \"+M2.getSize()+");
      pw.println ("      \" do not conform to \"+MR.getSize());");
      pw.println ("}");
      pw.println ("if (M1.isFixedSize() && M2.isFixedSize()) {");
      pw.println ("   switch ("+M1colSize+") {");
      pw.addIndentation (6);
      for (int nk : MatrixBlockBase.getMulDimensions (nr, nc)) {
         generateBlock (nr, nc, nk);
      }
      pw.addIndentation (-6);
      pw.println ("   }");
      pw.println ("}");
      pw.println ("else {");
      pw.println ("   for (int k=0; k<"+M1colSize+"; k++) {");
      pw.addIndentation (6);
      for (int i=0; i<nr; i++) {
         if (i > 0) {
            pw.println ("");
         }
         for (int j=0; j<nc; j++) {
            String M1term =
               M1Transposed() ? "M1.get(k,"+i+")" : "M1.get("+i+",k)";
            String M2term =
               M2Transposed() ? "M2.get("+j+",k)" : "M2.get(k,"+j+")";
            pw.println ("MR.m"+i+j+" += "+M1term+"*"+M2term+";");
         }
      }
      pw.addIndentation (-6);      
      pw.println ("   }");
      pw.println ("}");
      pw.addIndentation (-3);
      pw.println ("}");
      pw.println ("");
   }

   public void close() throws IOException {
      pw.addIndentation (-3);
      pw.println ("}");
      pw.close();
   }
   
   public void generate() {
      try {
         open();
         for (String dimen : MatrixBlockBase.getDefinedSizes()) {
            writeMethod (dimen);
         }
         close();
      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit(1); 
      }
      System.out.println ("Wrote " + myFileName);
   }

   public static void main (String[] args) {
      GenerateMulAdd generator;

      generator = new GenerateMulAdd (
         "MatrixMulAdd", Transpose.None);
      generator.generate();
      generator = new GenerateMulAdd (
         "MatrixMulTransposeLeftAdd", Transpose.Left);
      generator.generate();
      generator = new GenerateMulAdd (
         "MatrixMulTransposeRightAdd", Transpose.Right);
      generator.generate();
   }
}
