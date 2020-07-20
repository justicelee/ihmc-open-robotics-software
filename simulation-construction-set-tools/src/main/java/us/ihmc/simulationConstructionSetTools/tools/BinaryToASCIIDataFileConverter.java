package us.ihmc.simulationConstructionSetTools.tools;

import java.io.File;
import java.io.IOException;
import java.util.List;

import us.ihmc.simulationconstructionset.DataFileReader;
import us.ihmc.simulationconstructionset.DataFileWriter;
import us.ihmc.yoVariables.dataBuffer.YoBuffer;
import us.ihmc.yoVariables.registry.YoRegistry;
import us.ihmc.yoVariables.registry.YoVariableList;
import us.ihmc.yoVariables.variable.YoVariable;

public class BinaryToASCIIDataFileConverter
{
   private final String binaryFilename;
   private final String asciiFilename;

   public BinaryToASCIIDataFileConverter(String binaryFilename, String asciiFilename)
   {
      this.binaryFilename = binaryFilename;
      this.asciiFilename = asciiFilename;
   }

   public void convertData() throws IOException
   {
      File binaryFile = new File(binaryFilename);
      File asciiFile = new File(asciiFilename);

      int bufferSize = 16000;

      DataFileReader reader = new DataFileReader(binaryFile);

      YoVariableList newVars = new YoVariableList("Converter");

      YoBuffer dataBuffer = new YoBuffer(bufferSize);

      YoRegistry rootRegistry = new YoRegistry("root");

      reader.readData(newVars, rootRegistry, dataBuffer);

      DataFileWriter dataFileWriter = new DataFileWriter(asciiFile);

      boolean binary = false;
      boolean compress = false;

      List<YoVariable> varsToWrite = newVars.getVariables();

      dataFileWriter.writeData("ConvertedData", 0.001, dataBuffer, varsToWrite, binary, compress);
   }

   public static void main(String[] args)
   {
      if (args.length != 2)
         throw new RuntimeException("Need two arguments");
      BinaryToASCIIDataFileConverter convert = new BinaryToASCIIDataFileConverter(args[0], args[1]);
      try
      {
         convert.convertData();
      }
      catch (IOException e)
      {
         e.printStackTrace();
      }

   }
}
