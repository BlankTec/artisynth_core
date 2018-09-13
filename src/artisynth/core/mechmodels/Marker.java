/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.List;
import java.util.ArrayList;

import artisynth.core.modelbase.*;
import maspack.matrix.*;

public abstract class Marker extends Point implements HasAttachments {

   public abstract void updateState();

   protected void removeBackRefsIfConnected() {
      if (isConnectedToHierarchy()) {
         DynamicAttachmentBase.removeBackRefs (getAttachment());
      }
   }

   protected void addBackRefsIfConnected() {
      if (isConnectedToHierarchy()) {
         DynamicAttachmentBase.addBackRefs (getAttachment());
      }
   }

   /**
    * {@inheritDoc}
    */
   public void getAttachments (List<DynamicAttachment> list) {
      DynamicAttachment at = getAttachment();
      if (at != null) { // paranoid
         list.add (at);
      }
   }  

   @Override
   public void getHardReferences (List<ModelComponent> refs) {
      super.getHardReferences (refs);
      DynamicAttachment at = getAttachment();
      if (at instanceof DynamicAttachmentComp) {
         DynamicAttachmentComp ac = (DynamicAttachmentComp)at;
         ArrayList<ModelComponent> allrefs = new ArrayList<ModelComponent>();
         ac.getHardReferences (allrefs);
         allrefs.remove (this); // remove this component
         refs.addAll (allrefs);
      }
   }

   @Override
   public void connectToHierarchy () {
      super.connectToHierarchy ();
      DynamicAttachment at = getAttachment();
      if (at != null) {
         DynamicAttachmentBase.addBackRefs(at);
      }
      updateState(); // do we need this?
   }

   @Override
   public void disconnectFromHierarchy() {
      super.disconnectFromHierarchy();
      DynamicAttachment at = getAttachment();
      if (at != null) {
         DynamicAttachmentBase.removeBackRefs(at);
      }
   }

}
