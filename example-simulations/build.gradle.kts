plugins {
   id("us.ihmc.ihmc-build") version "0.20.1"
   id("us.ihmc.ihmc-ci") version "5.3"
   id("us.ihmc.ihmc-cd") version "1.14"
   id("us.ihmc.log-tools-plugin") version "0.5.0"
}

ihmc {
   loadProductProperties("../product.properties")
   
   configureDependencyResolution()
   configurePublications()
}

mainDependencies {
   api("org.ejml:ejml-ddense:0.39")
   api("org.ejml:ejml-core:0.39")
   api("org.apache.commons:commons-lang3:3.8.1")

   api("us.ihmc:euclid:0.15.0")
   api("us.ihmc:euclid-geometry:0.15.0")
   api("us.ihmc:euclid-frame:0.15.0")
   api("us.ihmc:euclid-shape:0.15.0")
   api("us.ihmc:ihmc-yovariables:0.8.0")
   api("us.ihmc.thirdparty.jinput:jinput:200128")
   api("us.ihmc:ihmc-model-file-loader:source")
   api("us.ihmc:ihmc-java-toolkit:source")
   api("us.ihmc:ihmc-quadruped-robotics:source")
   api("us.ihmc:ihmc-avatar-interfaces:source")
   api("us.ihmc:simulation-construction-set:0.19.0")
   api("us.ihmc:simulation-construction-set-tools:source")
   api("us.ihmc:ihmc-common-walking-control-modules:source")
   api("us.ihmc:ihmc-jmonkey-engine-toolkit:0.18.0")
   api("us.ihmc:ihmc-robotics-toolkit:source")
   api("us.ihmc:ihmc-robot-description:0.19.0")
   api("us.ihmc:ihmc-graphics-description:0.18.0")
   api("us.ihmc:ihmc-robot-data-logger:0.19.0")
   api("us.ihmc:ihmc-humanoid-robotics:source")
   api("us.ihmc:ihmc-sensor-processing:source")
   api("us.ihmc:ihmc-robot-models:source")
   api("us.ihmc:ihmc-simulation-toolkit:source")
}

testDependencies {
   api("us.ihmc:ihmc-commons-testing:0.30.0")
   api("us.ihmc:ihmc-robotics-toolkit-test:source")
   api("us.ihmc:ihmc-quadruped-robotics-test:source")
   api("us.ihmc:ihmc-quadruped-planning-test:source")

}