package us.ihmc.robotEnvironmentAwareness.ui.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import controller_msgs.msg.dds.PlanarRegionsListMessage;
import controller_msgs.msg.dds.StereoVisionPointCloudMessage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import us.ihmc.communication.IHMCROS2Publisher;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.communication.packets.PlanarRegionMessageConverter;
import us.ihmc.log.LogTools;
import us.ihmc.pubsub.DomainFactory.PubSubImplementation;
import us.ihmc.robotEnvironmentAwareness.communication.SLAMModuleAPI;
import us.ihmc.robotEnvironmentAwareness.tools.ExecutorServiceTools;
import us.ihmc.robotEnvironmentAwareness.tools.ExecutorServiceTools.ExceptionHandling;
import us.ihmc.robotEnvironmentAwareness.ui.io.StereoVisionPointCloudDataLoader;
import us.ihmc.robotics.PlanarRegionFileTools;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.ros2.Ros2Node;

public class SLAMDataManagerAnchorPaneController extends REABasicUIController
{
   @FXML
   private ToggleButton exportRawData;

   @FXML
   private TextField currentRawDataOutputFolderTextField;
   @FXML
   private TextField currentSLAMDataOutputFolderTextField;
   @FXML
   private TextField currentRawDataInputFolderTextField;
   @FXML
   private TextField currentPlanarRegionsInputFolderTextField;

   private final DirectoryChooser exportRawDataDirectoryChooser = new DirectoryChooser();
   private final DirectoryChooser exportSLAMDataDirectoryChooser = new DirectoryChooser();
   private final DirectoryChooser importRawDataDirectoryChooser = new DirectoryChooser();
   private final DirectoryChooser importPlanarRegionsDirectoryChooser = new DirectoryChooser();

   private final File defaultExportRawDataFile = new File("Data/SLAM/RawData/");
   private final File defaultExportSLAMDataFile = new File("Data/SLAM/SLAMData/");
   private final File defaultImportRawDataFile = new File("Data/SLAM/RawData/");
   private final File defaultImportPlanarRegionsFile = new File("Data/SLAM/PlanarRegion/");

   private Window ownerWindow;

   private static long DEFAULT_PUBLISHING_PERIOD_MS = 250;
   private final ScheduledExecutorService executor = ExecutorServiceTools.newSingleThreadScheduledExecutor(getClass(), ExceptionHandling.CANCEL_AND_REPORT);
   private final List<StereoVisionPointCloudMessage> stereoVisionPointCloudMessagesToPublish = new ArrayList<>();
   private int indexToPublish = 0;

   public SLAMDataManagerAnchorPaneController()
   {
      executor.scheduleAtFixedRate(this::publish, 0, DEFAULT_PUBLISHING_PERIOD_MS, TimeUnit.MILLISECONDS);
   }

   public void setMainWindow(Window ownerWindow)
   {
      this.ownerWindow = ownerWindow;
   }

   @Override
   public void bindControls()
   {
      currentRawDataOutputFolderTextField.setText(defaultExportRawDataFile.getAbsolutePath());
      currentSLAMDataOutputFolderTextField.setText(defaultExportSLAMDataFile.getAbsolutePath());
      currentRawDataInputFolderTextField.setText(defaultImportRawDataFile.getAbsolutePath());
      currentPlanarRegionsInputFolderTextField.setText(defaultImportPlanarRegionsFile.getAbsolutePath());
      uiMessager.bindBidirectionalGlobal(SLAMModuleAPI.UIRawDataExportRequest, exportRawData.selectedProperty());
   }

   @FXML
   private void exportSLAMData()
   {
      //TODO: implement
   }

   @FXML
   private void importRawData()
   {
      String rawDataFilePath = currentRawDataInputFolderTextField.getText();
      File rawDataFile = new File(rawDataFilePath);
      List<StereoVisionPointCloudMessage> messagesFromFile = StereoVisionPointCloudDataLoader.getMessagesFromFile(rawDataFile);
      stereoVisionPointCloudMessagesToPublish.clear();
      stereoVisionPointCloudMessagesToPublish.addAll(messagesFromFile);
      indexToPublish = 0;

      LogTools.info("Loading Data Is Done " + messagesFromFile.size() + " messages.");
      LogTools.info("Publishing Is Started.");
   }

