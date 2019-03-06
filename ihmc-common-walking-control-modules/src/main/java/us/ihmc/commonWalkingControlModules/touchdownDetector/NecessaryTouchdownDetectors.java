package us.ihmc.commonWalkingControlModules.touchdownDetector;

import java.util.ArrayList;

public class NecessaryTouchdownDetectors implements TouchdownDetector
{
   private final ArrayList<TouchdownDetector> touchdownDetectors = new ArrayList<>();

   public void addTouchdownDetector(TouchdownDetector touchdownDetector)
   {
      touchdownDetectors.add(touchdownDetector);
   }

   @Override
   public boolean hasTouchedDown()
   {
      for (int i = 0; i < touchdownDetectors.size(); i++)
      {
         if (!hasTouchedDown())
            return false;
      }

      return true;
   }

   @Override
   public void update()
   {
      for (int i = 0; i < touchdownDetectors.size(); i++)
         touchdownDetectors.get(i).update();
   }

   @Override
   public void reset()
   {
      for (int i = 0; i < touchdownDetectors.size(); i++)
         touchdownDetectors.get(i).reset();
   }

   @Override
   public String getName()
   {
      return "SufficientTouchdownDetectors";
   }
}
