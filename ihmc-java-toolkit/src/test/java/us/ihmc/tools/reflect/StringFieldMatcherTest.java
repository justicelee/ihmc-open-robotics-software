package us.ihmc.tools.reflect;

import static us.ihmc.robotics.Assert.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Disabled;
public class StringFieldMatcherTest
{

	@Test
   public void testStringFieldMatcher() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException
   {     
     ObjectWithAName barry = new ObjectWithAName("Obama2012", "Prez", 1.5);
     ObjectWithAName mitt = new ObjectWithAName("Romney2012", "Gov", 1.5);
     
     StringFieldMatcher matcher = new StringFieldMatcher();
     Field fieldToMatch = ObjectWithAName.class.getDeclaredField("name");
     matcher.addStringFieldToMatchExactly(ObjectWithAName.class, fieldToMatch, "Obama2012");
     
     assertTrue(matcher.matches(barry));
     assertFalse(matcher.matches(mitt));
     
     matcher = new StringFieldMatcher();
     matcher.addStringFieldToMatchRegularExpression(ObjectWithAName.class, fieldToMatch, ".*2012");
     
     assertTrue(matcher.matches(barry));
     assertTrue(matcher.matches(mitt));
     
     matcher = new StringFieldMatcher();
     matcher.addStringFieldToMatchRegularExpression(ObjectWithAName.class, fieldToMatch, "Romney.*");
     
     Field occupationFieldToMatch = ObjectWithAName.class.getDeclaredField("occupation");
     matcher.addStringFieldToMatchRegularExpression(ObjectWithAName.class, occupationFieldToMatch, "Prez");

     assertTrue(matcher.matches(barry));
     assertTrue(matcher.matches(mitt));
   }

	@Test
   public void testCombine() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException
   {
      ObjectWithAName barry = new ObjectWithAName("Obama2012", "Prez", 1.5);
      ObjectWithAName mitt = new ObjectWithAName("Romney2012", "Gov", 2.5);
      
      StringFieldMatcher matcherOne = new StringFieldMatcher();
      Field fieldToMatch = ObjectWithAName.class.getDeclaredField("name");
      matcherOne.addStringFieldToMatchExactly(ObjectWithAName.class, fieldToMatch, "Obama2012");
      
      StringFieldMatcher matcherTwo = new StringFieldMatcher();
      matcherTwo.addStringFieldToMatchExactly(ObjectWithAName.class, fieldToMatch, "Romney2012");

      assertTrue(matcherOne.matches(barry));
      assertTrue(matcherTwo.matches(mitt));
      
      assertFalse(matcherOne.matches(mitt));
      assertFalse(matcherTwo.matches(barry));
      
      matcherOne.combine(matcherTwo);
      
      assertTrue(matcherOne.matches(barry));
      assertTrue(matcherOne.matches(mitt));
   }
   
   private class ObjectWithAName
   {
      private final String name;
      private final String occupation;
      private final double value;
      
      public ObjectWithAName(String name, String occupation, double value)
      {
         this.name = name;
         this.occupation = occupation;
         this.value = value;
      }
      
      @Override
      public String toString()
      {
         return "name = " + name + ", occupation = " + occupation + ", value = " + value;
      }
   }

}