   @FXML
   private void importPlanarRegions()
   {
      String planarRegionsFilePath = currentPlanarRegionsInputFolderTextField.getText();
      File planarRegionsFile = new File(planarRegionsFilePath);
      PlanarRegionsList planarRegionDataToImport = PlanarRegionFileTools.importPlanarRegionData(planarRegionsFile);
      if (planarRegionDataToImport == null)
      {
         LogTools.warn("Empty file path.");
         return;
      }
      PlanarRegionsListMessage planarRegionsListMessage = PlanarRegionMessageConverter.convertToPlanarRegionsListMessage(planarRegionDataToImport);
      uiMessager.submitMessageInternal(SLAMModuleAPI.ImportedPlanarRegionsState, planarRegionsListMessage);
      uiMessager.submitMessageInternal(SLAMModuleAPI.ShowImportedPlanarRegions, true);
   }

   @FXML
   private void browseRawDataOutputFolder()
   {
      exportRawDataDirectoryChooser.setInitialDirectory(defaultExportRawDataFile);
      String newPath = exportRawDataDirectoryChooser.showDialog(ownerWindow).getAbsolutePath();
      uiMessager.submitMessageInternal(SLAMModuleAPI.UIRawDataExportDirectory, newPath);
      Platform.runLater(() -> currentRawDataOutputFolderTextField.setText(newPath));
   }

   @FXML
   private void browseSLAMDataOutputFolder()
   {
      exportSLAMDataDirectoryChooser.setInitialDirectory(defaultExportSLAMDataFile);
      String newPath = exportSLAMDataDirectoryChooser.showDialog(ownerWindow).getAbsolutePath();
      uiMessager.submitMessageInternal(SLAMModuleAPI.UISLAMDataExportDirectory, newPath);
      Platform.runLater(() -> currentSLAMDataOutputFolderTextField.setText(newPath));
   }

   @FXML
   private void browseRawDataInputFolder()
   {
      importRawDataDirectoryChooser.setInitialDirectory(defaultImportRawDataFile);
      String newPath = importRawDataDirectoryChooser.showDialog(ownerWindow).getAbsolutePath();
      Platform.runLater(() -> currentRawDataInputFolderTextField.setText(newPath));
   }

   @FXML
   private void browsePlanarRegionsInputFolder()
   {
      importPlanarRegionsDirectoryChooser.setInitialDirectory(defaultImportPlanarRegionsFile);
      String newPath = importPlanarRegionsDirectoryChooser.showDialog(ownerWindow).getAbsolutePath();
      Platform.runLater(() -> currentPlanarRegionsInputFolderTextField.setText(newPath));
   }

   @FXML
   private void hideImportedPlanarRegions()
   {
      uiMessager.submitMessageInternal(SLAMModuleAPI.ImportedPlanarRegionsVizClear, true);
   }

   private final Ros2Node ros2Node = ROS2Tools.createRos2Node(PubSubImplementation.FAST_RTPS, "stereoVisionPublisherNode");
   private final IHMCROS2Publisher<StereoVisionPointCloudMessage> stereoVisionPublisher = ROS2Tools.createPublisherTypeNamed(ros2Node,
                                                                                                                             StereoVisionPointCloudMessage.class,
                                                                                                                             ROS2Tools.IHMC_ROOT);

   private void publish()
   {
      if (stereoVisionPointCloudMessagesToPublish.size() == indexToPublish)
      {
         try
         {
            return;
         }
         catch (Exception e)
         {
            e.printStackTrace();
         }
      }
      else
      {
         System.out.println("publish " + indexToPublish);
         StereoVisionPointCloudMessage stereoVisionPointCloudMessage = stereoVisionPointCloudMessagesToPublish.get(indexToPublish);
         stereoVisionPublisher.publish(stereoVisionPointCloudMessage);
         indexToPublish++;
      }
   }
}