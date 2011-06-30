import sbt._

import Keys._

object AndroidKeys {
  val Android = config("android")

  /** Default Settings */
  val aaptName = SettingKey[String]("aapt-name")
  val adbName = SettingKey[String]("adb-name")
  val aidlName = SettingKey[String]("aidl-name")
  val dxName = SettingKey[String]("dx-name")
  val manifestName = SettingKey[String]("manifest-name")
  val jarName = SettingKey[String]("jar-name")
  val mapsJarName = SettingKey[String]("maps-jar-name")
  val assetsDirectoryName = SettingKey[String]("assets-dir-name")
  val resDirectoryName = SettingKey[String]("res-dir-name")
  val classesMinJarName = SettingKey[String]("classes-min-jar-name")
  val classesDexName = SettingKey[String]("classes-dex-name")
  val resourcesApkName = SettingKey[String]("resources-apk-name")
  val dxJavaOpts = SettingKey[String]("dx-java-opts")
  val manifestSchema = SettingKey[String]("manifest-schema")
  val envs = SettingKey[Seq[String]]("envs")

  // Determined on OS
  val packageApkName = SettingKey[String]("package-apk-name")
  val osDxName = SettingKey[String]("os-dx-name")

  // Override this setting
  val platformName = SettingKey[String]("platform-name", "Targetted android platform")

  // Determined Settings 
  val manifestPackage = SettingKey[String]("manifest-package")
  val minSdkVersion = SettingKey[Option[Int]]("min-sdk-version")
  val maxSdkVersion = SettingKey[Option[Int]]("max-sdk-version")
  val apiLevel = SettingKey[Int]("api-level")

  val sdkPath = SettingKey[File]("sdk-path")
  val toolsPath = SettingKey[File]("tools-path")
  val dbPath = SettingKey[File]("db-path")
  val platformPath = SettingKey[File]("platform-path")
  val platformToolsPath = SettingKey[File]("platform-tools-path")
  val aptPath = SettingKey[File]("apt-path")
  val idlPath = SettingKey[File]("idl-path")
  val dxPath = SettingKey[File]("dx-path")

  val manifestPath = SettingKey[File]("manifest-path")
  val jarPath = SettingKey[File]("jar-path")
  val nativeLibrariesPath = SettingKey[File]("natives-lib-path")
  val addonsPath = SettingKey[File]("addons-path")
  val mapsJarPath = SettingKey[File]("maps-jar-path")
  val mainAssetsPath = SettingKey[File]("main-asset-path")
  val mainResPath = SettingKey[File]("main-res-path")
  val managedJavaPath = SettingKey[File]("managed-java-path")
  val classesMinJarPath = SettingKey[File]("classes-min-jar-path")
  val classesDexPath = SettingKey[File]("classes-dex-path")
  val resourcesApkPath = SettingKey[File]("resources-apk-path")
  val packageApkPath = SettingKey[File]("package-apk-path")
  val skipProguard = SettingKey[Boolean]("skip-proguard")

  val addonsJarPath = SettingKey[Seq[File]]("addons-jar-path")

  /** General Tasks */
  val aptGenerate = TaskKey[Unit]("apt-generate")
  val aidlGenerate = TaskKey[Unit]("aidl-generate")

  /** Installable Tasks */
  val installEmulator = TaskKey[Unit]("install-emulator")
  val uninstallEmulator = TaskKey[Unit]("uninstall-emulator")

  val installDevice = TaskKey[Unit]("install-device")
  val uninstallDevice = TaskKey[Unit]("uninstall-device")

  val reinstallEmulator = TaskKey[Unit]("reinstall-emulator")
  val reinstallDevice = TaskKey[Unit]("reinstall-device")

  val aaptPackage = TaskKey[Unit]("aapt-package", "Package resources and assets.")
  val pacakgeDebug = TaskKey[Unit]("package-debug", "Package and sign with a debug key.")
  val packageRelease = TaskKey[Unit]("package-release", "Package without signing.")

  // Resource generated Seq[File]?
  val proguard = TaskKey[Unit]("proguard", "Optimize class files.")
  val dx = TaskKey[Unit]("dx", "Convert class files to dex files")

  /** Startable Tasks */
  val startDevice = TaskKey[Unit]("start-device", "Start package on device after installation")
  val startEmulator = TaskKey[Unit]("start-emulator", "Start package on emulator after installation")

  // Helpers
  def adbTask(dPath: String, emulator: Boolean, action: => String): Unit = 
    Process (<x>
      {dPath} {if (emulator) "-e" else "-d"} {action}
    </x>) !

  def startTask(emulator: Boolean) = 
    (dbPath, manifestSchema, manifestPackage, manifestPath) map { 
      (dp, schema, mPackage, amPath) =>
      adbTask(dp.absolutePath, 
              emulator, 
              "shell am start -a android.intent.action.MAIN -n "+mPackage+"/"+
              launcherActivity(schema, amPath, mPackage))
  }

  def launcherActivity(schema: String, amPath: File, mPackage: String) = {
    val launcher = for (
         activity <- (manifest(amPath) \\ "activity");
         action <- (activity \\ "action");
         val name = action.attribute(schema, "name").getOrElse(error{ 
            "action name not defined"
          }).text;
         if name == "android.intent.action.MAIN"
    ) yield {
      val act = activity.attribute(schema, "name").getOrElse(error("activity name not defined")).text
      if (act.contains(".")) act else mPackage+"."+act
    }
    launcher.headOption.getOrElse("")
  }

  def manifest(mpath: File) = xml.XML.loadFile(mpath)

  def installTask(emulator: Boolean) = (dbPath, packageApkPath) map { (dp, p) =>
    adbTask(dp.absolutePath, emulator, "install "+p.absolutePath) 
  }

  def reinstallTask(emulator: Boolean) = (dbPath, packageApkPath) map { (dp, p) =>
    adbTask(dp.absolutePath, emulator, "install -r"+p.absolutePath)
  }

  def uninstallTask(emulator: Boolean) = (dbPath, manifestPackage) map { (dp, m) =>
    adbTask(dp.absolutePath, emulator, "uninstall "+m)
  }
}