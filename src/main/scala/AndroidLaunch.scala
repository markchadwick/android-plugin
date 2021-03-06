import sbt._

import Keys._
import AndroidKeys._
import AndroidHelpers._

import complete.DefaultParsers._
import scala.collection.mutable.{Map => MMap}

object AndroidLaunch {

  private def startTask(emulator: Boolean) = 
    (dbPath, manifestSchema, manifestPackage, manifestPath) map { 
      (dp, schema, mPackage, amPath) =>
      adbTask(dp.absolutePath, 
              emulator, 
              "shell am start -a android.intent.action.MAIN -n "+mPackage+"/"+
              launcherActivity(schema, amPath, mPackage))
  }

  private def launcherActivity(schema: String, amPath: File, mPackage: String) = {
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

  val installedAvds = (s: State) => {
    val avds = (Path.userHome / ".android" / "avd" * "*.ini").get
    Space ~> avds.map(f => token(f.base))
                 .reduceLeftOption(_ | _).getOrElse(token("none"))
  }

  private def emulatorStartTask = (parsedTask: TaskKey[String]) =>
    (parsedTask, toolsPath) map { (avd, toolsPath) =>
      "%s/emulator -avd %s".format(toolsPath, avd).run
      ()
    }

  private def listDevicesTask: Project.Initialize[Task[Unit]] = (dbPath) map {
    _ +" devices".format(dbPath) !
  }

  private def emulatorStopTask = (dbPath, streams) map { (dbPath, s) =>
    s.log.info("Stopping emulators")
    val serial = "%s -e get-serialno".format(dbPath).!!
    "%s -s %s emu kill".format(dbPath, serial).!
    ()
  }

  lazy val settings: Seq[Setting[_]] = Seq (
    startDevice <<= startTask(false),
    startEmulator <<= startTask(true),

    startDevice <<= startDevice dependsOn reinstallDevice,
    startEmulator <<= startEmulator dependsOn reinstallEmulator,

    listDevices <<= listDevicesTask,
    emulatorStart <<= InputTask(installedAvds)(emulatorStartTask),
    emulatorStop <<= emulatorStopTask 
  )
}
